package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.fixtures.FixturePdfBuilder
import br.com.vinicius.personalfinance.fixtures.InterPositionFixture
import br.com.vinicius.personalfinance.portfolio.AssetResolution
import br.com.vinicius.personalfinance.portfolio.DeterministicAssetResolver
import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import br.com.vinicius.personalfinance.shared.ValueQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ImportPreviewBuilderTests {
    private val extractor = PdfBoxTextExtractor()
    private val parser = InterPositionDocumentParser()
    private val normalizer = InterPositionNormalizer()
    private val resolutionService = PositionAssetResolutionService(DeterministicAssetResolver())
    private val reconciler = PositionReconciler()
    private val builder = ImportPreviewBuilder(DomainClock.fixed(Instant.parse("2026-02-03T10:00:00Z")))

    private fun preview(
        case: InterPositionFixture.Case,
        version: Int = 1,
    ): ImportPreview {
        val password = if (case.protected) DocumentPassword.of(InterPositionFixture.TEST_PASSWORD) else null
        val document = normalizer.normalize(parser.parse(extractor.extract(FixturePdfBuilder.build(case), password)))
        return builder.build(
            importBatchId = ImportBatchId.random(),
            previewVersion = version,
            resolved = resolutionService.resolve(document),
            reconciliation = reconciler.reconcile(document),
        )
    }

    @Test
    fun `the preview reports the source identity a person needs to judge it`() {
        val preview = preview(InterPositionFixture.complete)

        assertEquals("BANCO_INTER", preview.source.descriptor.institution)
        assertEquals("POSITION_CONSOLIDATED", preview.source.descriptor.documentFamily)
        assertEquals("2024_07", preview.source.descriptor.layoutVersion)
        assertEquals("banco-inter-position", preview.source.parserId)
        assertEquals("2024_07.2", preview.source.parserVersion)
        assertEquals(LocalDate.of(2026, 1, 31), preview.temporality.positionDate)
        assertEquals("release-0.1/one-minor-unit", preview.toleranceId)
    }

    @Test
    fun `every position that was read appears, including the ones that cannot commit`() {
        val preview = preview(InterPositionFixture.complete)

        assertEquals(9, preview.positions.size)
        assertTrue(preview.positions.any { position -> position.identity.descriptionRaw == "BBBB11" })
        assertTrue(
            preview.positions.all { position -> position.evidence.pageNumber > 0 },
            "every position keeps the page it came from",
        )
    }

    @Test
    fun `quantity, price, value, quality and evidence travel with each position`() {
        val fund =
            preview(InterPositionFixture.complete)
                .positions
                .first { position -> position.category == PositionCategory.FUNDS }

        assertNotNull(fund.quantity)
        assertNotNull(fund.unitPrice)
        assertEquals(CurrencyCode.BRL, fund.currency)
        assertTrue(fund.gross is ValueQuality.Exact)
        assertTrue(fund.evidence.tokenRaw.isNotBlank())
    }

    @Test
    fun `the declared total is reported as the source published it`() {
        val preview = preview(InterPositionFixture.complete)

        assertEquals(1_200_000L, preview.declaredPositionTotal?.amountMinor)
        assertEquals(CurrencyCode.BRL, preview.declaredPositionTotal?.currency)
    }

    @Test
    fun `an unreadable amount stays unknown in the preview and blocks the commit`() {
        val preview = preview(InterPositionFixture.malformedValue)
        val unreadable =
            preview.positions.first { position -> position.gross is ValueQuality.Unknown }

        assertEquals("R$ 1.25X,00", unreadable.evidence.tokenRaw)
        assertFalse(preview.canCommit)
        assertTrue(
            preview.blockers.any { finding ->
                finding.origin == PreviewFindingOrigin.RECONCILIATION &&
                    finding.code == "RECON_SECTION_GROSS"
            },
        )
    }

    @Test
    fun `a reconciliation mismatch blocks and keeps both numbers in the finding`() {
        val preview = preview(InterPositionFixture.controlledMismatch)
        val blocker =
            preview.blockers.first { finding -> finding.code == "RECON_FIXED_INCOME_SUBTOTAL" }

        assertFalse(preview.canCommit)
        assertEquals(290_000L, blocker.amounts?.declared?.amountMinor)
        assertEquals(300_000L, blocker.amounts?.calculated?.amountMinor)
    }

    @Test
    fun `an asset that cannot be matched warns, while genuine ambiguity blocks`() {
        val preview = preview(InterPositionFixture.complete)
        val reviews = preview.findings.filter { finding -> finding.origin == PreviewFindingOrigin.ASSET_REVIEW }

        // Nothing to merge with is not ambiguity: those assets can be created.
        assertTrue(
            reviews
                .filter { finding -> finding.code == AssetResolution.ReviewReason.NO_STRONG_IDENTIFIER.name }
                .all { finding -> finding.severity == PreviewSeverity.WARNING },
        )
        // No category to file the asset under is a decision only a person makes.
        assertTrue(
            reviews
                .filter { finding -> finding.code == AssetResolution.ReviewReason.UNDETERMINED_TYPE.name }
                .all { finding -> finding.severity == PreviewSeverity.BLOCKER },
        )
    }

    @Test
    fun `canCommit is derived from the blockers and cannot disagree with them`() {
        val blocked = preview(InterPositionFixture.controlledMismatch)
        val summaryOnly = preview(InterPositionFixture.summaryOnly)

        assertEquals(blocked.blockers.isEmpty(), blocked.canCommit)
        assertEquals(summaryOnly.blockers.isEmpty(), summaryOnly.canCommit)
        assertTrue(summaryOnly.canCommit, "a report with no positions and no blockers is committable")
    }

    @Test
    fun `the unevidenced document total is visible without blocking`() {
        val preview = preview(InterPositionFixture.complete)
        val total = preview.findings.first { finding -> finding.code == "RECON_DOCUMENT_TOTAL" }

        assertEquals(PreviewSeverity.NOT_EVIDENCED, total.severity)
        assertFalse(total.blocks)
    }

    @Test
    fun `building a preview twice produces the same content`() {
        val first = preview(InterPositionFixture.complete)
        val second = preview(InterPositionFixture.complete)

        assertEquals(first.positions, second.positions)
        assertEquals(first.findings, second.findings)
        assertEquals(first.canCommit, second.canCommit)
    }
}
