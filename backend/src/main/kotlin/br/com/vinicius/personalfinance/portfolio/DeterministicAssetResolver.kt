package br.com.vinicius.personalfinance.portfolio

import org.springframework.stereotype.Component

/**
 * Decides asset identity from the claim alone, with no lookup and no learning.
 *
 * `NFR-PARSER-DET-001`: the same claim resolves the same way on every machine
 * and every run. Release 0.1 has no asset catalogue yet, so there is nothing to
 * match against; when issue #13 adds persistence, the stored catalogue becomes
 * an additional input and this policy stays the floor.
 *
 * The rule is conservative by construction: a merge that should not have
 * happened silently corrupts a portfolio and is expensive to unwind, while a
 * review that was not needed costs one click.
 */
@Component
class DeterministicAssetResolver : AssetResolver {
    override fun resolve(claim: AssetIdentityClaim): AssetResolution {
        val fingerprints = claim.strongIdentifiers.map(AssetFingerprint::of).distinct()
        return when {
            fingerprints.size > 1 -> conflicting(fingerprints)
            fingerprints.size == 1 -> resolved(claim, claim.strongIdentifiers.first(), fingerprints.single())
            else -> withoutStrongIdentifier(claim)
        }
    }

    /**
     * Two strong identifiers pointing at different assets means the reading is
     * wrong, and picking either one would bury that. `FR-ASSET-004` makes the
     * refusal explicit instead of tie-breaking.
     */
    private fun conflicting(fingerprints: List<AssetFingerprint>): AssetResolution =
        AssetResolution.ReviewRequired(
            fingerprint = fingerprints.sortedBy { fingerprint -> fingerprint.value }.first(),
            confidence = ResolutionConfidence.UNKNOWN,
            reason = AssetResolution.ReviewReason.CONFLICTING_IDENTIFIERS,
        )

    /**
     * A strong identifier settles which asset this is, but not what it is. An
     * undetermined type still needs a human, because `FR-ASSET-008` forbids
     * filing an asset under a category nobody established.
     */
    private fun resolved(
        claim: AssetIdentityClaim,
        identifier: StrongIdentifier,
        fingerprint: AssetFingerprint,
    ): AssetResolution =
        when (val type = claim.type) {
            null ->
                AssetResolution.ReviewRequired(
                    fingerprint = fingerprint,
                    confidence = ResolutionConfidence.MEDIUM,
                    reason = AssetResolution.ReviewReason.UNDETERMINED_TYPE,
                )

            else ->
                AssetResolution.Resolved(
                    fingerprint = fingerprint,
                    identifier = identifier,
                    type = type,
                )
        }

    /**
     * A name is not an identity (`FR-ASSET-002`), so a row identified only by
     * its description gets a provisional key and a review. Knowing the type
     * raises confidence enough to rank the review, never enough to skip it.
     */
    private fun withoutStrongIdentifier(claim: AssetIdentityClaim): AssetResolution =
        AssetResolution.ReviewRequired(
            fingerprint = AssetFingerprint.ofName(claim.nameRaw, claim.type, claim.currency),
            confidence =
                when (claim.type) {
                    null -> ResolutionConfidence.UNKNOWN
                    else -> ResolutionConfidence.LOW
                },
            reason = AssetResolution.ReviewReason.NO_STRONG_IDENTIFIER,
        )
}
