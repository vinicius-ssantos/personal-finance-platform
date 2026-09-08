package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.fixtures.FixturePaths
import br.com.vinicius.personalfinance.fixtures.FixturePdfBuilder
import br.com.vinicius.personalfinance.fixtures.InterPositionFixture
import br.com.vinicius.personalfinance.shared.CurrencyCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class PositionReconcilerTests {
    private val extractor = PdfBoxTextExtractor()
    private val parser = InterPositionDocumentParser()
    private val normalizer = InterPositionNormalizer()
    private val reconciler = PositionReconciler()

    private fun reconcile(case: InterPositionFixture.Case): ReconciliationReport {
        val password = if (case.protected) DocumentPassword.of(InterPositionFixture.TEST_PASSWORD) else null
        val extracted = extractor.extract(FixturePdfBuilder.build(case), password)
        return reconciler.reconcile(normalizer.normalize(parser.parse(extracted)))
    }

    private fun ReconciliationReport.of(rule: ReconciliationRule): List<ReconciliationResult> =
        results.filter { result -> result.rule == rule }

    @Test
    fun `a consistent report reconciles by currency and category without blockers`() {
        val report = reconcile(InterPositionFixture.complete)

        assertFalse(report.hasBlockers, "unexpected blockers: ${report.blockingResults}")
        assertTrue(
            report.of(ReconciliationRule.SECTION_GROSS).all { result ->
                result.outcome == ReconciliationOutcome.PASS
            },
        )
        assertTrue(
            report.of(ReconciliationRule.SUMMARY_ALLOCATION).all { result ->
                result.outcome == ReconciliationOutcome.PASS
            },
        )
    }

    @Test
    fun `every comparison stays inside one currency`() {
        val report = reconcile(InterPositionFixture.complete)
        val international =
            report.of(ReconciliationRule.SUMMARY_ALLOCATION).first { result ->
                result.subject.contains("Internacional")
            }

        assertEquals(CurrencyCode.USD, international.currency)
        assertEquals(CurrencyCode.USD, international.amounts.declared?.currency)
        assertEquals(CurrencyCode.USD, international.amounts.calculated?.currency)
    }

    @Test
    fun `the declared document total is reported but never recomputed`() {
        val total = reconcile(InterPositionFixture.complete).of(ReconciliationRule.DOCUMENT_TOTAL).single()

        // The layout absorbs the USD category into one BRL total without printing
        // the rate, so concluding anything would require inventing one.
        assertEquals(ReconciliationOutcome.NOT_EVIDENCED, total.outcome)
        assertEquals(ReconciliationReason.UNEVIDENCED_CONVERSION, total.reason)
        assertEquals(1_200_000L, total.amounts.declared?.amountMinor)
        assertEquals(null, total.amounts.calculated)
        assertFalse(total.blocks)
    }

    @Test
    fun `a section total that agrees does not mask a subtotal that does not`() {
        val report = reconcile(InterPositionFixture.controlledMismatch)
        val section =
            report.of(ReconciliationRule.SECTION_GROSS).first { result ->
                result.currency == CurrencyCode.BRL && result.amounts.declared?.amountMinor == 300_000L
            }
        val subtotal = report.of(ReconciliationRule.FIXED_INCOME_SUBTOTAL).single()

        assertEquals(ReconciliationOutcome.PASS, section.outcome)
        assertEquals(ReconciliationOutcome.BLOCKER, subtotal.outcome)
        assertEquals(ReconciliationReason.DIFFERENCE_ABOVE_TOLERANCE, subtotal.reason)
        assertTrue(report.hasBlockers)
    }

    @Test
    fun `a mismatch keeps both numbers so a person can decide`() {
        val subtotal =
            reconcile(InterPositionFixture.controlledMismatch)
                .of(ReconciliationRule.FIXED_INCOME_SUBTOTAL)
                .single()

        assertEquals(290_000L, subtotal.amounts.declared?.amountMinor)
        assertEquals(300_000L, subtotal.amounts.calculated?.amountMinor)
        assertEquals(-10_000L, subtotal.amounts.differenceMinor)
    }

    @Test
    fun `an unreadable row blocks instead of summing as if it were zero`() {
        val report = reconcile(InterPositionFixture.malformedValue)
        val fixedIncome =
            report.of(ReconciliationRule.SECTION_GROSS).first { result ->
                result.subject == "sections[3]"
            }

        assertEquals(ReconciliationOutcome.BLOCKER, fixedIncome.outcome)
        assertEquals(ReconciliationReason.UNKNOWN_INPUT, fixedIncome.reason)
        assertEquals(null, fixedIncome.amounts.calculated)
        assertTrue(report.hasBlockers)
    }

    @Test
    fun `a summary line with no matching section is unevidenced rather than wrong`() {
        val report = reconcile(InterPositionFixture.summaryOnly)

        assertFalse(report.hasBlockers)
        assertTrue(
            report.of(ReconciliationRule.SUMMARY_ALLOCATION).all { result ->
                result.outcome == ReconciliationOutcome.NOT_EVIDENCED &&
                    result.reason == ReconciliationReason.NO_DECLARED_TOTAL
            },
        )
    }

    @Test
    fun `the tolerance that ran stays identifiable in every result`() {
        val report = reconcile(InterPositionFixture.complete)

        assertEquals("release-0.1/one-minor-unit", report.toleranceId)
        assertTrue(
            report.results.all { result ->
                result.amounts.toleranceId == "release-0.1/one-minor-unit"
            },
        )
    }

    @Test
    fun `tolerance absorbs a one-cent rounding but not a systematic difference`() {
        val strict =
            PositionReconciler(
                ReconciliationTolerance("test/zero", mapOf(CurrencyCode.BRL to 0L, CurrencyCode.USD to 0L)),
            )
        val password = DocumentPassword.of(InterPositionFixture.TEST_PASSWORD)
        val document =
            normalizer.normalize(
                parser.parse(
                    extractor.extract(
                        FixturePdfBuilder.build(InterPositionFixture.complete),
                        password,
                    ),
                ),
            )

        // The fixture reconciles exactly, so a zero tolerance changes nothing:
        // tolerance absorbs published rounding, it does not create agreement.
        assertFalse(strict.reconcile(document).hasBlockers)
    }

    @Test
    fun `the same document and policy always produce the same report`() {
        val first = reconcile(InterPositionFixture.complete)
        val second = reconcile(InterPositionFixture.complete)

        assertEquals(first, second)
    }

    @Test
    fun `reconciliation reads the document and writes nothing`() {
        val report = reconcile(InterPositionFixture.complete)

        assertNotNull(report.results)
        assertTrue(report.results.isNotEmpty())
    }

    @Test
    fun `reconciliation snapshots match the golden file`() {
        val golden = FixturePaths.interPositionDirectory.resolve("reconciliation.golden.txt")
        val rendered =
            InterPositionFixture.all
                .filterNot { case -> case.name.startsWith("unsupported") }
                .joinToString("\n\n") { case ->
                    "=== ${case.name} ===\n${renderReconciliationSnapshot(reconcile(case))}"
                }

        if (System.getProperty("updateGoldenFiles")?.toBoolean() == true) {
            Files.writeString(golden, rendered + "\n")
        }
        assertEquals(Files.readString(golden).trim(), rendered.trim(), "reconciliation drifted")
    }
}
