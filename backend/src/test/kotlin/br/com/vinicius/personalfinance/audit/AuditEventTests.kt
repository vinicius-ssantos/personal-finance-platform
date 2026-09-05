package br.com.vinicius.personalfinance.audit

import br.com.vinicius.personalfinance.shared.CorrelationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AuditEventTests {
    private fun eventWith(details: Map<String, String>): AuditEvent =
        AuditEvent.of(
            id = UUID.randomUUID(),
            occurredAt = Instant.parse("2026-01-02T03:04:05Z"),
            actor = AuditActor.LOCAL_OWNER,
            action = AuditAction.IMPORT_TRANSITIONED,
            subject = "import-1",
            correlationId = CorrelationId.random(),
            details = details,
        )

    @Test
    fun `passwords never reach the trail whatever they are called`() {
        val event =
            eventWith(
                mapOf(
                    "password" to "hunter2",
                    "senha" to "hunter2",
                    "pdf_password" to "hunter2",
                    "Authorization" to "Bearer abc",
                ),
            )

        event.details.values.forEach { value ->
            assertEquals(AuditRedaction.PLACEHOLDER, value)
        }
    }

    @Test
    fun `financial values never reach the trail`() {
        val event = eventWith(mapOf("amount" to "1234", "saldo" to "99", "total" to "5"))

        event.details.values.forEach { value ->
            assertEquals(AuditRedaction.PLACEHOLDER, value)
        }
    }

    @Test
    fun `non sensitive context survives so the trail stays useful`() {
        val event = eventWith(mapOf("from" to "PARSING", "to" to "NORMALIZING"))

        assertEquals("PARSING", event.details["from"])
        assertEquals("NORMALIZING", event.details["to"])
    }

    @Test
    fun `the direct constructor refuses an unredacted secret`() {
        assertThrows(IllegalArgumentException::class.java) {
            AuditEvent(
                id = UUID.randomUUID(),
                occurredAt = Instant.EPOCH,
                actor = AuditActor.LOCAL_OWNER,
                action = AuditAction.IMPORT_TRANSITIONED,
                subject = "import-1",
                correlationId = CorrelationId.random(),
                details = mapOf("password" to "hunter2"),
            )
        }
    }

    @Test
    fun `oversized values are truncated instead of stored whole`() {
        val event = eventWith(mapOf("note" to "x".repeat(AuditRedaction.MAX_VALUE_LENGTH + 100)))

        assertEquals(AuditRedaction.MAX_VALUE_LENGTH, event.details.getValue("note").length)
    }

    @Test
    fun `the trail exposes no way to edit history`() {
        val mutators =
            AuditTrail::class.java.methods
                .map { it.name }
                .filter { it.startsWith("update") || it.startsWith("delete") || it.startsWith("set") }

        assertTrue(mutators.isEmpty(), "AuditTrail must stay append-only, found: $mutators")
    }

    @Test
    fun `an action must be a stable machine readable verb`() {
        assertThrows(IllegalArgumentException::class.java) { AuditAction("Import Transitioned") }
        assertThrows(IllegalArgumentException::class.java) { AuditAction("import") }
    }

    @Test
    fun `an actor must be present and bounded`() {
        assertThrows(IllegalArgumentException::class.java) { AuditActor(" ") }
        assertThrows(IllegalArgumentException::class.java) {
            AuditActor("a".repeat(AuditActor.MAX_LENGTH + 1))
        }
    }

    @Test
    fun `redaction recognises decorated key spellings`() {
        assertTrue(AuditRedaction.isSensitive("PDF-Password"))
        assertTrue(AuditRedaction.isSensitive("api_key"))
        assertFalse(AuditRedaction.isSensitive("status"))
    }
}
