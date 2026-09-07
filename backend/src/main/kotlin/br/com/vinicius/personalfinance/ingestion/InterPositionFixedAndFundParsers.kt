package br.com.vinicius.personalfinance.ingestion

private const val FIXED_INCOME_ROW_OFFSET = 3

private data class ParsedFixedIncomeRecord(
    val record: FixedIncomeRawRecord,
    val marketReferenceDateRaw: String,
)

internal fun parseFixedIncomeSection(block: List<SourceLine>): FixedIncomeRawSection {
    val heading = block.first()
    val headingValue = parseSectionHeadingValue(heading, "Renda Fixa")
    val parsedRecords =
        block.indices
            .filter { index -> block[index].raw == FIXED_INCOME_HEADER_1 }
            .map { index -> parseFixedIncomeRecord(block, index) }
    if (parsedRecords.isEmpty()) parserFailure("fixed-income records are missing")

    val referenceDates = parsedRecords.map { parsed -> parsed.marketReferenceDateRaw }.distinct()
    if (referenceDates.size != 1) parserFailure("fixed-income market reference dates disagree")

    return FixedIncomeRawSection(
        pageNumber = heading.pageNumber,
        headingRaw = heading.raw,
        currencyToken = headingValue.first,
        declaredGross = headingValue.second,
        marketReferenceDateRaw = referenceDates.single(),
        records = parsedRecords.map { parsed -> parsed.record },
        subtotal = parseFixedIncomeSubtotal(block),
    )
}

private fun parseFixedIncomeRecord(block: List<SourceLine>, headerIndex: Int): ParsedFixedIncomeRecord {
    val description = block.getOrNull(headerIndex - 1) ?: parserFailure("fixed-income description is missing")
    val secondHeader = block.getOrNull(headerIndex + 1) ?: parserFailure("fixed-income second header is missing")
    val marketHeader = block.getOrNull(headerIndex + 2) ?: parserFailure("fixed-income market header is missing")
    val row = block.getOrNull(headerIndex + FIXED_INCOME_ROW_OFFSET) ?: parserFailure("fixed-income row is missing")
    if (secondHeader.raw != FIXED_INCOME_HEADER_2) parserFailure("fixed-income second header changed structure")

    val marketMatch =
        FIXED_INCOME_HEADER_3.matchEntire(marketHeader.raw)
            ?: parserFailure("fixed-income market header changed structure")
    val (marketReferenceDate) = marketMatch.destructured
    val rowMatch =
        FIXED_INCOME_ROW.matchEntire(row.raw)
            ?: parserFailure("fixed-income row changed structure: ${row.raw}")
    val (
        assetCode,
        maturityDate,
        applicationDate,
        rate,
        indexer,
        appliedValue,
        expectedIof,
        expectedIr,
        grossValue,
        marketValue,
        netValue,
    ) = rowMatch.destructured

    return ParsedFixedIncomeRecord(
        record =
            FixedIncomeRawRecord(
                descriptionRaw = description.raw,
                assetCodeRaw = assetCode,
                maturityDate = SourceField.present(maturityDate, row.pageNumber),
                applicationDate = SourceField.present(applicationDate, row.pageNumber),
                rate = SourceField.present(rate, row.pageNumber),
                indexerRaw = indexer,
                appliedValue = SourceField.present(appliedValue, row.pageNumber),
                expectedIof = SourceField.fromToken(expectedIof, row.pageNumber),
                expectedIr = SourceField.fromToken(expectedIr, row.pageNumber),
                grossValue = SourceField.present(grossValue, row.pageNumber),
                marketValue = SourceField.fromToken(marketValue, row.pageNumber),
                netValue = SourceField.present(netValue, row.pageNumber),
            ),
        marketReferenceDateRaw = marketReferenceDate,
    )
}

private fun parseFixedIncomeSubtotal(block: List<SourceLine>): FixedIncomeSubtotalRaw {
    val line =
        block.singleOrNull { source -> source.raw.startsWith("Subtotal ") }
            ?: parserFailure("fixed-income subtotal is missing or ambiguous")
    val match =
        FIXED_INCOME_SUBTOTAL.matchEntire(line.raw)
            ?: parserFailure("fixed-income subtotal changed structure")
    val (appliedValue, grossValue, netValue) = match.destructured
    return FixedIncomeSubtotalRaw(
        appliedValue = SourceField.present(appliedValue, line.pageNumber),
        grossValue = SourceField.present(grossValue, line.pageNumber),
        netValue = SourceField.present(netValue, line.pageNumber),
    )
}

internal fun parseFundSection(block: List<SourceLine>): FundRawSection {
    val heading = block.first()
    val headingValue = parseSectionHeadingValue(heading, "Fundos de Investimentos")
    val records =
        block.indices
            .filter { index -> block[index].raw == FUNDS_HEADER_1 }
            .map { index -> parseFundRecord(block, index) }
    if (records.isEmpty()) parserFailure("fund records are missing")
    return FundRawSection(
        pageNumber = heading.pageNumber,
        headingRaw = heading.raw,
        currencyToken = headingValue.first,
        declaredGross = headingValue.second,
        records = records,
    )
}

private fun parseFundRecord(block: List<SourceLine>, headerIndex: Int): FundRawRecord {
    val description = block.getOrNull(headerIndex - 1) ?: parserFailure("fund description is missing")
    val secondHeader = block.getOrNull(headerIndex + 1) ?: parserFailure("fund second header is missing")
    val row = block.getOrNull(headerIndex + 2) ?: parserFailure("fund row is missing")
    if (secondHeader.raw != FUNDS_HEADER_2) parserFailure("fund second header changed structure")
    val match = FUNDS_ROW.matchEntire(row.raw) ?: parserFailure("fund row changed structure: ${row.raw}")
    val (
        quantity,
        marketPrice,
        appliedValue,
        redemptionAvailability,
        grossValue,
        netValue,
        iofValue,
        irValue,
    ) = match.destructured
    return FundRawRecord(
        descriptionRaw = description.raw,
        quantity = SourceField.present(quantity, row.pageNumber),
        marketPrice = SourceField.present(marketPrice, row.pageNumber),
        appliedValue = SourceField.present(appliedValue, row.pageNumber),
        redemptionAvailability = SourceField.fromToken(redemptionAvailability, row.pageNumber),
        grossValue = SourceField.present(grossValue, row.pageNumber),
        netValue = SourceField.present(netValue, row.pageNumber),
        iofValue = SourceField.fromToken(iofValue, row.pageNumber),
        irValue = SourceField.present(irValue, row.pageNumber),
    )
}
