package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.BLOCKED
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.COMMITTED
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.COMMITTING
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.DUPLICATE
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.EXTRACTING
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.FAILED
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.FINGERPRINTED
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.LAYOUT_DETECTED
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.NORMALIZING
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.PARSING
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.PASSWORD_REQUIRED
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.PREVIEW_READY
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.RECEIVED
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.RECONCILING
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus.REJECTED

/**
 * The single authority on which lifecycle moves are legal.
 *
 * Kept separate from the aggregate so the table is pure data, exhaustively
 * testable without persistence or Spring. [ImportBatch] consults it and is the
 * one that refuses an illegal move.
 */
object ImportBatchTransitions {
    private val ALLOWED: Map<ImportBatchStatus, Set<ImportBatchStatus>> =
        mapOf(
            RECEIVED to setOf(FINGERPRINTED, FAILED),
            FINGERPRINTED to setOf(DUPLICATE, PASSWORD_REQUIRED, EXTRACTING, FAILED),
            PASSWORD_REQUIRED to setOf(EXTRACTING, FAILED),
            EXTRACTING to setOf(LAYOUT_DETECTED, FAILED),
            LAYOUT_DETECTED to setOf(PARSING, FAILED),
            PARSING to setOf(NORMALIZING, FAILED),
            NORMALIZING to setOf(RECONCILING, FAILED),
            RECONCILING to setOf(PREVIEW_READY, BLOCKED, FAILED),
            // A preview may be superseded: re-reconciling withdraws the current
            // offer and produces a new version. Nothing can commit while the
            // batch is back in RECONCILING, which is what makes the previous
            // offer detectably stale rather than silently replaced.
            PREVIEW_READY to setOf(COMMITTING, REJECTED, RECONCILING),
            BLOCKED to setOf(REJECTED),
            COMMITTING to setOf(COMMITTED, FAILED),
            COMMITTED to emptySet(),
            REJECTED to emptySet(),
            FAILED to emptySet(),
            DUPLICATE to emptySet(),
        )

    init {
        val missing = ImportBatchStatus.entries.filterNot { ALLOWED.containsKey(it) }
        check(missing.isEmpty()) { "transition table is missing states: $missing" }
    }

    /** States reachable from [from] through a normal lifecycle move. */
    fun successorsOf(from: ImportBatchStatus): Set<ImportBatchStatus> = ALLOWED.getValue(from)

    /**
     * True when moving [from] to [to] is legal.
     *
     * A terminal source is always false, which is `INV-015`. `FAILED` has no
     * successors at all, so it cannot silently return to `PARSING`
     * (`FR-IMPORT-001`); resuming work means a new import, not a rewind.
     */
    fun isAllowed(
        from: ImportBatchStatus,
        to: ImportBatchStatus,
    ): Boolean = to in ALLOWED.getValue(from)
}
