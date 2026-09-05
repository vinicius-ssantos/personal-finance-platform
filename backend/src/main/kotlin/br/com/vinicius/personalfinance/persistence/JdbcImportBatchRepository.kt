package br.com.vinicius.personalfinance.persistence

import br.com.vinicius.personalfinance.ingestion.ImportBatch
import br.com.vinicius.personalfinance.ingestion.ImportBatchRepository
import br.com.vinicius.personalfinance.ingestion.ImportBatchStatus
import br.com.vinicius.personalfinance.ingestion.ParserMetadata
import br.com.vinicius.personalfinance.ingestion.StoredDocumentRef
import br.com.vinicius.personalfinance.shared.CorrelationId
import br.com.vinicius.personalfinance.shared.ImportBatchId
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

@Repository
class JdbcImportBatchRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : ImportBatchRepository {
    override fun insert(batch: ImportBatch) {
        jdbc.update(
            """
            INSERT INTO import_batch (
                id, status, version, preview_version, raw_sha256,
                semantic_fingerprint, parser_id, parser_version,
                correlation_id, created_at, updated_at, stored_document_ref
            ) VALUES (
                :id, :status, :version, :previewVersion, :rawSha256,
                :semanticFingerprint, :parserId, :parserVersion,
                :correlationId, :createdAt, :updatedAt, :storedDocumentRef
            )
            """.trimIndent(),
            parametersOf(batch),
        )
    }

    override fun findById(id: ImportBatchId): ImportBatch? =
        jdbc
            .query(
                "SELECT * FROM import_batch WHERE id = :id",
                MapSqlParameterSource("id", id.value),
            ) { rs, _ -> mapRow(rs) }
            .firstOrNull()

    /**
     * One conditional statement, so concurrency is decided by the database.
     *
     * The row moves only if it still carries [expectedVersion]; the losing
     * writer updates zero rows and is told so. No explicit row lock is taken,
     * which keeps the write a single round trip and avoids lock contention.
     */
    override fun updateIfUnchanged(
        batch: ImportBatch,
        expectedVersion: Long,
    ): Boolean {
        val affected =
            jdbc.update(
                """
                UPDATE import_batch SET
                    status = :status,
                    version = :version,
                    preview_version = :previewVersion,
                    semantic_fingerprint = :semanticFingerprint,
                    parser_id = :parserId,
                    parser_version = :parserVersion,
                    stored_document_ref = :storedDocumentRef,
                    updated_at = :updatedAt
                WHERE id = :id AND version = :expectedVersion
                """.trimIndent(),
                parametersOf(batch).addValue("expectedVersion", expectedVersion),
            )
        return affected == 1
    }

    private fun parametersOf(batch: ImportBatch): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", batch.id.value)
            .addValue("status", batch.status.name)
            .addValue("version", batch.version)
            .addValue("previewVersion", batch.previewVersion)
            .addValue("rawSha256", batch.rawSha256)
            .addValue("semanticFingerprint", batch.semanticFingerprint)
            .addValue("parserId", batch.parser?.parserId)
            .addValue("parserVersion", batch.parser?.parserVersion)
            .addValue("storedDocumentRef", batch.storedDocumentRef?.value)
            .addValue("correlationId", batch.correlationId.value)
            .addValue("createdAt", Timestamp.from(batch.createdAt))
            .addValue("updatedAt", Timestamp.from(batch.updatedAt))

    private fun mapRow(rs: ResultSet): ImportBatch {
        val parserId: String? = rs.getString("parser_id")
        val parserVersion: String? = rs.getString("parser_version")
        return ImportBatch(
            id = ImportBatchId(rs.getObject("id", UUID::class.java)),
            status = ImportBatchStatus.valueOf(rs.getString("status")),
            version = rs.getLong("version"),
            previewVersion = rs.getInt("preview_version"),
            rawSha256 = rs.getString("raw_sha256"),
            semanticFingerprint = rs.getString("semantic_fingerprint"),
            parser =
                if (parserId != null && parserVersion != null) {
                    ParserMetadata(parserId, parserVersion)
                } else {
                    null
                },
            storedDocumentRef =
                rs
                    .getObject("stored_document_ref", UUID::class.java)
                    ?.let { StoredDocumentRef(it) },
            correlationId = CorrelationId(rs.getObject("correlation_id", UUID::class.java)),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
        )
    }
}
