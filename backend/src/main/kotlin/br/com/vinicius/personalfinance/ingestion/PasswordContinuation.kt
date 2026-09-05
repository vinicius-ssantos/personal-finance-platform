package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Why a continuation could not be used. */
enum class ContinuationRefusal {
    UNKNOWN_BATCH,
    EXPIRED,
    ALREADY_USED,
    ATTEMPTS_EXCEEDED,
}

/**
 * Holds a submitted password until the extraction step consumes it, once.
 *
 * Deliberately in memory and deliberately not in the persistence module:
 * `INV-011` and `FR-PASSWORD-003` say the password is never persisted, and the
 * clearest way to guarantee that is to have no adapter capable of writing it.
 * A process restart losing the continuation is correct behaviour, not a defect
 * — `FR-PASSWORD-007` already requires a new submission after the TTL.
 *
 * Attempt counts, unlike the password, are kept per batch so the limit survives
 * repeated submissions within the process (`FR-PASSWORD-005`).
 */
@Component
class PasswordContinuationRegistry(
    private val policy: UploadPolicy,
    private val clock: DomainClock,
) {
    private data class Entry(
        val password: DocumentPassword,
        val expiresAt: Instant,
    )

    private val entries = ConcurrentHashMap<ImportBatchId, Entry>()

    private val attempts = ConcurrentHashMap<ImportBatchId, Int>()

    /**
     * Registers [password] for [importId], replacing any previous submission.
     *
     * Returns the refusal when the attempt limit is already spent, so the
     * caller can fail with a stable code instead of silently accepting.
     */
    fun submit(
        importId: ImportBatchId,
        password: DocumentPassword,
    ): ContinuationRefusal? {
        val used = attempts.getOrDefault(importId, 0)
        if (used >= policy.passwordAttemptLimit) {
            password.clear()
            return ContinuationRefusal.ATTEMPTS_EXCEEDED
        }
        attempts[importId] = used + 1
        entries
            .put(importId, Entry(password, clock.now().plus(policy.passwordTtl)))
            ?.password
            ?.clear()
        return null
    }

    /**
     * Takes the password for [importId] exactly once.
     *
     * The entry is removed before the expiry check, so an expired continuation
     * cannot be retried by racing the clock (`FR-PASSWORD-006`).
     */
    fun consume(importId: ImportBatchId): Result<DocumentPassword> {
        val entry = entries.remove(importId)
        return when {
            entry == null ->
                Result.failure(ContinuationUnavailable(ContinuationRefusal.UNKNOWN_BATCH))

            clock.now().isAfter(entry.expiresAt) -> {
                entry.password.clear()
                Result.failure(ContinuationUnavailable(ContinuationRefusal.EXPIRED))
            }

            else -> Result.success(entry.password)
        }
    }

    fun attemptsUsed(importId: ImportBatchId): Int = attempts.getOrDefault(importId, 0)

    /** Drops everything held for [importId]. Safe to call on every terminal path. */
    fun discard(importId: ImportBatchId) {
        entries.remove(importId)?.password?.clear()
        attempts.remove(importId)
    }

    /** Removes entries whose TTL has passed, so expired secrets do not linger. */
    fun evictExpired(): Int {
        val now = clock.now()
        val expired = entries.entries.filter { now.isAfter(it.value.expiresAt) }
        expired.forEach { entry ->
            entries.remove(entry.key)?.password?.clear()
        }
        return expired.size
    }
}

/** Failure carrying why a continuation could not be used. */
class ContinuationUnavailable(
    val refusal: ContinuationRefusal,
) : RuntimeException(refusal.name)
