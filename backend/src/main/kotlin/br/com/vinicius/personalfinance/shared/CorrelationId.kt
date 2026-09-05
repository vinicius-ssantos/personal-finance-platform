package br.com.vinicius.personalfinance.shared

import java.util.UUID

/**
 * Correlates every artefact produced while handling one request: lifecycle
 * transitions, audit events and error responses (`NFR-OBS-001`, `NFR-OBS-004`).
 *
 * It is an opaque identifier on purpose. It must never carry user input,
 * financial values or anything else that would leak through logs.
 */
@JvmInline
value class CorrelationId(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): CorrelationId = CorrelationId(UUID.randomUUID())

        fun parse(raw: String): CorrelationId = CorrelationId(UUID.fromString(raw))
    }
}
