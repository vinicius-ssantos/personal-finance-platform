package br.com.vinicius.personalfinance.ingestion

/**
 * Native text extraction from a PDF.
 *
 * A port so the pipeline can be tested without building real PDFs for every
 * case; the PDFBox implementation is the only production one (ADR 0008).
 */
interface PdfTextExtractor {
    /**
     * Extracts [content], using [password] when the document is encrypted.
     *
     * Implementations must never execute active content and must not let the
     * password escape into the result.
     */
    fun extract(
        content: ByteArray,
        password: DocumentPassword?,
    ): ExtractedDocument
}
