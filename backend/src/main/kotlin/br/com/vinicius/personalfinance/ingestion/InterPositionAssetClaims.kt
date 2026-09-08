package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.portfolio.AssetIdentityClaim
import br.com.vinicius.personalfinance.portfolio.AssetType
import br.com.vinicius.personalfinance.portfolio.StrongIdentifier

/**
 * Turns what the Banco Inter position layout prints into an identity claim.
 *
 * This is layout knowledge, so it lives in ingestion: which column carries a
 * code, which market a ticker belongs to, and which sections say enough about an
 * asset to type it. The decision that follows from the claim belongs to the
 * portfolio resolver, which knows nothing about Banco Inter.
 */
internal object InterPositionAssetClaims {
    /**
     * B3 is not read from the document; it follows from the layout itself. The
     * `Renda Variável` section of a Brazilian broker's position report lists
     * domestic listed assets, so the exchange is a property of the versioned
     * layout in the same way its column headers are.
     */
    private const val DOMESTIC_EXCHANGE = "B3"

    private const val INSTITUTION = "BANCO_INTER"

    /**
     * B3 suffixes whose asset class cannot be told apart from the ticker alone:
     * units, ETFs and real-estate funds all end in 11.
     */
    private val AMBIGUOUS_DOMESTIC_SUFFIX = Regex("11$")

    fun of(candidate: PositionCandidate): AssetIdentityClaim =
        AssetIdentityClaim(
            nameRaw = candidate.identity.descriptionRaw,
            strongIdentifiers = strongIdentifiers(candidate),
            type = typeOf(candidate),
            currency = candidate.currency,
        )

    private fun strongIdentifiers(candidate: PositionCandidate): List<StrongIdentifier> {
        val code = candidate.identity.assetCodeRaw
        return when {
            code == null -> emptyList()

            candidate.category == PositionCategory.BRAZILIAN_EQUITY ->
                listOf(StrongIdentifier.Ticker(code, DOMESTIC_EXCHANGE))

            candidate.category == PositionCategory.FIXED_INCOME ->
                listOf(StrongIdentifier.InstitutionalCode(INSTITUTION, code))

            /*
             * The international section prints a bare symbol and never names the
             * market it trades on. The same symbol denotes different assets on
             * different exchanges, so an unscoped ticker is not a strong
             * identifier (`FR-ASSET-002`) and the row goes to review instead.
             */
            else -> emptyList()
        }
    }

    /**
     * `FR-ASSET-008`: a type is returned only where the section establishes it.
     *
     * Treasury and the fixed-income section are unambiguously fixed income, and
     * the funds section is unambiguously a fund. A domestic ticker ending in 11
     * is not: it may be a unit, an ETF or a real-estate fund, and choosing one
     * would file the asset under a category nobody established.
     */
    private fun typeOf(candidate: PositionCandidate): AssetType? =
        when (candidate.category) {
            PositionCategory.TREASURY -> AssetType.FIXED_INCOME
            PositionCategory.FIXED_INCOME -> AssetType.FIXED_INCOME
            PositionCategory.FUNDS -> AssetType.FUND
            PositionCategory.INTERNATIONAL_EQUITY -> AssetType.FOREIGN_ASSET
            PositionCategory.BRAZILIAN_EQUITY -> domesticEquityType(candidate.identity.assetCodeRaw)
        }

    private fun domesticEquityType(code: String?): AssetType? =
        when {
            code == null -> null
            AMBIGUOUS_DOMESTIC_SUFFIX.containsMatchIn(code) -> null
            else -> AssetType.EQUITY
        }
}
