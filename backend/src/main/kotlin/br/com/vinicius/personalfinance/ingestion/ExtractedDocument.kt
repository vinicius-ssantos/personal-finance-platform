package br.com.vinicius.personalfinance.ingestion

/**
 * Text of one page, with its provenance.
 *
 * `FR-PDF-009` requires a stable page number under a documented convention:
 * **pages are numbered from 1**, matching what a reader sees, not the
 * zero-based index PDFBox uses internally. Evidence recorded later points back
 * here, so the convention must never drift.
 */
data class ExtractedPage(
    val pageNumber: Int,
    val text: String,
) {
    init {
        require(pageNumber >= FIRST_PAGE_NUMBER) {
            "pageNumber starts at $FIRST_PAGE_NUMBER, got $pageNumber"
        }
    }

    /** Characters that count towards the structured-text threshold. */
    val meaningfulCharacterCount: Int
        get() = text.count { character -> !character.isWhitespace() }

    companion object {
        const val FIRST_PAGE_NUMBER: Int = 1
    }
}

/**
 * The result of native text extraction.
 *
 * Carries no bytes and no password: once extraction is done, the document is
 * text plus provenance. Metadata is sanitised by the extractor, because PDF
 * metadata is author-controlled and would otherwise be a path for arbitrary
 * content to reach logs.
 */
data class ExtractedDocument(
    val pages: List<ExtractedPage>,
    val sanitizedMetadata: Map<String, String>,
) {
    init {
        require(pages.isNotEmpty()) { "an extracted document must have at least one page" }
        val expected = pages.indices.map { index -> index + ExtractedPage.FIRST_PAGE_NUMBER }
        require(pages.map { it.pageNumber } == expected) {
            "pages must be ordered and contiguous from ${ExtractedPage.FIRST_PAGE_NUMBER}"
        }
    }

    val pageCount: Int
        get() = pages.size

    val meaningfulCharacterCount: Int
        get() = pages.sumOf { page -> page.meaningfulCharacterCount }

    /** Whole-document text, page order preserved (`FR-PDF-008`). */
    fun fullText(): String = pages.joinToString(separator = "\n") { page -> page.text }

    /**
     * True when there is too little text to treat this as a digital document.
     *
     * A scanned report extracts to almost nothing. Without this gate it would
     * flow onward and fail much later as "no positions found", which reads like
     * a parser bug rather than the real cause. `FR-PDF-004` asks for a safe
     * failure; OCR is out of scope by ADR 0008.
     */
    fun isBelowTextThreshold(minimumCharacters: Int): Boolean = meaningfulCharacterCount < minimumCharacters
}
