package br.com.vinicius.personalfinance.ingestion

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

/**
 * Fixtures are built here rather than committed: ADR 0029 keeps real reports out
 * of the repository, and a generated PDF is unambiguously synthetic.
 */
private fun syntheticPdf(
    pages: List<String>,
    password: String? = null,
): ByteArray {
    PDDocument().use { document ->
        pages.forEach { line ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                stream.newLineAtOffset(50f, 700f)
                stream.showText(line)
                stream.endText()
            }
        }
        password?.let {
            val permissions = AccessPermission()
            document.protect(StandardProtectionPolicy(it, it, permissions))
        }
        val out = ByteArrayOutputStream()
        document.save(out)
        return out.toByteArray()
    }
}

class ExtractedDocumentTests {
    @Test
    fun `pages are numbered from one`() {
        assertEquals(1, ExtractedPage.FIRST_PAGE_NUMBER)
        assertThrows(IllegalArgumentException::class.java) { ExtractedPage(0, "text") }
    }

    @Test
    fun `pages must be contiguous and ordered`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExtractedDocument(
                pages = listOf(ExtractedPage(1, "a"), ExtractedPage(3, "c")),
                sanitizedMetadata = emptyMap(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExtractedDocument(
                pages = listOf(ExtractedPage(2, "b"), ExtractedPage(1, "a")),
                sanitizedMetadata = emptyMap(),
            )
        }
    }

    @Test
    fun `an empty document is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExtractedDocument(pages = emptyList(), sanitizedMetadata = emptyMap())
        }
    }

    @Test
    fun `whitespace does not count towards the text threshold`() {
        val document =
            ExtractedDocument(
                pages = listOf(ExtractedPage(1, "   \n\t  ")),
                sanitizedMetadata = emptyMap(),
            )

        assertEquals(0, document.meaningfulCharacterCount)
        assertTrue(document.isBelowTextThreshold(minimumCharacters = 1))
    }

    @Test
    fun `full text preserves page order`() {
        val document =
            ExtractedDocument(
                pages = listOf(ExtractedPage(1, "first"), ExtractedPage(2, "second")),
                sanitizedMetadata = emptyMap(),
            )

        assertEquals("first\nsecond", document.fullText())
    }
}

class SemanticFingerprintTests {
    @Test
    fun `two exports differing only in whitespace and case agree`() {
        val first = SemanticFingerprint.ofText("Saldo Total   BRL 1.234,56\n\n")
        val second = SemanticFingerprint.ofText("saldo total BRL 1.234,56")

        assertEquals(first, second)
    }

    @Test
    fun `a different amount produces a different fingerprint`() {
        val first = SemanticFingerprint.ofText("Saldo Total BRL 1.234,56")
        val second = SemanticFingerprint.ofText("Saldo Total BRL 1.234,57")

        assertNotEquals(first, second)
    }

    @Test
    fun `blank lines are dropped so a reexport with extra spacing still matches`() {
        assertEquals("a\nb", SemanticFingerprint.canonicalize("A\n\n\n  B  \n"))
    }

    @Test
    fun `the semantic fingerprint differs from the raw digest of the same text`() {
        val text = "Saldo Total BRL 1.234,56"

        assertNotEquals(SourceFingerprint.of(text.toByteArray()), SemanticFingerprint.ofText(text))
    }
}

class PdfBoxTextExtractorTests {
    private val extractor = PdfBoxTextExtractor()

    @Test
    fun `text is extracted with page provenance in order`() {
        val content = syntheticPdf(listOf("Pagina um", "Pagina dois", "Pagina tres"))

        val document = extractor.extract(content, password = null)

        assertEquals(3, document.pageCount)
        assertEquals(listOf(1, 2, 3), document.pages.map { it.pageNumber })
        assertTrue(document.pages[0].text.contains("Pagina um"))
        assertTrue(document.pages[2].text.contains("Pagina tres"))
    }

    @Test
    fun `a protected document opens with the ephemeral password`() {
        val content = syntheticPdf(listOf("Conteudo protegido"), password = "s3nh4-de-teste")

        val document =
            extractor.extract(content, DocumentPassword.of("s3nh4-de-teste"))

        assertTrue(document.fullText().contains("Conteudo protegido"))
    }

    @Test
    fun `a wrong password fails instead of returning empty text`() {
        val content = syntheticPdf(listOf("Conteudo protegido"), password = "s3nh4-de-teste")

        assertThrows(Exception::class.java) {
            extractor.extract(content, DocumentPassword.of("senha-errada"))
        }
    }

    @Test
    fun `extraction is byte-for-byte repeatable`() {
        val content = syntheticPdf(listOf("Linha estavel"))

        val first = extractor.extract(content, null)
        val second = extractor.extract(content, null)

        assertEquals(first.fullText(), second.fullText())
        assertEquals(SemanticFingerprint.of(first), SemanticFingerprint.of(second))
    }

    @Test
    fun `line separators do not depend on the platform`() {
        val document = extractor.extract(syntheticPdf(listOf("Uma linha")), null)

        assertTrue(
            !document.fullText().contains("\r"),
            "carriage returns would make the fingerprint platform-dependent",
        )
    }

    @Test
    fun `an image-only document falls below the text threshold`() {
        // A page with no text operators is what a scan extracts to.
        val document = extractor.extract(syntheticPdf(listOf("")), null)

        assertTrue(document.isBelowTextThreshold(UploadPolicy.DEFAULT_MINIMUM_TEXT_CHARACTERS))
    }

    @Test
    fun `metadata is limited to non identifying fields`() {
        val document = extractor.extract(syntheticPdf(listOf("Conteudo")), null)

        assertTrue(document.sanitizedMetadata.containsKey("pageCount"))
        assertTrue(document.sanitizedMetadata.keys.none { it == "title" || it == "subject" })
    }
}
