package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CurrencyCode

internal fun BrazilianEquityRawSection.asView(): EquitySectionView =
    EquitySectionView(pageNumber, currencyToken, declaredGross, records)

internal fun InternationalEquityRawSection.asView(): EquitySectionView =
    EquitySectionView(pageNumber, currencyToken, declaredGross, records)

internal fun treasurySection(
    section: TreasuryRawSection,
    index: Int,
    issues: IssueCollector,
): CanonicalSection {
    val path = "sections[$index]"
    val currency = currencyOf(section.currencyToken)
    return CanonicalSection(
        category = PositionCategory.TREASURY,
        currency = currency,
        sourceIndex = index,
        pageNumber = section.pageNumber,
        declaredGross = issues.money(section.declaredGross, "$path.declaredGross", currency),
        declaredSubtotal = null,
        candidates =
            section.records.mapIndexed { position, record ->
                treasuryCandidate(record, currency, "$path.candidates[$position]", issues)
            },
    )
}

private fun treasuryCandidate(
    record: TreasuryRawRecord,
    currency: CurrencyCode,
    path: String,
    issues: IssueCollector,
): PositionCandidate =
    PositionCandidate(
        category = PositionCategory.TREASURY,
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
                unitPrice = null,
            ),
        amounts =
            PositionAmounts(
                gross = issues.money(record.grossValue, "$path.gross", currency),
                applied = issues.money(record.appliedValue, "$path.applied", currency),
                net = null,
                market = null,
                redemptionAvailability = null,
                taxes = null,
            ),
        schedule =
            PositionSchedule(
                applicationDate =
                    issues.readRequired(record.applicationDate, "$path.applicationDate", PtBrTokens::date),
                maturityDate =
                    issues.readRequired(record.maturityDate, "$path.maturityDate", PtBrTokens::date),
                rate = null,
            ),
    )

internal fun equitySection(
    category: PositionCategory,
    section: EquitySectionView,
    index: Int,
    issues: IssueCollector,
): CanonicalSection {
    val path = "sections[$index]"
    val currency = currencyOf(section.currencyToken)
    return CanonicalSection(
        category = category,
        currency = currency,
        sourceIndex = index,
        pageNumber = section.pageNumber,
        declaredGross = issues.money(section.declaredGross, "$path.declaredGross", currency),
        declaredSubtotal = null,
        candidates =
            section.records.mapIndexed { position, record ->
                equityCandidate(category, record, currency, "$path.candidates[$position]", issues)
            },
    )
}

/**
 * The equity block prints a single token that serves as both the label and the
 * institutional code. It is recorded in both roles rather than being promoted to
 * an identity here: whether that token is a strong identifier is a decision for
 * asset resolution, under `FR-ASSET-001` and `FR-ASSET-002`.
 */
private fun equityCandidate(
    category: PositionCategory,
    record: EquityRawRecord,
    currency: CurrencyCode,
    path: String,
    issues: IssueCollector,
): PositionCandidate =
    PositionCandidate(
        category = category,
        currency = currency,
        identity =
            PositionIdentityHints(
                descriptionRaw = record.assetCodeRaw,
                assetCodeRaw = record.assetCodeRaw,
                indexerRaw = null,
                pageNumber = record.grossValue.pageNumber,
            ),
        holding =
            PositionHolding(
                quantity = issues.readRequired(record.quantity, "$path.quantity", PtBrTokens::quantity),
                unitPrice = null,
            ),
        amounts =
            PositionAmounts(
                gross = issues.money(record.grossValue, "$path.gross", currency),
                applied = null,
                net = null,
                market = null,
                redemptionAvailability = null,
                taxes = null,
            ),
        schedule = null,
    )
