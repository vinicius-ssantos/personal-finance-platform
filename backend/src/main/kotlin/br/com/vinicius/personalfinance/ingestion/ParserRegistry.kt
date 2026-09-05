package br.com.vinicius.personalfinance.ingestion

import org.springframework.stereotype.Component

/**
 * Turns a recognised layout into structured data.
 *
 * This issue defines the contract and the selection rules; the first concrete
 * parser arrives with the Banco Inter layout. [parserVersion] is persisted on
 * the batch, so a later reinterpretation is auditable (`FR-PARSER-004`).
 *
 * `FR-PARSER-003`: an incompatible change means a new [parserVersion], never an
 * edit that silently changes what past imports would have produced.
 */
interface DocumentParser {
    val descriptor: LayoutDescriptor

    val parserId: String

    val parserVersion: String
}

/** The outcome of asking the registry to handle a document. */
sealed interface LayoutSelection {
    data class Selected(
        val evidence: LayoutEvidence,
        val parser: DocumentParser,
    ) : LayoutSelection

    /** Nothing recognised the document, or nothing can parse what was recognised. */
    data class Unsupported(
        val reason: Reason,
        val detail: String,
    ) : LayoutSelection {
        enum class Reason {
            NO_LAYOUT_RECOGNISED,
            BELOW_CONFIDENCE_THRESHOLD,
            AMBIGUOUS_LAYOUT,
            NO_PARSER_FOR_LAYOUT,
        }
    }
}

/**
 * Selects the parser for a document, or refuses.
 *
 * Fail-closed by construction (`FR-LAYOUT-002`, `FR-LAYOUT-003`): there is no
 * default parser and no "closest match". Every refusal names a stable reason
 * instead of a guess, because a guessed parser produces plausible wrong numbers
 * — the worst possible failure for financial data.
 *
 * Ambiguity is a refusal, not a tie-break. Two detectors claiming the same
 * document means the detectors are wrong, and picking the more confident one
 * would hide that.
 */
@Component
class ParserRegistry(
    detectors: List<LayoutDetector>,
    parsers: List<DocumentParser>,
    private val minimumConfidence: Double = DEFAULT_MINIMUM_CONFIDENCE,
) {
    private val detectors: List<LayoutDetector> = detectors.toList()

    private val parsersByDescriptor: Map<LayoutDescriptor, DocumentParser> =
        parsers.associateBy { parser -> parser.descriptor }

    init {
        val duplicated =
            parsers
                .groupBy { parser -> parser.descriptor }
                .filterValues { registered -> registered.size > 1 }
                .keys
        require(duplicated.isEmpty()) {
            "more than one parser registered for: ${duplicated.map { it.toString() }.sorted()}"
        }
    }

    fun select(document: ExtractedDocument): LayoutSelection {
        val claims = detectors.mapNotNull { detector -> detector.detect(document) }
        val confident = claims.filter { claim -> claim.confidence >= minimumConfidence }
        return when {
            claims.isEmpty() ->
                unsupported(
                    LayoutSelection.Unsupported.Reason.NO_LAYOUT_RECOGNISED,
                    "no detector recognised the document",
                )

            confident.isEmpty() ->
                unsupported(
                    LayoutSelection.Unsupported.Reason.BELOW_CONFIDENCE_THRESHOLD,
                    "best confidence ${claims.maxOf { it.confidence }} is below $minimumConfidence",
                )

            confident.map { it.descriptor }.distinct().size > 1 ->
                unsupported(
                    LayoutSelection.Unsupported.Reason.AMBIGUOUS_LAYOUT,
                    "several layouts claimed the document: " +
                        confident.map { it.descriptor.toString() }.sorted().joinToString(),
                )

            else -> selectParser(confident.maxBy { it.confidence })
        }
    }

    private fun selectParser(evidence: LayoutEvidence): LayoutSelection {
        val parser =
            parsersByDescriptor[evidence.descriptor]
                ?: return unsupported(
                    LayoutSelection.Unsupported.Reason.NO_PARSER_FOR_LAYOUT,
                    "no parser registered for ${evidence.descriptor}",
                )
        return LayoutSelection.Selected(evidence, parser)
    }

    private fun unsupported(
        reason: LayoutSelection.Unsupported.Reason,
        detail: String,
    ): LayoutSelection = LayoutSelection.Unsupported(reason, detail)

    companion object {
        const val DEFAULT_MINIMUM_CONFIDENCE: Double = 0.7
    }
}
