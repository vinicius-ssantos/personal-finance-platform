package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.ImportBatchId

/**
 * Persistence port for [ImportBatch]. The adapter lives in the persistence
 * module; `ingestion` must not depend on it, so the contract is declared here.
 */
interface ImportBatchRepository {
    fun insert(batch: ImportBatch)

    fun findById(id: ImportBatchId): ImportBatch?

    /**
     * Applies [batch] only if the stored row still has [expectedVersion].
     *
     * Returns false when another writer already moved the batch. Implementations
     * must do this as a single conditional statement, so two concurrent
     * finalisations cannot both succeed.
     */
    fun updateIfUnchanged(
        batch: ImportBatch,
        expectedVersion: Long,
    ): Boolean
}
