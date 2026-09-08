package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.DecimalRatio
import br.com.vinicius.personalfinance.shared.Money
import br.com.vinicius.personalfinance.shared.Quantity
import br.com.vinicius.personalfinance.shared.RoundingPolicy
import br.com.vinicius.personalfinance.shared.UnitPrice
import br.com.vinicius.personalfinance.shared.ValueQuality
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * A token reading: the canonical value when the grammar accepted it, and the
 * reason when it did not.
 *
 * `INV-003`: a rejected token yields [ValueQuality.Unknown], never zero and
 * never a best-effort number.
 */
data class Normalized<out T : Any>(
    val value: ValueQuality<T>,
    val issue: NormalizationIssueCode?,
) {
    companion object {
        fun <T : Any> exact(value: T): Normalized<T> = Normalized(ValueQuality.Exact(value), null)

        fun <T : Any> rejected(issue: NormalizationIssueCode): Normalized<T> = Normalized(ValueQuality.Unknown, issue)
    }
}

/**
 * The number, date and currency grammar of the Banco Inter consolidated-position
 * layout.
 *
 * `FR-NORMALIZE-001` scopes locale to the layout, so nothing here consults the
 * JVM default locale, the default timezone or the wall clock
 * (`NFR-PARSER-DET-002`, `NFR-PARSER-DET-003`). `FR-NORMALIZE-004` forbids a
 * permissive fallback: a token either matches the declared grammar exactly or
 * it is rejected.
 */
object PtBrTokens {
    /** The layout placeholder for a column that carries no value in this row. */
    const val ABSENT_TOKEN: String = "-"

    /**
     * `1.234,56`, `1234,56` or `10`.
     *
     * Thousand grouping, when present, must be complete: `1.2345` is a defect
     * either in the source or in our reading of it, and deciding which would be
     * exactly the permissive repair `FR-NORMALIZE-003` and `FR-NORMALIZE-004`
     * prohibit.
     */
    private val DECIMAL = Regex("^-?(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,\\d+)?$")

    /**
     * Captures any currency-looking prefix, not only the supported ones, so an
     * unsupported symbol is diagnosed as a currency problem instead of falling
     * through and being reported as a malformed number. The distinction matters
     * to whoever reads the issue: one means the layout changed, the other means
     * the value is unreadable.
     */
    private val CURRENCY_PREFIX = Regex("^(\\S*\\$) (.+)$")

    private val PERCENTAGE = Regex("^(.+)%$")

    private val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    private val DATE_TIME_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT)

    fun currencyOf(symbol: String): CurrencyCode? =
        when (symbol) {
            "R$" -> CurrencyCode.BRL
            "US$" -> CurrencyCode.USD
            else -> null
        }

    /**
     * Reads a pt-BR decimal preserving the source scale.
     *
     * The scale carries meaning: it states how precise the source was, and
     * flattening it would turn `0,50` and `0,50000000` into the same claim.
     */
    fun decimal(token: String): BigDecimal? =
        when {
            !DECIMAL.matches(token) -> null
            else -> BigDecimal(token.replace(".", "").replace(',', '.'))
        }

    /**
     * Reads `R$ 1.234,56`, or a bare `1.234,56` whose currency its column declares.
     *
     * A symbol disagreeing with the declared column currency is a mismatch, not
     * something to coerce: `INV-002` depends on currency being observed rather
     * than assumed.
     */
    fun money(
        token: String,
        declaredCurrency: CurrencyCode,
    ): Normalized<Money> {
        val prefixed = CURRENCY_PREFIX.matchEntire(token)
        val symbol = prefixed?.groupValues?.get(1)
        val currency = symbol?.let(::currencyOf)
        return when {
            symbol != null && currency == null ->
                Normalized.rejected(NormalizationIssueCode.CURRENCY_UNSUPPORTED)

            currency != null && currency != declaredCurrency ->
                Normalized.rejected(NormalizationIssueCode.CURRENCY_MISMATCH)

            else -> moneyAmount(prefixed?.groupValues?.get(2) ?: token, declaredCurrency)
        }
    }

    private fun moneyAmount(
        amountToken: String,
        currency: CurrencyCode,
    ): Normalized<Money> {
        val amount = decimal(amountToken)
        return when {
            amount == null -> Normalized.rejected(NormalizationIssueCode.VALUE_MALFORMED)
            amount.scale() > currency.fractionDigits ->
                Normalized.rejected(NormalizationIssueCode.VALUE_MALFORMED)

            else -> Normalized.exact(Money.fromMajor(amount, currency, RoundingPolicy.EXACT))
        }
    }

    fun quantity(token: String): Normalized<Quantity> {
        val amount = decimal(token)
        return when {
            amount == null -> Normalized.rejected(NormalizationIssueCode.VALUE_MALFORMED)
            amount.scale() > QUANTITY_SCALE -> Normalized.rejected(NormalizationIssueCode.VALUE_MALFORMED)
            else -> Normalized.exact(Quantity.of(amount, RoundingPolicy.EXACT))
        }
    }

    fun unitPrice(
        token: String,
        declaredCurrency: CurrencyCode,
    ): Normalized<UnitPrice> {
        val amount = unitPriceAmount(token, declaredCurrency)
        return when {
            amount == null -> Normalized.rejected(NormalizationIssueCode.VALUE_MALFORMED)
            amount.scale() > UNIT_PRICE_SCALE || amount.signum() < 0 ->
                Normalized.rejected(NormalizationIssueCode.VALUE_MALFORMED)

            else -> Normalized.exact(UnitPrice.of(amount, RoundingPolicy.EXACT))
        }
    }

    private fun unitPriceAmount(
        token: String,
        declaredCurrency: CurrencyCode,
    ): BigDecimal? {
        val prefixed = CURRENCY_PREFIX.matchEntire(token)
        val symbol = prefixed?.groupValues?.get(1)
        return when {
            symbol != null && currencyOf(symbol) != declaredCurrency -> null
            else -> decimal(prefixed?.groupValues?.get(2) ?: token)
        }
    }

    /**
     * `100,00%` becomes the ratio `1.00`, per ADR 0036: percentages are stored as
     * decimal ratios, so `0,30%` is `0.003` and never `0.30`.
     */
    fun rate(token: String): Normalized<DecimalRatio> {
        val points =
            PERCENTAGE
                .matchEntire(token)
                ?.groupValues
                ?.get(1)
                ?.let(::decimal)
        return when {
            points == null -> Normalized.rejected(NormalizationIssueCode.VALUE_MALFORMED)
            points.scale() + PERCENTAGE_SHIFT > RATIO_SCALE ->
                Normalized.rejected(NormalizationIssueCode.VALUE_MALFORMED)

            else -> Normalized.exact(DecimalRatio.fromPercentagePoints(points, RoundingPolicy.EXACT))
        }
    }

    fun date(token: String): Normalized<LocalDate> =
        runCatching { LocalDate.parse(token, DATE_FORMAT) }
            .fold(
                onSuccess = { parsed -> Normalized.exact(parsed) },
                onFailure = { Normalized.rejected(NormalizationIssueCode.DATE_INVALID) },
            )

    /**
     * `02/02/2026 08:15` resolved in the layout own zone.
     *
     * The source prints no offset, so the zone comes from the versioned layout
     * definition rather than from the machine running the backend.
     */
    fun instant(token: String): Normalized<Instant> =
        runCatching {
            LocalDateTime
                .parse(token, DATE_TIME_FORMAT)
                .atZone(InterPositionLayout202407.zone)
                .toInstant()
        }.fold(
            onSuccess = { parsed -> Normalized.exact(parsed) },
            onFailure = { Normalized.rejected(NormalizationIssueCode.DATE_INVALID) },
        )

    private const val QUANTITY_SCALE = 12
    private const val UNIT_PRICE_SCALE = 8
    private const val RATIO_SCALE = 12
    private const val PERCENTAGE_SHIFT = 2
}
