package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CurrencyCode

internal fun fixedIncomeSection(
    section: FixedIncomeRawSection,
    index: Int,
    issues: IssueCollector,
): CanonicalSection {
    val path = "sections[$index]"
    val currency = currencyOf(section.currencyToken)
    return CanonicalSection(
        category = PositionCategory.FIXED_INCOME,
        currency = currency,
        pageNumber = section.pageNumber,
        declaredGross = issues.money(section.declaredGross, "$path.declaredGross", currency),
        declaredSubtotal = fixedIncomeSubtotal(section.subtotal, currency, "$path.subtotal", issues),
        candidates =
            section.records.mapIndexed { position, record ->
                fixedIncomeCandidate(record, currency, "$path.candidates[$position]", issues)
            },
    )
}

/**
 * The printed subtotal stays separate from the section total and from the sum of
 * the rows. `FR-RECON-008` exists because a global total can hide an internal
 * mismatch, so the three numbers are carried side by side and compared later.
 */
private fun fixedIncomeSubtotal(
    subtotal: FixedIncomeSubtotalRaw,
    currency: CurrencyCode,
    path: String,
    issues: IssueCollector,
): FixedIncomeSubtotal =
    FixedIncomeSubtotal(
        applied = issues.money(subtotal.appliedValue, "$path.applied", currency),
        gross = issues.money(subtotal.grossValue, "$path.gross", currency),
        net = issues.money(subtotal.netValue, "$path.net", currency),
    )

private fun fixedIncomeCandidate(
    record: FixedIncomeRawRecord,
    currency: CurrencyCode,
    path: String,
    issues: IssueCollector,
): PositionCandidate =
    PositionCandidate(
        category = PositionCategory.FIXED_INCOME,
        currency = currency,
        identity =
            PositionIdentityHints(
                descriptionRaw = record.descriptionRaw,
                assetCodeRaw = record.assetCodeRaw,
                indexerRaw = record.indexerRaw,
                pageNumber = record.grossValue.pageNumber,
            ),
        holding = PositionHolding(quantity = null, unitPrice = null),
        amounts =
            PositionAmounts(
                gross = issues.money(record.grossValue, "$path.gross", currency),
                applied = issues.money(record.appliedValue, "$path.applied", currency),
                net = issues.money(record.netValue, "$path.net", currency),
                market = issues.money(record.marketValue, "$path.market", currency),
                redemptionAvailability = null,
                taxes =
                    PositionTaxes(
                        expectedIof = issues.money(record.expectedIof, "$path.expectedIof", currency),
                        expectedIr = issues.money(record.expectedIr, "$path.expectedIr", currency),
                    ),
            ),
        schedule =
            PositionSchedule(
                applicationDate =
                    issues.readRequired(record.applicationDate, "$path.applicationDate", PtBrTokens::date),
                maturityDate =
                    issues.readRequired(record.maturityDate, "$path.maturityDate", PtBrTokens::date),
                rate = issues.readRequired(record.rate, "$path.rate", PtBrTokens::rate),
            ),
    )

internal fun fundSection(
    section: FundRawSection,
    index: Int,
    issues: IssueCollector,
): CanonicalSection {
    val path = "sections[$index]"
    val currency = currencyOf(section.currencyToken)
    return CanonicalSection(
        category = PositionCategory.FUNDS,
        currency = currency,
        pageNumber = section.pageNumber,
        declaredGross = issues.money(section.declaredGross, "$path.declaredGross", currency),
        declaredSubtotal = null,
        candidates =
            section.records.mapIndexed { position, record ->
                fundCandidate(record, currency, "$path.candidates[$position]", issues)
            },
    )
}

private fun fundCandidate(
    record: FundRawRecord,
    currency: CurrencyCode,
    path: String,
    issues: IssueCollector,
): PositionCandidate =
    PositionCandidate(
        category = PositionCategory.FUNDS,
        currency = currency,
        identity =
            PositionIdentityHints(
                descriptionRaw = record.descriptionRaw,
                assetCodeRaw = null,
                indexerRaw = null,
                pageNumber = record.grossValue.pageNumber,
            ),
        holding =
            PositionHolding(
                quantity = issues.readRequired(record.quantity, "$path.quantity", PtBrTokens::quantity),
                unitPrice =
                    issues.readRequired(record.marketPrice, "$path.unitPrice") { token ->
                        PtBrTokens.unitPrice(token, currency)
                    },
            ),
        amounts =
            PositionAmounts(
                gross = issues.money(record.grossValue, "$path.gross", currency),
                applied = issues.money(record.appliedValue, "$path.applied", currency),
                net = issues.money(record.netValue, "$path.net", currency),
                market = null,
                redemptionAvailability =
                    issues.money(record.redemptionAvailability, "$path.redemptionAvailability", currency),
                taxes =
                    PositionTaxes(
                        expectedIof = issues.money(record.iofValue, "$path.expectedIof", currency),
                        expectedIr = issues.money(record.irValue, "$path.expectedIr", currency),
                    ),
            ),
        schedule = null,
    )
