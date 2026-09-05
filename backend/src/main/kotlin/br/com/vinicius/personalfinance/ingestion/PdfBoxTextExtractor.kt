package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CorrelationId
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Component

/**
 * PDFBox-backed extraction (ADR 0008, `FR-PDF-002`).
 *
 * Determinism is configured, not assumed:
 *
 * - the line separator is pinned to `\n`, because the PDFBox default is the
 *   platform separator and the semantic fingerprint would otherwise differ
 *   between a developer's Windows machine and CI;
 * - text is sorted by position, so output does not depend on the order objects
 *   happen to appear inside the file.
 *
 * Active content is never executed: PDFBox reads structure, it does not run
 * embedded scripts (`FR-PDF-006`).
 */
@Component
class PdfBoxTextExtractor : PdfTextExtractor {
    override fun extract(
        content: ByteArray,
        password: DocumentPassword?,
    ): ExtractedDocument =
        openDocument(content, password).use { document ->
            ExtractedDocument(
                pages = extractPages(document),
                sanitizedMetadata = sanitizedMetadataOf(document),
            )
        }

    private fun openDocument(
        content: ByteArray,
        password: DocumentPassword?,
    ): PDDocument =
        if (password == null) {
            Loader.loadPDF(content)
        } else {
            // The characters are handed to PDFBox and dropped immediately; the
            // password itself is cleared by whoever owns it.
            password.use { characters -> Loader.loadPDF(content, String(characters)) }
        }

    private fun extractPages(document: PDDocument): List<ExtractedPage> =
        (1..document.numberOfPages).map { pageNumber ->
            val stripper =
                PDFTextStripper().apply {
                    sortByPosition = true
                    // pageEnd and paragraphEnd are initialised from the platform
                    // separator at construction and do not follow lineSeparator,
                    // so each one is pinned explicitly.
                    lineSeparator = LINE_SEPARATOR
                    pageEnd = LINE_SEPARATOR
                    paragraphEnd = LINE_SEPARATOR
                    paragraphStart = ""
                    startPage = pageNumber
                    endPage = pageNumber
                }
            ExtractedPage(pageNumber = pageNumber, text = stripper.getText(document))
        }

    /**
     * PDF metadata is author-controlled, so only a known set of fields is kept
     * and each is length-bounded. Producer and creator help diagnose a layout;
     * title and subject are omitted because they routinely carry the account
     * holder's name.
     */
    private fun sanitizedMetadataOf(document: PDDocument): Map<String, String> {
        val information = document.documentInformation ?: return emptyMap()
        return buildMap {
            information.producer?.let { put("producer", it.take(MAX_METADATA_LENGTH)) }
            information.creator?.let { put("creator", it.take(MAX_METADATA_LENGTH)) }
            put("pageCount", document.numberOfPages.toString())
            put("encrypted", document.isEncrypted.toString())
        }
    }

    private companion object {
        const val LINE_SEPARATOR = "\n"

        const val MAX_METADATA_LENGTH = 120
    }
}

/** Raised when PDFBox cannot open or read the document. */
class ExtractionFailed(
    val correlationId: CorrelationId,
    cause: Throwable,
) : RuntimeException("extraction failed", cause)
