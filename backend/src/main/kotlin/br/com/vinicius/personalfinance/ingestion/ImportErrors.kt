package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CorrelationId

/**
 * How a caller may react to a failure.
 *
 * `NFR-RETRY-001`: retry must not be "wait and hope" against a deterministic
 * error, so the class is declared by the catalogue instead of being guessed
 * from the HTTP status.
 */
enum class RetryClass {
    NEVER,
    RETRY_SAFE,
    RETRY_WITH_BACKOFF,
    USER_ACTION_REQUIRED,
    REFRESH_REQUIRED,
    REPROCESS_REQUIRED,
}

/** How bad it is. `FR-ERROR-006`: severity and code are separate concepts. */
enum class ErrorSeverity {
    INFO,
    WARNING,
    FAILURE,
    BLOCKER,
}

/** Whether any server-side detail may reach the client. */
enum class DetailExposure {
    /** Only the stable code and a generic message. */
    NONE,

    /** A curated, non-sensitive summary may accompany the code. */
    SAFE_SUMMARY,
}

/**
 * The stable error catalogue, mirroring
 * `docs/specification/ERROR-CATALOG.md`.
 *
 * A published code never changes meaning (`FR-ERROR-001..006`). Human-facing
 * text may evolve; [code] may not. Codes for phases that later issues deliver
 * are declared now so the contract is fixed before callers depend on it.
 *
 * Where the specification lists two statuses for one code, the primary one is
 * used and the alternative is noted, so the mapping stays single-valued.
 */
enum class ImportErrorCode(
    val code: String,
    val httpStatus: Int,
    val severity: ErrorSeverity,
    val retryClass: RetryClass,
    val detailExposure: DetailExposure,
) {
    INVALID_TRANSITION(
        code = "PF_IMPORT_INVALID_TRANSITION",
        httpStatus = 409,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.NEVER,
        detailExposure = DetailExposure.SAFE_SUMMARY,
    ),
    DUPLICATE_SOURCE(
        code = "PF_IMPORT_DUPLICATE_SOURCE",
        httpStatus = 409,
        severity = ErrorSeverity.WARNING,
        retryClass = RetryClass.USER_ACTION_REQUIRED,
        detailExposure = DetailExposure.SAFE_SUMMARY,
    ),
    PDF_PASSWORD_REQUIRED(
        code = "PF_PDF_PASSWORD_REQUIRED",
        httpStatus = 409,
        severity = ErrorSeverity.INFO,
        retryClass = RetryClass.USER_ACTION_REQUIRED,
        detailExposure = DetailExposure.NONE,
    ),
    PDF_INVALID_PASSWORD(
        code = "PF_PDF_INVALID_PASSWORD",
        httpStatus = 422,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.USER_ACTION_REQUIRED,
        detailExposure = DetailExposure.NONE,
    ),
    PDF_PASSWORD_ATTEMPTS_EXCEEDED(
        // The specification allows 429; 422 is used as the primary status.
        code = "PF_PDF_PASSWORD_ATTEMPTS_EXCEEDED",
        httpStatus = 422,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.USER_ACTION_REQUIRED,
        detailExposure = DetailExposure.NONE,
    ),
    PDF_EXTRACTION_FAILED(
        code = "PF_PDF_EXTRACTION_FAILED",
        httpStatus = 422,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.REPROCESS_REQUIRED,
        detailExposure = DetailExposure.NONE,
    ),
    LAYOUT_UNSUPPORTED(
        code = "PF_LAYOUT_UNSUPPORTED",
        httpStatus = 422,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.REPROCESS_REQUIRED,
        detailExposure = DetailExposure.SAFE_SUMMARY,
    ),
    PARSER_FAILED(
        // The specification allows 500; 422 is used as the primary status.
        code = "PF_PARSER_FAILED",
        httpStatus = 422,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.REPROCESS_REQUIRED,
        detailExposure = DetailExposure.NONE,
    ),
    RECON_BLOCKING_MISMATCH(
        code = "PF_RECON_BLOCKING_MISMATCH",
        httpStatus = 422,
        severity = ErrorSeverity.BLOCKER,
        retryClass = RetryClass.USER_ACTION_REQUIRED,
        detailExposure = DetailExposure.SAFE_SUMMARY,
    ),
    PREVIEW_VERSION_CONFLICT(
        code = "PF_PREVIEW_VERSION_CONFLICT",
        httpStatus = 409,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.REFRESH_REQUIRED,
        detailExposure = DetailExposure.SAFE_SUMMARY,
    ),
    COMMIT_NOT_ALLOWED(
        // The specification allows 422; 409 is used as the primary status.
        code = "PF_COMMIT_NOT_ALLOWED",
        httpStatus = 409,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.NEVER,
        detailExposure = DetailExposure.SAFE_SUMMARY,
    ),
    DATABASE_UNAVAILABLE(
        code = "PF_DATABASE_UNAVAILABLE",
        httpStatus = 503,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.RETRY_WITH_BACKOFF,
        detailExposure = DetailExposure.NONE,
    ),
    INTERNAL_ERROR(
        code = "PF_INTERNAL_ERROR",
        httpStatus = 500,
        severity = ErrorSeverity.FAILURE,
        retryClass = RetryClass.RETRY_WITH_BACKOFF,
        detailExposure = DetailExposure.NONE,
    ),
    ;

    companion object {
        private val BY_CODE: Map<String, ImportErrorCode> = entries.associateBy { it.code }

        fun ofCode(code: String): ImportErrorCode? = BY_CODE[code]
    }
}

/**
 * A lifecycle failure carrying a stable code.
 *
 * The message is for operators. `NFR-OBS-004` requires the failure to be
 * correlatable without exposing a stack trace to the client, so the mapping
 * layer must publish [code] and [correlationId] and never this exception's
 * cause.
 */
class ImportLifecycleException(
    val code: ImportErrorCode,
    val correlationId: CorrelationId,
    val summary: String,
) : RuntimeException("${code.code}: $summary (correlationId=$correlationId)")
