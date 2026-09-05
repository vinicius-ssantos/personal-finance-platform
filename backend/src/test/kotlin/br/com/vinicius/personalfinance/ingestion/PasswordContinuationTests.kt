package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

/** A clock the test moves on purpose, so TTL behaviour is deterministic. */
private class MovableClock(
    private var instant: Instant,
) : DomainClock {
    override fun now(): Instant = instant

    fun advance(amount: Duration) {
        instant = instant.plus(amount)
    }
}

class PasswordContinuationTests {
    private val ttl = Duration.ofMinutes(15)

    private val policy = UploadPolicy(passwordAttemptLimit = 3, passwordTtl = ttl)

    private val clock = MovableClock(Instant.parse("2026-01-02T03:04:05Z"))

    private val registry = PasswordContinuationRegistry(policy, clock)

    private val importId = ImportBatchId.random()

    @Test
    fun `a submitted password can be consumed once`() {
        assertNull(registry.submit(importId, DocumentPassword.of("correct-horse")))

        val first = registry.consume(importId)
        val second = registry.consume(importId)

        assertTrue(first.isSuccess)
        assertTrue(second.isFailure)
        assertEquals(
            ContinuationRefusal.UNKNOWN_BATCH,
            (second.exceptionOrNull() as ContinuationUnavailable).refusal,
        )
    }

    @Test
    fun `a password is unusable after the TTL`() {
        registry.submit(importId, DocumentPassword.of("correct-horse"))

        clock.advance(ttl.plusSeconds(1))
        val result = registry.consume(importId)

        assertTrue(result.isFailure)
        assertEquals(
            ContinuationRefusal.EXPIRED,
            (result.exceptionOrNull() as ContinuationUnavailable).refusal,
        )
    }

    @Test
    fun `a password is still usable at the edge of the TTL`() {
        registry.submit(importId, DocumentPassword.of("correct-horse"))

        clock.advance(ttl)

        assertTrue(registry.consume(importId).isSuccess)
    }

    @Test
    fun `attempts are bounded`() {
        repeat(policy.passwordAttemptLimit) { attempt ->
            assertNull(
                registry.submit(importId, DocumentPassword.of("try-$attempt")),
                "attempt $attempt should be accepted",
            )
        }

        val refusal = registry.submit(importId, DocumentPassword.of("one-too-many"))

        assertEquals(ContinuationRefusal.ATTEMPTS_EXCEEDED, refusal)
        assertEquals(policy.passwordAttemptLimit, registry.attemptsUsed(importId))
    }

    @Test
    fun `a refused submission is not retained`() {
        repeat(policy.passwordAttemptLimit) { registry.submit(importId, DocumentPassword.of("x")) }
        registry.consume(importId)

        registry.submit(importId, DocumentPassword.of("after-the-limit"))

        assertTrue(registry.consume(importId).isFailure)
    }

    @Test
    fun `resubmitting replaces the previous secret instead of accumulating`() {
        registry.submit(importId, DocumentPassword.of("first"))
        registry.submit(importId, DocumentPassword.of("second"))

        val consumed = registry.consume(importId)

        assertTrue(consumed.isSuccess)
        assertEquals("second", consumed.getOrThrow().use { String(it) })
        assertTrue(registry.consume(importId).isFailure)
    }

    @Test
    fun `discarding clears everything held for the batch`() {
        registry.submit(importId, DocumentPassword.of("correct-horse"))

        registry.discard(importId)

        assertTrue(registry.consume(importId).isFailure)
        assertEquals(0, registry.attemptsUsed(importId))
    }

    @Test
    fun `expired entries are evicted so secrets do not linger`() {
        registry.submit(importId, DocumentPassword.of("correct-horse"))
        clock.advance(ttl.plusSeconds(1))

        assertEquals(1, registry.evictExpired())
        assertEquals(0, registry.evictExpired())
    }

    @Test
    fun `a password never prints itself`() {
        val password = DocumentPassword.of("correct-horse")

        assertEquals(DocumentPassword.REDACTED, password.toString())
        assertFalse("$password".contains("correct-horse"))
    }

    @Test
    fun `a cleared password cannot be used again`() {
        val password = DocumentPassword.of("correct-horse")
        password.clear()

        assertThrows(IllegalStateException::class.java) { password.use { String(it) } }
    }

    @Test
    fun `an empty password is refused at construction`() {
        assertThrows(IllegalArgumentException::class.java) { DocumentPassword.of("") }
    }
}
