package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.audit.AuditActor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestConstructor
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Testcontainers
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DocumentIntakeTests(
    private val intake: DocumentIntakeService,
    private val store: SourceDocumentStore,
    private val repository: ImportBatchRepository,
    private val jdbcTemplate: JdbcTemplate,
) {
    private fun plainPdf(marker: String): ByteArray =
        "%PDF-1.7\n1 0 obj << /Title ($marker) >> endobj\n%%EOF".toByteArray(Charsets.ISO_8859_1)

    private fun encryptedPdf(): ByteArray =
        "%PDF-1.7\ntrailer << /Encrypt 9 0 R >>\n%%EOF".toByteArray(Charsets.ISO_8859_1)

    @Test
    fun `a plain document is stored fingerprinted and left ready for extraction`() {
        val content = plainPdf("plain")

        val batch = intake.receive(content, AuditActor.LOCAL_OWNER)

        assertEquals(ImportBatchStatus.FINGERPRINTED, batch.status)
        assertEquals(SourceFingerprint.of(content), batch.rawSha256)
        assertNotNull(batch.storedDocumentRef)
        assertTrue(content.contentEquals(store.read(batch.storedDocumentRef!!)))
    }

    @Test
    fun `an encrypted document waits for a password`() {
        val batch = intake.receive(encryptedPdf(), AuditActor.LOCAL_OWNER)

        assertEquals(ImportBatchStatus.PASSWORD_REQUIRED, batch.status)
    }

    @Test
    fun `the fingerprint survives a reload from the database`() {
        val content = plainPdf("durable")
        val batch = intake.receive(content, AuditActor.LOCAL_OWNER)

        val reloaded = repository.findById(batch.id)

        assertEquals(SourceFingerprint.of(content), reloaded?.rawSha256)
        assertEquals(batch.storedDocumentRef, reloaded?.storedDocumentRef)
    }

    @Test
    fun `rejected content is never written to storage`() {
        // Purging with a zero window empties the store, so anything counted
        // afterwards can only have been written by the rejected upload.
        store.purgeOlderThan(Duration.ZERO)

        assertThrows(ImportLifecycleException::class.java) {
            intake.receive("MZ not a pdf at all".toByteArray(), AuditActor.LOCAL_OWNER)
        }

        assertEquals(
            0,
            store.purgeOlderThan(Duration.ZERO),
            "a rejected upload must leave nothing behind",
        )
    }

    @Test
    fun `releasing ephemeral state removes the stored bytes`() {
        val batch = intake.receive(plainPdf("released"), AuditActor.LOCAL_OWNER)

        val removed = intake.releaseEphemeralState(batch)

        assertTrue(removed)
        assertNull(store.read(batch.storedDocumentRef!!))
    }

    @Test
    fun `the sweeper removes files older than the retention window`() {
        intake.receive(plainPdf("sweepable"), AuditActor.LOCAL_OWNER)

        val purged = store.purgeOlderThan(Duration.ZERO)

        assertTrue(purged >= 1, "the sweeper must remove aged files")
    }

    @Test
    fun `the handle is opaque and carries no filesystem path`() {
        val batch = intake.receive(plainPdf("opaque"), AuditActor.LOCAL_OWNER)

        val handle = batch.storedDocumentRef.toString()

        assertFalse(handle.contains("/"), "handle must not look like a path: $handle")
        assertFalse(handle.contains("\\"), "handle must not look like a path: $handle")
        assertFalse(handle.contains(".."), "handle must not permit traversal: $handle")
        assertTrue(
            Regex("^[0-9a-f-]{36}$").matches(handle),
            "handle must be an opaque identifier: $handle",
        )
    }

    @Test
    fun `a submitted password reaches neither the batch nor the audit trail`() {
        val batch = intake.receive(encryptedPdf(), AuditActor.LOCAL_OWNER)
        val secret = "correct-horse-battery-staple"

        intake.submitPassword(batch.id, DocumentPassword.of(secret))

        assertEquals(0, rowsMentioning("import_batch", secret))
        assertEquals(0, rowsMentioning("audit_event", secret))
    }

    @Test
    fun `exceeding the attempt limit fails with the stable code`() {
        val batch = intake.receive(encryptedPdf(), AuditActor.LOCAL_OWNER)
        repeat(UploadPolicy.DEFAULT_PASSWORD_ATTEMPT_LIMIT) {
            intake.submitPassword(batch.id, DocumentPassword.of("wrong-$it"))
        }

        val failure =
            assertThrows(ImportLifecycleException::class.java) {
                intake.submitPassword(batch.id, DocumentPassword.of("one-too-many"))
            }

        assertEquals(ImportErrorCode.PDF_PASSWORD_ATTEMPTS_EXCEEDED, failure.code)
        assertFalse(
            failure.message.orEmpty().contains("one-too-many"),
            "the failure must not echo the password",
        )
    }

    private fun rowsMentioning(
        table: String,
        needle: String,
    ): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from $table t where cast(t as text) like ?",
            Int::class.java,
            "%$needle%",
        ) ?: 0

    private companion object {
        private const val POSTGRES_IMAGE = "postgres:18.1-alpine"

        @Container
        @ServiceConnection
        @JvmField
        val postgres =
            PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName("personal_finance_test")
                .withUsername("personal_finance_test")
                .withPassword("test-only")
    }
}
