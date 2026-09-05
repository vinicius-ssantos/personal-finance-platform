package br.com.vinicius.personalfinance.ingestion

import java.time.Duration
import java.util.UUID

/**
 * Opaque handle to a stored source document.
 *
 * It is deliberately not a path. `FR-UPLOAD-005` forbids returning the real
 * filesystem path, and the surest way to honour that is for the domain never to
 * hold one: only the store can turn a handle into a location.
 */
@JvmInline
value class StoredDocumentRef(
    val value: UUID,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): StoredDocumentRef = StoredDocumentRef(UUID.randomUUID())

        fun parse(raw: String): StoredDocumentRef = StoredDocumentRef(UUID.fromString(raw))
    }
}

/**
 * Ephemeral storage for received documents.
 *
 * The source file is sensitive (`FR-UPLOAD-006`) and temporary
 * (`FR-UPLOAD-007`). The store names files itself (`FR-UPLOAD-004`), so a
 * caller-supplied name can never influence the location and path traversal has
 * no surface to attack.
 */
interface SourceDocumentStore {
    /** Stores [content] under a server-generated name and returns its handle. */
    fun store(content: ByteArray): StoredDocumentRef

    fun read(ref: StoredDocumentRef): ByteArray?

    /** Returns true when something was actually removed. */
    fun delete(ref: StoredDocumentRef): Boolean

    /**
     * Removes anything older than [age].
     *
     * `FR-UPLOAD-008`: a failure mid-ingestion must not leave orphans forever,
     * and a crash cannot be relied on to run a cleanup callback.
     */
    fun purgeOlderThan(age: Duration): Int
}
