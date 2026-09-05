package br.com.vinicius.personalfinance.ingestion

/**
 * Lifecycle of one import, using the vocabulary of the canonical product
 * specification (`docs/specification/PRODUCT-SPECIFICATION.md`).
 *
 * Modelled as an `enum` rather than a `sealed interface` because no state
 * carries state-specific data: the value is a pure tag that also becomes a
 * database column, an API field and an audit key. Exhaustive `when` still
 * applies, and mapping stays a single stable string.
 *
 * Most states are unreachable until later issues deliver upload, extraction,
 * parsing, reconciliation and commit. They are declared and tested now so the
 * contract does not change underneath those issues.
 */
enum class ImportBatchStatus {
    RECEIVED,
    FINGERPRINTED,
    PASSWORD_REQUIRED,
    EXTRACTING,
    LAYOUT_DETECTED,
    PARSING,
    NORMALIZING,
    RECONCILING,
    PREVIEW_READY,
    BLOCKED,
    COMMITTING,
    COMMITTED,
    REJECTED,
    FAILED,
    DUPLICATE,
    ;

    /**
     * A terminal state must not accept a normal lifecycle transition
     * (`INV-015`).
     */
    val isTerminal: Boolean
        get() = this in TERMINAL

    companion object {
        val TERMINAL: Set<ImportBatchStatus> =
            setOf(COMMITTED, REJECTED, FAILED, DUPLICATE)

        val INITIAL: ImportBatchStatus = RECEIVED
    }
}
