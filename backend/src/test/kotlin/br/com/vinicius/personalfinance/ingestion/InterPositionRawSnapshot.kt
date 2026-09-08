package br.com.vinicius.personalfinance.ingestion

internal fun renderRawSnapshot(document: InterPositionSourceDocument): String =
    buildString {
        appendLine(
            listOf(
                "document",
                "${document.descriptor.institution}/${document.descriptor.documentFamily}@" +
                    document.descriptor.layoutVersion,
                document.parserId,
                document.parserVersion,
                document.declaredPositionTotalCurrencyToken ?: "<none>",
                document.declaredPositionTotal.snapshot(),
            ).joinToString("|"),
        )
        document.sections.forEach { section -> appendSection(section) }
    }.trimEnd()

private fun StringBuilder.appendSection(section: InterPositionRawSection) {
    when (section) {
        is SummaryRawSection -> appendSummary(section)
        is TreasuryRawSection -> appendTreasury(section)
        is BrazilianEquityRawSection -> appendEquity("BRAZILIAN_EQUITY", section)
        is InternationalEquityRawSection -> appendEquity("INTERNATIONAL_EQUITY", section)
        is FixedIncomeRawSection -> appendFixedIncome(section)
        is FundRawSection -> appendFunds(section)
        is UnknownRawSection -> appendUnknown(section)
    }
}

private fun StringBuilder.appendSummary(section: SummaryRawSection) {
    appendLine(
        "section|SUMMARY|p${section.pageNumber}|${section.headingRaw}|" +
            "${section.totalCurrencyToken}|${section.total.snapshot()}",
    )
    section.allocations.forEach { allocation ->
        appendLine(
            "summary|${allocation.labelRaw}|${allocation.currencyToken}|${allocation.amount.snapshot()}",
        )
    }
}

private fun StringBuilder.appendTreasury(section: TreasuryRawSection) {
    appendLine(
        "section|TREASURY|p${section.pageNumber}|${section.headingRaw}|" +
            "${section.currencyToken}|${section.declaredGross.snapshot()}",
    )
    section.records.forEach { record ->
        appendLine(
            listOf(
                "treasury",
                record.descriptionRaw,
                record.applicationDate.snapshot(),
                record.maturityDate.snapshot(),
                record.quantity.snapshot(),
                record.appliedValue.snapshot(),
                record.grossValue.snapshot(),
            ).joinToString("|"),
        )
    }
}

private fun StringBuilder.appendEquity(
    type: String,
    section: InterPositionRawSection,
) {
    val data =
        when (section) {
            is BrazilianEquityRawSection -> Triple(section.currencyToken, section.declaredGross, section.records)
            is InternationalEquityRawSection -> Triple(section.currencyToken, section.declaredGross, section.records)
            else -> error("not an equity section")
        }
    appendLine(
        "section|$type|p${section.pageNumber}|${section.headingRaw}|${data.first}|${data.second.snapshot()}",
    )
    data.third.forEach { record ->
        appendLine("equity|${record.assetCodeRaw}|${record.quantity.snapshot()}|${record.grossValue.snapshot()}")
    }
}

private fun StringBuilder.appendFixedIncome(section: FixedIncomeRawSection) {
    appendLine(
        "section|FIXED_INCOME|p${section.pageNumber}|${section.headingRaw}|" +
            "${section.currencyToken}|${section.declaredGross.snapshot()}|${section.marketReferenceDateRaw}",
    )
    section.records.forEach { record ->
        appendLine(
            listOf(
                "fixed",
                record.descriptionRaw,
                record.assetCodeRaw,
                record.maturityDate.snapshot(),
                record.applicationDate.snapshot(),
                record.rate.snapshot(),
                record.indexerRaw,
                record.appliedValue.snapshot(),
                record.expectedIof.snapshot(),
                record.expectedIr.snapshot(),
                record.grossValue.snapshot(),
                record.marketValue.snapshot(),
                record.netValue.snapshot(),
            ).joinToString("|"),
        )
    }
    appendLine(
        "subtotal|${section.subtotal.appliedValue.snapshot()}|" +
            "${section.subtotal.grossValue.snapshot()}|${section.subtotal.netValue.snapshot()}",
    )
}

private fun StringBuilder.appendFunds(section: FundRawSection) {
    appendLine(
        "section|FUNDS|p${section.pageNumber}|${section.headingRaw}|" +
            "${section.currencyToken}|${section.declaredGross.snapshot()}",
    )
    section.records.forEach { record ->
        appendLine(
            listOf(
                "fund",
                record.descriptionRaw,
                record.quantity.snapshot(),
                record.marketPrice.snapshot(),
                record.appliedValue.snapshot(),
                record.redemptionAvailability.snapshot(),
                record.grossValue.snapshot(),
                record.netValue.snapshot(),
                record.iofValue.snapshot(),
                record.irValue.snapshot(),
            ).joinToString("|"),
        )
    }
}

private fun StringBuilder.appendUnknown(section: UnknownRawSection) {
    appendLine(
        "section|UNKNOWN|p${section.pageNumber}|${section.headingRaw}|${section.record.heading.snapshot()}",
    )
    section.record.lines.forEach { line -> appendLine("unknown|p${line.pageNumber}|${line.raw}") }
}

private fun SourceField?.snapshot(): String =
    this?.let { field ->
        "${field.tokenRaw}~${field.raw ?: "<absent>"}~p${field.pageNumber}"
    } ?: "<none>"
