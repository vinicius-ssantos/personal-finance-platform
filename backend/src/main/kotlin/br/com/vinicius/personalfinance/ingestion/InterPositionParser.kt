package br.com.vinicius.personalfinance.ingestion

import org.springframework.stereotype.Component
import java.util.Locale

@Component
class InterPositionLayoutDetector : LayoutDetector {
    override val descriptor = InterPositionLayout202407.descriptor

    override fun detect(document: ExtractedDocument): LayoutEvidence? {
        val lines = sourceLines(document)
        val text = lines.joinToString("\n") { line -> line.raw.lowercase(Locale.ROOT) }
        return when {
            NEGATIVE_FAMILY_MARKERS.any(text::contains) -> null
            !REQUIRED_MARKERS.all(text::contains) -> null
            !knownStructuresAreValid(lines) -> null
            else -> LayoutEvidence(descriptor, 1.0, EVIDENCE_MARKERS)
        }
    }

    private fun knownStructuresAreValid(lines: List<SourceLine>): Boolean {
        val starts = lines.indices.filter { index -> knownSectionHeading(lines[index].raw) }
        return starts.mapIndexed { index, start ->
            val end = starts.getOrNull(index + 1) ?: lines.size
            lines.subList(start, end)
        }.all(::knownStructureIsValid)
    }

    private fun knownStructureIsValid(block: List<SourceLine>): Boolean {
        val raw = block.map { line -> line.raw }
        return when (sectionHeadingType(raw.first())) {
            InterPositionSectionType.TREASURY -> raw.any { line -> line == TREASURY_HEADER }
            InterPositionSectionType.BRAZILIAN_EQUITY -> raw.any { line -> line == BRAZILIAN_EQUITY_HEADER }
            InterPositionSectionType.FIXED_INCOME ->
                raw.any { line -> line == FIXED_INCOME_HEADER_1 } &&
                    raw.any { line -> line == FIXED_INCOME_HEADER_2 } &&
                    raw.any { line -> line.startsWith(FIXED_INCOME_HEADER_3_PREFIX) }
            InterPositionSectionType.INTERNATIONAL_EQUITY ->
                raw.any { line -> line == INTERNATIONAL_EQUITY_HEADER }
            InterPositionSectionType.FUNDS ->
                raw.any { line -> line == FUNDS_HEADER_1 } && raw.any { line -> line == FUNDS_HEADER_2 }
            else -> true
        }
    }

    companion object {
        private val REQUIRED_MARKERS =
            listOf(
                "posição consolidada",
                "seu patrimônio atual",
                "distribuição da carteira",
            )
        private val NEGATIVE_FAMILY_MARKERS =
            listOf(
                "extrato de movimentações",
                "\nmovimentações\n",
                "notas de renda fixa",
                "nota de negociação",
                "características do título",
                "características de operação",
            )
        private val EVIDENCE_MARKERS =
            listOf(
                "document-family-title",
                "portfolio-summary",
                "portfolio-distribution",
                "known-section-structures",
            )
    }
}

@Component
class InterPositionDocumentParser : DocumentParser {
    override val descriptor = InterPositionLayout202407.descriptor
    override val parserId = InterPositionLayout202407.PARSER_ID
    override val parserVersion = InterPositionLayout202407.PARSER_VERSION

    override fun parse(document: ExtractedDocument): InterPositionSourceDocument {
        val lines = sourceLines(document)
        val total = parseDeclaredPositionTotal(lines)
        val starts = sectionStarts(lines)
        val sections =
            starts.mapIndexed { index, start ->
                val end = starts.getOrNull(index + 1)?.lineIndex ?: lines.size
                parseSection(start.type, lines.subList(start.lineIndex, end))
            }
        return InterPositionSourceDocument(
            descriptor = descriptor,
            parserId = parserId,
            parserVersion = parserVersion,
            declaredPositionTotalCurrencyToken = total?.first,
            declaredPositionTotal = total?.second,
            sections = sections,
        )
    }

    private fun parseDeclaredPositionTotal(lines: List<SourceLine>): Pair<String, SourceField>? {
        val matches =
            lines.mapNotNull { line ->
                DECLARED_POSITION_TOTAL.matchEntire(line.raw)?.let { match -> match to line }
            }
        if (matches.size > 1) parserFailure("more than one declared Posição Total")
        val matched = matches.singleOrNull() ?: return null
        val (currency, amount) = matched.first.destructured
        return currency to SourceField.present("$currency $amount", matched.second.pageNumber)
    }

    private fun sectionStarts(lines: List<SourceLine>): List<SectionStart> =
        buildList {
            lines.forEachIndexed { index, line ->
                val known = sectionHeadingType(line.raw)
                when {
                    known != null -> add(SectionStart(index, known))
                    isUnknownHeading(lines, index) -> add(SectionStart(index, InterPositionSectionType.UNKNOWN))
                }
            }
        }

    private fun parseSection(
        type: InterPositionSectionType,
        block: List<SourceLine>,
    ): InterPositionRawSection =
        when (type) {
            InterPositionSectionType.SUMMARY -> parseSummarySection(block)
            InterPositionSectionType.TREASURY -> parseTreasurySection(block)
            InterPositionSectionType.BRAZILIAN_EQUITY -> parseBrazilianEquitySection(block)
            InterPositionSectionType.FIXED_INCOME -> parseFixedIncomeSection(block)
            InterPositionSectionType.INTERNATIONAL_EQUITY -> parseInternationalEquitySection(block)
            InterPositionSectionType.FUNDS -> parseFundSection(block)
            InterPositionSectionType.UNKNOWN -> parseUnknownSection(block)
        }

    private data class SectionStart(
        val lineIndex: Int,
        val type: InterPositionSectionType,
    )
}

internal const val TREASURY_HEADER = "Aplicação Vencimento Quantidade Valor Aplicado (R$) Valor Bruto (R$)"
internal const val BRAZILIAN_EQUITY_HEADER = "Quantidade Valor Bruto (R$)"
internal const val INTERNATIONAL_EQUITY_HEADER = "Quantidade Valor Bruto (US$)"
internal const val FIXED_INCOME_HEADER_1 = "Código Ativo Vencimento Aplicação Taxa Indexador"
internal const val FIXED_INCOME_HEADER_2 =
    "Valor Aplicado (R$) IOF Previsto (R$) IR Previsto (R$) Valor Bruto (R$)"
internal const val FIXED_INCOME_HEADER_3_PREFIX = "Valor Mercado ("
internal const val FUNDS_HEADER_1 =
    "Quantidade de cotas Preço mercado (R$) Valor aplicado (R$) Disp. Resgate (R$)"
internal const val FUNDS_HEADER_2 = "Valor Bruto (R$) Valor Líquido (R$) Valor IOF (R$) Valor IR (R$)"

private val DECLARED_POSITION_TOTAL = Regex("^Posição Total (R\\$|US\\$) (\\S+)$")
internal val SUMMARY_TOTAL = Regex("^(R\\$|US\\$) (\\S+)$")
internal val SUMMARY_ALLOCATION =
    Regex(
        "^(Tesouro Direto|Renda Variável Internacional|Renda Variável|Renda Fixa|" +
            "Fundos de Investimentos) (R\\$|US\\$) (\\S+)$",
    )
internal val SECTION_HEADING =
    Regex(
        "^\\S+% (Tesouro Direto|Renda Variável Internacional|Renda Variável|Renda Fixa|" +
            "Fundos de Investimentos) Valor Bruto (R\\$|US\\$) (\\S+)$",
    )
internal val TREASURY_ROW = Regex("^(\\S+) (\\S+) (\\S+) (R\\$ \\S+) (R\\$ \\S+)$")
internal val BRAZILIAN_EQUITY_ROW = Regex("^(\\S+) (R\\$ \\S+)$")
internal val INTERNATIONAL_EQUITY_ROW = Regex("^(\\S+) (US\\$ \\S+)$")
internal val FIXED_INCOME_HEADER_3 = Regex("^Valor Mercado \\(([^)]+)\\) Valor Líquido \\(R\\$\\)$")
internal val FIXED_INCOME_ROW =
    Regex(
        "^(\\S+) (\\S+) (\\S+) (\\S+) (\\S+) (R\\$ \\S+) " +
            "(\\S+) (\\S+) (R\\$ \\S+) ((?:R\\$ \\S+)|-) (R\\$ \\S+)$",
    )
internal val FIXED_INCOME_SUBTOTAL = Regex("^Subtotal (R\\$ \\S+) (R\\$ \\S+) (R\\$ \\S+)$")
internal val FUNDS_ROW =
    Regex(
        "^(\\S+) (R\\$ \\S+) (R\\$ \\S+) ((?:R\\$ \\S+)|-) " +
            "(R\\$ \\S+) (R\\$ \\S+) ((?:R\\$ \\S+)|-) (R\\$ \\S+)$",
    )

private val KNOWN_SECTION_TYPES =
    setOf(
        InterPositionSectionType.TREASURY,
        InterPositionSectionType.BRAZILIAN_EQUITY,
        InterPositionSectionType.FIXED_INCOME,
        InterPositionSectionType.INTERNATIONAL_EQUITY,
        InterPositionSectionType.FUNDS,
    )

internal fun sourceLines(document: ExtractedDocument): List<SourceLine> =
    document.pages.flatMap { page ->
        page.text
            .lineSequence()
            .filter(String::isNotBlank)
            .map { raw -> SourceLine(page.pageNumber, raw) }
            .toList()
    }

private fun knownSectionHeading(raw: String): Boolean = sectionHeadingType(raw) in KNOWN_SECTION_TYPES

internal fun sectionHeadingType(raw: String): InterPositionSectionType? =
    if (raw == "Seu patrimônio atual") {
        InterPositionSectionType.SUMMARY
    } else {
        SECTION_HEADING.matchEntire(raw)?.destructured?.let { (label) ->
            when (label) {
                "Tesouro Direto" -> InterPositionSectionType.TREASURY
                "Renda Variável" -> InterPositionSectionType.BRAZILIAN_EQUITY
                "Renda Fixa" -> InterPositionSectionType.FIXED_INCOME
                "Renda Variável Internacional" -> InterPositionSectionType.INTERNATIONAL_EQUITY
                "Fundos de Investimentos" -> InterPositionSectionType.FUNDS
                else -> null
            }
        }
    }

internal fun isUnknownHeading(
    lines: List<SourceLine>,
    index: Int,
): Boolean =
    lines.getOrNull(index + 1)?.let { next ->
        val raw = lines[index].raw
        !raw.contains('%') &&
            !raw.contains("R$") &&
            !raw.contains("US$") &&
            raw.contains(' ') &&
            raw.uppercase(Locale.ROOT) == raw &&
            raw != "POSIÇÃO CONSOLIDADA" &&
            next.raw.contains("Valor Bruto") &&
            sectionHeadingType(raw) == null
    } ?: false

internal fun parserFailure(message: String): Nothing = throw InterPositionParserException(message)
