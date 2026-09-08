package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.portfolio.AssetResolution
import br.com.vinicius.personalfinance.shared.DomainClock
import br.com.vinicius.personalfinance.shared.ImportBatchId
import br.com.vinicius.personalfinance.shared.ValueQuality
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Assembles the proposal a person decides on.
 *
 * It composes three independent readings — normalization, asset resolution and
 * reconciliation — and adds no judgement of its own beyond severity mapping.
 * Every position that was read appears, including the ones that cannot be
 * committed: hiding a row because it has a problem would be the one behaviour
 * that makes the blocker impossible to understand.
 *
 * `FR-PREVIEW-004`: nothing here writes portfolio state.
 */
@Component
class ImportPreviewBuilder(
    private val clock: DomainClock,
) {
    fun build(
        importBatchId: ImportBatchId,
        previewVersion: Int,
        resolved: ResolvedPositionDocument,
        reconciliation: ReconciliationReport,
    ): ImportPreview {
        val document = resolved.document
        return ImportPreview(
            id = UUID.randomUUID(),
            importBatchId = importBatchId,
            previewVersion = previewVersion,
            source =
                PreviewSource(
                    descriptor = document.identity.descriptor,
                    parserId = document.identity.parserId,
                    parserVersion = document.identity.parserVersion,
                ),
            temporality =
                PreviewTemporality(
                    positionDate = exactOrNull(document.temporality.positionDate)?.value,
                    generatedAt = exactOrNull(document.temporality.generatedAt)?.value,
                    marketReferenceDate = exactOrNull(document.temporality.marketReferenceDate)?.value,
                ),
            declaredPositionTotal = document.declaredPositionTotal?.value?.let(::exactOrNull),
            toleranceId = reconciliation.toleranceId,
            positions = resolved.candidates.map(::position),
            findings = findings(resolved, reconciliation),
            createdAt = clock.now(),
        )
    }

    private fun position(entry: ResolvedPositionCandidate): PreviewPosition {
        val resolution = entry.resolution
        return PreviewPosition(
            category = entry.candidate.category,
            currency = entry.candidate.currency,
            identity = entry.candidate.identity,
            holding = entry.candidate.holding,
            gross = entry.candidate.amounts.gross.value,
            evidence = entry.candidate.amounts.gross.evidence,
            resolution =
                PreviewResolution(
                    resolved = resolution is AssetResolution.Resolved,
                    reason = (resolution as? AssetResolution.ReviewRequired)?.reason,
                    confidence = resolution.confidence,
                    fingerprint = resolution.fingerprint.value,
                ),
        )
    }

    /**
     * Findings in a stable order: reconciliation, then reading problems, then
     * identity reviews. Order is part of the artefact, because a preview that
     * reshuffles between builds is unreviewable.
     */
    private fun findings(
        resolved: ResolvedPositionDocument,
        reconciliation: ReconciliationReport,
    ): List<PreviewFinding> =
        reconciliation.results.map(::reconciliationFinding) +
            resolved.document.issues.map(::normalizationFinding) +
            resolved.reviewRequired.map(::assetFinding)

    private fun reconciliationFinding(result: ReconciliationResult): PreviewFinding =
        PreviewFinding(
            origin = PreviewFindingOrigin.RECONCILIATION,
            severity =
                when (result.outcome) {
                    ReconciliationOutcome.PASS -> PreviewSeverity.INFO
                    ReconciliationOutcome.WARNING -> PreviewSeverity.WARNING
                    ReconciliationOutcome.BLOCKER -> PreviewSeverity.BLOCKER
                    ReconciliationOutcome.NOT_EVIDENCED -> PreviewSeverity.NOT_EVIDENCED
                },
            code = result.rule.code,
            subject = result.subject,
            pageNumber = null,
            amounts = result.amounts,
        )

    /**
     * A reading problem is a warning here, not a blocker.
     *
     * The value it refers to is already unknown, so whatever depended on it
     * fails reconciliation on its own. Blocking twice for one cause would tell
     * the person to fix two things when there is one.
     */
    private fun normalizationFinding(issue: NormalizationIssue): PreviewFinding =
        PreviewFinding(
            origin = PreviewFindingOrigin.NORMALIZATION,
            severity =
                when (issue.code.severity) {
                    ErrorSeverity.INFO -> PreviewSeverity.INFO
                    else -> PreviewSeverity.WARNING
                },
            code = issue.code.name,
            subject = issue.fieldPath,
            pageNumber = issue.pageNumber,
            amounts = null,
        )

    /**
     * `FR-ASSET-005` blocks when identity is *needed* and cannot be established.
     *
     * Not every review is that case. `FR-ASSET-004` forbids automatic *merging*
     * at low confidence, and an asset with no strong identifier is not ambiguous
     * — there is simply nothing to merge it with, so it can be created as new.
     * Blocking there would mean no ordinary report ever commits: the observed
     * layout prints no strong identifier for Treasury, funds or international
     * rows, which is six of nine positions in the reference fixture.
     *
     * Genuine ambiguity does block. Two identifiers disagreeing means the
     * reading is wrong, and an undetermined type means there is no category to
     * file the asset under; both are decisions a person must make, and neither
     * is recoverable by editing a number after the fact.
     */
    private fun assetFinding(item: AssetReviewItem): PreviewFinding =
        PreviewFinding(
            origin = PreviewFindingOrigin.ASSET_REVIEW,
            severity =
                when (item.reason) {
                    AssetResolution.ReviewReason.NO_STRONG_IDENTIFIER -> PreviewSeverity.WARNING
                    else -> PreviewSeverity.BLOCKER
                },
            code = item.reason.name,
            subject = item.fieldPath,
            pageNumber = item.pageNumber,
            amounts = null,
        )
}

private fun <T : Any> exactOrNull(value: ValueQuality<T>): T? = (value as? ValueQuality.Exact)?.value
