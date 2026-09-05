package br.com.vinicius.personalfinance.audit

import br.com.vinicius.personalfinance.shared.CorrelationId
import java.time.Instant
import java.util.UUID

/** Who caused the event. Never a raw credential or a personal identifier. */
@JvmInline
value class AuditActor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "audit actor must not be blank" }
        require(value.length <= MAX_LENGTH) { "audit actor must not exceed $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH: Int = 120

        val LOCAL_OWNER: AuditActor = AuditActor("local-owner")
    }
}

/** What happened, as a stable machine-readable verb. */
@JvmInline
value class AuditAction(
    val value: String,
) {
    init {
        require(PATTERN.matches(value)) { "audit action must match ${PATTERN.pattern}" }
    }

    companion object {
        private val PATTERN = Regex("[a-z][a-z0-9]*([.][a-z][a-z0-9_]*)+")

        val IMPORT_TRANSITIONED: AuditAction = AuditAction("import.transitioned")

        val IMPORT_TRANSITION_REJECTED: AuditAction = AuditAction("import.transition_rejected")

        val IMPORT_REJECTED: AuditAction = AuditAction("import.rejected")

        val IMPORT_REJECT_REPEATED: AuditAction = AuditAction("import.reject_repeated")
    }
}

/**
 * An append-only record of one lifecycle decision.
 *
 * `INV-019` forbids editing a recorded event to "fix" history, so this type is
 * immutable and the trail exposes no update operation. `NFR-AUDIT-001` requires
 * a unique identity, carried by [id].
 *
 * [details] is redacted at construction: see [AuditRedaction]. Passwords, raw
 * document bytes and financial amounts must never reach it.
 */
data class AuditEvent(
    val id: UUID,
    val occurredAt: Instant,
    val actor: AuditActor,
    val action: AuditAction,
    val subject: String,
    val correlationId: CorrelationId,
    val details: Map<String, String>,
) {
    init {
        require(subject.isNotBlank()) { "audit subject must not be blank" }
        AuditRedaction.requireSafe(details)
    }

    companion object {
        fun of(
            id: UUID,
            occurredAt: Instant,
            actor: AuditActor,
            action: AuditAction,
            subject: String,
            correlationId: CorrelationId,
            details: Map<String, String> = emptyMap(),
        ): AuditEvent =
            AuditEvent(
                id = id,
                occurredAt = occurredAt,
                actor = actor,
                action = action,
                subject = subject,
                correlationId = correlationId,
                details = AuditRedaction.redact(details),
            )
    }
}
