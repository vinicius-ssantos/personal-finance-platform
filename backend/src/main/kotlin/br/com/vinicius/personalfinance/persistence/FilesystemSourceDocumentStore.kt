package br.com.vinicius.personalfinance.persistence

import br.com.vinicius.personalfinance.ingestion.SourceDocumentStore
import br.com.vinicius.personalfinance.ingestion.StoredDocumentRef
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.time.Instant
import kotlin.io.path.deleteIfExists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

/**
 * Keeps received documents in a restricted directory under server-chosen names.
 *
 * The filename is derived from the handle alone, so nothing a caller supplies
 * ever reaches the path. That is what makes path traversal structurally
 * impossible here rather than merely filtered: there is no caller-controlled
 * string to traverse with.
 *
 * Directory permissions are tightened on POSIX. On Windows the call is skipped
 * — the platform has no POSIX bits — and the directory still lives under the
 * process-private temporary root.
 */
@Component
class FilesystemSourceDocumentStore(
    @Value("\${personal-finance.upload.directory:}") configuredDirectory: String,
) : SourceDocumentStore {
    private val root: Path = resolveRoot(configuredDirectory)

    override fun store(content: ByteArray): StoredDocumentRef {
        val ref = StoredDocumentRef.random()
        val target = pathOf(ref)
        Files.write(target, content)
        restrictFile(target)
        return ref
    }

    override fun read(ref: StoredDocumentRef): ByteArray? {
        val path = pathOf(ref)
        return if (path.isRegularFile()) Files.readAllBytes(path) else null
    }

    override fun delete(ref: StoredDocumentRef): Boolean = pathOf(ref).deleteIfExists()

    override fun purgeOlderThan(age: Duration): Int {
        val cutoff = Instant.now().minus(age)
        return root
            .listDirectoryEntries("*$SUFFIX")
            .filter { entry -> entry.isRegularFile() }
            .filter { entry -> entry.getLastModifiedTime().toInstant().isBefore(cutoff) }
            .count { entry -> runCatching { entry.deleteIfExists() }.getOrDefault(false) }
    }

    /** The handle never reveals this, and nothing outside the store calls it. */
    private fun pathOf(ref: StoredDocumentRef): Path = root.resolve("${ref.value}$SUFFIX")

    private fun restrictFile(path: Path) {
        runCatching {
            Files.setPosixFilePermissions(
                path,
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            )
        }
    }

    private fun resolveRoot(configured: String): Path {
        val path =
            if (configured.isBlank()) {
                Files.createTempDirectory("personal-finance-source")
            } else {
                Path.of(configured)
            }
        createRestrictedDirectory(path)
        return path
    }

    private fun createRestrictedDirectory(path: Path) {
        if (Files.exists(path)) return
        runCatching {
            Files.createDirectories(
                path,
                PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString(OWNER_ONLY_DIRECTORY),
                ),
            )
        }.recoverCatching {
            // Windows has no POSIX permissions; the directory still applies.
            Files.createDirectories(path)
        }.getOrElse { failure ->
            throw IOException("cannot create source document directory", failure)
        }
    }

    private companion object {
        const val SUFFIX = ".bin"

        const val OWNER_ONLY_DIRECTORY = "rwx------"
    }
}
