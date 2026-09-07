package br.com.vinicius.personalfinance.ingestion

internal fun parseSummarySection(block: List<SourceLine>): SummaryRawSection {
    val heading = block.first()
    val totalMatch =
        block.mapNotNull { line -> SUMMARY_TOTAL.matchEntire(line.raw)?.let { match -> match to line } }
            .firstOrNull()
            ?: parserFailure("summary total is missing")
    val (currency, amount) = totalMatch.first.destructured
    val allocations =
        block.mapNotNull { source ->
            SUMMARY_ALLOCATION.matchEntire(source.raw)?.let { allocation ->
                val (label, allocationCurrency, allocationAmount) = allocation.destructured
                SummaryAllocationRawRecord(
                    labelRaw = label,
                    currencyToken = allocationCurrency,
                    amount = SourceField.present("$allocationCurrency $allocationAmount", source.pageNumber),
                )
            }
        }
    if (allocations.isEmpty()) parserFailure("summary allocations are missing")
    return SummaryRawSection(
        pageNumber = heading.pageNumber,
        headingRaw = heading.raw,
        totalCurrencyToken = currency,
        total = SourceField.present("$currency $amount", totalMatch.second.pageNumber),
        allocations = allocations,
    )
}

internal fun parseTreasurySection(block: List<SourceLine>): TreasuryRawSection {
    val heading = block.first()
    val headingValue = parseSectionHeadingValue(heading, "Tesouro Direto")
    val records =
        block.indices
            .filter { index -> block[index].raw == TREASURY_HEADER }
            .map { index -> parseTreasuryRecord(block, index) }
    if (records.isEmpty()) parserFailure("Treasury records are missing")
    return TreasuryRawSection(
        pageNumber = heading.pageNumber,
        headingRaw = heading.raw,
        currencyToken = headingValue.first,
        declaredGross = headingValue.second,
        records = records,
    )
}

private fun parseTreasuryRecord(block: List<SourceLine>, headerIndex: Int): TreasuryRawRecord {
    val description = block.getOrNull(headerIndex - 1) ?: parserFailure("Treasury description is missing")
    val row = block.getOrNull(headerIndex + 1) ?: parserFailure("Treasury row is missing")
    val match = TREASURY_ROW.matchEntire(row.raw) ?: parserFailure("Treasury row changed structure: ${row.raw}")
    val fields = match.destructured.toList().iterator()
    val applicationDate = fields.next()
    val maturityDate = fields.next()
    val quantity = fields.next()
    val appliedValue = fields.next()
    val grossValue = fields.next()
    return TreasuryRawRecord(
        descriptionRaw = description.raw,
        applicationDate = SourceField.present(applicationDate, row.pageNumber),
        maturityDate = SourceField.present(maturityDate, row.pageNumber),
        quantity = SourceField.present(quantity, row.pageNumber),
        appliedValue = SourceField.present(appliedValue, row.pageNumber),
        grossValue = SourceField.present(grossValue, row.pageNumber),
    )
}

internal fun parseBrazilianEquitySection(block: List<SourceLine>): BrazilianEquityRawSection {
    val heading = block.first()
    val headingValue = parseSectionHeadingValue(heading, "Renda Variável")
    return BrazilianEquityRawSection(
        pageNumber = heading.pageNumber,
        headingRaw = heading.raw,
        currencyToken = headingValue.first,
        declaredGross = headingValue.second,
        records = parseEquityRecords(block, BRAZILIAN_EQUITY_HEADER, BRAZILIAN_EQUITY_ROW),
    )
}

internal fun parseInternationalEquitySection(block: List<SourceLine>): InternationalEquityRawSection {
    val heading = block.first()
    val headingValue = parseSectionHeadingValue(heading, "Renda Variável Internacional")
    return InternationalEquityRawSection(
        pageNumber = heading.pageNumber,
        headingRaw = heading.raw,
        currencyToken = headingValue.first,
        declaredGross = headingValue.second,
        records = parseEquityRecords(block, INTERNATIONAL_EQUITY_HEADER, INTERNATIONAL_EQUITY_ROW),
    )
}

private fun parseEquityRecords(
    block: List<SourceLine>,
    header: String,
    rowRegex: Regex,
): List<EquityRawRecord> {
    val records =
        block.indices
            .filter { index -> block[index].raw == header }
            .map { index ->
                val asset = block.getOrNull(index - 1) ?: parserFailure("equity asset code is missing")
                val row = block.getOrNull(index + 1) ?: parserFailure("equity row is missing")
                val match = rowRegex.matchEntire(row.raw) ?: parserFailure("equity row changed structure: ${row.raw}")
                val (quantity, grossValue) = match.destructured
                EquityRawRecord(
                    assetCodeRaw = asset.raw,
                    quantity = SourceField.present(quantity, row.pageNumber),
                    grossValue = SourceField.present(grossValue, row.pageNumber),
                )
            }
    if (records.isEmpty()) parserFailure("equity records are missing")
    return records
}

internal fun parseUnknownSection(block: List<SourceLine>): UnknownRawSection {
    val heading = block.first()
    return UnknownRawSection(
        pageNumber = heading.pageNumber,
        headingRaw = heading.raw,
        record =
            UnknownRawRecord(
                heading = SourceField.present(heading.raw, heading.pageNumber),
                lines = block.drop(1),
            ),
    )
}

internal fun parseSectionHeadingValue(
    heading: SourceLine,
    expectedLabel: String,
): Pair<String, SourceField> {
    val match =
        SECTION_HEADING.matchEntire(heading.raw)
            ?: parserFailure("section heading changed structure: ${heading.raw}")
    val (label, currency, amount) = match.destructured
    if (label != expectedLabel) parserFailure("expected $expectedLabel section, got $label")
    return currency to SourceField.present("$currency $amount", heading.pageNumber)
}
