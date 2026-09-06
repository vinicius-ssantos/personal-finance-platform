package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.fixtures.FixturePdfBuilder
import br.com.vinicius.personalfinance.fixtures.InterPositionFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class InterPositionParserTests {
    private val extractor = PdfBoxTextExtractor()

    private val detector = InterPositionLayoutDetector()

    private val parser = InterPositionDocumentParser()

    private fun extract(case: InterPositionFixture.Case): ExtractedDocument {
        val password =
            if (case.protected) DocumentPassword.of(InterPositionFixture.TEST_PASSWORD) else null
        return extractor.extract(FixturePdfBuilder.build(case), password)
    }

    @Test
    fun `complete position layout is recognised with stable evidence`() {
        val evidence = detector.detect(extract(InterPositionFixture.complete))

        assertNotNull(evidence)
        requireNotNull(evidence)
        assertEquals(InterPositionLayout2024_07.descriptor, evidence.descriptor)
        assertEquals(1.0, evidence.confidence)
        assertEquals(
            listOf("document-family-title", "portfolio-summary", "portfolio-distribution"),
            evidence.markers,
        )
    }

    @Test
    fun `same institution movement and notes families are refused`() {
        assertNull(detector.detect(extract(InterPositionFixture.unsupportedMovements)))
        assertNull(detector.detect(extract(InterPositionFixture.unsupportedFixedIncomeNotes)))
    }

    @Test
    fun `complete fixture is split into all supported source sections with page provenance`() {
        val parsed = parser.parse(extract(InterPositionFixture.complete))
        val sectionsByType = parsed.sections.associateBy { section -> section.type }

        assertEquals(InterPositionLayout2024_07.descriptor, parsed.descriptor)
        assertEquals(InterPositionLayout2024_07.PARSER_ID, parsed.parserId)
        assertEquals(InterPositionLayout2024_07.PARSER_VERSION, parsed.parserVersion)
        assertEquals(
            setOf(
                InterPositionSectionType.SUMMARY,
                InterPositionSectionType.TREASURY,
                InterPositionSectionType.BRAZILIAN_EQUITY,
                InterPositionSectionType.FIXED_INCOME,
                InterPositionSectionType.INTERNATIONAL_EQUITY,
                InterPositionSectionType.FUNDS,
            ),
            sectionsByType.keys,
        )
        assertEquals(3, sectionsByType.getValue(InterPositionSectionType.SUMMARY).pageNumber)
        assertEquals(4, sectionsByType.getValue(InterPositionSectionType.TREASURY).pageNumber)
        assertEquals(4, sectionsByType.getValue(InterPositionSectionType.BRAZILIAN_EQUITY).pageNumber)
        assertEquals(5, sectionsByType.getValue(InterPositionSectionType.FIXED_INCOME).pageNumber)
        assertEquals(6, sectionsByType.getValue(InterPositionSectionType.INTERNATIONAL_EQUITY).pageNumber)
        assertEquals(7, sectionsByType.getValue(InterPositionSectionType.FUNDS).pageNumber)
    }

    @Test
    fun `optional missing and malformed values remain raw rather than becoming zero`() {
        val missing = parser.parse(extract(InterPositionFixture.missingOptionalField))
        val malformed = parser.parse(extract(InterPositionFixture.malformedValue))

        assertTrue(missing.sections.flatMap { it.lines }.any { line -> line.raw.contains(" - R$ 2.000,00") })
        assertTrue(malformed.sections.flatMap { it.lines }.any { line -> line.raw.contains("1.25X,00") })
    }

    @Test
    fun `unknown section remains explicit and is never guessed as a supported product`() {
        val parsed = parser.parse(extract(InterPositionFixture.unknownSection))
        val unknown = parsed.sections.single { section -> section.type == InterPositionSectionType.UNKNOWN }

        assertEquals("PRODUTO ESTRUTURADO DESCONHECIDO", unknown.headingRaw)
        assertEquals(4, unknown.pageNumber)
    }

    @Test
    fun `selection and parsing are independent from the JVM default locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val firstEvidence = detector.detect(extract(InterPositionFixture.complete))
            val firstParsed = parser.parse(extract(InterPositionFixture.complete))

            Locale.setDefault(Locale.JAPAN)
            val secondEvidence = detector.detect(extract(InterPositionFixture.complete))
            val secondParsed = parser.parse(extract(InterPositionFixture.complete))

            assertEquals(firstEvidence, secondEvidence)
            assertEquals(firstParsed, secondParsed)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `registry selects and executes the concrete Inter parser`() {
        val document = extract(InterPositionFixture.complete)
        val registry = ParserRegistry(listOf(detector), listOf(parser))

        val selection = registry.select(document)

        assertTrue(selection is LayoutSelection.Selected)
        val selected = selection as LayoutSelection.Selected
        val parsed = selected.parser.parse(document)
        assertTrue(parsed is InterPositionSourceDocument)
    }
}
