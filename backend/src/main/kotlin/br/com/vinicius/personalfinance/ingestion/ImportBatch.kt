package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CorrelationId
import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import java.time.Instant

/** Immutable identity of the parser that produced the current interpretation. */
data class ParserMetadata(
    val parserId: String,
    val parserVersion: String,
) {
    init {
        require(parserId.isNotBlank()) { "parserId must not be blank" }
        require(parserVersion.isNotBlank()) { "parserVersion must not be blank" }
    }
}

/** The batch after an operation, plus whether the operation actually changed it. */
data class ImportBatchOutcome(
    val batch: ImportBatch,
    val changed: Boolean,
)

/**
 * One import, from reception to a terminal state.
 *
 * The aggregate is immutable: every operation returns a new instance. It owns
 * the invariants and refuses illegal moves; [ImportBatchTransitions] owns the
 * table of which moves exist.
 *
 * Two independent version counters, deliberately not merged:
 *
 * - [version] guards the aggregate itself. The persistence adapter writes with
 *   a conditional update on it, so two concurrent finalisations cannot both
 *   win.
 * - [previewVersion] guards the *decision*. A commit carrying a stale preview
 *   version is refused with `PF_PREVIEW_VERSION_CONFLICT`, because the user
 *   confirmed something the server no longer offers.
 *
 * This issue delivers no financial effect: nothing here writes portfolio data.
 */
data class ImportBatch(
    val id: ImportBatchId,
    val status: ImportBatchStatus,
    val version: Long,
    val previewVersion: Int,
    val rawSha256: String,
    val semanticFingerprint: String?,
    val parser: ParserMetadata?,
    val correlationId: CorrelationId,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(RAW_SHA256_PATTERN.matches(rawSha256)) {
            "rawSha256 must be a lowercase hex SHA-256 digest"
        }
        require(previewVersion >= 0) { "previewVersion must not be negative" }
        require(version >= 0) { "version must not be negative" }
        semanticFingerprint?.let {
            require(it.isNotBlank()) { "semanticFingerprint must not be blank when present" }
        }
    }

    /**
     * Moves to [target] or fails with `PF_IMPORT_INVALID_TRANSITION`.
     *
     * A terminal state has no successors, so this is also what enforces
     * `INV-015` and stops `FAILED` from rewinding to `PARSING`
     * (`FR-IMPORT-001`).
     */
    fun transitionTo(
        target: ImportBatchStatus,
        clock: DomainClock,
    ): ImportBatch {
        if (!ImportBatchTransitions.isAllowed(status, target)) {
            throw ImportLifecycleException(
                code = ImportErrorCode.INVALID_TRANSITION,
                correlationId = correlationId,
                summary = "cannot move from $status to $target",
            )
        }
        return copy(
            status = target,
            version = version + 1,
            updatedAt = clock.now(),
        )
    }

    /** Records the semantic fingerprint once; it is immutable afterwards. */
    fun fingerprintedAs(
        fingerprint: String,
        clock: DomainClock,
    ): ImportBatch {
        require(semanticFingerprint == null || semanticFingerprint == fingerprint) {
            "semanticFingerprint is immutable once recorded"
        }
        return transitionTo(ImportBatchStatus.FINGERPRINTED, clock)
            .copy(semanticFingerprint = fingerprint)
    }

    /** Attaches parser metadata, auditable when reprocessing changes it (`FR-IMPORT-003`). */
    fun withParser(
        metadata: ParserMetadata,
        clock: DomainClock,
    ): ImportBatch = copy(parser = metadata, version = version + 1, updatedAt = clock.now())

    /**
     * Publishes a new preview, invalidating any decision taken against the
     * previous one.
     */
    fun previewReady(clock: DomainClock): ImportBatch =
        transitionTo(ImportBatchStatus.PREVIEW_READY, clock)
            .copy(previewVersion = previewVersion + 1)

    /**
     * Starts the commit for [confirmedPreviewVersion].
     *
     * Fails with `PF_COMMIT_NOT_ALLOWED` when the batch is not committable, and
     * with `PF_PREVIEW_VERSION_CONFLICT` when the confirmed preview is stale.
     * The two are distinct on purpose: the first is never retryable, the second
     * is fixed by refreshing.
     */
    fun startCommit(
        confirmedPreviewVersion: Int,
        clock: DomainClock,
    ): ImportBatch {
        if (status != ImportBatchStatus.PREVIEW_READY) {
            throw ImportLifecycleException(
                code = ImportErrorCode.COMMIT_NOT_ALLOWED,
                correlationId = correlationId,
                summary = "commit requires PREVIEW_READY but batch is $status",
            )
        }
        if (confirmedPreviewVersion != previewVersion) {
            throw ImportLifecycleException(
                code = ImportErrorCode.PREVIEW_VERSION_CONFLICT,
                correlationId = correlationId,
                summary = "confirmed preview version is stale",
            )
        }
        return transitionTo(ImportBatchStatus.COMMITTING, clock)
    }

    /**
     * Rejects the batch, idempotently.
     *
     * `FR-REJECT-004` and `FR-REJECT-005`: repeating a reject returns a
     * consistent state without a second effect, so the caller is told through
     * [ImportBatchOutcome.changed] whether anything actually happened and can
     * skip writing a duplicate audit event.
     */
    fun reject(clock: DomainClock): ImportBatchOutcome =
        if (status == ImportBatchStatus.REJECTED) {
            ImportBatchOutcome(batch = this, changed = false)
        } else {
            ImportBatchOutcome(
                batch = transitionTo(ImportBatchStatus.REJECTED, clock),
                changed = true,
            )
        }

    companion object {
        private val RAW_SHA256_PATTERN = Regex("^[0-9a-f]{64}$")

        /** Creates a batch in [ImportBatchStatus.INITIAL]. */
        fun receive(
            id: ImportBatchId,
            rawSha256: String,
            correlationId: CorrelationId,
            clock: DomainClock,
        ): ImportBatch {
            val now = clock.now()
            return ImportBatch(
                id = id,
                status = ImportBatchStatus.INITIAL,
                version = 0,
                previewVersion = 0,
                rawSha256 = rawSha256,
                semanticFingerprint = null,
                parser = null,
                correlationId = correlationId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
