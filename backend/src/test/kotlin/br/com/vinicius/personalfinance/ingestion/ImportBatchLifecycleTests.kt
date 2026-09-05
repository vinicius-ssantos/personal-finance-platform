package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CorrelationId
import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class ImportBatchLifecycleTests {
    private val clock = DomainClock.fixed(Instant.parse("2026-01-02T03:04:05Z"))

    private fun newBatch(): ImportBatch =
        ImportBatch.receive(
            id = ImportBatchId.random(),
            rawSha256 = "a".repeat(64),
            correlationId = CorrelationId.random(),
            clock = clock,
        )

    private fun batchAt(target: ImportBatchStatus): ImportBatch {
        var batch = newBatch()
        val path = pathTo(target) ?: error("no path to $target")
        path.forEach { step -> batch = batch.transitionTo(step, clock) }
        return batch
    }

    /** Breadth-first walk of the real table, so tests never encode a private path. */
    private fun pathTo(target: ImportBatchStatus): List<ImportBatchStatus>? {
        if (target == ImportBatchStatus.INITIAL) return emptyList()
        val queue = ArrayDeque(listOf(listOf(ImportBatchStatus.INITIAL)))
        val seen = mutableSetOf(ImportBatchStatus.INITIAL)
        var found: List<ImportBatchStatus>? = null
        while (found == null && queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val unvisited = ImportBatchTransitions.successorsOf(path.last()).filter(seen::add)
            found = unvisited.firstOrNull { it == target }?.let { path.drop(1) + it }
            unvisited.forEach { next -> queue.addLast(path + next) }
        }
        return found
    }

    @Test
    fun `every state is reachable from the initial state`() {
        val unreachable = ImportBatchStatus.entries.filter { pathTo(it) == null }

        assertTrue(unreachable.isEmpty(), "unreachable states: $unreachable")
    }

    @Test
    fun `terminal states have no successors`() {
        ImportBatchStatus.entries.filter { it.isTerminal }.forEach { terminal ->
            assertTrue(
                ImportBatchTransitions.successorsOf(terminal).isEmpty(),
                "$terminal must not have successors",
            )
        }
    }

    @Test
    fun `terminal states reject every lifecycle transition`() {
        ImportBatchStatus.entries.filter { it.isTerminal }.forEach { terminal ->
            val batch = batchAt(terminal)
            ImportBatchStatus.entries.forEach { target ->
                val failure =
                    assertThrows(ImportLifecycleException::class.java) {
                        batch.transitionTo(target, clock)
                    }
                assertEquals(ImportErrorCode.INVALID_TRANSITION, failure.code)
            }
        }
    }

    @Test
    fun `FAILED does not return to PARSING`() {
        val failed = batchAt(ImportBatchStatus.FAILED)

        val failure =
            assertThrows(ImportLifecycleException::class.java) {
                failed.transitionTo(ImportBatchStatus.PARSING, clock)
            }

        assertEquals(ImportErrorCode.INVALID_TRANSITION, failure.code)
        assertEquals("PF_IMPORT_INVALID_TRANSITION", failure.code.code)
    }

    @Test
    fun `only PREVIEW_READY starts COMMITTING and only COMMITTING reaches COMMITTED`() {
        val startsCommitting =
            ImportBatchStatus.entries.filter {
                ImportBatchTransitions.isAllowed(it, ImportBatchStatus.COMMITTING)
            }
        val reachesCommitted =
            ImportBatchStatus.entries.filter {
                ImportBatchTransitions.isAllowed(it, ImportBatchStatus.COMMITTED)
            }

        assertEquals(listOf(ImportBatchStatus.PREVIEW_READY), startsCommitting)
        assertEquals(listOf(ImportBatchStatus.COMMITTING), reachesCommitted)
    }

    @Test
    fun `each allowed transition increments the aggregate version`() {
        val batch = newBatch()

        val moved = batch.transitionTo(ImportBatchStatus.FINGERPRINTED, clock)

        assertEquals(batch.version + 1, moved.version)
        assertEquals(ImportBatchStatus.FINGERPRINTED, moved.status)
    }

    @Test
    fun `preview publication bumps the preview version`() {
        val reconciling = batchAt(ImportBatchStatus.RECONCILING)

        val previewed = reconciling.previewReady(clock)

        assertEquals(ImportBatchStatus.PREVIEW_READY, previewed.status)
        assertEquals(reconciling.previewVersion + 1, previewed.previewVersion)
    }

    @Test
    fun `commit with a stale preview version is refused as a conflict`() {
        val previewed = batchAt(ImportBatchStatus.RECONCILING).previewReady(clock)

        val failure =
            assertThrows(ImportLifecycleException::class.java) {
                previewed.startCommit(previewed.previewVersion - 1, clock)
            }

        assertEquals(ImportErrorCode.PREVIEW_VERSION_CONFLICT, failure.code)
        assertEquals(RetryClass.REFRESH_REQUIRED, failure.code.retryClass)
    }

    @Test
    fun `commit from a non committable state is refused and never retryable`() {
        val parsing = batchAt(ImportBatchStatus.PARSING)

        val failure =
            assertThrows(ImportLifecycleException::class.java) {
                parsing.startCommit(parsing.previewVersion, clock)
            }

        assertEquals(ImportErrorCode.COMMIT_NOT_ALLOWED, failure.code)
        assertEquals(RetryClass.NEVER, failure.code.retryClass)
    }

    @Test
    fun `commit with the current preview version moves to COMMITTING`() {
        val previewed = batchAt(ImportBatchStatus.RECONCILING).previewReady(clock)

        val committing = previewed.startCommit(previewed.previewVersion, clock)

        assertEquals(ImportBatchStatus.COMMITTING, committing.status)
    }

    @Test
    fun `rejecting twice changes nothing the second time`() {
        val blocked = batchAt(ImportBatchStatus.BLOCKED)

        val first = blocked.reject(clock)
        val second = first.batch.reject(clock)

        assertTrue(first.changed)
        assertEquals(ImportBatchStatus.REJECTED, first.batch.status)
        assertFalse(second.changed)
        assertSame(first.batch, second.batch)
        assertEquals(first.batch.version, second.batch.version)
    }

    @Test
    fun `the semantic fingerprint is immutable once recorded`() {
        val fingerprinted = newBatch().fingerprintedAs("fingerprint-1", clock)

        assertEquals("fingerprint-1", fingerprinted.semanticFingerprint)
        assertThrows(IllegalArgumentException::class.java) {
            fingerprinted
                .copy(status = ImportBatchStatus.RECEIVED)
                .fingerprintedAs("fingerprint-2", clock)
        }
    }

    @Test
    fun `a raw digest that is not a hex sha256 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ImportBatch.receive(
                id = ImportBatchId.random(),
                rawSha256 = "not-a-digest",
                correlationId = CorrelationId.random(),
                clock = clock,
            )
        }
    }
}
