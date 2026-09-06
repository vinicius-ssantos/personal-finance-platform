package br.com.vinicius.personalfinance.ingestion

import org.springframework.stereotype.Component
import java.util.Locale

/**
 * First Banco Inter consolidated-position layout observed privately.
 *
 * This descriptor is intentionally explicit and versioned. A changed report
 * shape must be introduced as a new layout instead of weakening detection.
 */
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

/** One source line with page provenance. No normalized financial value lives here. */
data class SourceLine(
    val pageNumber: Int,
    val raw: String,
)

/** Sections exposed by the observed Inter consolidated-position layout. */
enum class InterPositionSectionType {
    SUMMARY,
    TREASURY,
    BRAZILIAN_EQUITY,
    FIXED_INCOME,
    INTERNATIONAL_EQUITY,
    FUNDS,
    UNKNOWN,
}

/**
 * Raw parser output consumed by the later normalization issue.
 *
 * Values remain strings exactly as extracted. Missing values remain missing in
 * [lines]; the parser never manufactures zeroes, money, assets or positions.
 */
data class InterPositionSourceSection(
    val type: InterPositionSectionType,
    val pageNumber: Int,
    val headingRaw: String,
    val lines: List<SourceLine>,
)

data class InterPositionSourceDocument(
    val descriptor: LayoutDescriptor,
    val parserId: String,
    val parserVersion: String,
    val sections: List<InterPositionSourceSection>,
) : ParsedSourceDocument

/**
 * Fail-closed detector for the first observed consolidated-position layout.
 *
 * Institution alone is never sufficient evidence: movement statements and
 * fixed-income notes from the same bank are explicit negative families.
 */
@Component
class InterPositionLayoutDetector : LayoutDetector {
    override val descriptor: LayoutDescriptor = InterPositionLayout2024_07.descriptor

    override fun detect(document: ExtractedDocument): LayoutEvidence? {
        val text = document.fullText().lowercase(Locale.ROOT)
        if (NEGATIVE_FAMILY_MARKERS.any { marker -> text.contains(marker) }) return null
        if (!REQUIRED_MARKERS.all { marker -> text.contains(marker) }) return null

        return LayoutEvidence(
            descriptor = descriptor,
            confidence = 1.0,
            markers = EVIDENCE_MARKERS,
        )
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
                "notas de renda fixa",
                "nota de negociação",
            )

        private val EVIDENCE_MARKERS =
            listOf(
                "document-family-title",
                "portfolio-summary",
                "portfolio-distribution",
            )
    }
}

/**
 * Parses the observed Inter position layout into raw source sections.
 *
 * Section recognition uses fixed, versioned markers only. Unknown constructs
 * remain explicit instead of being guessed as one of the supported sections.
 */
@Component
class InterPositionDocumentParser : DocumentParser {
    override val descriptor: LayoutDescriptor = InterPositionLayout2024_07.descriptor

    override val parserId: String = InterPositionLayout2024_07.PARSER_ID

    override val parserVersion: String = InterPositionLayout2024_07.PARSER_VERSION

    override fun parse(document: ExtractedDocument): InterPositionSourceDocument {
        val sourceLines =
            document.pages.flatMap { page ->
                page.text
                    .lineSequence()
                    .filter { line -> line.isNotBlank() }
                    .map { line -> SourceLine(page.pageNumber, line) }
                    .toList()
            }
        val sections = buildSections(sourceLines)
        return InterPositionSourceDocument(
            descriptor = descriptor,
            parserId = parserId,
            parserVersion = parserVersion,
            sections = sections,
        )
    }

    private fun buildSections(lines: List<SourceLine>): List<InterPositionSourceSection> {
        val starts =
            lines.mapIndexedNotNull { index, line ->
                sectionType(line.raw)?.let { type -> SectionStart(index, type) }
            }
        return starts.mapIndexed { position, start ->
            val endExclusive = starts.getOrNull(position + 1)?.lineIndex ?: lines.size
            val sectionLines = lines.subList(start.lineIndex, endExclusive)
            InterPositionSourceSection(
                type = start.type,
                pageNumber = sectionLines.first().pageNumber,
                headingRaw = sectionLines.first().raw,
                lines = sectionLines,
            )
        }
    }

    private fun sectionType(raw: String): InterPositionSectionType? {
        val line = raw.lowercase(Locale.ROOT)
        return when {
            line == "seu patrimônio atual" -> InterPositionSectionType.SUMMARY
            line.contains("tesouro direto") && line.contains("valor bruto") ->
                InterPositionSectionType.TREASURY

            line.contains("renda variável internacional") && line.contains("valor bruto") ->
                InterPositionSectionType.INTERNATIONAL_EQUITY

            line.contains("renda variável") && line.contains("valor bruto") ->
                InterPositionSectionType.BRAZILIAN_EQUITY

            line.contains("renda fixa") && line.contains("valor bruto") ->
                InterPositionSectionType.FIXED_INCOME

            line.contains("fundos de investimentos") && line.contains("valor bruto") ->
                InterPositionSectionType.FUNDS

            line == "produto estruturado desconhecido" -> InterPositionSectionType.UNKNOWN
            else -> null
        }
    }

    private data class SectionStart(
        val lineIndex: Int,
        val type: InterPositionSectionType,
    )
}
