package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.audit.AuditEvent
import br.com.vinicius.personalfinance.audit.AuditTrail
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Records decisions that must survive the rollback of the work that failed.
 *
 * A refused transition is audited inside the same call that then throws. Written
 * on the caller's transaction, that event would be rolled back with it and the
 * refusal would vanish from the trail — the trail would only ever show what
 * succeeded. `NFR-AUDIT-004` asks for the lifecycle decisions to be
 * reconstructable, and a refusal is a decision.
 *
 * A separate bean is required because `REQUIRES_NEW` is applied by the proxy;
 * a private method on the service would be self-invoked and silently ignored.
 */
@Component
class RefusalAuditRecorder(
    private val auditTrail: AuditTrail,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(event: AuditEvent) {
        auditTrail.record(event)
    }
}
