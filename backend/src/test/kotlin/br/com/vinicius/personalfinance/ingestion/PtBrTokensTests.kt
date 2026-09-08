package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.ValueQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class PtBrTokensTests {
    private fun <T : Any> valueOf(normalized: Normalized<T>): T? = (normalized.value as? ValueQuality.Exact)?.value

    @Test
    fun `pt-BR grouping and decimal comma are read exactly`() {
        assertEquals(BigDecimal("1234.56"), PtBrTokens.decimal("1.234,56"))
        assertEquals(BigDecimal("1234.56"), PtBrTokens.decimal("1234,56"))
        assertEquals(BigDecimal("10"), PtBrTokens.decimal("10"))
        assertEquals(BigDecimal("-1234.56"), PtBrTokens.decimal("-1.234,56"))
    }

    @Test
    fun `source scale is preserved because it states how precise the source was`() {
        assertEquals(2, PtBrTokens.decimal("0,50")?.scale())
        assertEquals(8, PtBrTokens.decimal("0,50000000")?.scale())
    }

    @TestFactory
    fun `tokens outside the layout grammar are rejected instead of repaired`(): List<DynamicTest> =
        listOf(
            "1.25X,00" to "letter inside the amount",
            "1.2345" to "incomplete thousand grouping",
            "1,234,56" to "two decimal separators",
            "1.234.5" to "grouping that does not close",
            "" to "empty token",
            "R$ 1.000,00" to "currency symbol is not part of a bare decimal",
            "1 234,56" to "space as a grouping separator",
        ).map { (token, why) ->
            DynamicTest.dynamicTest("$why: '$token'") {
                assertNull(PtBrTokens.decimal(token), "expected '$token' to be rejected")
            }
        }

    @Test
    fun `money keeps minor units and never becomes floating point`() {
        val money = valueOf(PtBrTokens.money("R$ 1.234,56", CurrencyCode.BRL))

        assertEquals(123_456L, money?.amountMinor)
        assertEquals(CurrencyCode.BRL, money?.currency)
    }

    @Test
    fun `a bare amount takes the currency its column declares`() {
        assertEquals(0L, valueOf(PtBrTokens.money("0,00", CurrencyCode.BRL))?.amountMinor)
        assertEquals(CurrencyCode.USD, valueOf(PtBrTokens.money("12,00", CurrencyCode.USD))?.currency)
    }

    @Test
    fun `a symbol disagreeing with its column is a mismatch and not a conversion`() {
        val mismatch = PtBrTokens.money("US$ 200,00", CurrencyCode.BRL)

        assertEquals(ValueQuality.Unknown, mismatch.value)
        assertEquals(NormalizationIssueCode.CURRENCY_MISMATCH, mismatch.issue)
    }

    @Test
    fun `an unsupported currency symbol is refused rather than dropped`() {
        val unsupported = PtBrTokens.money("EUR$ 10,00", CurrencyCode.BRL)

        assertEquals(ValueQuality.Unknown, unsupported.value)
        assertEquals(NormalizationIssueCode.CURRENCY_UNSUPPORTED, unsupported.issue)
    }

    @Test
    fun `more decimals than the currency has is malformed, not rounded`() {
        val overPrecise = PtBrTokens.money("R$ 1,005", CurrencyCode.BRL)

        assertEquals(ValueQuality.Unknown, overPrecise.value)
        assertEquals(NormalizationIssueCode.VALUE_MALFORMED, overPrecise.issue)
    }

    @Test
    fun `percentages become decimal ratios per ADR 0036`() {
        assertEquals(BigDecimal("1.000000000000"), valueOf(PtBrTokens.rate("100,00%"))?.value)
        assertEquals(BigDecimal("0.003000000000"), valueOf(PtBrTokens.rate("0,30%"))?.value)
    }

    @Test
    fun `an impossible date is refused instead of rolled into a nearby one`() {
        val impossible = PtBrTokens.date("31/02/2026")

        assertEquals(ValueQuality.Unknown, impossible.value)
        assertEquals(NormalizationIssueCode.DATE_INVALID, impossible.issue)
        assertEquals(NormalizationIssueCode.DATE_INVALID, PtBrTokens.date("2026-01-31").issue)
    }

    @Test
    fun `the generation instant resolves in the layout zone, not the machine zone`() {
        val expected = Instant.parse("2026-02-02T11:15:00Z")

        withJvmDefaults(Locale.of("en", "US"), TimeZone.getTimeZone("UTC")) {
            assertEquals(expected, valueOf(PtBrTokens.instant("02/02/2026 08:15")))
        }
        withJvmDefaults(Locale.of("ja", "JP"), TimeZone.getTimeZone("Asia/Tokyo")) {
            assertEquals(expected, valueOf(PtBrTokens.instant("02/02/2026 08:15")))
        }
    }

    @Test
    fun `reading does not depend on the JVM default locale`() {
        val expected = valueOf(PtBrTokens.money("R$ 1.234,56", CurrencyCode.BRL))

        withJvmDefaults(Locale.of("de", "DE"), TimeZone.getTimeZone("Europe/Berlin")) {
            assertEquals(expected, valueOf(PtBrTokens.money("R$ 1.234,56", CurrencyCode.BRL)))
            assertEquals(BigDecimal("1234.56"), PtBrTokens.decimal("1.234,56"))
        }
    }

    private fun withJvmDefaults(
        locale: Locale,
        zone: TimeZone,
        block: () -> Unit,
    ) {
        val previousLocale = Locale.getDefault()
        val previousZone = TimeZone.getDefault()
        try {
            Locale.setDefault(locale)
            TimeZone.setDefault(zone)
            block()
        } finally {
            Locale.setDefault(previousLocale)
            TimeZone.setDefault(previousZone)
        }
    }
}
