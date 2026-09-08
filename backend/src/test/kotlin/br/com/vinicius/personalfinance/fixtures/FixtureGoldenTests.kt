package br.com.vinicius.personalfinance.fixtures

import br.com.vinicius.personalfinance.ingestion.DocumentPassword
import br.com.vinicius.personalfinance.ingestion.PdfBoxTextExtractor
import br.com.vinicius.personalfinance.ingestion.SemanticFingerprint
import br.com.vinicius.personalfinance.ingestion.SourceDocumentValidator
import br.com.vinicius.personalfinance.ingestion.SourceFingerprint
import br.com.vinicius.personalfinance.ingestion.UploadPolicy
import br.com.vinicius.personalfinance.shared.CorrelationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/** Golden files for the fully synthetic Banco Inter fixture pack. */
class FixtureGoldenTests {
    private val extractor = PdfBoxTextExtractor()

    private val updateGoldenFiles: Boolean =
        System.getProperty("updateGoldenFiles")?.toBoolean() ?: false

    private fun goldenPathFor(case: InterPositionFixture.Case): Path =
        FixturePaths.interPositionDirectory.resolve("${case.name}.golden.txt")

    private fun canonicalTextOf(case: InterPositionFixture.Case): String {
        val pdf = FixturePdfBuilder.build(case)
        val password =
            if (case.protected) DocumentPassword.of(InterPositionFixture.TEST_PASSWORD) else null
        val document = extractor.extract(pdf, password)
        return SemanticFingerprint.canonicalize(document.fullText())
    }

    @TestFactory
    fun `each case matches its golden file`(): List<DynamicTest> =
        InterPositionFixture.all.map { case ->
            DynamicTest.dynamicTest(case.name) {
                val golden = goldenPathFor(case)
                val actual = canonicalTextOf(case)
                if (updateGoldenFiles) {
                    Files.createDirectories(golden.parent)
                    Files.writeString(golden, actual)
                }
                assertTrue(Files.exists(golden), "missing golden file: $golden")
                assertEquals(
                    Files.readString(golden).trim(),
                    actual.trim(),
                    "canonical text drifted for ${case.name}",
                )
            }
        }

    @Test
    fun `every case is covered by a golden file and nothing is orphaned`() {
        val expected = InterPositionFixture.all.map { "${it.name}.golden.txt" }.toSet()
        val present =
            Files
                .list(FixturePaths.interPositionDirectory)
                .use { entries -> entries.map { it.fileName.toString() }.toList() }
                .filter { name -> name.endsWith(".golden.txt") && name !in DOCUMENT_LEVEL_GOLDENS }
                .toSet()

        assertEquals(expected, present, "golden files and fixture cases disagree")
    }

    @Test
    fun `complete fixture preserves the observed page-provenance shape`() {
        val pdf = FixturePdfBuilder.build(InterPositionFixture.complete)
        val document =
            extractor.extract(pdf, DocumentPassword.of(InterPositionFixture.TEST_PASSWORD))
        val markers =
            listOf(
                "POSIÇÃO CONSOLIDADA",
                "TITULAR FICTÍCIO",
                "Seu patrimônio atual",
                "Distribuição da carteira",
                "Renda Fixa",
                "Renda Variável Internacional",
                "Fundos de Investimentos",
                "Material informativo sintético",
                "Extrato de posição em",
            )

        assertEquals(InterPositionFixture.complete.pages.size, document.pages.size)
        markers.zip(document.pages).forEach { (marker, page) ->
            assertTrue(page.text.contains(marker), "page ${page.pageNumber} should contain $marker")
        }
    }

    @Test
    fun `regenerating a fixture yields the same text though not the same protected bytes`() {
        val first = FixturePdfBuilder.build(InterPositionFixture.complete)
        val second = FixturePdfBuilder.build(InterPositionFixture.complete)

        assertFalse(
            first.contentEquals(second),
            "identical protected bytes would mean the encryption salt is fixed",
        )
        assertEquals(
            canonicalTextOf(InterPositionFixture.complete),
            canonicalTextOf(InterPositionFixture.complete),
            "extracted text must be reproducible",
        )
    }

    @Test
    fun `the protected fixture really requires its password`() {
        val pdf = FixturePdfBuilder.build(InterPositionFixture.complete)

        assertThrowsOnWrongPassword(pdf)
    }

    private fun assertThrowsOnWrongPassword(pdf: ByteArray) {
        val failed =
            runCatching { extractor.extract(pdf, DocumentPassword.of("not-the-password")) }
                .isFailure

        assertTrue(failed, "a protected fixture must not open with the wrong password")
    }

    @Test
    fun `fixtures pass the upload gate they will be fed through`() {
        val validator = SourceDocumentValidator(UploadPolicy())

        InterPositionFixture.all.forEach { case ->
            validator.validate(FixturePdfBuilder.build(case), CorrelationId.random())
        }
    }

    @Test
    fun `the mismatch case really differs from the complete one`() {
        assertNotEquals(
            canonicalTextOf(InterPositionFixture.complete),
            canonicalTextOf(InterPositionFixture.controlledMismatch),
            "the mismatch fixture must not be identical to the reconciling one",
        )
    }

    @Test
    fun `the observed shape preserves BRL and USD without inventing an exchange rate`() {
        val text = canonicalTextOf(InterPositionFixture.complete)

        assertTrue(text.contains("posição total r$"), "declared BRL position total missing")
        assertTrue(
            text.contains("renda variável internacional us$"),
            "international USD category missing",
        )
        assertTrue(
            !text.contains("câmbio") && !text.contains("convertido") && !text.contains("exchange rate"),
            "fixtures must not invent FX evidence that the observed layout does not expose",
        )
    }

    @Test
    fun `same institution alternate families remain explicit negative cases`() {
        val movements = canonicalTextOf(InterPositionFixture.unsupportedMovements)
        val notes = canonicalTextOf(InterPositionFixture.unsupportedFixedIncomeNotes)

        assertTrue(movements.contains("extrato de movimentações"))
        assertTrue(notes.contains("notas de renda fixa"))
        assertTrue(!movements.contains("posição consolidada"))
        assertTrue(!notes.contains("posição consolidada"))
    }

    @Test
    fun `a generated fixture is never written into the repository`(
        @TempDir scratch: Path,
    ) {
        val target = scratch.resolve("generated.pdf")
        Files.write(target, FixturePdfBuilder.build(InterPositionFixture.complete))

        val pdfsInFixtures =
            Files
                .list(FixturePaths.interPositionDirectory)
                .use { entries -> entries.map { it.fileName.toString() }.toList() }
                .filter { name -> name.endsWith(".pdf") }

        assertTrue(Files.exists(target))
        assertTrue(pdfsInFixtures.isEmpty(), "PDFs must not be committed: $pdfsInFixtures")
    }

    @Test
    fun `raw and semantic digests of a fixture disagree as they must`() {
        val pdf = FixturePdfBuilder.build(InterPositionFixture.complete)
        val document =
            extractor.extract(pdf, DocumentPassword.of(InterPositionFixture.TEST_PASSWORD))

        assertNotEquals(SourceFingerprint.of(pdf), SemanticFingerprint.of(document))
    }

    private companion object {
        /**
         * Goldens that cover every case in one artefact instead of one file per
         * case, so the per-case pairing check must not treat them as orphans.
         */
        val DOCUMENT_LEVEL_GOLDENS =
            setOf(
                "raw-parser.golden.txt",
                "canonical.golden.txt",
                "asset-resolution.golden.txt",
            )
    }
}
