package br.com.vinicius.personalfinance.shared

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@JvmInline
value class PositionDate(
    val value: LocalDate,
) : Comparable<PositionDate> {
    override fun compareTo(other: PositionDate): Int = value.compareTo(other.value)
}

@JvmInline
value class ReportGeneratedAt(
    val value: Instant,
) : Comparable<ReportGeneratedAt> {
    override fun compareTo(other: ReportGeneratedAt): Int = value.compareTo(other.value)
}

@JvmInline
value class MarketReferenceDate(
    val value: LocalDate,
) : Comparable<MarketReferenceDate> {
    override fun compareTo(other: MarketReferenceDate): Int = value.compareTo(other.value)
}

data class FinancialTimeline(
    val positionDate: PositionDate,
    val generatedAt: ReportGeneratedAt,
    val marketReferenceDate: ValueQuality<MarketReferenceDate>,
)

fun interface DomainClock {
    fun now(): Instant

    companion object {
        fun systemUtc(): DomainClock = from(Clock.systemUTC())

        fun fixed(instant: Instant): DomainClock =
            from(Clock.fixed(instant, ZoneOffset.UTC))

        fun from(clock: Clock): DomainClock = DomainClock(clock::instant)
    }
}
