package br.com.vinicius.personalfinance.portfolio

import br.com.vinicius.personalfinance.shared.CurrencyCode
import java.text.Normalizer
import java.util.Locale

/**
 * The initial asset taxonomy.
 *
 * `FR-ASSET-008`: there is no member meaning "we could not tell". [OTHER] means
 * the semantics really are other, and absence of a type is expressed by the
 * absence of a value, never by defaulting into a bucket that reads as knowledge.
 */
enum class AssetType {
    CASH,
    FIXED_INCOME,
    FUND,
    EQUITY,
    ETF,
    FOREIGN_ASSET,
    OTHER,
}

/**
 * An identifier strong enough to mean "the same asset".
 *
 * `FR-ASSET-002`: a name is never one of these. A ticker without the exchange
 * that scopes it is not one either, because the same symbol belongs to different
 * assets in different markets.
 */
sealed interface StrongIdentifier {
    data class Ticker(
        val symbol: String,
        val exchange: String,
    ) : StrongIdentifier

    /** A code stable inside one institution, such as the report asset code. */
    data class InstitutionalCode(
        val institution: String,
        val code: String,
    ) : StrongIdentifier

    data class FundTaxId(
        val taxId: String,
    ) : StrongIdentifier
}

/** How sure the resolver is that two readings denote one asset. */
enum class ResolutionConfidence {
    STRONG,
    MEDIUM,
    LOW,
    UNKNOWN,
}

/**
 * The stable key under which readings of the same asset gather.
 *
 * A fingerprint derived from a strong identifier is safe to merge on. One
 * derived from a name is not, and the resolver keeps that distinction rather
 * than letting both look alike downstream.
 */
@JvmInline
value class AssetFingerprint(
    val value: String,
) {
    companion object {
        fun of(identifier: StrongIdentifier): AssetFingerprint =
            when (identifier) {
                is StrongIdentifier.Ticker ->
                    AssetFingerprint("ticker:${identifier.exchange}:${identifier.symbol}".lowercase(Locale.ROOT))

                is StrongIdentifier.InstitutionalCode ->
                    AssetFingerprint(
                        "code:${identifier.institution}:${identifier.code}".lowercase(Locale.ROOT),
                    )

                is StrongIdentifier.FundTaxId -> AssetFingerprint("taxid:${identifier.taxId}")
            }

        /**
         * A provisional key for a reading with no strong identifier.
         *
         * It groups obvious re-readings of the same row across imports, and it is
         * deliberately not enough to merge on: `FR-ASSET-004` forbids automatic
         * merging at low confidence, so this key always travels with a review.
         */
        fun ofName(
            name: String,
            type: AssetType?,
            currency: CurrencyCode,
        ): AssetFingerprint = AssetFingerprint("name:${type?.name ?: "UNTYPED"}:$currency:${canonicalName(name)}")

        /**
         * Folds away differences that carry no meaning for comparison.
         *
         * Per the specification's text-normalization rules it may touch spacing,
         * Unicode form and casing only. It never rewrites digits, so a name
         * carrying a year or a rate keeps saying the same thing.
         */
        fun canonicalName(name: String): String =
            Normalizer
                .normalize(name, Normalizer.Form.NFKC)
                .trim()
                .replace(WHITESPACE, " ")
                .lowercase(Locale.ROOT)

        private val WHITESPACE = Regex("\\s+")
    }
}

/**
 * What one document row claims about the asset it describes.
 *
 * Produced by whoever read the document, consumed by the resolver. It is a
 * claim, not an identity: nothing here has been matched against the portfolio.
 */
data class AssetIdentityClaim(
    val nameRaw: String,
    val strongIdentifiers: List<StrongIdentifier>,
    val type: AssetType?,
    val currency: CurrencyCode,
)

/**
 * The resolver's answer.
 *
 * `FR-ASSET-003` and `FR-ASSET-005`: uncertainty survives as a first-class
 * outcome instead of being flattened into a best guess, so a preview can turn
 * [ReviewRequired] into a blocker rather than discovering the ambiguity after a
 * commit.
 */
sealed interface AssetResolution {
    val fingerprint: AssetFingerprint
    val confidence: ResolutionConfidence

    data class Resolved(
        override val fingerprint: AssetFingerprint,
        val identifier: StrongIdentifier,
        val type: AssetType,
    ) : AssetResolution {
        override val confidence: ResolutionConfidence = ResolutionConfidence.STRONG
    }

    data class ReviewRequired(
        override val fingerprint: AssetFingerprint,
        override val confidence: ResolutionConfidence,
        val reason: ReviewReason,
    ) : AssetResolution

    enum class ReviewReason {
        /** The row offered no identifier the release accepts as strong. */
        NO_STRONG_IDENTIFIER,

        /** Several strong identifiers disagreed about which asset this is. */
        CONFLICTING_IDENTIFIERS,

        /** The type could not be determined and must not be assumed. */
        UNDETERMINED_TYPE,
    }
}

/** The port ingestion calls to turn a claim into a decision. */
fun interface AssetResolver {
    fun resolve(claim: AssetIdentityClaim): AssetResolution
}
