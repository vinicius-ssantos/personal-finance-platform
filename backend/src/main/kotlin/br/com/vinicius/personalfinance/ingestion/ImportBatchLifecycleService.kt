package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.audit.AuditAction
import br.com.vinicius.personalfinance.audit.AuditActor
import br.com.vinicius.personalfinance.audit.AuditEvent
import br.com.vinicius.personalfinance.audit.AuditTrail
import br.com.vinicius.personalfinance.shared.CorrelationId
import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Drives the import lifecycle and records what it decided.
 *
 * A successful move writes exactly one audit event in the same transaction as
 * the state change, so the trail cannot disagree with the batch. A refused move
 * is audited on its own transaction, because the caller's transaction is about
 * to roll back (see [RefusalAuditRecorder]).
 *
 * This service produces no financial effect: nothing here writes portfolio data.
 */
@Service
class ImportBatchLifecycleService(
    private val repository: ImportBatchRepository,
    private val auditTrail: AuditTrail,
    private val refusalAuditRecorder: RefusalAuditRecorder,
    private val clock: DomainClock,
) {
    @Transactional
    fun receive(
        id: ImportBatchId,
        rawSha256: String,
        actor: AuditActor,
        storedDocumentRef: StoredDocumentRef? = null,
    ): ImportBatch {
        val batch =
            ImportBatch.receive(
                id = id,
                rawSha256 = rawSha256,
                storedDocumentRef = storedDocumentRef,
                correlationId = CorrelationId.random(),
                clock = clock,
            )
        repository.insert(batch)
        audit(batch, AuditAction.IMPORT_TRANSITIONED, actor, mapOf("to" to batch.status.name))
        return batch
    }

    /** Moves the batch to [target], or fails with a stable code. */
    @Transactional
    fun transition(
        id: ImportBatchId,
        target: ImportBatchStatus,
        actor: AuditActor,
    ): ImportBatch {
        val current = load(id)
        val moved =
            try {
                current.transitionTo(target, clock)
            } catch (failure: ImportLifecycleException) {
                refusalAuditRecorder.record(
                    auditEventFor(
                        batch = current,
                        action = AuditAction.IMPORT_TRANSITION_REJECTED,
                        actor = actor,
                        details = mapOf("from" to current.status.name, "to" to target.name),
                    ),
                )
                throw failure
            }
        persist(moved, current.version)
        audit(
            batch = moved,
            action = AuditAction.IMPORT_TRANSITIONED,
            actor = actor,
            details = mapOf("from" to current.status.name, "to" to moved.status.name),
        )
        return moved
    }

    /**
     * Starts the commit for [confirmedPreviewVersion].
     *
     * Concurrency is settled by the conditional update: the first caller moves
     * the row from its observed version, the second finds the version changed
     * and is refused. Exactly one commit becomes effective.
     */
    @Transactional
    fun startCommit(
        id: ImportBatchId,
        confirmedPreviewVersion: Int,
        actor: AuditActor,
    ): ImportBatch {
        val current = load(id)
        val committing = current.startCommit(confirmedPreviewVersion, clock)
        persist(committing, current.version)
        audit(
            batch = committing,
            action = AuditAction.IMPORT_TRANSITIONED,
            actor = actor,
            details = mapOf("from" to current.status.name, "to" to committing.status.name),
        )
        return committing
    }

    /**
     * Rejects the batch. Repeating it is a no-op that writes no second event
     * (`FR-REJECT-004`, `FR-REJECT-005`).
     */
    @Transactional
    fun reject(
        id: ImportBatchId,
        actor: AuditActor,
    ): ImportBatch {
        val current = load(id)
        val outcome = current.reject(clock)
        if (!outcome.changed) {
            return outcome.batch
        }
        persist(outcome.batch, current.version)
        audit(
            batch = outcome.batch,
            action = AuditAction.IMPORT_REJECTED,
            actor = actor,
            details = mapOf("from" to current.status.name),
        )
        return outcome.batch
    }

    private fun load(id: ImportBatchId): ImportBatch =
        repository.findById(id)
            ?: throw ImportLifecycleException(
                code = ImportErrorCode.INTERNAL_ERROR,
                correlationId = CorrelationId.random(),
                summary = "import batch not found",
            )

    private fun persist(
        batch: ImportBatch,
        expectedVersion: Long,
    ) {
        val applied = repository.updateIfUnchanged(batch, expectedVersion)
        if (!applied) {
            throw ImportLifecycleException(
                code = ImportErrorCode.COMMIT_NOT_ALLOWED,
                correlationId = batch.correlationId,
                summary = "batch was modified concurrently",
            )
        }
    }

    private fun audit(
        batch: ImportBatch,
        action: AuditAction,
        actor: AuditActor,
        details: Map<String, String>,
    ) {
        auditTrail.record(auditEventFor(batch, action, actor, details))
    }

    private fun auditEventFor(
        batch: ImportBatch,
        action: AuditAction,
        actor: AuditActor,
        details: Map<String, String>,
    ): AuditEvent =
        AuditEvent.of(
            id = UUID.randomUUID(),
            occurredAt = clock.now(),
            actor = actor,
            action = action,
            subject = batch.id.toString(),
            correlationId = batch.correlationId,
            details = details,
        )
}
