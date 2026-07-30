package br.com.vinicius.personalfinance.shared

import java.math.BigDecimal

data class DecimalPolicy(
    val scale: Int,
    val defaultRounding: RoundingPolicy = RoundingPolicy.EXACT,
) {
    init {
        require(scale >= 0) { "Decimal scale must not be negative" }
    }

    fun normalize(
        value: BigDecimal,
        roundingPolicy: RoundingPolicy = defaultRounding,
    ): BigDecimal = value.setScale(scale, roundingPolicy.mode)
}

object FinancialDecimalPolicies {
    val QUANTITY = DecimalPolicy(scale = 12)
    val UNIT_PRICE = DecimalPolicy(scale = 8)
    val RATE = DecimalPolicy(scale = 12)
    val DECIMAL_RATIO = DecimalPolicy(scale = 12)
}

@JvmInline
value class Quantity private constructor(
    val value: BigDecimal,
) : Comparable<Quantity> {
    override fun compareTo(other: Quantity): Int = value.compareTo(other.value)

    companion object {
        fun of(
            value: BigDecimal,
            roundingPolicy: RoundingPolicy = RoundingPolicy.EXACT,
        ): Quantity =
            Quantity(
                FinancialDecimalPolicies.QUANTITY.normalize(value, roundingPolicy),
            )
    }
}

@JvmInline
value class UnitPrice private constructor(
    val value: BigDecimal,
) : Comparable<UnitPrice> {
    init {
        require(value.signum() >= 0) { "Unit price must not be negative" }
    }

    override fun compareTo(other: UnitPrice): Int = value.compareTo(other.value)

    companion object {
        fun of(
            value: BigDecimal,
            roundingPolicy: RoundingPolicy = RoundingPolicy.EXACT,
        ): UnitPrice =
            UnitPrice(
                FinancialDecimalPolicies.UNIT_PRICE.normalize(value, roundingPolicy),
            )
    }
}

@JvmInline
value class Rate private constructor(
    val value: BigDecimal,
) : Comparable<Rate> {
    override fun compareTo(other: Rate): Int = value.compareTo(other.value)

    companion object {
        fun of(
            value: BigDecimal,
            roundingPolicy: RoundingPolicy = RoundingPolicy.EXACT,
        ): Rate =
            Rate(
                FinancialDecimalPolicies.RATE.normalize(value, roundingPolicy),
            )
    }
}

@JvmInline
value class DecimalRatio private constructor(
    val value: BigDecimal,
) : Comparable<DecimalRatio> {
    override fun compareTo(other: DecimalRatio): Int = value.compareTo(other.value)

    fun toPercentagePoints(): BigDecimal =
        FinancialDecimalPolicies.DECIMAL_RATIO.normalize(
            value.movePointRight(PERCENTAGE_SHIFT),
        )

    companion object {
        private const val PERCENTAGE_SHIFT = 2

        fun of(
            value: BigDecimal,
            roundingPolicy: RoundingPolicy = RoundingPolicy.EXACT,
        ): DecimalRatio =
            DecimalRatio(
                FinancialDecimalPolicies.DECIMAL_RATIO.normalize(value, roundingPolicy),
            )

        fun fromPercentagePoints(
            percentagePoints: BigDecimal,
            roundingPolicy: RoundingPolicy = RoundingPolicy.EXACT,
        ): DecimalRatio =
            of(
                percentagePoints.movePointLeft(PERCENTAGE_SHIFT),
                roundingPolicy,
            )
    }
}
