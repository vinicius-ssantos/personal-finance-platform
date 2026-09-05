package br.com.vinicius.personalfinance.persistence

import br.com.vinicius.personalfinance.audit.AuditEvent
import br.com.vinicius.personalfinance.audit.AuditTrail
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp

/**
 * Append-only adapter for the audit trail.
 *
 * There is no update or delete statement in this class by design: `INV-019`
 * makes a recorded event immutable, and a correction is a new event.
 */
@Repository
class JdbcAuditTrail(
    private val jdbc: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) : AuditTrail {
    override fun record(event: AuditEvent) {
        jdbc.update(
            """
            INSERT INTO audit_event (
                id, occurred_at, actor, action, subject, correlation_id, details
            ) VALUES (
                :id, :occurredAt, :actor, :action, :subject, :correlationId, CAST(:details AS jsonb)
            )
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", event.id)
                .addValue("occurredAt", Timestamp.from(event.occurredAt))
                .addValue("actor", event.actor.value)
                .addValue("action", event.action.value)
                .addValue("subject", event.subject)
                .addValue("correlationId", event.correlationId.value)
                .addValue("details", objectMapper.writeValueAsString(event.details)),
        )
    }
}
