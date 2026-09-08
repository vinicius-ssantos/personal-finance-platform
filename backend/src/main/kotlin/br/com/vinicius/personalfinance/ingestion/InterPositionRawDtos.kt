package br.com.vinicius.personalfinance.ingestion

object InterPositionLayout202407 {
    val descriptor: LayoutDescriptor =
        LayoutDescriptor(
            institution = "BANCO_INTER",
            documentFamily = "POSITION_CONSOLIDATED",
            layoutVersion = "2024_07",
        )

    const val PARSER_ID: String = "banco-inter-position"
    const val PARSER_VERSION: String = "2024_07.1"
}

data class SourceLine(
    val pageNumber: Int,
    val raw: String,
)

data class SourceField(
    val tokenRaw: String,
    val raw: String?,
    val pageNumber: Int,
) {
    companion object {
        fun present(
            tokenRaw: String,
            pageNumber: Int,
        ): SourceField = SourceField(tokenRaw = tokenRaw, raw = tokenRaw, pageNumber = pageNumber)

        fun fromToken(
            tokenRaw: String,
            pageNumber: Int,
        ): SourceField =
            if (tokenRaw == "-") {
                SourceField(tokenRaw = tokenRaw, raw = null, pageNumber = pageNumber)
            } else {
                present(tokenRaw, pageNumber)
            }
    }
}

enum class InterPositionSectionType {
    SUMMARY,
    TREASURY,
    BRAZILIAN_EQUITY,
    FIXED_INCOME,
    INTERNATIONAL_EQUITY,
    FUNDS,
    UNKNOWN,
}

sealed interface InterPositionRawSection {
    val type: InterPositionSectionType
    val pageNumber: Int
    val headingRaw: String
}

data class SummaryAllocationRawRecord(
    val labelRaw: String,
    val currencyToken: String,
    val amount: SourceField,
)

data class SummaryRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val totalCurrencyToken: String,
    val total: SourceField,
    val allocations: List<SummaryAllocationRawRecord>,
) : InterPositionRawSection {
    override val type = InterPositionSectionType.SUMMARY
}

data class TreasuryRawRecord(
    val descriptionRaw: String,
    val applicationDate: SourceField,
    val maturityDate: SourceField,
    val quantity: SourceField,
    val appliedValue: SourceField,
    val grossValue: SourceField,
)

data class TreasuryRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val currencyToken: String,
    val declaredGross: SourceField,
    val records: List<TreasuryRawRecord>,
) : InterPositionRawSection {
    override val type = InterPositionSectionType.TREASURY
}

data class EquityRawRecord(
    val assetCodeRaw: String,
    val quantity: SourceField,
    val grossValue: SourceField,
)

data class BrazilianEquityRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val currencyToken: String,
    val declaredGross: SourceField,
    val records: List<EquityRawRecord>,
) : InterPositionRawSection {
    override val type = InterPositionSectionType.BRAZILIAN_EQUITY
}

data class InternationalEquityRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val currencyToken: String,
    val declaredGross: SourceField,
    val records: List<EquityRawRecord>,
) : InterPositionRawSection {
    override val type = InterPositionSectionType.INTERNATIONAL_EQUITY
}

data class FixedIncomeRawRecord(
    val descriptionRaw: String,
    val assetCodeRaw: String,
    val maturityDate: SourceField,
    val applicationDate: SourceField,
    val rate: SourceField,
    val indexerRaw: String,
    val appliedValue: SourceField,
    val expectedIof: SourceField,
    val expectedIr: SourceField,
    val grossValue: SourceField,
    val marketValue: SourceField,
    val netValue: SourceField,
)

data class FixedIncomeSubtotalRaw(
    val appliedValue: SourceField,
    val grossValue: SourceField,
    val netValue: SourceField,
)

data class FixedIncomeRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val currencyToken: String,
    val declaredGross: SourceField,
    val marketReferenceDateRaw: String,
    val records: List<FixedIncomeRawRecord>,
    val subtotal: FixedIncomeSubtotalRaw,
) : InterPositionRawSection {
    override val type = InterPositionSectionType.FIXED_INCOME
}

data class FundRawRecord(
    val descriptionRaw: String,
    val quantity: SourceField,
    val marketPrice: SourceField,
    val appliedValue: SourceField,
    val redemptionAvailability: SourceField,
    val grossValue: SourceField,
    val netValue: SourceField,
    val iofValue: SourceField,
    val irValue: SourceField,
)

data class FundRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val currencyToken: String,
    val declaredGross: SourceField,
    val records: List<FundRawRecord>,
) : InterPositionRawSection {
    override val type = InterPositionSectionType.FUNDS
}

data class UnknownRawRecord(
    val heading: SourceField,
    val lines: List<SourceLine>,
)

data class UnknownRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val record: UnknownRawRecord,
) : InterPositionRawSection {
    override val type = InterPositionSectionType.UNKNOWN
}

data class InterPositionSourceDocument(
    val descriptor: LayoutDescriptor,
    val parserId: String,
    val parserVersion: String,
    val declaredPositionTotalCurrencyToken: String?,
    val declaredPositionTotal: SourceField?,
    val sections: List<InterPositionRawSection>,
) : ParsedSourceDocument

class InterPositionParserException(
    message: String,
) : IllegalArgumentException(message)
