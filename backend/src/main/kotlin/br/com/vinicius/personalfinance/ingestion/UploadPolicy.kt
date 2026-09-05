package br.com.vinicius.personalfinance.ingestion

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Boundary limits for accepting a source document.
 *
 * Defaults follow `docs/architecture/INGESTION.md`. Every value is
 * configurable (`FR-UPLOAD-002`), because the safe limit for a laptop is not
 * the safe limit for a server.
 *
 * Page count is deliberately absent: counting pages needs a PDF parser, which
 * arrives with PDFBox in the extraction issue.
 */
@ConfigurationProperties(prefix = "personal-finance.upload")
data class UploadPolicy(
    val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
    val passwordAttemptLimit: Int = DEFAULT_PASSWORD_ATTEMPT_LIMIT,
    val passwordTtl: Duration = DEFAULT_PASSWORD_TTL,
    val minimumTextCharacters: Int = DEFAULT_MINIMUM_TEXT_CHARACTERS,
) {
    init {
        require(maxSizeBytes > 0) { "maxSizeBytes must be positive" }
        require(passwordAttemptLimit > 0) { "passwordAttemptLimit must be positive" }
        require(!passwordTtl.isNegative && !passwordTtl.isZero) {
            "passwordTtl must be positive"
        }
        require(minimumTextCharacters > 0) { "minimumTextCharacters must be positive" }
    }

    companion object {
        const val DEFAULT_MAX_SIZE_BYTES: Long = 25L * 1024 * 1024

        const val DEFAULT_PASSWORD_ATTEMPT_LIMIT: Int = 5

        val DEFAULT_PASSWORD_TTL: Duration = Duration.ofMinutes(15)

        /**
         * Below this, the document is treated as image-only and fails safely.
         * A one-page statement carries thousands of characters; a scan carries
         * almost none. The gap is wide, so the exact value is not delicate.
         */
        const val DEFAULT_MINIMUM_TEXT_CHARACTERS: Int = 200
    }
}
