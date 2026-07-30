package br.com.vinicius.personalfinance.shared

import java.math.BigDecimal
import java.math.RoundingMode

enum class CurrencyCode(
    val fractionDigits: Int,
) {
    BRL(2),
    USD(2),
}

enum class RoundingPolicy(
    val mode: RoundingMode,
) {
    EXACT(RoundingMode.UNNECESSARY),
    HALF_EVEN(RoundingMode.HALF_EVEN),
    HALF_UP(RoundingMode.HALF_UP),
    DOWN(RoundingMode.DOWN),
}

@ConsistentCopyVisibility
data class Money private constructor(
    val amountMinor: Long,
    val currency: CurrencyCode,
) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return ofMinor(Math.addExact(amountMinor, other.amountMinor), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return ofMinor(Math.subtractExact(amountMinor, other.amountMinor), currency)
    }

    operator fun unaryMinus(): Money = ofMinor(Math.negateExact(amountMinor), currency)

    operator fun times(multiplier: Long): Money =
        ofMinor(Math.multiplyExact(amountMinor, multiplier), currency)

    fun toMajor(): BigDecimal = BigDecimal.valueOf(amountMinor, currency.fractionDigits)

    fun convert(
        exchangeRate: ExchangeRate,
        roundingPolicy: RoundingPolicy,
    ): Money {
        require(currency == exchangeRate.source) {
            "Exchange rate source ${exchangeRate.source} does not match money currency $currency"
        }

        val convertedMajor = toMajor().multiply(exchangeRate.ratio)
        return fromMajor(convertedMajor, exchangeRate.target, roundingPolicy)
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amountMinor.compareTo(other.amountMinor)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Currency mismatch: $currency and ${other.currency}"
        }
    }

    companion object {
        fun zero(currency: CurrencyCode): Money = ofMinor(0L, currency)

        fun ofMinor(
            amountMinor: Long,
            currency: CurrencyCode,
        ): Money = Money(amountMinor, currency)

        fun fromMajor(
            amountMajor: BigDecimal,
            currency: CurrencyCode,
            roundingPolicy: RoundingPolicy = RoundingPolicy.EXACT,
        ): Money {
            val amountMinor =
                amountMajor
                    .movePointRight(currency.fractionDigits)
                    .setScale(0, roundingPolicy.mode)
                    .longValueExact()

            return ofMinor(amountMinor, currency)
        }
    }
}

@ConsistentCopyVisibility
data class ExchangeRate private constructor(
    val source: CurrencyCode,
    val target: CurrencyCode,
    val ratio: BigDecimal,
) {
    init {
        require(source != target) { "Exchange rate currencies must be different" }
        require(ratio.signum() > 0) { "Exchange rate ratio must be positive" }
    }

    fun inverse(roundingPolicy: RoundingPolicy = RoundingPolicy.HALF_EVEN): ExchangeRate {
        val inverseRatio =
            BigDecimal.ONE.divide(
                ratio,
                RATE_SCALE,
                roundingPolicy.mode,
            )

        return of(target, source, inverseRatio)
    }

    companion object {
        const val RATE_SCALE = 12

        fun of(
            source: CurrencyCode,
            target: CurrencyCode,
            ratio: BigDecimal,
            roundingPolicy: RoundingPolicy = RoundingPolicy.EXACT,
        ): ExchangeRate =
            ExchangeRate(
                source = source,
                target = target,
                ratio = ratio.setScale(RATE_SCALE, roundingPolicy.mode),
            )
    }
}
