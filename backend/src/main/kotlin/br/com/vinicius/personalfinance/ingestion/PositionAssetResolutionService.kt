package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.portfolio.AssetIdentityClaim
import br.com.vinicius.personalfinance.portfolio.AssetResolution
import br.com.vinicius.personalfinance.portfolio.AssetResolver
import br.com.vinicius.personalfinance.portfolio.ResolutionConfidence
import org.springframework.stereotype.Service

/** One candidate with the identity decision taken for it. */
data class ResolvedPositionCandidate(
    val candidate: PositionCandidate,
    val claim: AssetIdentityClaim,
    val resolution: AssetResolution,
    val fieldPath: String,
)

/**
 * A candidate whose identity a human must settle before it can be committed.
 *
 * `FR-ASSET-005`: the preview turns these into blockers. Carrying them
 * separately from the candidates means the commit path cannot accidentally
 * proceed by ignoring a field it did not know to check.
 */
data class AssetReviewItem(
    val fieldPath: String,
    val pageNumber: Int,
    val reason: AssetResolution.ReviewReason,
    val confidence: ResolutionConfidence,
)

data class ResolvedPositionDocument(
    val document: CanonicalPositionDocument,
    val candidates: List<ResolvedPositionCandidate>,
    val reviewRequired: List<AssetReviewItem>,
)

/**
 * Attaches an identity decision to every canonical candidate.
 *
 * Creates no `Asset` and writes nothing: `INV-006` holds through this step, and
 * `FR-ASSET-006` allows a candidate to exist without an `AssetId` precisely so
 * that identity can be decided later, by a person, in the preview.
 */
@Service
class PositionAssetResolutionService(
    private val resolver: AssetResolver,
) {
    fun resolve(document: CanonicalPositionDocument): ResolvedPositionDocument {
        val resolved =
            document.sections.flatMap { section ->
                section.candidates.mapIndexed { position, candidate ->
                    val claim = InterPositionAssetClaims.of(candidate)
                    ResolvedPositionCandidate(
                        candidate = candidate,
                        claim = claim,
                        resolution = resolver.resolve(claim),
                        fieldPath = "sections[${section.sourceIndex}].candidates[$position]",
                    )
                }
            }
        return ResolvedPositionDocument(
            document = document,
            candidates = resolved,
            reviewRequired =
                resolved.mapNotNull { entry ->
                    (entry.resolution as? AssetResolution.ReviewRequired)?.let { review ->
                        AssetReviewItem(
                            fieldPath = entry.fieldPath,
                            pageNumber = entry.candidate.identity.pageNumber,
                            reason = review.reason,
                            confidence = review.confidence,
                        )
                    }
                },
        )
    }
}
