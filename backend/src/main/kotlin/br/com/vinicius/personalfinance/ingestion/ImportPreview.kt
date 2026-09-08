package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.portfolio.AssetResolution
import br.com.vinicius.personalfinance.portfolio.ResolutionConfidence
import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.ImportBatchId
import br.com.vinicius.personalfinance.shared.Money
import br.com.vinicius.personalfinance.shared.Quantity
import br.com.vinicius.personalfinance.shared.UnitPrice
import br.com.vinicius.personalfinance.shared.ValueQuality
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Where a finding came from, so a person can tell a reading problem from a sum problem. */
enum class PreviewFindingOrigin {
    RECONCILIATION,
    NORMALIZATION,
    ASSET_REVIEW,
}

enum class PreviewSeverity {
    INFO,
    WARNING,
    BLOCKER,
    NOT_EVIDENCED,
}

/**
 * One thing the person must see before deciding.
 *
 * Reconciliation, normalization and asset review all land here, because the
 * question being answered is "why can I not commit?" and splitting the answer by
 * which subsystem produced it would make that question a join.
 */
data class PreviewFinding(
    val origin: PreviewFindingOrigin,
    val severity: PreviewSeverity,
    val code: String,
    val subject: String,
    val pageNumber: Int?,
    val amounts: ReconciliationAmounts?,
) {
    val blocks: Boolean
        get() = severity == PreviewSeverity.BLOCKER
}

/** How the identity question was answered for one row. */
data class PreviewResolution(
    val resolved: Boolean,
    val reason: AssetResolution.ReviewReason?,
    val confidence: ResolutionConfidence,
    val fingerprint: String,
)

/**
 * One proposed position, with the quality and evidence that qualify it.
 *
 * [gross] may be [ValueQuality.Unknown] while [evidence] still points at the
 * token: the preview has to be able to say "the source printed something here
 * that we refused to interpret", which is exactly what stops a commit.
 */
data class PreviewPosition(
    val category: PositionCategory,
    val currency: CurrencyCode,
    val identity: PositionIdentityHints,
    val holding: PositionHolding,
    val gross: ValueQuality<Money>,
    val evidence: FieldEvidence,
    val resolution: PreviewResolution,
) {
    val quantity: Quantity?
        get() = (holding.quantity?.value as? ValueQuality.Exact)?.value

    val unitPrice: UnitPrice?
        get() = (holding.unitPrice?.value as? ValueQuality.Exact)?.value
}

/** The source identity the preview reports, per `FR-PREVIEW-003`. */
data class PreviewSource(
    val descriptor: LayoutDescriptor,
    val parserId: String,
    val parserVersion: String,
)

data class PreviewTemporality(
    val positionDate: LocalDate?,
    val generatedAt: Instant?,
    val marketReferenceDate: LocalDate?,
)

/**
 * The proposal a person confirms or rejects.
 *
 * `FR-PREVIEW-004`: building one changes no snapshot. `INV-013`: it is confirmed
 * whole or not at all, so there is no per-position selection anywhere in the
 * model — offering one would create partial states that reconciliation and
 * idempotency would then have to reason about.
 *
 * [canCommit] is derived, never stored as an independent opinion: a preview that
 * carries a blocker cannot be committable, and keeping the two in one place
 * removes any chance of them disagreeing.
 */
data class ImportPreview(
    val id: UUID,
    val importBatchId: ImportBatchId,
    val previewVersion: Int,
    val source: PreviewSource,
    val temporality: PreviewTemporality,
    val declaredPositionTotal: Money?,
    val toleranceId: String,
    val positions: List<PreviewPosition>,
    val findings: List<PreviewFinding>,
    val createdAt: Instant,
) {
    val blockers: List<PreviewFinding>
        get() = findings.filter { finding -> finding.blocks }

    val warnings: List<PreviewFinding>
        get() = findings.filter { finding -> finding.severity == PreviewSeverity.WARNING }

    val canCommit: Boolean
        get() = blockers.isEmpty()
}

/**
 * Persistence port for [ImportPreview]. The adapter lives in the persistence
 * module; `ingestion` must not depend on it, so the contract is declared here.
 */
interface ImportPreviewRepository {
    fun insert(preview: ImportPreview)

    /** The preview offered for [previewVersion], or null when that version never existed. */
    fun find(
        importBatchId: ImportBatchId,
        previewVersion: Int,
    ): ImportPreview?

    /** The version currently on offer, which is the only one a commit may confirm. */
    fun findLatest(importBatchId: ImportBatchId): ImportPreview?
}
