package br.com.vinicius.personalfinance.ingestion

/**
 * Why a source token did not become a canonical value.
 *
 * `FR-NORMALIZE-005`: an unreadable value is reported, never repaired. The code
 * says what happened; the decision about whether it blocks a commit belongs to
 * reconciliation and preview, not to normalization.
 */
enum class NormalizationIssueCode(
    val severity: ErrorSeverity,
) {
    /** The layout printed its "no value here" placeholder. Expected, not a defect. */
    VALUE_ABSENT(ErrorSeverity.INFO),

    /** The token exists but does not satisfy the layout's number grammar. */
    VALUE_MALFORMED(ErrorSeverity.WARNING),

    /** A well-formed date that does not exist, such as 31/02. */
    DATE_INVALID(ErrorSeverity.WARNING),

    /** A currency symbol the release does not support. */
    CURRENCY_UNSUPPORTED(ErrorSeverity.WARNING),

    /** The token carries a currency other than the one its column declares. */
    CURRENCY_MISMATCH(ErrorSeverity.WARNING),

    /** The document never stated a value the canonical model expects. */
    VALUE_NOT_STATED(ErrorSeverity.WARNING),

    /** A section no parser understands survived into normalization. */
    SECTION_UNKNOWN(ErrorSeverity.WARNING),
}

/**
 * One normalization finding, addressed by field path and source page.
 *
 * Deliberately carries no amount, date or token. The raw value stays in the
 * parser DTO, which is the single place that holds source financial content;
 * duplicating it here would spread C2 data into a channel that preview,
 * logging and audit all read (`NFR-SEC` redaction, `FR-EVIDENCE-003`).
 */
data class NormalizationIssue(
    val code: NormalizationIssueCode,
    val fieldPath: String,
    val pageNumber: Int?,
)
