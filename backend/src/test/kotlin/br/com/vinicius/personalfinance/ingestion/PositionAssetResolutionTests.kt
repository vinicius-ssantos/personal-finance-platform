package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.fixtures.FixturePaths
import br.com.vinicius.personalfinance.fixtures.FixturePdfBuilder
import br.com.vinicius.personalfinance.fixtures.InterPositionFixture
import br.com.vinicius.personalfinance.portfolio.AssetFingerprint
import br.com.vinicius.personalfinance.portfolio.AssetIdentityClaim
import br.com.vinicius.personalfinance.portfolio.AssetResolution
import br.com.vinicius.personalfinance.portfolio.AssetType
import br.com.vinicius.personalfinance.portfolio.DeterministicAssetResolver
import br.com.vinicius.personalfinance.portfolio.ResolutionConfidence
import br.com.vinicius.personalfinance.portfolio.StrongIdentifier
import br.com.vinicius.personalfinance.shared.CurrencyCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class PositionAssetResolutionTests {
    private val extractor = PdfBoxTextExtractor()
    private val parser = InterPositionDocumentParser()
    private val normalizer = InterPositionNormalizer()
    private val service = PositionAssetResolutionService(DeterministicAssetResolver())

    private fun resolve(case: InterPositionFixture.Case): ResolvedPositionDocument {
        val password = if (case.protected) DocumentPassword.of(InterPositionFixture.TEST_PASSWORD) else null
        val extracted = extractor.extract(FixturePdfBuilder.build(case), password)
        return service.resolve(normalizer.normalize(parser.parse(extracted)))
    }

    private fun ResolvedPositionDocument.byName(name: String): ResolvedPositionCandidate =
        candidates.first { entry -> entry.candidate.identity.descriptionRaw == name }

    @Test
    fun `an institutional code resolves a fixed-income row on its own`() {
        val resolution = resolve(InterPositionFixture.complete).byName("LCI FICTÍCIA 3 ANOS").resolution

        assertTrue(resolution is AssetResolution.Resolved)
        assertEquals(ResolutionConfidence.STRONG, resolution.confidence)
        assertEquals(
            StrongIdentifier.InstitutionalCode("BANCO_INTER", "FICTICIO001"),
            (resolution as AssetResolution.Resolved).identifier,
        )
        assertEquals(AssetType.FIXED_INCOME, resolution.type)
    }

    @Test
    fun `a domestic ticker is strong because the layout scopes its exchange`() {
        val resolution = resolve(InterPositionFixture.complete).byName("AAAA3").resolution

        assertEquals(
            StrongIdentifier.Ticker("AAAA3", "B3"),
            (resolution as AssetResolution.Resolved).identifier,
        )
        assertEquals(AssetType.EQUITY, resolution.type)
    }

    @Test
    fun `an ambiguous domestic suffix is identified but not typed`() {
        val resolution = resolve(InterPositionFixture.complete).byName("BBBB11").resolution

        // Units, ETFs and real-estate funds all end in 11, so the ticker settles
        // which asset this is while saying nothing about what it is.
        assertTrue(resolution is AssetResolution.ReviewRequired)
        assertEquals(
            AssetResolution.ReviewReason.UNDETERMINED_TYPE,
            (resolution as AssetResolution.ReviewRequired).reason,
        )
        assertEquals(ResolutionConfidence.MEDIUM, resolution.confidence)
        assertEquals(AssetFingerprint.of(StrongIdentifier.Ticker("BBBB11", "B3")), resolution.fingerprint)
    }

    @Test
    fun `an unscoped international ticker is not a strong identifier`() {
        val resolution = resolve(InterPositionFixture.complete).byName("ZZZZ").resolution

        assertTrue(resolution is AssetResolution.ReviewRequired)
        assertEquals(
            AssetResolution.ReviewReason.NO_STRONG_IDENTIFIER,
            (resolution as AssetResolution.ReviewRequired).reason,
        )
        assertEquals(ResolutionConfidence.LOW, resolution.confidence)
    }

    @Test
    fun `a row identified only by its name never resolves automatically`() {
        val resolved = resolve(InterPositionFixture.complete)

        listOf("Tesouro Fictício 2029", "FUNDO FICTÍCIO MULTIMERCADO").forEach { name ->
            val resolution = resolved.byName(name).resolution
            assertTrue(resolution is AssetResolution.ReviewRequired, "$name resolved without evidence")
            assertEquals(
                AssetResolution.ReviewReason.NO_STRONG_IDENTIFIER,
                (resolution as AssetResolution.ReviewRequired).reason,
            )
        }
    }

    @Test
    fun `description variants of the same asset share a fingerprint`() {
        val claim = { name: String ->
            AssetIdentityClaim(name, emptyList(), AssetType.FUND, CurrencyCode.BRL)
        }
        val resolver = DeterministicAssetResolver()

        assertEquals(
            resolver.resolve(claim("FUNDO  FICTÍCIO   MULTIMERCADO")).fingerprint,
            resolver.resolve(claim("fundo fictício multimercado")).fingerprint,
        )
    }

    @Test
    fun `name normalization never rewrites digits`() {
        val resolver = DeterministicAssetResolver()
        val fingerprintOf = { name: String ->
            resolver
                .resolve(AssetIdentityClaim(name, emptyList(), AssetType.FIXED_INCOME, CurrencyCode.BRL))
                .fingerprint
        }

        assertNotEquals(fingerprintOf("Tesouro Fictício 2029"), fingerprintOf("Tesouro Fictício 2030"))
    }

    @Test
    fun `conflicting strong identifiers refuse instead of tie-breaking`() {
        val conflicting =
            AssetIdentityClaim(
                nameRaw = "AMBÍGUO",
                strongIdentifiers =
                    listOf(
                        StrongIdentifier.Ticker("AAAA3", "B3"),
                        StrongIdentifier.InstitutionalCode("BANCO_INTER", "FICTICIO001"),
                    ),
                type = AssetType.EQUITY,
                currency = CurrencyCode.BRL,
            )

        val resolution = DeterministicAssetResolver().resolve(conflicting)

        assertEquals(
            AssetResolution.ReviewReason.CONFLICTING_IDENTIFIERS,
            (resolution as AssetResolution.ReviewRequired).reason,
        )
        assertEquals(ResolutionConfidence.UNKNOWN, resolution.confidence)
    }

    @Test
    fun `every unresolved candidate reaches the review list with its location`() {
        val resolved = resolve(InterPositionFixture.complete)
        val reviewed = resolved.candidates.count { entry -> entry.resolution is AssetResolution.ReviewRequired }

        assertEquals(reviewed, resolved.reviewRequired.size)
        assertTrue(
            resolved.reviewRequired.all { item ->
                item.fieldPath.startsWith("sections[") && item.pageNumber > 0
            },
        )
    }

    @Test
    fun `resolution creates no asset and touches no portfolio state`() {
        val resolved = resolve(InterPositionFixture.complete)

        // The only outputs are decisions about the document being read. Nothing
        // here carries an AssetId, an account or a snapshot (`INV-006`).
        assertTrue(resolved.candidates.isNotEmpty())
        assertEquals(resolved.document.sections.sumOf { it.candidates.size }, resolved.candidates.size)
    }

    @Test
    fun `asset resolution snapshots match the golden file`() {
        val golden = FixturePaths.interPositionDirectory.resolve("asset-resolution.golden.txt")
        val rendered =
            InterPositionFixture.all
                .filterNot { case -> case.name.startsWith("unsupported") }
                .joinToString("\n\n") { case ->
                    "=== ${case.name} ===\n${renderResolutionSnapshot(resolve(case))}"
                }

        if (System.getProperty("updateGoldenFiles")?.toBoolean() == true) {
            Files.writeString(golden, rendered + "\n")
        }
        assertEquals(Files.readString(golden).trim(), rendered.trim(), "asset resolution drifted")
    }
}
