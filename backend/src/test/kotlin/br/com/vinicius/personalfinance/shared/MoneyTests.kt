package br.com.vinicius.personalfinance.shared

import java.math.BigDecimal
import java.util.SplittableRandom
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoneyTests {
    @Test
    fun `same-currency arithmetic preserves currency and minor units`() {
        val first = Money.ofMinor(12_345L, CurrencyCode.BRL)
        val second = Money.ofMinor(655L, CurrencyCode.BRL)

        assertEquals(Money.ofMinor(13_000L, CurrencyCode.BRL), first + second)
        assertEquals(Money.ofMinor(11_690L, CurrencyCode.BRL), first - second)
        assertEquals(Money.ofMinor(24_690L, CurrencyCode.BRL), first * 2L)
        assertEquals(Money.ofMinor(-12_345L, CurrencyCode.BRL), -first)
    }

    @Test
    fun `cross-currency arithmetic and ordering are rejected`() {
        val brl = Money.ofMinor(100L, CurrencyCode.BRL)
        val usd = Money.ofMinor(100L, CurrencyCode.USD)

        assertThrows(IllegalArgumentException::class.java) { brl + usd }
        assertThrows(IllegalArgumentException::class.java) { brl - usd }
        assertThrows(IllegalArgumentException::class.java) { brl.compareTo(usd) }
    }

    @Test
    fun `currency conversion requires an explicit directional exchange rate`() {
        val usd = Money.fromMajor(BigDecimal("10.00"), CurrencyCode.USD)
        val usdToBrl =
            ExchangeRate.of(
                source = CurrencyCode.USD,
                target = CurrencyCode.BRL,
                ratio = BigDecimal("5.250000000000"),
            )

        val converted = usd.convert(usdToBrl, RoundingPolicy.EXACT)

        assertEquals(Money.fromMajor(BigDecimal("52.50"), CurrencyCode.BRL), converted)
        assertThrows(IllegalArgumentException::class.java) {
            Money.ofMinor(1_000L, CurrencyCode.BRL)
                .convert(usdToBrl, RoundingPolicy.EXACT)
        }
    }

    @Test
    fun `major-unit conversion uses the selected deterministic rounding policy`() {
        assertThrows(ArithmeticException::class.java) {
            Money.fromMajor(
                BigDecimal("1.005"),
                CurrencyCode.BRL,
                RoundingPolicy.EXACT,
            )
        }

        assertEquals(
            Money.ofMinor(100L, CurrencyCode.BRL),
            Money.fromMajor(
                BigDecimal("1.005"),
                CurrencyCode.BRL,
                RoundingPolicy.HALF_EVEN,
            ),
        )
        assertEquals(
            Money.ofMinor(101L, CurrencyCode.BRL),
            Money.fromMajor(
                BigDecimal("1.005"),
                CurrencyCode.BRL,
                RoundingPolicy.HALF_UP,
            ),
        )
    }

    @Test
    fun `money addition satisfies identity commutativity and associativity properties`() {
        val random = SplittableRandom(PROPERTY_SEED)

        repeat(PROPERTY_CASES) {
            val first = random.nextLong(MIN_PROPERTY_VALUE, MAX_PROPERTY_VALUE)
            val second = random.nextLong(MIN_PROPERTY_VALUE, MAX_PROPERTY_VALUE)
            val third = random.nextLong(MIN_PROPERTY_VALUE, MAX_PROPERTY_VALUE)
            val a = Money.ofMinor(first, CurrencyCode.BRL)
            val b = Money.ofMinor(second, CurrencyCode.BRL)
            val c = Money.ofMinor(third, CurrencyCode.BRL)

            assertEquals(a, a + Money.zero(CurrencyCode.BRL))
            assertEquals(a + b, b + a)
            assertEquals((a + b) + c, a + (b + c))
            assertEquals(Money.zero(CurrencyCode.BRL), a + -a)
            assertTrue(a.compareTo(b).sign == first.compareTo(second).sign)
        }
    }

    private val Int.sign: Int
        get() = compareTo(0)

    private companion object {
        const val PROPERTY_CASES = 5_000
        const val PROPERTY_SEED = 5_202_607_30L
        const val MIN_PROPERTY_VALUE = -1_000_000_000L
        const val MAX_PROPERTY_VALUE = 1_000_000_001L
    }
}
