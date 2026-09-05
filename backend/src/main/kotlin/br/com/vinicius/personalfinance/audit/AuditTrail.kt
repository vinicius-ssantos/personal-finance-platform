package br.com.vinicius.personalfinance.audit

/**
 * Append-only port for the audit trail.
 *
 * There is intentionally no update or delete operation: `INV-019` states that a
 * recorded event must not be altered to "correct" history. A correction is a new
 * event, never an edit.
 *
 * The persistence adapter lives outside this module; `audit` may only depend on
 * `shared`, so the port is declared here and implemented by an adapter.
 */
fun interface AuditTrail {
    /** Records [event]. Implementations must reject a duplicate [AuditEvent.id]. */
    fun record(event: AuditEvent)
}
