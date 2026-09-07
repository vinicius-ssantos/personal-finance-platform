package br.com.vinicius.personalfinance.ingestion

import org.springframework.stereotype.Component
import java.util.Locale

object InterPositionLayout2024_07 {
    val descriptor: LayoutDescriptor =
        LayoutDescriptor(
            institution = "BANCO_INTER",
            documentFamily = "POSITION_CONSOLIDATED",
            layoutVersion = "2024_07",
        )

    const val PARSER_ID: String = "banco-inter-position"
    const val PARSER_VERSION: String = "2024_07.1"
}

data class SourceLine(val pageNumber: Int, val raw: String)

data class SourceField(val tokenRaw: String, val raw: String?, val pageNumber: Int) {
    companion object {
        fun present(tokenRaw: String, pageNumber: Int): SourceField = SourceField(tokenRaw, tokenRaw, pageNumber)
        fun fromToken(tokenRaw: String, pageNumber: Int): SourceField =
            if (tokenRaw == "-") SourceField(tokenRaw, null, pageNumber) else present(tokenRaw, pageNumber)
    }
}

enum class InterPositionSectionType { SUMMARY, TREASURY, BRAZILIAN_EQUITY, FIXED_INCOME, INTERNATIONAL_EQUITY, FUNDS, UNKNOWN }

sealed interface InterPositionRawSection {
    val type: InterPositionSectionType
    val pageNumber: Int
    val headingRaw: String
}

data class SummaryAllocationRawRecord(val labelRaw: String, val currencyToken: String, val amount: SourceField)
data class SummaryRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val totalCurrencyToken: String,
    val total: SourceField,
    val allocations: List<SummaryAllocationRawRecord>,
) : InterPositionRawSection { override val type = InterPositionSectionType.SUMMARY }

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
) : InterPositionRawSection { override val type = InterPositionSectionType.TREASURY }

data class EquityRawRecord(val assetCodeRaw: String, val quantity: SourceField, val grossValue: SourceField)
data class BrazilianEquityRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val currencyToken: String,
    val declaredGross: SourceField,
    val records: List<EquityRawRecord>,
) : InterPositionRawSection { override val type = InterPositionSectionType.BRAZILIAN_EQUITY }
data class InternationalEquityRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val currencyToken: String,
    val declaredGross: SourceField,
    val records: List<EquityRawRecord>,
) : InterPositionRawSection { override val type = InterPositionSectionType.INTERNATIONAL_EQUITY }

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
data class FixedIncomeSubtotalRaw(val appliedValue: SourceField, val grossValue: SourceField, val netValue: SourceField)
data class FixedIncomeRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val currencyToken: String,
    val declaredGross: SourceField,
    val marketReferenceDateRaw: String,
    val records: List<FixedIncomeRawRecord>,
    val subtotal: FixedIncomeSubtotalRaw,
) : InterPositionRawSection { override val type = InterPositionSectionType.FIXED_INCOME }

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
) : InterPositionRawSection { override val type = InterPositionSectionType.FUNDS }

data class UnknownRawRecord(val heading: SourceField, val lines: List<SourceLine>)
data class UnknownRawSection(
    override val pageNumber: Int,
    override val headingRaw: String,
    val record: UnknownRawRecord,
) : InterPositionRawSection { override val type = InterPositionSectionType.UNKNOWN }

data class InterPositionSourceDocument(
    val descriptor: LayoutDescriptor,
    val parserId: String,
    val parserVersion: String,
    val declaredPositionTotalCurrencyToken: String?,
    val declaredPositionTotal: SourceField?,
    val sections: List<InterPositionRawSection>,
) : ParsedSourceDocument

class InterPositionParserException(message: String) : IllegalArgumentException(message)

@Component
class InterPositionLayoutDetector : LayoutDetector {
    override val descriptor = InterPositionLayout2024_07.descriptor

    override fun detect(document: ExtractedDocument): LayoutEvidence? {
        val lines = sourceLines(document)
        val text = lines.joinToString("\n") { it.raw.lowercase(Locale.ROOT) }
        if (NEGATIVE_FAMILY_MARKERS.any(text::contains)) return null
        if (!REQUIRED_MARKERS.all(text::contains)) return null
        if (!knownStructuresAreValid(lines)) return null
        return LayoutEvidence(descriptor, 1.0, EVIDENCE_MARKERS)
    }

    private fun knownStructuresAreValid(lines: List<SourceLine>): Boolean {
        val raw = lines.map { it.raw }
        return raw.filter(::knownSectionHeading).all { heading ->
            when (sectionHeadingType(heading)) {
                InterPositionSectionType.TREASURY -> raw.any { it == TREASURY_HEADER }
                InterPositionSectionType.BRAZILIAN_EQUITY -> raw.any { it == BRAZILIAN_EQUITY_HEADER }
                InterPositionSectionType.FIXED_INCOME -> raw.any { it == FIXED_INCOME_HEADER_1 } && raw.any { it == FIXED_INCOME_HEADER_2 } && raw.any { it.startsWith(FIXED_INCOME_HEADER_3_PREFIX) }
                InterPositionSectionType.INTERNATIONAL_EQUITY -> raw.any { it == INTERNATIONAL_EQUITY_HEADER }
                InterPositionSectionType.FUNDS -> raw.any { it == FUNDS_HEADER_1 } && raw.any { it == FUNDS_HEADER_2 }
                else -> true
            }
        }
    }

    companion object {
        private val REQUIRED_MARKERS = listOf("posição consolidada", "seu patrimônio atual", "distribuição da carteira")
        private val NEGATIVE_FAMILY_MARKERS = listOf("extrato de movimentações", "\nmovimentações\n", "notas de renda fixa", "nota de negociação", "características do título", "características de operação")
        private val EVIDENCE_MARKERS = listOf("document-family-title", "portfolio-summary", "portfolio-distribution", "known-section-structures")
    }
}

@Component
class InterPositionDocumentParser : DocumentParser {
    override val descriptor = InterPositionLayout2024_07.descriptor
    override val parserId = InterPositionLayout2024_07.PARSER_ID
    override val parserVersion = InterPositionLayout2024_07.PARSER_VERSION

    override fun parse(document: ExtractedDocument): InterPositionSourceDocument {
        val lines = sourceLines(document)
        val total = parseDeclaredPositionTotal(lines)
        val starts = sectionStarts(lines)
        val sections = starts.mapIndexed { index, start ->
            val end = starts.getOrNull(index + 1)?.lineIndex ?: lines.size
            val block = lines.subList(start.lineIndex, end)
            when (start.type) {
                InterPositionSectionType.SUMMARY -> parseSummary(block)
                InterPositionSectionType.TREASURY -> parseTreasury(block)
                InterPositionSectionType.BRAZILIAN_EQUITY -> parseBrazilianEquity(block)
                InterPositionSectionType.FIXED_INCOME -> parseFixedIncome(block)
                InterPositionSectionType.INTERNATIONAL_EQUITY -> parseInternationalEquity(block)
                InterPositionSectionType.FUNDS -> parseFunds(block)
                InterPositionSectionType.UNKNOWN -> parseUnknown(block)
            }
        }
        return InterPositionSourceDocument(descriptor, parserId, parserVersion, total?.first, total?.second, sections)
    }

    private fun parseDeclaredPositionTotal(lines: List<SourceLine>): Pair<String, SourceField>? {
        val matches = lines.mapNotNull { line -> DECLARED_POSITION_TOTAL.matchEntire(line.raw)?.let { it to line } }
        if (matches.size > 1) fail("more than one declared Posição Total")
        val (match, line) = matches.singleOrNull() ?: return null
        val currency = match.groupValues[1]
        return currency to SourceField.present("$currency ${match.groupValues[2]}", line.pageNumber)
    }

    private fun sectionStarts(lines: List<SourceLine>): List<SectionStart> {
        val starts = mutableListOf<SectionStart>()
        lines.forEachIndexed { index, line ->
            val known = sectionHeadingType(line.raw)
            if (known != null) starts += SectionStart(index, known)
            else if (isUnknownHeading(lines, index)) starts += SectionStart(index, InterPositionSectionType.UNKNOWN)
        }
        return starts
    }

    private fun parseSummary(block: List<SourceLine>): SummaryRawSection {
        val heading = block.first()
        val totals = block.mapNotNull { line -> SUMMARY_TOTAL.matchEntire(line.raw)?.let { it to line } }
        if (totals.isEmpty()) fail("summary total is missing")
        val (match, line) = totals.first()
        val currency = match.groupValues[1]
        val allocations = block.mapNotNull { source ->
            SUMMARY_ALLOCATION.matchEntire(source.raw)?.let { allocation ->
                val allocationCurrency = allocation.groupValues[2]
                SummaryAllocationRawRecord(allocation.groupValues[1], allocationCurrency, SourceField.present("$allocationCurrency ${allocation.groupValues[3]}", source.pageNumber))
            }
        }
        if (allocations.isEmpty()) fail("summary allocations are missing")
        return SummaryRawSection(heading.pageNumber, heading.raw, currency, SourceField.present("$currency ${match.groupValues[2]}", line.pageNumber), allocations)
    }

    private fun parseTreasury(block: List<SourceLine>): TreasuryRawSection {
        val heading = block.first()
        val headingValue = parseSectionHeading(heading, "Tesouro Direto")
        val records = mutableListOf<TreasuryRawRecord>()
        block.forEachIndexed { index, line ->
            if (line.raw == TREASURY_HEADER) {
                val description = block.getOrNull(index - 1) ?: fail("Treasury description is missing")
                val row = block.getOrNull(index + 1) ?: fail("Treasury row is missing")
                val match = TREASURY_ROW.matchEntire(row.raw) ?: fail("Treasury row changed structure: ${row.raw}")
                records += TreasuryRawRecord(description.raw, SourceField.present(match.groupValues[1], row.pageNumber), SourceField.present(match.groupValues[2], row.pageNumber), SourceField.present(match.groupValues[3], row.pageNumber), SourceField.present(match.groupValues[4], row.pageNumber), SourceField.present(match.groupValues[5], row.pageNumber))
            }
        }
        if (records.isEmpty()) fail("Treasury records are missing")
        return TreasuryRawSection(heading.pageNumber, heading.raw, headingValue.first, headingValue.second, records)
    }

    private fun parseBrazilianEquity(block: List<SourceLine>): BrazilianEquityRawSection {
        val heading = block.first()
        val value = parseSectionHeading(heading, "Renda Variável")
        return BrazilianEquityRawSection(heading.pageNumber, heading.raw, value.first, value.second, parseEquityRecords(block, BRAZILIAN_EQUITY_HEADER, BRAZILIAN_EQUITY_ROW))
    }

    private fun parseInternationalEquity(block: List<SourceLine>): InternationalEquityRawSection {
        val heading = block.first()
        val value = parseSectionHeading(heading, "Renda Variável Internacional")
        return InternationalEquityRawSection(heading.pageNumber, heading.raw, value.first, value.second, parseEquityRecords(block, INTERNATIONAL_EQUITY_HEADER, INTERNATIONAL_EQUITY_ROW))
    }

    private fun parseEquityRecords(block: List<SourceLine>, header: String, rowRegex: Regex): List<EquityRawRecord> {
        val records = mutableListOf<EquityRawRecord>()
        block.forEachIndexed { index, line ->
            if (line.raw == header) {
                val asset = block.getOrNull(index - 1) ?: fail("equity asset code is missing")
                val row = block.getOrNull(index + 1) ?: fail("equity row is missing")
                val match = rowRegex.matchEntire(row.raw) ?: fail("equity row changed structure: ${row.raw}")
                records += EquityRawRecord(asset.raw, SourceField.present(match.groupValues[1], row.pageNumber), SourceField.present(match.groupValues[2], row.pageNumber))
            }
        }
        if (records.isEmpty()) fail("equity records are missing")
        return records
    }

    private fun parseFixedIncome(block: List<SourceLine>): FixedIncomeRawSection {
        val heading = block.first()
        val value = parseSectionHeading(heading, "Renda Fixa")
        val records = mutableListOf<FixedIncomeRawRecord>()
        var marketReferenceDate: String? = null
        block.forEachIndexed { index, line ->
            if (line.raw == FIXED_INCOME_HEADER_1) {
                val description = block.getOrNull(index - 1) ?: fail("fixed-income description is missing")
                val header2 = block.getOrNull(index + 1) ?: fail("fixed-income second header is missing")
                val header3 = block.getOrNull(index + 2) ?: fail("fixed-income market header is missing")
                val row = block.getOrNull(index + 3) ?: fail("fixed-income row is missing")
                if (header2.raw != FIXED_INCOME_HEADER_2) fail("fixed-income second header changed structure")
                val marketHeader = FIXED_INCOME_HEADER_3.matchEntire(header3.raw) ?: fail("fixed-income market header changed structure")
                val reference = marketHeader.groupValues[1]
                if (marketReferenceDate != null && marketReferenceDate != reference) fail("fixed-income market reference dates disagree")
                marketReferenceDate = reference
                val match = FIXED_INCOME_ROW.matchEntire(row.raw) ?: fail("fixed-income row changed structure: ${row.raw}")
                records += FixedIncomeRawRecord(description.raw, match.groupValues[1], SourceField.present(match.groupValues[2], row.pageNumber), SourceField.present(match.groupValues[3], row.pageNumber), SourceField.present(match.groupValues[4], row.pageNumber), match.groupValues[5], SourceField.present(match.groupValues[6], row.pageNumber), SourceField.fromToken(match.groupValues[7], row.pageNumber), SourceField.fromToken(match.groupValues[8], row.pageNumber), SourceField.present(match.groupValues[9], row.pageNumber), SourceField.fromToken(match.groupValues[10], row.pageNumber), SourceField.present(match.groupValues[11], row.pageNumber))
            }
        }
        if (records.isEmpty()) fail("fixed-income records are missing")
        val subtotalLine = block.singleOrNull { it.raw.startsWith("Subtotal ") } ?: fail("fixed-income subtotal is missing or ambiguous")
        val subtotalMatch = FIXED_INCOME_SUBTOTAL.matchEntire(subtotalLine.raw) ?: fail("fixed-income subtotal changed structure")
        val subtotal = FixedIncomeSubtotalRaw(SourceField.present(subtotalMatch.groupValues[1], subtotalLine.pageNumber), SourceField.present(subtotalMatch.groupValues[2], subtotalLine.pageNumber), SourceField.present(subtotalMatch.groupValues[3], subtotalLine.pageNumber))
        return FixedIncomeRawSection(heading.pageNumber, heading.raw, value.first, value.second, marketReferenceDate ?: fail("fixed-income market reference date is missing"), records, subtotal)
    }

    private fun parseFunds(block: List<SourceLine>): FundRawSection {
        val heading = block.first()
        val value = parseSectionHeading(heading, "Fundos de Investimentos")
        val records = mutableListOf<FundRawRecord>()
        block.forEachIndexed { index, line ->
            if (line.raw == FUNDS_HEADER_1) {
                val description = block.getOrNull(index - 1) ?: fail("fund description is missing")
                val header2 = block.getOrNull(index + 1) ?: fail("fund second header is missing")
                val row = block.getOrNull(index + 2) ?: fail("fund row is missing")
                if (header2.raw != FUNDS_HEADER_2) fail("fund second header changed structure")
                val match = FUNDS_ROW.matchEntire(row.raw) ?: fail("fund row changed structure: ${row.raw}")
                records += FundRawRecord(description.raw, SourceField.present(match.groupValues[1], row.pageNumber), SourceField.present(match.groupValues[2], row.pageNumber), SourceField.present(match.groupValues[3], row.pageNumber), SourceField.fromToken(match.groupValues[4], row.pageNumber), SourceField.present(match.groupValues[5], row.pageNumber), SourceField.present(match.groupValues[6], row.pageNumber), SourceField.fromToken(match.groupValues[7], row.pageNumber), SourceField.present(match.groupValues[8], row.pageNumber))
            }
        }
        if (records.isEmpty()) fail("fund records are missing")
        return FundRawSection(heading.pageNumber, heading.raw, value.first, value.second, records)
    }

    private fun parseUnknown(block: List<SourceLine>): UnknownRawSection {
        val heading = block.first()
        return UnknownRawSection(heading.pageNumber, heading.raw, UnknownRawRecord(SourceField.present(heading.raw, heading.pageNumber), block.drop(1)))
    }

    private fun parseSectionHeading(heading: SourceLine, expectedLabel: String): Pair<String, SourceField> {
        val match = SECTION_HEADING.matchEntire(heading.raw) ?: fail("section heading changed structure: ${heading.raw}")
        if (match.groupValues[1] != expectedLabel) fail("expected $expectedLabel section, got ${match.groupValues[1]}")
        val currency = match.groupValues[2]
        return currency to SourceField.present("$currency ${match.groupValues[3]}", heading.pageNumber)
    }

    private fun fail(message: String): Nothing = throw InterPositionParserException(message)
    private data class SectionStart(val lineIndex: Int, val type: InterPositionSectionType)
}

private const val TREASURY_HEADER = "Aplicação Vencimento Quantidade Valor Aplicado (R$) Valor Bruto (R$)"
private const val BRAZILIAN_EQUITY_HEADER = "Quantidade Valor Bruto (R$)"
private const val INTERNATIONAL_EQUITY_HEADER = "Quantidade Valor Bruto (US$)"
private const val FIXED_INCOME_HEADER_1 = "Código Ativo Vencimento Aplicação Taxa Indexador"
private const val FIXED_INCOME_HEADER_2 = "Valor Aplicado (R$) IOF Previsto (R$) IR Previsto (R$) Valor Bruto (R$)"
private const val FIXED_INCOME_HEADER_3_PREFIX = "Valor Mercado ("
private const val FUNDS_HEADER_1 = "Quantidade de cotas Preço mercado (R$) Valor aplicado (R$) Disp. Resgate (R$)"
private const val FUNDS_HEADER_2 = "Valor Bruto (R$) Valor Líquido (R$) Valor IOF (R$) Valor IR (R$)"
private val DECLARED_POSITION_TOTAL = Regex("^Posição Total (R\\$|US\\$) (\\S+)$")
private val SUMMARY_TOTAL = Regex("^(R\\$|US\\$) (\\S+)$")
private val SUMMARY_ALLOCATION = Regex("^(Tesouro Direto|Renda Variável Internacional|Renda Variável|Renda Fixa|Fundos de Investimentos) (R\\$|US\\$) (\\S+)$")
private val SECTION_HEADING = Regex("^\\S+% (Tesouro Direto|Renda Variável Internacional|Renda Variável|Renda Fixa|Fundos de Investimentos) Valor Bruto (R\\$|US\\$) (\\S+)$")
private val TREASURY_ROW = Regex("^(\\S+) (\\S+) (\\S+) (R\\$ \\S+) (R\\$ \\S+)$")
private val BRAZILIAN_EQUITY_ROW = Regex("^(\\S+) (R\\$ \\S+)$")
private val INTERNATIONAL_EQUITY_ROW = Regex("^(\\S+) (US\\$ \\S+)$")
private val FIXED_INCOME_HEADER_3 = Regex("^Valor Mercado \\(([^)]+)\\) Valor Líquido \\(R\\$\\)$")
private val FIXED_INCOME_ROW = Regex("^(\\S+) (\\S+) (\\S+) (\\S+) (\\S+) (R\\$ \\S+) (\\S+) (\\S+) (R\\$ \\S+) ((?:R\\$ \\S+)|-) (R\\$ \\S+)$")
private val FIXED_INCOME_SUBTOTAL = Regex("^Subtotal (R\\$ \\S+) (R\\$ \\S+) (R\\$ \\S+)$")
private val FUNDS_ROW = Regex("^(\\S+) (R\\$ \\S+) (R\\$ \\S+) ((?:R\\$ \\S+)|-) (R\\$ \\S+) (R\\$ \\S+) ((?:R\\$ \\S+)|-) (R\\$ \\S+)$")

private fun sourceLines(document: ExtractedDocument): List<SourceLine> = document.pages.flatMap { page ->
    page.text.lineSequence().filter(String::isNotBlank).map { SourceLine(page.pageNumber, it) }.toList()
}
private fun knownSectionHeading(raw: String): Boolean = sectionHeadingType(raw) in setOf(InterPositionSectionType.TREASURY, InterPositionSectionType.BRAZILIAN_EQUITY, InterPositionSectionType.FIXED_INCOME, InterPositionSectionType.INTERNATIONAL_EQUITY, InterPositionSectionType.FUNDS)
private fun sectionHeadingType(raw: String): InterPositionSectionType? {
    if (raw == "Seu patrimônio atual") return InterPositionSectionType.SUMMARY
    val match = SECTION_HEADING.matchEntire(raw) ?: return null
    return when (match.groupValues[1]) {
        "Tesouro Direto" -> InterPositionSectionType.TREASURY
        "Renda Variável" -> InterPositionSectionType.BRAZILIAN_EQUITY
        "Renda Fixa" -> InterPositionSectionType.FIXED_INCOME
        "Renda Variável Internacional" -> InterPositionSectionType.INTERNATIONAL_EQUITY
        "Fundos de Investimentos" -> InterPositionSectionType.FUNDS
        else -> null
    }
}
private fun isUnknownHeading(lines: List<SourceLine>, index: Int): Boolean {
    val current = lines[index]
    val next = lines.getOrNull(index + 1) ?: return false
    val raw = current.raw
    if (raw.contains('%') || raw.contains("R$") || raw.contains("US$")) return false
    if (!raw.contains(' ')) return false
    if (raw.uppercase(Locale.ROOT) != raw) return false
    if (raw == "POSIÇÃO CONSOLIDADA") return false
    if (!next.raw.contains("Valor Bruto")) return false
    return sectionHeadingType(raw) == null
}
