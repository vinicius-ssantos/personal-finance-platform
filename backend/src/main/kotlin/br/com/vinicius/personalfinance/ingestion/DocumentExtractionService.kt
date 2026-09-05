package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.audit.AuditActor
import org.springframework.stereotype.Service
import java.io.IOException

/**
 * Takes a received document from stored bytes to a selected parser.
 *
 * Everything here happens before any financial interpretation: the result is a
 * batch that knows which parser will read it, not one that has been read.
 *
 * Failures move the batch to `FAILED` with a stable code rather than leaving it
 * in an intermediate state, so a stuck import is always a bug and never a normal
 * outcome.
 */
@Service
class DocumentExtractionService(
    private val store: SourceDocumentStore,
    private val extractor: PdfTextExtractor,
    private val continuations: PasswordContinuationRegistry,
    private val registry: ParserRegistry,
    private val lifecycle: ImportBatchLifecycleService,
    private val policy: UploadPolicy,
) {
    /**
     * Extracts [batch] and selects its parser.
     *
     * The password, when the document needs one, is consumed from the
     * continuation registry — once. A wrong password is spent, which is what
     * makes the attempt limit meaningful.
     */
    fun extractAndSelect(
        batch: ImportBatch,
        actor: AuditActor,
    ): ImportBatch {
        val content = storedContentOf(batch)
        val password = passwordFor(batch)
        val extracting = lifecycle.transition(batch.id, ImportBatchStatus.EXTRACTING, actor)
        val document = extractDocument(extracting, content, password, actor)
        return select(extracting, document, actor)
    }

    private fun storedContentOf(batch: ImportBatch): ByteArray {
        val ref =
            batch.storedDocumentRef
                ?: throw lifecycleFailure(
                    batch,
                    ImportErrorCode.PDF_EXTRACTION_FAILED,
                    "batch has no stored document",
                )
        return store.read(ref)
            ?: throw lifecycleFailure(
                batch,
                ImportErrorCode.PDF_EXTRACTION_FAILED,
                "stored document is gone",
            )
    }

    private fun passwordFor(batch: ImportBatch): DocumentPassword? =
        if (batch.status == ImportBatchStatus.PASSWORD_REQUIRED) {
            continuations.consume(batch.id).getOrElse { unavailable ->
                throw lifecycleFailure(
                    batch,
                    ImportErrorCode.PDF_PASSWORD_REQUIRED,
                    "no usable password continuation",
                    unavailable,
                )
            }
        } else {
            null
        }

    private fun extractDocument(
        batch: ImportBatch,
        content: ByteArray,
        password: DocumentPassword?,
        actor: AuditActor,
    ): ExtractedDocument {
        val document =
            try {
                extractor.extract(content, password)
            } catch (unreadable: IOException) {
                // PDFBox reports a wrong password and a corrupt file the same
                // way. The cause is kept for operators and never published: the
                // catalogue marks this code as exposing no detail.
                fail(batch, actor)
                throw lifecycleFailure(
                    batch,
                    ImportErrorCode.PDF_INVALID_PASSWORD,
                    "cannot open document",
                    unreadable,
                )
            } finally {
                password?.clear()
            }
        if (document.isBelowTextThreshold(policy.minimumTextCharacters)) {
            fail(batch, actor)
            throw lifecycleFailure(
                batch,
                ImportErrorCode.PDF_EXTRACTION_FAILED,
                "document carries too little text to be read natively",
            )
        }
        return document
    }

    private fun select(
        batch: ImportBatch,
        document: ExtractedDocument,
        actor: AuditActor,
    ): ImportBatch =
        when (val selection = registry.select(document)) {
            is LayoutSelection.Unsupported -> {
                fail(batch, actor)
                throw lifecycleFailure(
                    batch,
                    ImportErrorCode.LAYOUT_UNSUPPORTED,
                    "${selection.reason}: ${selection.detail}",
                )
            }

            is LayoutSelection.Selected ->
                lifecycle.recordInterpretation(
                    id = batch.id,
                    semanticFingerprint = SemanticFingerprint.of(document),
                    parser =
                        ParserMetadata(
                            layout = selection.parser.descriptor,
                            parserId = selection.parser.parserId,
                            parserVersion = selection.parser.parserVersion,
                        ),
                    actor = actor,
                )
        }

    private fun fail(
        batch: ImportBatch,
        actor: AuditActor,
    ) {
        runCatching { lifecycle.transition(batch.id, ImportBatchStatus.FAILED, actor) }
    }

    private fun lifecycleFailure(
        batch: ImportBatch,
        code: ImportErrorCode,
        summary: String,
        cause: Throwable? = null,
    ): ImportLifecycleException =
        ImportLifecycleException(
            code = code,
            correlationId = batch.correlationId,
            summary = summary,
            cause = cause,
        )
}
