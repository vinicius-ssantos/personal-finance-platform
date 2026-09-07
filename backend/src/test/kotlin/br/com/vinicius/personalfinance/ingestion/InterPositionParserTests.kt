package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.fixtures.FixturePaths
import br.com.vinicius.personalfinance.fixtures.FixturePdfBuilder
import br.com.vinicius.personalfinance.fixtures.InterPositionFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.Locale
import java.util.TimeZone

class InterPositionParserTests {
    private val extractor = PdfBoxTextExtractor()
    private val detector = InterPositionLayoutDetector()
    private val parser = InterPositionDocumentParser()

    private fun extract(case: InterPositionFixture.Case): ExtractedDocument {
        val password = if (case.protected) DocumentPassword.of(InterPositionFixture.TEST_PASSWORD) else null
        return extractor.extract(FixturePdfBuilder.build(case), password)
    }

    @Test
    fun `complete report is detected with explicit layout and stable evidence`() {
        val evidence = detector.detect(extract(InterPositionFixture.complete))

        requireNotNull(evidence)
        assertEquals(InterPositionLayout202407.descriptor, evidence.descriptor)
        assertEquals(1.0, evidence.confidence)
        assertEquals(
            listOf(
                "document-family-title",
                "portfolio-summary",
                "portfolio-distribution",
                "known-section-structures",
            ),
            evidence.markers,
        )
    }

    @Test
    fun `same institution movement and fixed income note families are rejected`() {
        assertNull(detector.detect(extract(InterPositionFixture.unsupportedMovements)))
        assertNull(detector.detect(extract(InterPositionFixture.unsupportedFixedIncomeNotes)))
    }

    @Test
    fun `complete report produces six typed sections with source pages`() {
        val parsed = parser.parse(extract(InterPositionFixture.complete))
        val sections = parsed.sections.associateBy { it.type }

        assertEquals("R$", parsed.declaredPositionTotalCurrencyToken)
        assertEquals("R$ 12.000,00", parsed.declaredPositionTotal?.raw)
        assertTrue(sections.getValue(InterPositionSectionType.SUMMARY) is SummaryRawSection)
        assertTrue(sections.getValue(InterPositionSectionType.TREASURY) is TreasuryRawSection)
        assertTrue(sections.getValue(InterPositionSectionType.BRAZILIAN_EQUITY) is BrazilianEquityRawSection)
        assertTrue(sections.getValue(InterPositionSectionType.FIXED_INCOME) is FixedIncomeRawSection)
        assertTrue(sections.getValue(InterPositionSectionType.INTERNATIONAL_EQUITY) is InternationalEquityRawSection)
        assertTrue(sections.getValue(InterPositionSectionType.FUNDS) is FundRawSection)
        assertEquals(3, sections.getValue(InterPositionSectionType.SUMMARY).pageNumber)
        assertEquals(4, sections.getValue(InterPositionSectionType.TREASURY).pageNumber)
        assertEquals(5, sections.getValue(InterPositionSectionType.FIXED_INCOME).pageNumber)
        assertEquals(6, sections.getValue(InterPositionSectionType.INTERNATIONAL_EQUITY).pageNumber)
        assertEquals(7, sections.getValue(InterPositionSectionType.FUNDS).pageNumber)
    }

    @Test
    fun `missing optional fund value stays absent while source token and page remain`() {
        val section =
            parser.parse(extract(InterPositionFixture.missingOptionalField)).sections.single() as FundRawSection
        val field = section.records.first().redemptionAvailability

        assertEquals("-", field.tokenRaw)
        assertNull(field.raw)
        assertEquals(1, field.pageNumber)
    }

    @Test
    fun `malformed financial token remains exactly raw`() {
        val section =
            parser.parse(extract(InterPositionFixture.malformedValue)).sections
                .single { it.type == InterPositionSectionType.FIXED_INCOME } as FixedIncomeRawSection

        assertEquals("R$ 1.25X,00", section.records.first().grossValue.raw)
    }

    @Test
    fun `controlled mismatch preserves declared section total and subtotal independently`() {
        val section =
            parser.parse(extract(InterPositionFixture.controlledMismatch)).sections
                .single { it.type == InterPositionSectionType.FIXED_INCOME } as FixedIncomeRawSection

        assertEquals("R$ 3.000,00", section.declaredGross.raw)
        assertEquals("R$ 2.900,00", section.subtotal.grossValue.raw)
    }

    @Test
    fun `international section stays USD without implicit conversion`() {
        val section =
            parser.parse(extract(InterPositionFixture.internationalOnly)).sections.single()
                .let { it as InternationalEquityRawSection }

        assertEquals("US$", section.currencyToken)
        assertEquals("US$ 400,00", section.declaredGross.raw)
        assertTrue(section.records.all { it.grossValue.raw?.startsWith("US$ ") == true })
    }

    @Test
    fun `unknown section is explicit without stealing known asset descriptions`() {
        val parsed = parser.parse(extract(InterPositionFixture.unknownSection))
        val unknown = parsed.sections.single { it.type == InterPositionSectionType.UNKNOWN } as UnknownRawSection

        assertEquals("PRODUTO ESTRUTURADO DESCONHECIDO", unknown.headingRaw)
        assertEquals(4, unknown.pageNumber)
        assertEquals(1, parsed.sections.count { it.type == InterPositionSectionType.TREASURY })
        assertEquals(1, parsed.sections.count { it.type == InterPositionSectionType.UNKNOWN })
    }

    @Test
    fun `known section structural change fails closed in detector and parser`() {
        val original = extract(InterPositionFixture.complete)
        val changed =
            original.copy(
                pages =
                    original.pages.map { page ->
                        page.copy(
                            text =
                                page.text.replace(
                                    "Aplicação Vencimento Quantidade Valor Aplicado (R$) Valor Bruto (R$)",
                                    "Aplicação Vencimento Quantidade Valor Aplicado Valor Bruto",
                                ),
                        )
                    },
            )

        assertNull(detector.detect(changed))
        assertThrows(InterPositionParserException::class.java) { parser.parse(changed) }
    }

    @Test
    fun `detection and parsing ignore JVM default locale and timezone`() {
        val document = extract(InterPositionFixture.complete)
        val originalLocale = Locale.getDefault()
        val originalTimeZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
            val firstEvidence = detector.detect(document)
            val firstParsed = parser.parse(document)

            Locale.setDefault(Locale.JAPAN)
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(firstEvidence, detector.detect(document))
            assertEquals(firstParsed, parser.parse(document))
        } finally {
            Locale.setDefault(originalLocale)
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun `raw parser DTO snapshots match all eleven parseable fixtures`() {
        val cases =
            listOf(
                InterPositionFixture.complete,
                InterPositionFixture.summaryOnly,
                InterPositionFixture.treasuryOnly,
                InterPositionFixture.brazilianEquityOnly,
                InterPositionFixture.fixedIncomeOnly,
                InterPositionFixture.internationalOnly,
                InterPositionFixture.fundsOnly,
                InterPositionFixture.missingOptionalField,
                InterPositionFixture.malformedValue,
                InterPositionFixture.controlledMismatch,
                InterPositionFixture.unknownSection,
            )
        val actual =
            cases.joinToString(separator = "\n\n", postfix = "\n") { case ->
                "=== ${case.name} ===\n${renderRawSnapshot(parser.parse(extract(case)))}"
            }
        val golden = FixturePaths.interPositionDirectory.resolve("raw-parser.golden.txt")

        if (System.getProperty("updateGoldenFiles") == "true") {
            Files.writeString(golden, actual)
        }
        assertEquals(Files.readString(golden), actual)
    }
}
