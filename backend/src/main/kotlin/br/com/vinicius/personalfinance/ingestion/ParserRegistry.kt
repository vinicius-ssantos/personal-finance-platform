package br.com.vinicius.personalfinance.ingestion

import org.springframework.stereotype.Component

/** Marker for deterministic raw parser output before normalization. */
interface ParsedSourceDocument

/**
 * Turns a recognised layout into structured source data.
 *
 * parserVersion is persisted on the batch. Incompatible interpretation changes
 * require a new version instead of silently changing historical semantics.
 */
interface DocumentParser {
    val descriptor: LayoutDescriptor
    val parserId: String
    val parserVersion: String
    fun parse(document: ExtractedDocument): ParsedSourceDocument
}

sealed interface LayoutSelection {
    data class Selected(val evidence: LayoutEvidence, val parser: DocumentParser) : LayoutSelection

    data class Unsupported(val reason: Reason, val detail: String) : LayoutSelection {
        enum class Reason {
            NO_LAYOUT_RECOGNISED,
            BELOW_CONFIDENCE_THRESHOLD,
            AMBIGUOUS_LAYOUT,
            NO_PARSER_FOR_LAYOUT,
        }
    }
}

@Component
class ParserRegistry(
    detectors: List<LayoutDetector>,
    parsers: List<DocumentParser>,
    private val minimumConfidence: Double = DEFAULT_MINIMUM_CONFIDENCE,
) {
    private val detectors = detectors.toList()
    private val parsersByDescriptor = parsers.associateBy { it.descriptor }

    init {
        val duplicated = parsers.groupBy { it.descriptor }.filterValues { it.size > 1 }.keys
        require(duplicated.isEmpty()) {
            "more than one parser registered for: ${duplicated.map { it.toString() }.sorted()}"
        }
    }

    fun select(document: ExtractedDocument): LayoutSelection {
        val claims = detectors.mapNotNull { it.detect(document) }
        return when {
            claims.isEmpty() -> unsupported(LayoutSelection.Unsupported.Reason.NO_LAYOUT_RECOGNISED, "no detector recognised the document")
            claims.size > 1 -> unsupported(
                LayoutSelection.Unsupported.Reason.AMBIGUOUS_LAYOUT,
                "several detectors claimed the document: " + claims.map { it.descriptor.toString() }.sorted().joinToString(),
            )
            claims.single().confidence < minimumConfidence -> unsupported(
                LayoutSelection.Unsupported.Reason.BELOW_CONFIDENCE_THRESHOLD,
                "confidence ${claims.single().confidence} is below $minimumConfidence",
            )
            else -> selectParser(claims.single())
        }
    }

    private fun selectParser(evidence: LayoutEvidence): LayoutSelection {
        val parser = parsersByDescriptor[evidence.descriptor]
            ?: return unsupported(
                LayoutSelection.Unsupported.Reason.NO_PARSER_FOR_LAYOUT,
                "no parser registered for ${evidence.descriptor}",
            )
        return LayoutSelection.Selected(evidence, parser)
    }

    private fun unsupported(reason: LayoutSelection.Unsupported.Reason, detail: String): LayoutSelection =
        LayoutSelection.Unsupported(reason, detail)

    companion object {
        const val DEFAULT_MINIMUM_CONFIDENCE: Double = 0.7
    }
}
