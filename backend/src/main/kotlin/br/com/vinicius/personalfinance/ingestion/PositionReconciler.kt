package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.Money
import br.com.vinicius.personalfinance.shared.ValueQuality
import org.springframework.stereotype.Component
import kotlin.math.abs

/** A sum that knows why it could not be produced. */
internal sealed interface Summed {
    data class Exact(
        val total: Money,
    ) : Summed

    data class Estimated(
        val total: Money,
    ) : Summed

    data object Unknown : Summed
}

/**
 * What to report when one side of a comparison is missing.
 *
 * The distinction is the point: a value we failed to read blocks, because
 * committing an amount nobody could verify is worse than committing a
 * disagreement somebody saw. A value the source never published does not block,
 * because no defect exists to fix.
 */
internal data class MissingSidePolicy(
    val outcome: ReconciliationOutcome,
    val reason: ReconciliationReason,
) {
    companion object {
        val UNREADABLE =
            MissingSidePolicy(ReconciliationOutcome.BLOCKER, ReconciliationReason.UNKNOWN_INPUT)

        val NOT_PUBLISHED =
            MissingSidePolicy(ReconciliationOutcome.NOT_EVIDENCED, ReconciliationReason.NO_DECLARED_TOTAL)
    }
}

/**
 * Compares what the document says against what its own rows add up to.
 *
 * Every comparison stays inside one currency (`FR-RECON-001`, `FR-RECON-002`).
 * Nothing here converts, and the one place the source itself mixes currencies is
 * reported as unevidenced rather than reconstructed.
 *
 * The reconciler reads; it never repairs. A disagreement is surfaced with both
 * numbers so a person can decide, because silently preferring the declared total
 * or the computed one would destroy the evidence that they disagreed.
 */
@Component
class PositionReconciler(
    private val tolerance: ReconciliationTolerance = ReconciliationTolerance.DEFAULT,
) {
    fun reconcile(document: CanonicalPositionDocument): ReconciliationReport =
        ReconciliationReport(
            toleranceId = tolerance.id,
            results =
                buildList {
                    document.sections.forEach { section ->
                        add(sectionGross(section))
                        section.declaredSubtotal?.let { subtotal -> add(fixedIncomeSubtotal(section, subtotal)) }
                    }
                    addAll(summaryAllocations(document))
                    add(documentTotal(document))
                },
        )

    /** The rows of a section against the total its header declares. */
    private fun sectionGross(section: CanonicalSection): ReconciliationResult =
        compare(
            rule = ReconciliationRule.SECTION_GROSS,
            currency = section.currency,
            subject = "sections[${section.sourceIndex}]",
            declared = section.declaredGross.value,
            calculated = sumOfRows(section),
            missing = MissingSidePolicy.UNREADABLE,
        )

    /**
     * The printed subtotal against the rows, kept separate from the header
     * comparison. `FR-RECON-008`: a section total that agrees must not be allowed
     * to mask a subtotal that does not.
     */
    private fun fixedIncomeSubtotal(
        section: CanonicalSection,
        subtotal: FixedIncomeSubtotal,
    ): ReconciliationResult =
        compare(
            rule = ReconciliationRule.FIXED_INCOME_SUBTOTAL,
            currency = section.currency,
            subject = "sections[${section.sourceIndex}].subtotal",
            declared = subtotal.gross.value,
            calculated = sumOfRows(section),
            missing = MissingSidePolicy.UNREADABLE,
        )

    /**
     * Each summary line against the section reporting the same category.
     *
     * Matching is by category and currency, never by position in the list, so a
     * report that reorders its summary cannot silently compare the wrong pair. A
     * summary line with no corresponding section is unevidenced rather than
     * wrong: the fixtures that carry a summary alone are exactly that case.
     */
    private fun summaryAllocations(document: CanonicalPositionDocument): List<ReconciliationResult> =
        document.summaries.flatMap { summary ->
            summary.allocations.map { allocation ->
                val section =
                    document.sections.firstOrNull { candidate ->
                        candidate.category == categoryOf(allocation.labelRaw) &&
                            candidate.currency == allocation.currency
                    }
                compare(
                    rule = ReconciliationRule.SUMMARY_ALLOCATION,
                    currency = allocation.currency,
                    subject = "summary[${allocation.labelRaw}]",
                    declared = allocation.amount.value,
                    calculated = section?.let { found -> single(found.declaredGross.value) } ?: Summed.Unknown,
                    missing =
                        when (section) {
                            null -> MissingSidePolicy.NOT_PUBLISHED
                            else -> MissingSidePolicy.UNREADABLE
                        },
                )
            }
        }

    /**
     * The document total is reported, never recomputed.
     *
     * The observed layout prints one BRL `Posição Total` that already absorbs the
     * USD category without publishing the rate it used. Any sum produced here
     * would disagree with it for a reason that is not an error, so the rule
     * records the declared value and refuses to conclude (ADR 0034, `INV-002`).
     */
    private fun documentTotal(document: CanonicalPositionDocument): ReconciliationResult {
        val declared = document.declaredPositionTotal?.value
        val declaredMoney = (declared as? ValueQuality.Exact)?.value
        val currency = declaredMoney?.currency ?: CurrencyCode.BRL
        val mixesCurrencies =
            document.sections
                .map { section -> section.currency }
                .distinct()
                .size > 1
        return ReconciliationResult(
            rule = ReconciliationRule.DOCUMENT_TOTAL,
            currency = currency,
            subject = "declaredPositionTotal",
            amounts = amounts(declaredMoney, null, null, currency),
            outcome = ReconciliationOutcome.NOT_EVIDENCED,
            reason =
                when {
                    mixesCurrencies -> ReconciliationReason.UNEVIDENCED_CONVERSION
                    else -> ReconciliationReason.NO_DECLARED_TOTAL
                },
        )
    }

    private fun sumOfRows(section: CanonicalSection): Summed =
        sum(section.candidates.map { candidate -> candidate.amounts.gross.value })

    private fun compare(
        rule: ReconciliationRule,
        currency: CurrencyCode,
        subject: String,
        declared: ValueQuality<Money>,
        calculated: Summed,
        missing: MissingSidePolicy,
    ): ReconciliationResult {
        val declaredMoney = moneyOf(declared)
        val calculatedMoney = totalOf(calculated)
        val difference =
            when {
                declaredMoney == null || calculatedMoney == null -> null
                else -> declaredMoney.amountMinor - calculatedMoney.amountMinor
            }
        val verdict = verdict(difference, currency, calculated, declared, missing)
        return ReconciliationResult(
            rule = rule,
            currency = currency,
            subject = subject,
            amounts = amounts(declaredMoney, calculatedMoney, difference, currency),
            outcome = verdict.outcome,
            reason = verdict.reason,
        )
    }

    private fun amounts(
        declared: Money?,
        calculated: Money?,
        difference: Long?,
        currency: CurrencyCode,
    ): ReconciliationAmounts =
        ReconciliationAmounts(
            declared = declared,
            calculated = calculated,
            differenceMinor = difference,
            toleranceMinor = tolerance.minorUnitsFor(currency),
            toleranceId = tolerance.id,
        )

    /**
     * A value we failed to read always blocks, whichever side it was on. Only an
     * absent counterpart falls back to the caller's policy.
     */
    private fun verdict(
        difference: Long?,
        currency: CurrencyCode,
        calculated: Summed,
        declared: ValueQuality<Money>,
        missing: MissingSidePolicy,
    ): Verdict =
        when {
            difference != null -> comparedVerdict(difference, currency, calculated)
            declared is ValueQuality.Unknown ->
                Verdict(ReconciliationOutcome.BLOCKER, ReconciliationReason.UNKNOWN_INPUT)

            else -> Verdict(missing.outcome, missing.reason)
        }

    private fun comparedVerdict(
        difference: Long,
        currency: CurrencyCode,
        calculated: Summed,
    ): Verdict =
        when {
            abs(difference) > tolerance.minorUnitsFor(currency) ->
                Verdict(ReconciliationOutcome.BLOCKER, ReconciliationReason.DIFFERENCE_ABOVE_TOLERANCE)

            calculated is Summed.Estimated ->
                Verdict(ReconciliationOutcome.WARNING, ReconciliationReason.ESTIMATED_INPUT)

            difference != 0L ->
                Verdict(ReconciliationOutcome.WARNING, ReconciliationReason.DIFFERENCE_WITHIN_TOLERANCE)

            else -> Verdict(ReconciliationOutcome.PASS, null)
        }

    private data class Verdict(
        val outcome: ReconciliationOutcome,
        val reason: ReconciliationReason?,
    )
}

/**
 * Adds up values that may not be addable.
 *
 * One unreadable row makes the whole sum unknown rather than smaller: treating a
 * missing amount as zero would produce a confident, wrong total and point the
 * resulting mismatch at the wrong place (`INV-003`).
 */
internal fun sum(values: List<ValueQuality<Money>>): Summed =
    when {
        values.isEmpty() -> Summed.Unknown
        values.any { value -> value is ValueQuality.Unknown } -> Summed.Unknown
        else -> {
            val total = values.mapNotNull(::moneyOf).reduce(Money::plus)
            when {
                values.any { value -> value is ValueQuality.Estimated } -> Summed.Estimated(total)
                else -> Summed.Exact(total)
            }
        }
    }

internal fun single(value: ValueQuality<Money>): Summed =
    when (value) {
        is ValueQuality.Exact -> Summed.Exact(value.value)
        is ValueQuality.Estimated -> Summed.Estimated(value.value)
        ValueQuality.Unknown -> Summed.Unknown
    }

private fun totalOf(summed: Summed): Money? =
    when (summed) {
        is Summed.Exact -> summed.total
        is Summed.Estimated -> summed.total
        Summed.Unknown -> null
    }

private fun moneyOf(value: ValueQuality<Money>): Money? =
    when (value) {
        is ValueQuality.Exact -> value.value
        is ValueQuality.Estimated -> value.value
        ValueQuality.Unknown -> null
    }

/**
 * Maps a summary label to the section category it reports on.
 *
 * The labels come from the versioned layout, so this is a lookup, not inference.
 */
internal fun categoryOf(label: String): PositionCategory? =
    when (label) {
        "Tesouro Direto" -> PositionCategory.TREASURY
        "Renda Variável" -> PositionCategory.BRAZILIAN_EQUITY
        "Renda Fixa" -> PositionCategory.FIXED_INCOME
        "Renda Variável Internacional" -> PositionCategory.INTERNATIONAL_EQUITY
        "Fundos de Investimentos" -> PositionCategory.FUNDS
        else -> null
    }
