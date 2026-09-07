package br.com.vinicius.personalfinance.ingestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val INTER_POSITION =
    LayoutDescriptor("BANCO_INTER", "POSITION_CONSOLIDATED", "2026.1")

private val OTHER_LAYOUT =
    LayoutDescriptor("OTHER_BANK", "POSITION_CONSOLIDATED", "1.0")

private class StubDetector(
    override val descriptor: LayoutDescriptor,
    private val confidence: Double?,
) : LayoutDetector {
    override fun detect(document: ExtractedDocument): LayoutEvidence? =
        confidence?.let { value ->
            LayoutEvidence(descriptor, value, listOf("stub-marker"))
        }
}

private class StubParser(
    override val descriptor: LayoutDescriptor,
    override val parserId: String = "stub-parser",
    override val parserVersion: String = "1.0.0",
) : DocumentParser {
    override fun parse(document: ExtractedDocument): ParsedSourceDocument =
        object : ParsedSourceDocument {}
}

class ParserRegistryTests {
    private val document =
        ExtractedDocument(
            pages = listOf(ExtractedPage(1, "any text")),
            sanitizedMetadata = emptyMap(),
        )

    private fun registryOf(
        detectors: List<LayoutDetector>,
        parsers: List<DocumentParser>,
    ) = ParserRegistry(detectors, parsers)

    private fun unsupportedReason(selection: LayoutSelection): LayoutSelection.Unsupported.Reason {
        assertTrue(selection is LayoutSelection.Unsupported, "expected a refusal, got $selection")
        return (selection as LayoutSelection.Unsupported).reason
    }

    @Test
    fun `a recognised layout selects its parser`() {
        val registry =
            registryOf(
                detectors = listOf(StubDetector(INTER_POSITION, confidence = 0.95)),
                parsers = listOf(StubParser(INTER_POSITION)),
            )

        val selection = registry.select(document)

        assertTrue(selection is LayoutSelection.Selected)
        val selected = selection as LayoutSelection.Selected
        assertEquals(INTER_POSITION, selected.parser.descriptor)
        assertEquals(0.95, selected.evidence.confidence)
        assertEquals(listOf("stub-marker"), selected.evidence.markers)
    }

    @Test
    fun `an unrecognised document is refused rather than guessed`() {
        val registry =
            registryOf(
                detectors = listOf(StubDetector(INTER_POSITION, confidence = null)),
                parsers = listOf(StubParser(INTER_POSITION)),
            )

        assertEquals(
            LayoutSelection.Unsupported.Reason.NO_LAYOUT_RECOGNISED,
            unsupportedReason(registry.select(document)),
        )
    }

    @Test
    fun `an empty registry never falls through to a default parser`() {
        val registry = registryOf(detectors = emptyList(), parsers = emptyList())

        assertEquals(
            LayoutSelection.Unsupported.Reason.NO_LAYOUT_RECOGNISED,
            unsupportedReason(registry.select(document)),
        )
    }

    @Test
    fun `weak evidence is refused instead of accepted as a best effort`() {
        val registry =
            registryOf(
                detectors = listOf(StubDetector(INTER_POSITION, confidence = 0.4)),
                parsers = listOf(StubParser(INTER_POSITION)),
            )

        assertEquals(
            LayoutSelection.Unsupported.Reason.BELOW_CONFIDENCE_THRESHOLD,
            unsupportedReason(registry.select(document)),
        )
    }

    @Test
    fun `two layouts claiming the same document is a refusal, not a tie-break`() {
        val registry =
            registryOf(
                detectors =
                    listOf(
                        StubDetector(INTER_POSITION, confidence = 0.9),
                        StubDetector(OTHER_LAYOUT, confidence = 0.8),
                    ),
                parsers = listOf(StubParser(INTER_POSITION), StubParser(OTHER_LAYOUT)),
            )

        assertEquals(
            LayoutSelection.Unsupported.Reason.AMBIGUOUS_LAYOUT,
            unsupportedReason(registry.select(document)),
        )
    }

    @Test
    fun `a low confidence competing detector is still an ambiguity`() {
        val registry =
            registryOf(
                detectors =
                    listOf(
                        StubDetector(INTER_POSITION, confidence = 0.95),
                        StubDetector(OTHER_LAYOUT, confidence = 0.60),
                    ),
                parsers = listOf(StubParser(INTER_POSITION), StubParser(OTHER_LAYOUT)),
            )

        assertEquals(
            LayoutSelection.Unsupported.Reason.AMBIGUOUS_LAYOUT,
            unsupportedReason(registry.select(document)),
        )
    }

    @Test
    fun `a recognised layout without a parser is refused explicitly`() {
        val registry =
            registryOf(
                detectors = listOf(StubDetector(INTER_POSITION, confidence = 0.9)),
                parsers = emptyList(),
            )

        assertEquals(
            LayoutSelection.Unsupported.Reason.NO_PARSER_FOR_LAYOUT,
            unsupportedReason(registry.select(document)),
        )
    }

    @Test
    fun `two parsers for one layout is rejected at construction`() {
        assertThrows(IllegalArgumentException::class.java) {
            registryOf(
                detectors = emptyList(),
                parsers = listOf(StubParser(INTER_POSITION), StubParser(INTER_POSITION, "other")),
            )
        }
    }

    @Test
    fun `selection is deterministic across repeated calls`() {
        val registry =
            registryOf(
                detectors = listOf(StubDetector(INTER_POSITION, confidence = 0.9)),
                parsers = listOf(StubParser(INTER_POSITION)),
            )

        val first = registry.select(document)
        val second = registry.select(document)

        assertEquals(first, second)
    }

    @Test
    fun `evidence rejects an impossible confidence`() {
        assertThrows(IllegalArgumentException::class.java) {
            LayoutEvidence(INTER_POSITION, confidence = 1.5, markers = listOf("m"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LayoutEvidence(INTER_POSITION, confidence = 0.9, markers = emptyList())
        }
    }
}
