package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.MarketReferenceDate
import br.com.vinicius.personalfinance.shared.Money
import br.com.vinicius.personalfinance.shared.PositionDate
import br.com.vinicius.personalfinance.shared.ReportGeneratedAt
import br.com.vinicius.personalfinance.shared.ValueQuality
import br.com.vinicius.personalfinance.shared.map
import org.springframework.stereotype.Component

/**
 * Collects normalization findings while a document is being read.
 *
 * Findings accumulate rather than throw: one unreadable cell must not cost the
 * whole report, and whoever reviews the preview needs to see every problem at
 * once instead of discovering them one failed import at a time.
 */
internal class IssueCollector {
    private val collected = mutableListOf<NormalizationIssue>()

    val issues: List<NormalizationIssue>
        get() = collected.toList()

    fun record(
        code: NormalizationIssueCode,
        path: String,
        pageNumber: Int?,
    ) {
        collected += NormalizationIssue(code, path, pageNumber)
    }

    /**
     * Reads one source field into a canonical value, keeping its evidence.
     *
     * A field the layout does not define at all is `null`. A field the layout
     * defines but the document left empty is present with an
     * [ValueQuality.Unknown] value, because "the column exists and holds
     * nothing" and "there is no such column" are different facts (`INV-003`).
     */
    fun <T : Any> readRequired(
        field: SourceField,
        path: String,
        reader: (String) -> Normalized<T>,
    ): EvidencedValue<T> {
        val normalized =
            when (val raw = field.raw) {
                null -> Normalized.rejected<T>(NormalizationIssueCode.VALUE_ABSENT)
                else -> reader(raw)
            }
        normalized.issue?.let { code -> record(code, path, field.pageNumber) }
        return EvidencedValue(normalized.value, FieldEvidence(field.pageNumber, field.tokenRaw))
    }

    fun <T : Any> read(
        field: SourceField?,
        path: String,
        reader: (String) -> Normalized<T>,
    ): EvidencedValue<T>? = field?.let { present -> readRequired(present, path, reader) }

    fun money(
        field: SourceField,
        path: String,
        currency: CurrencyCode,
    ): EvidencedValue<Money> = readRequired(field, path) { token -> PtBrTokens.money(token, currency) }
}

/**
 * Turns the raw Banco Inter position DTOs into canonical candidates.
 *
 * The boundary this class defends: it produces candidates, never portfolio
 * state (`INV-006`). It resolves no asset identity, compares no total against
 * another and converts no currency. Reconciliation and asset resolution are
 * separate steps with separate failure modes, and merging them here would make
 * a reading error indistinguishable from a financial mismatch.
 */
@Component
class InterPositionNormalizer {
    fun normalize(document: InterPositionSourceDocument): CanonicalPositionDocument {
        val issues = IssueCollector()
        val indexed = document.sections.withIndex().toList()
        return CanonicalPositionDocument(
            identity =
                ParsedDocumentIdentity(
                    descriptor = document.descriptor,
                    parserId = document.parserId,
                    parserVersion = document.parserVersion,
                ),
            temporality = temporality(document, issues),
            declaredPositionTotal = declaredTotal(document, issues),
            summaries =
                indexed.mapNotNull { (index, section) ->
                    (section as? SummaryRawSection)?.let { raw -> summarySection(raw, index, issues) }
                },
            sections = indexed.mapNotNull { (index, section) -> canonicalSection(section, index, issues) },
            unknownSections =
                indexed.mapNotNull { (index, section) ->
                    (section as? UnknownRawSection)?.let { raw -> unknownSection(raw, index, issues) }
                },
            issues = issues.issues,
        )
    }

    private fun declaredTotal(
        document: InterPositionSourceDocument,
        issues: IssueCollector,
    ): EvidencedValue<Money>? {
        val currency = document.declaredPositionTotalCurrencyToken?.let(PtBrTokens::currencyOf)
        return when (currency) {
            null -> null
            else ->
                issues.read(document.declaredPositionTotal, "declaredPositionTotal") { token ->
                    PtBrTokens.money(token, currency)
                }
        }
    }

    private fun temporality(
        document: InterPositionSourceDocument,
        issues: IssueCollector,
    ): CanonicalTemporality =
        CanonicalTemporality(
            positionDate =
                issues
                    .read(document.temporality.positionDate, POSITION_DATE_PATH, PtBrTokens::date)
                    ?.value
                    ?.map(::PositionDate)
                    ?: notStated(issues, POSITION_DATE_PATH),
            generatedAt =
                issues
                    .read(document.temporality.generatedAt, GENERATED_AT_PATH, PtBrTokens::instant)
                    ?.value
                    ?.map(::ReportGeneratedAt)
                    ?: notStated(issues, GENERATED_AT_PATH),
            marketReferenceDate = marketReferenceDate(document, issues),
        )

    private fun notStated(
        issues: IssueCollector,
        path: String,
    ): ValueQuality<Nothing> {
        issues.record(NormalizationIssueCode.VALUE_NOT_STATED, path, null)
        return ValueQuality.Unknown
    }

    /**
     * The market reference date lives inside a fixed-income column header, so a
     * report without that section simply never states it. Two sections stating
     * different dates is a contradiction the reader must not resolve on its own.
     */
    private fun marketReferenceDate(
        document: InterPositionSourceDocument,
        issues: IssueCollector,
    ): ValueQuality<MarketReferenceDate> {
        val fixedIncome = document.sections.filterIsInstance<FixedIncomeRawSection>()
        val declared = fixedIncome.map { section -> section.marketReferenceDateRaw }.distinct()
        return when (declared.size) {
            0 -> ValueQuality.Unknown
            1 -> readMarketReferenceDate(declared.single(), fixedIncome.first().pageNumber, issues)
            else -> {
                issues.record(NormalizationIssueCode.DATE_INVALID, MARKET_DATE_PATH, null)
                ValueQuality.Unknown
            }
        }
    }

    private fun readMarketReferenceDate(
        token: String,
        pageNumber: Int,
        issues: IssueCollector,
    ): ValueQuality<MarketReferenceDate> {
        val normalized = PtBrTokens.date(token)
        normalized.issue?.let { code -> issues.record(code, MARKET_DATE_PATH, pageNumber) }
        return normalized.value.map(::MarketReferenceDate)
    }

    private fun summarySection(
        section: SummaryRawSection,
        index: Int,
        issues: IssueCollector,
    ): CanonicalSummary {
        val path = "sections[$index]"
        val currency = currencyOf(section.totalCurrencyToken)
        return CanonicalSummary(
            currency = currency,
            pageNumber = section.pageNumber,
            declaredTotal = issues.money(section.total, "$path.declaredTotal", currency),
            allocations =
                section.allocations.mapIndexed { position, allocation ->
                    val allocationCurrency = currencyOf(allocation.currencyToken)
                    SummaryAllocation(
                        labelRaw = allocation.labelRaw,
                        currency = allocationCurrency,
                        amount =
                            issues.money(
                                allocation.amount,
                                "$path.allocations[$position]",
                                allocationCurrency,
                            ),
                    )
                },
        )
    }

    private fun unknownSection(
        section: UnknownRawSection,
        index: Int,
        issues: IssueCollector,
    ): UnknownSectionRecord {
        issues.record(NormalizationIssueCode.SECTION_UNKNOWN, "sections[$index]", section.pageNumber)
        return UnknownSectionRecord(
            headingRaw = section.headingRaw,
            pageNumber = section.pageNumber,
            lineCount = section.record.lines.size,
        )
    }

    private fun canonicalSection(
        section: InterPositionRawSection,
        index: Int,
        issues: IssueCollector,
    ): CanonicalSection? =
        when (section) {
            is TreasuryRawSection -> treasurySection(section, index, issues)
            is BrazilianEquityRawSection ->
                equitySection(PositionCategory.BRAZILIAN_EQUITY, section.asView(), index, issues)

            is InternationalEquityRawSection ->
                equitySection(PositionCategory.INTERNATIONAL_EQUITY, section.asView(), index, issues)

            is FixedIncomeRawSection -> fixedIncomeSection(section, index, issues)
            is FundRawSection -> fundSection(section, index, issues)
            is SummaryRawSection, is UnknownRawSection -> null
        }

    private companion object {
        const val POSITION_DATE_PATH = "temporality.positionDate"
        const val GENERATED_AT_PATH = "temporality.generatedAt"
        const val MARKET_DATE_PATH = "temporality.marketReferenceDate"
    }
}

internal fun currencyOf(token: String): CurrencyCode =
    checkNotNull(PtBrTokens.currencyOf(token)) {
        "parser emitted a currency token outside its own grammar: $token"
    }
