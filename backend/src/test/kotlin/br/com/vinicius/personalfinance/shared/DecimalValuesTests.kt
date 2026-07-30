package br.com.vinicius.personalfinance.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.SplittableRandom

class DecimalValuesTests {
    @Test
    fun `exact policy rejects digits beyond the configured scale`() {
        assertThrows(ArithmeticException::class.java) {
            UnitPrice.of(BigDecimal("10.123456789"))
        }
        assertThrows(ArithmeticException::class.java) {
            Quantity.of(BigDecimal("1.1234567890123"))
        }
    }

    @Test
    fun `explicit rounding normalizes quantity price and rate deterministically`() {
        assertEquals(
            BigDecimal("1.123456789012"),
            Quantity
                .of(
                    BigDecimal("1.1234567890124"),
                    RoundingPolicy.HALF_EVEN,
                ).value,
        )
        assertEquals(
            BigDecimal("10.12345679"),
            UnitPrice
                .of(
                    BigDecimal("10.123456789"),
                    RoundingPolicy.HALF_UP,
                ).value,
        )
        assertEquals(
            BigDecimal("-0.123456789012"),
            Rate
                .of(
                    BigDecimal("-0.1234567890124"),
                    RoundingPolicy.HALF_EVEN,
                ).value,
        )
    }

    @Test
    fun `unit price rejects negative values while zero remains explicit`() {
        assertThrows(IllegalArgumentException::class.java) {
            UnitPrice.of(BigDecimal("-0.01"))
        }

        assertEquals(BigDecimal("0.00000000"), UnitPrice.of(BigDecimal.ZERO).value)
    }

    @Test
    fun `decimal ratio uses zero point thirty to represent thirty percent`() {
        val ratio = DecimalRatio.fromPercentagePoints(BigDecimal("30"))

        assertEquals(BigDecimal("0.300000000000"), ratio.value)
        assertEquals(BigDecimal("30.000000000000"), ratio.toPercentagePoints())
    }

    @Test
    fun `normalization is idempotent and ordering follows numeric values`() {
        val random = SplittableRandom(PROPERTY_SEED)

        repeat(PROPERTY_CASES) {
            val first = randomDecimal(random)
            val second = randomDecimal(random)
            val normalizedFirst = Quantity.of(first, RoundingPolicy.HALF_EVEN)
            val normalizedAgain = Quantity.of(normalizedFirst.value)
            val normalizedSecond = Quantity.of(second, RoundingPolicy.HALF_EVEN)

            assertEquals(normalizedFirst, normalizedAgain)
            assertTrue(
                normalizedFirst.compareTo(normalizedSecond).sign ==
                    normalizedFirst.value.compareTo(normalizedSecond.value).sign,
            )
        }
    }

    private fun randomDecimal(random: SplittableRandom): BigDecimal =
        BigDecimal.valueOf(
            random.nextLong(MIN_UNSCALED, MAX_UNSCALED),
            random.nextInt(MIN_SOURCE_SCALE, MAX_SOURCE_SCALE),
        )

    private val Int.sign: Int
        get() = compareTo(0)

    private companion object {
        const val PROPERTY_CASES = 5_000
        const val PROPERTY_SEED = 5_202_607_31L
        const val MIN_UNSCALED = -1_000_000_000_000L
        const val MAX_UNSCALED = 1_000_000_000_001L
        const val MIN_SOURCE_SCALE = 0
        const val MAX_SOURCE_SCALE = 16
    }
}
