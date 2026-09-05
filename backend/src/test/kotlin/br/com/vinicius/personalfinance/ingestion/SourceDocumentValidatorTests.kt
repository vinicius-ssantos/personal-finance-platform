package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CorrelationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SourceDocumentValidatorTests {
    private val policy = UploadPolicy(maxSizeBytes = 1024)

    private val validator = SourceDocumentValidator(policy)

    private val correlationId = CorrelationId.random()

    private fun pdf(body: String = "\nnothing special\n%%EOF"): ByteArray =
        ("%PDF-1.7$body").toByteArray(Charsets.ISO_8859_1)

    private fun refusalOf(content: ByteArray): ImportErrorCode =
        assertThrows(ImportLifecycleException::class.java) {
            validator.validate(content, correlationId)
        }.code

    @Test
    fun `a plain PDF is accepted`() {
        validator.validate(pdf(), correlationId)
    }

    @Test
    fun `a file that only claims to be a PDF is rejected`() {
        val disguised = "MZ\u0090\u0000this is an executable".toByteArray(Charsets.ISO_8859_1)

        assertEquals(ImportErrorCode.UPLOAD_NOT_A_PDF, refusalOf(disguised))
    }

    @Test
    fun `an empty upload is rejected rather than treated as a short PDF`() {
        assertEquals(ImportErrorCode.UPLOAD_NOT_A_PDF, refusalOf(ByteArray(0)))
    }

    @Test
    fun `a signature appearing later in the file does not count`() {
        val smuggled = "harmless prefix %PDF-1.7".toByteArray(Charsets.ISO_8859_1)

        assertEquals(ImportErrorCode.UPLOAD_NOT_A_PDF, refusalOf(smuggled))
    }

    @Test
    fun `an oversize upload is rejected`() {
        val oversize = pdf("\n" + "x".repeat(policy.maxSizeBytes.toInt()))

        assertEquals(ImportErrorCode.UPLOAD_TOO_LARGE, refusalOf(oversize))
    }

    @Test
    fun `size is checked before content so a huge file is not scanned`() {
        val oversizeAndDisguised = "x".repeat(policy.maxSizeBytes.toInt() + 1).toByteArray()

        assertEquals(ImportErrorCode.UPLOAD_TOO_LARGE, refusalOf(oversizeAndDisguised))
    }

    @Test
    fun `documents carrying active content are rejected`() {
        listOf("/JavaScript", "/JS", "/Launch", "/EmbeddedFile", "/OpenAction", "/RichMedia")
            .forEach { marker ->
                assertEquals(
                    ImportErrorCode.UPLOAD_ACTIVE_CONTENT,
                    refusalOf(pdf("\n1 0 obj << $marker >> endobj\n%%EOF")),
                    "$marker must be rejected",
                )
            }
    }

    @Test
    fun `the refusal never echoes document content`() {
        val secretBearing = pdf("\n/JavaScript (alert('4111111111111111'))\n%%EOF")

        val failure =
            assertThrows(ImportLifecycleException::class.java) {
                validator.validate(secretBearing, correlationId)
            }

        assertEquals(false, failure.message?.contains("4111111111111111"))
    }

    @Test
    fun `every upload refusal is final rather than retryable`() {
        listOf(
            ImportErrorCode.UPLOAD_TOO_LARGE,
            ImportErrorCode.UPLOAD_NOT_A_PDF,
            ImportErrorCode.UPLOAD_ACTIVE_CONTENT,
        ).forEach { code ->
            assertEquals(RetryClass.NEVER, code.retryClass, "${code.code} must not invite a retry")
        }
    }
}
