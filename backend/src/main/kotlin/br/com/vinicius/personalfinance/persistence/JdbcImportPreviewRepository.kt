package br.com.vinicius.personalfinance.persistence

import br.com.vinicius.personalfinance.ingestion.EvidencedValue
import br.com.vinicius.personalfinance.ingestion.FieldEvidence
import br.com.vinicius.personalfinance.ingestion.ImportPreview
import br.com.vinicius.personalfinance.ingestion.ImportPreviewRepository
import br.com.vinicius.personalfinance.ingestion.LayoutDescriptor
import br.com.vinicius.personalfinance.ingestion.PositionCategory
import br.com.vinicius.personalfinance.ingestion.PositionHolding
import br.com.vinicius.personalfinance.ingestion.PositionIdentityHints
import br.com.vinicius.personalfinance.ingestion.PreviewFinding
import br.com.vinicius.personalfinance.ingestion.PreviewFindingOrigin
import br.com.vinicius.personalfinance.ingestion.PreviewPosition
import br.com.vinicius.personalfinance.ingestion.PreviewResolution
import br.com.vinicius.personalfinance.ingestion.PreviewSeverity
import br.com.vinicius.personalfinance.ingestion.PreviewSource
import br.com.vinicius.personalfinance.ingestion.PreviewTemporality
import br.com.vinicius.personalfinance.ingestion.ReconciliationAmounts
import br.com.vinicius.personalfinance.portfolio.AssetResolution
import br.com.vinicius.personalfinance.portfolio.ResolutionConfidence
import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.ImportBatchId
import br.com.vinicius.personalfinance.shared.Money
import br.com.vinicius.personalfinance.shared.Quantity
import br.com.vinicius.personalfinance.shared.UnitPrice
import br.com.vinicius.personalfinance.shared.ValueQuality
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Stores a preview as three tables rather than one document.
 *
 * Money stays in BIGINT minor units with an explicit currency, so the schema's
 * CHECK constraints can defend `INV-003` and ADR 0005 instead of trusting the
 * application to have serialised a blob correctly.
 */
@Repository
class JdbcImportPreviewRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) : ImportPreviewRepository {
    /**
     * One transaction: a preview with only some of its positions would be a
     * proposal nobody agreed to show.
     */
    @Transactional
    override fun insert(preview: ImportPreview) {
        jdbc.update(INSERT_PREVIEW, headerParameters(preview))
        preview.positions.forEachIndexed { ordinal, position ->
            jdbc.update(INSERT_POSITION, positionParameters(preview.id, ordinal, position))
        }
        preview.findings.forEachIndexed { ordinal, finding ->
            jdbc.update(INSERT_FINDING, findingParameters(preview.id, ordinal, finding))
        }
    }

    override fun find(
        importBatchId: ImportBatchId,
        previewVersion: Int,
    ): ImportPreview? =
        loadHeader(
            "SELECT * FROM import_preview WHERE import_batch_id = :id AND preview_version = :version",
            MapSqlParameterSource()
                .addValue("id", importBatchId.value)
                .addValue("version", previewVersion),
        )

    override fun findLatest(importBatchId: ImportBatchId): ImportPreview? =
        loadHeader(
            """
            SELECT * FROM import_preview
            WHERE import_batch_id = :id
            ORDER BY preview_version DESC
            LIMIT 1
            """.trimIndent(),
            MapSqlParameterSource("id", importBatchId.value),
        )

    private fun loadHeader(
        sql: String,
        parameters: MapSqlParameterSource,
    ): ImportPreview? =
        jdbc
            .query(sql, parameters) { rs, _ -> headerOf(rs) }
            .firstOrNull()
            ?.let { header ->
                header.copy(
                    positions = positionsOf(header.id),
                    findings = findingsOf(header.id),
                )
            }

    private fun positionsOf(previewId: UUID): List<PreviewPosition> =
        jdbc.query(
            "SELECT * FROM import_preview_position WHERE preview_id = :id ORDER BY ordinal",
            MapSqlParameterSource("id", previewId),
        ) { rs, _ -> positionOf(rs) }

    private fun findingsOf(previewId: UUID): List<PreviewFinding> =
        jdbc.query(
            "SELECT * FROM import_preview_finding WHERE preview_id = :id ORDER BY ordinal",
            MapSqlParameterSource("id", previewId),
        ) { rs, _ -> findingOf(rs) }

    private fun headerOf(rs: ResultSet): ImportPreview =
        ImportPreview(
            id = rs.getObject("id", UUID::class.java),
            importBatchId = ImportBatchId(rs.getObject("import_batch_id", UUID::class.java)),
            previewVersion = rs.getInt("preview_version"),
            source =
                PreviewSource(
                    descriptor =
                        LayoutDescriptor(
                            institution = rs.getString("institution"),
                            documentFamily = rs.getString("document_family"),
                            layoutVersion = rs.getString("layout_version"),
                        ),
                    parserId = rs.getString("parser_id"),
                    parserVersion = rs.getString("parser_version"),
                ),
            temporality =
                PreviewTemporality(
                    positionDate = rs.getDate("position_date")?.toLocalDate(),
                    generatedAt = rs.getTimestamp("generated_at")?.toInstant(),
                    marketReferenceDate = rs.getDate("market_reference_date")?.toLocalDate(),
                ),
            declaredPositionTotal =
                rs.getString("declared_total_ccy")?.let { currency ->
                    Money.ofMinor(rs.getLong("declared_total_minor"), CurrencyCode.valueOf(currency))
                },
            toleranceId = rs.getString("tolerance_id"),
            positions = emptyList(),
            findings = emptyList(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )

    private fun positionOf(rs: ResultSet): PreviewPosition {
        val currency = CurrencyCode.valueOf(rs.getString("currency"))
        val evidence = FieldEvidence(rs.getInt("page_number"), rs.getString("evidence_token"))
        return PreviewPosition(
            category = PositionCategory.valueOf(rs.getString("category")),
            currency = currency,
            identity =
                PositionIdentityHints(
                    descriptionRaw = rs.getString("description_raw"),
                    assetCodeRaw = rs.getString("asset_code_raw"),
                    indexerRaw = null,
                    pageNumber = rs.getInt("page_number"),
                ),
            holding =
                PositionHolding(
                    quantity = evidencedDecimal(rs, "quantity", evidence) { value -> Quantity.of(value) },
                    unitPrice = evidencedDecimal(rs, "unit_price", evidence) { value -> UnitPrice.of(value) },
                ),
            gross = grossOf(rs, currency),
            evidence = evidence,
            resolution =
                PreviewResolution(
                    resolved = rs.getString("resolution") == "RESOLVED",
                    reason =
                        rs.getString("resolution_reason")?.let { reason ->
                            AssetResolution.ReviewReason.valueOf(reason)
                        },
                    confidence = ResolutionConfidence.valueOf(rs.getString("confidence")),
                    fingerprint = rs.getString("fingerprint"),
                ),
        )
    }

    private fun findingOf(rs: ResultSet): PreviewFinding {
        val currency = rs.getString("currency")?.let(CurrencyCode::valueOf)
        return PreviewFinding(
            origin = PreviewFindingOrigin.valueOf(rs.getString("origin")),
            severity = PreviewSeverity.valueOf(rs.getString("severity")),
            code = rs.getString("code"),
            subject = rs.getString("subject"),
            pageNumber = rs.getObject("page_number") as Int?,
            amounts =
                currency?.let {
                    ReconciliationAmounts(
                        declared = nullableMoney(rs, "declared_minor", currency),
                        calculated = nullableMoney(rs, "calculated_minor", currency),
                        differenceMinor = rs.getObject("difference_minor") as Long?,
                        toleranceMinor = rs.getLong("tolerance_minor"),
                        toleranceId = "",
                    )
                },
        )
    }

    private companion object {
        const val INSERT_PREVIEW = """
            INSERT INTO import_preview (
                id, import_batch_id, preview_version, can_commit,
                institution, document_family, layout_version, parser_id, parser_version,
                position_date, generated_at, market_reference_date,
                declared_total_minor, declared_total_ccy, tolerance_id, created_at
            ) VALUES (
                :id, :importBatchId, :previewVersion, :canCommit,
                :institution, :documentFamily, :layoutVersion, :parserId, :parserVersion,
                :positionDate, :generatedAt, :marketReferenceDate,
                :declaredTotalMinor, :declaredTotalCcy, :toleranceId, :createdAt
            )
        """

        const val INSERT_POSITION = """
            INSERT INTO import_preview_position (
                id, preview_id, ordinal, category, currency, description_raw, asset_code_raw,
                quantity, unit_price, gross_minor, gross_quality, page_number, evidence_token,
                resolution, resolution_reason, confidence, fingerprint
            ) VALUES (
                :id, :previewId, :ordinal, :category, :currency, :descriptionRaw, :assetCodeRaw,
                :quantity, :unitPrice, :grossMinor, :grossQuality, :pageNumber, :evidenceToken,
                :resolution, :resolutionReason, :confidence, :fingerprint
            )
        """

        const val INSERT_FINDING = """
            INSERT INTO import_preview_finding (
                id, preview_id, ordinal, origin, severity, code, subject, page_number,
                currency, declared_minor, calculated_minor, difference_minor, tolerance_minor
            ) VALUES (
                :id, :previewId, :ordinal, :origin, :severity, :code, :subject, :pageNumber,
                :currency, :declaredMinor, :calculatedMinor, :differenceMinor, :toleranceMinor
            )
        """
    }
}

private fun moneyOf(value: ValueQuality<Money>): Money? =
    when (value) {
        is ValueQuality.Exact -> value.value
        is ValueQuality.Estimated -> value.value
        ValueQuality.Unknown -> null
    }

private fun qualityNameOf(value: ValueQuality<Money>): String =
    when (value) {
        is ValueQuality.Exact -> "EXACT"
        is ValueQuality.Estimated -> "ESTIMATED"
        ValueQuality.Unknown -> "UNKNOWN"
    }

private fun grossOf(
    rs: ResultSet,
    currency: CurrencyCode,
): ValueQuality<Money> =
    when (rs.getString("gross_quality")) {
        "EXACT" -> ValueQuality.Exact(Money.ofMinor(rs.getLong("gross_minor"), currency))
        "ESTIMATED" -> ValueQuality.Estimated(Money.ofMinor(rs.getLong("gross_minor"), currency))
        else -> ValueQuality.Unknown
    }

private fun nullableMoney(
    rs: ResultSet,
    column: String,
    currency: CurrencyCode,
): Money? = (rs.getObject(column) as Long?)?.let { minor -> Money.ofMinor(minor, currency) }

private fun <T : Any> evidencedDecimal(
    rs: ResultSet,
    column: String,
    evidence: FieldEvidence,
    build: (BigDecimal) -> T,
): EvidencedValue<T>? =
    rs.getBigDecimal(column)?.let { value ->
        EvidencedValue(ValueQuality.Exact(build(value)), evidence)
    }

private fun headerParameters(preview: ImportPreview): MapSqlParameterSource =
    MapSqlParameterSource()
        .addValue("id", preview.id)
        .addValue("importBatchId", preview.importBatchId.value)
        .addValue("previewVersion", preview.previewVersion)
        .addValue("canCommit", preview.canCommit)
        .addValue("institution", preview.source.descriptor.institution)
        .addValue("documentFamily", preview.source.descriptor.documentFamily)
        .addValue("layoutVersion", preview.source.descriptor.layoutVersion)
        .addValue("parserId", preview.source.parserId)
        .addValue("parserVersion", preview.source.parserVersion)
        .addValue("positionDate", preview.temporality.positionDate)
        .addValue("generatedAt", preview.temporality.generatedAt?.let(Timestamp::from))
        .addValue("marketReferenceDate", preview.temporality.marketReferenceDate)
        .addValue("declaredTotalMinor", preview.declaredPositionTotal?.amountMinor)
        .addValue("declaredTotalCcy", preview.declaredPositionTotal?.currency?.name)
        .addValue("toleranceId", preview.toleranceId)
        .addValue("createdAt", Timestamp.from(preview.createdAt))

private fun positionParameters(
    previewId: UUID,
    ordinal: Int,
    position: PreviewPosition,
): MapSqlParameterSource =
    MapSqlParameterSource()
        .addValue("id", UUID.randomUUID())
        .addValue("previewId", previewId)
        .addValue("ordinal", ordinal)
        .addValue("category", position.category.name)
        .addValue("currency", position.currency.name)
        .addValue("descriptionRaw", position.identity.descriptionRaw)
        .addValue("assetCodeRaw", position.identity.assetCodeRaw)
        .addValue("quantity", position.quantity?.value)
        .addValue("unitPrice", position.unitPrice?.value)
        .addValue("grossMinor", moneyOf(position.gross)?.amountMinor)
        .addValue("grossQuality", qualityNameOf(position.gross))
        .addValue("pageNumber", position.evidence.pageNumber)
        .addValue("evidenceToken", position.evidence.tokenRaw)
        .addValue("resolution", if (position.resolution.resolved) "RESOLVED" else "REVIEW_REQUIRED")
        .addValue("resolutionReason", position.resolution.reason?.name)
        .addValue("confidence", position.resolution.confidence.name)
        .addValue("fingerprint", position.resolution.fingerprint)

private fun findingParameters(
    previewId: UUID,
    ordinal: Int,
    finding: PreviewFinding,
): MapSqlParameterSource =
    MapSqlParameterSource()
        .addValue("id", UUID.randomUUID())
        .addValue("previewId", previewId)
        .addValue("ordinal", ordinal)
        .addValue("origin", finding.origin.name)
        .addValue("severity", finding.severity.name)
        .addValue("code", finding.code)
        .addValue("subject", finding.subject)
        .addValue("pageNumber", finding.pageNumber)
        .addValue(
            "currency",
            finding.amounts
                ?.declared
                ?.currency
                ?.name,
        ).addValue("declaredMinor", finding.amounts?.declared?.amountMinor)
        .addValue("calculatedMinor", finding.amounts?.calculated?.amountMinor)
        .addValue("differenceMinor", finding.amounts?.differenceMinor)
        .addValue("toleranceMinor", finding.amounts?.toleranceMinor)
