package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.audit.AuditActor
import br.com.vinicius.personalfinance.shared.CorrelationId
import br.com.vinicius.personalfinance.shared.ImportBatchId
import org.springframework.stereotype.Service

/**
 * Accepts a document and takes it as far as the lifecycle can go without a PDF
 * parser: validated, fingerprinted, stored, and either ready for extraction or
 * waiting for a password.
 *
 * Ordering is deliberate. The content is validated *before* anything is
 * written, so a rejected file never reaches the disk. Everything after the
 * store is wrapped: if the batch cannot be created, the stored bytes are
 * removed in the same call rather than waiting for the sweeper
 * (`FR-UPLOAD-008`).
 */
@Service
class DocumentIntakeService(
    private val validator: SourceDocumentValidator,
    private val store: SourceDocumentStore,
    private val lifecycle: ImportBatchLifecycleService,
    private val encryptionProbe: PdfEncryptionProbe,
    private val continuations: PasswordContinuationRegistry,
) {
    /**
     * Receives [content] and returns the resulting batch.
     *
     * The caller learns the batch, never where the bytes went.
     */
    fun receive(
        content: ByteArray,
        actor: AuditActor,
    ): ImportBatch {
        val id = ImportBatchId.random()
        val correlationId = CorrelationId.random()
        validator.validate(content, correlationId)

        val fingerprint = SourceFingerprint.of(content)
        val ref = store.store(content)

        return runCatching {
            val received =
                lifecycle.receive(
                    id = id,
                    rawSha256 = fingerprint,
                    actor = actor,
                    storedDocumentRef = ref,
                )
            val fingerprinted =
                lifecycle.transition(received.id, ImportBatchStatus.FINGERPRINTED, actor)
            if (encryptionProbe.looksEncrypted(content)) {
                lifecycle.transition(
                    fingerprinted.id,
                    ImportBatchStatus.PASSWORD_REQUIRED,
                    actor,
                )
            } else {
                fingerprinted
            }
        }.onFailure {
            store.delete(ref)
        }.getOrThrow()
    }

    /**
     * Registers a password attempt for [importId].
     *
     * The password is held in memory for the extraction step and never returned,
     * logged or persisted. Exceeding the attempt limit fails with the stable
     * code and drops the batch's continuation entirely.
     */
    fun submitPassword(
        importId: ImportBatchId,
        password: DocumentPassword,
    ) {
        val refusal = continuations.submit(importId, password)
        if (refusal == ContinuationRefusal.ATTEMPTS_EXCEEDED) {
            continuations.discard(importId)
            throw ImportLifecycleException(
                code = ImportErrorCode.PDF_PASSWORD_ATTEMPTS_EXCEEDED,
                correlationId = CorrelationId.random(),
                summary = "password attempt limit reached",
            )
        }
    }

    /**
     * Releases everything ephemeral for [importId]: the stored bytes and any
     * held password. Safe to call on success, failure and cancellation alike.
     */
    fun releaseEphemeralState(batch: ImportBatch): Boolean {
        continuations.discard(batch.id)
        return batch.storedDocumentRef?.let { ref -> store.delete(ref) } ?: false
    }
}
