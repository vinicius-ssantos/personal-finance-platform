package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.fixtures.FixturePaths
import br.com.vinicius.personalfinance.fixtures.FixturePdfBuilder
import br.com.vinicius.personalfinance.fixtures.InterPositionFixture
import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.ValueQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate

class InterPositionNormalizerTests {
    private val extractor = PdfBoxTextExtractor()
    private val parser = InterPositionDocumentParser()
    private val normalizer = InterPositionNormalizer()

    private fun normalize(case: InterPositionFixture.Case): CanonicalPositionDocument {
        val password = if (case.protected) DocumentPassword.of(InterPositionFixture.TEST_PASSWORD) else null
        return normalizer.normalize(parser.parse(extractor.extract(FixturePdfBuilder.build(case), password)))
    }

    private fun CanonicalPositionDocument.section(category: PositionCategory): CanonicalSection =
        sections.first { section -> section.category == category }

    private fun <T : Any> exact(value: ValueQuality<T>): T = (value as ValueQuality.Exact<T>).value

    @Test
    fun `the three financial dates stay separate concepts`() {
        val canonical = normalize(InterPositionFixture.complete)

        assertEquals(LocalDate.of(2026, 1, 31), exact(canonical.temporality.positionDate).value)
        assertEquals("2026-02-02T11:15:00Z", exact(canonical.temporality.generatedAt).value.toString())
        assertEquals(LocalDate.of(2026, 2, 2), exact(canonical.temporality.marketReferenceDate).value)
    }

    @Test
    fun `money reaches the domain in minor units with an explicit currency`() {
        val treasury = normalize(InterPositionFixture.complete).section(PositionCategory.TREASURY)
        val candidate = treasury.candidates.single()

        assertEquals(100_000L, exact(treasury.declaredGross.value).amountMinor)
        assertEquals(CurrencyCode.BRL, treasury.currency)
        assertEquals(100_000L, exact(candidate.amounts.gross.value).amountMinor)
        assertEquals(90_000L, exact(candidate.amounts.applied!!.value).amountMinor)
    }

    @Test
    fun `an absent optional value stays unknown and never becomes zero`() {
        val canonical = normalize(InterPositionFixture.missingOptionalField)
        val candidate = canonical.section(PositionCategory.FUNDS).candidates.first()
        val redemption = candidate.amounts.redemptionAvailability

        assertNotNull(redemption)
        assertEquals(ValueQuality.Unknown, redemption!!.value)
        assertEquals("-", redemption.evidence.tokenRaw)
        assertTrue(
            canonical.issues.any { issue ->
                issue.code == NormalizationIssueCode.VALUE_ABSENT &&
                    issue.fieldPath.endsWith("redemptionAvailability")
            },
        )
    }

    @Test
    fun `a malformed money token stays unknown with its evidence intact`() {
        val canonical = normalize(InterPositionFixture.malformedValue)
        val malformed =
            canonical
                .section(PositionCategory.FIXED_INCOME)
                .candidates
                .first()
                .amounts.gross

        assertEquals(ValueQuality.Unknown, malformed.value)
        assertEquals("R$ 1.25X,00", malformed.evidence.tokenRaw)
        assertEquals(5, malformed.evidence.pageNumber)
        assertTrue(
            canonical.issues.any { issue -> issue.code == NormalizationIssueCode.VALUE_MALFORMED },
        )
    }

    @Test
    fun `USD stays USD and is never folded into the BRL total`() {
        val canonical = normalize(InterPositionFixture.complete)
        val international = canonical.section(PositionCategory.INTERNATIONAL_EQUITY)

        assertEquals(CurrencyCode.USD, international.currency)
        assertTrue(
            international.candidates.all { candidate -> candidate.currency == CurrencyCode.USD },
        )
        assertEquals(CurrencyCode.BRL, exact(canonical.declaredPositionTotal!!.value).currency)
        assertEquals(1_200_000L, exact(canonical.declaredPositionTotal.value).amountMinor)
    }

    @Test
    fun `the declared total is preserved as stated and never recomputed`() {
        val canonical = normalize(InterPositionFixture.complete)
        val sectionSum =
            canonical.sections
                .filter { section -> section.currency == CurrencyCode.BRL }
                .sumOf { section -> exact(section.declaredGross.value).amountMinor }

        assertEquals(1_200_000L, exact(canonical.declaredPositionTotal!!.value).amountMinor)
        assertEquals(1_000_000L, sectionSum)
    }

    @Test
    fun `the fixed-income subtotal stays independent from the declared section total`() {
        val section = normalize(InterPositionFixture.controlledMismatch).section(PositionCategory.FIXED_INCOME)

        assertEquals(300_000L, exact(section.declaredGross.value).amountMinor)
        assertEquals(290_000L, exact(section.declaredSubtotal!!.gross.value).amountMinor)
    }

    @Test
    fun `rates become decimal ratios and indexers stay raw`() {
        val candidate =
            normalize(InterPositionFixture.fixedIncomeOnly)
                .section(PositionCategory.FIXED_INCOME)
                .candidates
                .first()

        assertEquals("1.000000000000", exact(candidate.schedule!!.rate!!.value).value.toPlainString())
        assertEquals("IPCA", candidate.identity.indexerRaw)
    }

    @Test
    fun `fund quantities reach the canonical scale without losing value`() {
        val candidate =
            normalize(InterPositionFixture.fundsOnly)
                .section(PositionCategory.FUNDS)
                .candidates
                .first()
        val quantity = exact(candidate.holding.quantity!!.value).value

        // The source prints eight decimals; `Quantity` normalises to the domain
        // scale from issue #5. Widening the scale adds zeros, it never rounds,
        // so the numeric claim is untouched.
        assertEquals(0, quantity.compareTo(BigDecimal("1000.00000000")))
        assertEquals(12, quantity.scale())
        assertEquals(200L, exact(candidate.holding.unitPrice!!.value).value.movePointRight(2).toLong())
    }

    @Test
    fun `a section without the report header reports the dates it cannot state`() {
        val canonical = normalize(InterPositionFixture.treasuryOnly)

        assertEquals(ValueQuality.Unknown, canonical.temporality.generatedAt)
        assertTrue(
            canonical.issues.any { issue ->
                issue.code == NormalizationIssueCode.VALUE_NOT_STATED &&
                    issue.fieldPath == "temporality.generatedAt"
            },
        )
    }

    @Test
    fun `an unknown section is surfaced rather than dropped or guessed`() {
        val canonical = normalize(InterPositionFixture.unknownSection)

        assertEquals(1, canonical.unknownSections.size)
        assertTrue(
            canonical.issues.any { issue -> issue.code == NormalizationIssueCode.SECTION_UNKNOWN },
        )
        assertTrue(
            canonical.sections.none { section -> section.category == PositionCategory.FUNDS },
        )
    }

    @Test
    fun `no candidate carries an identifier, account or snapshot`() {
        val canonical = normalize(InterPositionFixture.complete)

        assertTrue(canonical.sections.flatMap { section -> section.candidates }.isNotEmpty())
        assertTrue(
            canonical.sections.flatMap { section -> section.candidates }.all { candidate ->
                candidate.identity.descriptionRaw.isNotBlank()
            },
        )
    }

    @Test
    fun `issues carry a location and never a financial value`() {
        val canonical = normalize(InterPositionFixture.malformedValue)

        assertTrue(canonical.issues.isNotEmpty())
        canonical.issues.forEach { issue ->
            assertTrue(issue.fieldPath.isNotBlank())
            assertNull(
                Regex("\\d").find(issue.fieldPath.substringAfterLast('.')),
                "issue field path must not carry a value: ${issue.fieldPath}",
            )
        }
    }

    @Test
    fun `canonical snapshots match the golden file for every parseable fixture`() {
        val golden = FixturePaths.interPositionDirectory.resolve("canonical.golden.txt")
        val rendered =
            InterPositionFixture.all
                .filterNot { case -> case.name.startsWith("unsupported") }
                .joinToString("\n\n") { case ->
                    "=== ${case.name} ===\n${renderCanonicalSnapshot(normalize(case))}"
                }

        if (System.getProperty("updateGoldenFiles")?.toBoolean() == true) {
            Files.writeString(golden, rendered + "\n")
        }
        assertEquals(Files.readString(golden).trim(), rendered.trim(), "canonical output drifted")
    }
}
