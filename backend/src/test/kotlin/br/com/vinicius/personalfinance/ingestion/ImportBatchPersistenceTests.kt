package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.audit.AuditActor
import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Testcontainers
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ImportBatchPersistenceTests(
    private val service: ImportBatchLifecycleService,
    private val repository: ImportBatchRepository,
    private val jdbcTemplate: JdbcTemplate,
) {
    private fun digestOf(seed: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(seed.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun auditRowsFor(id: ImportBatchId): Long =
        jdbcTemplate.queryForObject(
            "select count(*) from audit_event where subject = ?",
            Long::class.java,
            id.toString(),
        ) ?: 0L

    private fun receiveBatch(seed: String): ImportBatch =
        service.receive(
            id = ImportBatchId.random(),
            rawSha256 = digestOf(seed),
            actor = AuditActor.LOCAL_OWNER,
        )

    private fun driveToPreviewReady(seed: String): ImportBatch {
        var batch = receiveBatch(seed)
        listOf(
            ImportBatchStatus.FINGERPRINTED,
            ImportBatchStatus.EXTRACTING,
            ImportBatchStatus.LAYOUT_DETECTED,
            ImportBatchStatus.PARSING,
            ImportBatchStatus.NORMALIZING,
            ImportBatchStatus.RECONCILING,
            ImportBatchStatus.PREVIEW_READY,
        ).forEach { target ->
            batch = service.transition(batch.id, target, AuditActor.LOCAL_OWNER)
        }
        return batch
    }

    @Test
    fun `a batch round-trips through PostgreSQL`() {
        val received = receiveBatch("round-trip")

        val loaded = repository.findById(received.id)

        assertNotNull(loaded)
        assertEquals(received.status, loaded?.status)
        assertEquals(received.rawSha256, loaded?.rawSha256)
        assertEquals(received.correlationId, loaded?.correlationId)
    }

    @Test
    fun `every transition writes exactly one audit event`() {
        val batch = receiveBatch("audit-count")
        val afterReceive = auditRowsFor(batch.id)

        service.transition(batch.id, ImportBatchStatus.FINGERPRINTED, AuditActor.LOCAL_OWNER)

        assertEquals(afterReceive + 1, auditRowsFor(batch.id))
    }

    @Test
    fun `a refused transition is itself audited`() {
        val batch = receiveBatch("audit-refusal")
        val before = auditRowsFor(batch.id)

        runCatching {
            service.transition(batch.id, ImportBatchStatus.COMMITTED, AuditActor.LOCAL_OWNER)
        }

        assertEquals(before + 1, auditRowsFor(batch.id))
    }

    @Test
    fun `repeating a reject writes no second audit event`() {
        val batch = driveToPreviewReady("reject-idempotent")
        service.reject(batch.id, AuditActor.LOCAL_OWNER)
        val afterFirstReject = auditRowsFor(batch.id)

        val second = service.reject(batch.id, AuditActor.LOCAL_OWNER)

        assertEquals(ImportBatchStatus.REJECTED, second.status)
        assertEquals(afterFirstReject, auditRowsFor(batch.id))
    }

    @Test
    fun `a conditional update loses against a stale expected version`() {
        val batch = receiveBatch("conditional-update")
        val moved = batch.transitionTo(ImportBatchStatus.FINGERPRINTED, DOMAIN_CLOCK)

        val firstWriter = repository.updateIfUnchanged(moved, batch.version)
        val secondWriter = repository.updateIfUnchanged(moved, batch.version)

        assertTrue(firstWriter, "the first writer must win")
        assertTrue(!secondWriter, "the second writer must lose against a stale version")
    }

    @Test
    fun `concurrent commits result in exactly one effective commit`() {
        val batch = driveToPreviewReady("concurrent-commit")
        val threads = 8
        val barrier = CyclicBarrier(threads)
        val pool = Executors.newFixedThreadPool(threads)

        val results =
            try {
                pool
                    .invokeAll(
                        List(threads) {
                            Callable {
                                barrier.await(10, TimeUnit.SECONDS)
                                runCatching {
                                    service.startCommit(
                                        id = batch.id,
                                        confirmedPreviewVersion = batch.previewVersion,
                                        actor = AuditActor.LOCAL_OWNER,
                                    )
                                }
                            }
                        },
                    ).map { it.get(30, TimeUnit.SECONDS) }
            } finally {
                pool.shutdownNow()
            }

        val succeeded = results.count { it.isSuccess }
        assertEquals(1, succeeded, "exactly one commit must become effective")
        assertEquals(
            ImportBatchStatus.COMMITTING,
            repository.findById(batch.id)?.status,
        )
        results.filter { it.isFailure }.forEach { failure ->
            val cause = failure.exceptionOrNull()
            assertTrue(
                cause is ImportLifecycleException,
                "losers must fail with a stable lifecycle code, got: $cause",
            )
        }
    }

    private companion object {
        val DOMAIN_CLOCK: DomainClock = DomainClock.systemUtc()

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
