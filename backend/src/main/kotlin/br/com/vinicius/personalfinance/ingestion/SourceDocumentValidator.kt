package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CorrelationId
import org.springframework.stereotype.Component

/**
 * Decides whether received bytes may enter the pipeline at all.
 *
 * The check is on content, never on the filename or the declared MIME type
 * (`FR-UPLOAD-003`): a caller controls both, so neither is evidence. Only the
 * magic signature is.
 *
 * Active-content detection is a byte scan, not a parse. It cannot be complete
 * without a real PDF reader, and it is not meant to be: it is a cheap gate that
 * rejects the obvious cases before the file is stored. The specification says
 * "where detectable", and this is the detectable part.
 */
@Component
class SourceDocumentValidator(
    private val policy: UploadPolicy,
) {
    fun validate(
        content: ByteArray,
        correlationId: CorrelationId,
    ) {
        val refusal = refusalFor(content) ?: return
        throw ImportLifecycleException(
            code = refusal.code,
            correlationId = correlationId,
            summary = refusal.summary,
        )
    }

    /** Why the content is unacceptable, or null when it may proceed. */
    private fun refusalFor(content: ByteArray): Refusal? =
        when {
            content.size > policy.maxSizeBytes ->
                Refusal(
                    ImportErrorCode.UPLOAD_TOO_LARGE,
                    "upload exceeds ${policy.maxSizeBytes} bytes",
                )

            !startsWithPdfSignature(content) ->
                Refusal(
                    ImportErrorCode.UPLOAD_NOT_A_PDF,
                    "content does not start with a PDF signature",
                )

            else ->
                firstActiveContentMarker(content)?.let { marker ->
                    // The marker names a PDF construct, never document content.
                    Refusal(
                        ImportErrorCode.UPLOAD_ACTIVE_CONTENT,
                        "document carries active content: $marker",
                    )
                }
        }

    private data class Refusal(
        val code: ImportErrorCode,
        val summary: String,
    )

    private fun startsWithPdfSignature(content: ByteArray): Boolean {
        if (content.size < PDF_SIGNATURE.size) return false
        return PDF_SIGNATURE.indices.all { index -> content[index] == PDF_SIGNATURE[index] }
    }

    private fun firstActiveContentMarker(content: ByteArray): String? {
        val text = String(content, Charsets.ISO_8859_1)
        return ACTIVE_CONTENT_MARKERS.firstOrNull { marker -> text.contains(marker) }
    }

    private companion object {
        /** `%PDF-` */
        val PDF_SIGNATURE: ByteArray = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D)

        val ACTIVE_CONTENT_MARKERS =
            listOf(
                "/JavaScript",
                "/JS",
                "/Launch",
                "/EmbeddedFile",
                "/OpenAction",
                "/AA",
                "/RichMedia",
            )
    }
}
