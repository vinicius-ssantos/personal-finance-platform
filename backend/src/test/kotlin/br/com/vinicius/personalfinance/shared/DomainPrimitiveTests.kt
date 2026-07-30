package br.com.vinicius.personalfinance.shared

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DomainPrimitiveTests {
    @Test
    fun `unknown value is distinct from exact and estimated zero`() {
        val unknown: ValueQuality<Money> = ValueQuality.Unknown
        val exactZero = ValueQuality.Exact(Money.zero(CurrencyCode.BRL))
        val estimatedZero = ValueQuality.Estimated(Money.zero(CurrencyCode.BRL))

        assertNull(unknown.valueOrNull())
        assertNotEquals(unknown, exactZero)
        assertNotEquals(unknown, estimatedZero)
        assertEquals(Money.zero(CurrencyCode.BRL), exactZero.valueOrNull())
        assertEquals(Money.zero(CurrencyCode.BRL), estimatedZero.valueOrNull())
    }

    @Test
    fun `quality mapping preserves unknown estimated and exact semantics`() {
        val unknown: ValueQuality<Int> = ValueQuality.Unknown
        val estimated: ValueQuality<Int> = ValueQuality.Estimated(2)
        val exact: ValueQuality<Int> = ValueQuality.Exact(3)

        assertEquals(ValueQuality.Unknown, unknown.map(Int::toString))
        assertEquals(ValueQuality.Estimated("2"), estimated.map(Int::toString))
        assertEquals(ValueQuality.Exact("3"), exact.map(Int::toString))
    }

    @Test
    fun `financial timeline keeps position generation and market dates explicit`() {
        val positionDate = PositionDate(LocalDate.parse("2026-07-01"))
        val generatedAt = ReportGeneratedAt(Instant.parse("2026-07-02T10:15:30Z"))
        val marketReferenceDate = MarketReferenceDate(LocalDate.parse("2026-06-30"))
        val timeline =
            FinancialTimeline(
                positionDate = positionDate,
                generatedAt = generatedAt,
                marketReferenceDate = ValueQuality.Exact(marketReferenceDate),
            )

        assertEquals(positionDate, timeline.positionDate)
        assertEquals(generatedAt, timeline.generatedAt)
        assertEquals(ValueQuality.Exact(marketReferenceDate), timeline.marketReferenceDate)
    }

    @Test
    fun `domain clock can be injected with a deterministic instant`() {
        val expected = Instant.parse("2026-07-30T18:00:00Z")
        val clock = DomainClock.fixed(expected)

        assertEquals(expected, clock.now())
        assertEquals(expected, clock.now())
    }

    @Test
    fun `typed identifiers round-trip without becoming interchangeable strings`() {
        val raw = "123e4567-e89b-12d3-a456-426614174000"

        assertEquals(raw, ImportBatchId.parse(raw).toString())
        assertEquals(raw, FinancialAccountId.parse(raw).toString())
        assertEquals(raw, AssetId.parse(raw).toString())
        assertEquals(raw, PositionSnapshotId.parse(raw).toString())
        assertTrue(ImportBatchId.random().value != FinancialAccountId.random().value)
    }

    @Test
    fun `financial primitives expose no floating-point fields parameters or return types`() {
        val forbiddenTypes =
            setOf(
                Float::class.javaPrimitiveType,
                Float::class.javaObjectType,
                Double::class.javaPrimitiveType,
                Double::class.javaObjectType,
            )

        FINANCIAL_PRIMITIVE_CLASSES.forEach { type ->
            type.declaredFields.forEach { field ->
                assertFalse(
                    field.type in forbiddenTypes,
                    "${type.simpleName}.${field.name} uses floating point",
                )
            }
            type.declaredMethods.forEach { method ->
                assertFalse(
                    method.returnType in forbiddenTypes,
                    "${type.simpleName}.${method.name} returns floating point",
                )
                method.parameterTypes.forEach { parameterType ->
                    assertFalse(
                        parameterType in forbiddenTypes,
                        "${type.simpleName}.${method.name} accepts floating point",
                    )
                }
            }
        }
    }

    private companion object {
        val FINANCIAL_PRIMITIVE_CLASSES =
            listOf(
                Money::class.java,
                ExchangeRate::class.java,
                DecimalPolicy::class.java,
                Quantity::class.java,
                UnitPrice::class.java,
                Rate::class.java,
                DecimalRatio::class.java,
                FinancialTimeline::class.java,
                BigDecimal::class.java,
            )
    }
}
