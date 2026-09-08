package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.Money

/**
 * What a reconciliation rule compared.
 *
 * `FR-RECON-007`: the level is part of the result, because "the totals agree"
 * means something different at document level than at section level.
 */
enum class ReconciliationScope {
    DOCUMENT,
    CATEGORY,
    SECTION,
    SECTION_SUBTOTAL,
}

/**
 * The verdict of one comparison.
 *
 * [NOT_EVIDENCED] is not in the specification's three-way list, and it exists
 * because `FR-RECON-009` forbids the alternative: when the source states no
 * comparable total, or an input could not be read, reporting [PASS] would claim
 * a verification that never happened.
 */
enum class ReconciliationOutcome {
    PASS,
    WARNING,
    BLOCKER,
    NOT_EVIDENCED,
}

/** Why a comparison did not simply pass. */
enum class ReconciliationReason {
    /** Difference above the tolerance for this rule. */
    DIFFERENCE_ABOVE_TOLERANCE,

    /** Difference within tolerance but not zero. */
    DIFFERENCE_WITHIN_TOLERANCE,

    /** At least one input could not be read, so nothing can be concluded. */
    UNKNOWN_INPUT,

    /** At least one input is an estimate, so agreement proves less. */
    ESTIMATED_INPUT,

    /** The source states no comparable total at this level. */
    NO_DECLARED_TOTAL,

    /**
     * The declared total mixes currencies without publishing the rate it used,
     * so no arithmetic on our side can reproduce it (ADR 0034, `INV-002`).
     */
    UNEVIDENCED_CONVERSION,
}

/**
 * Stable identifiers for the comparisons this release performs.
 *
 * `NFR-RECON-DET-001` and `NFR-RECON-DET-002`: tolerance is parameterised per
 * identifiable rule and the rule that ran stays visible in preview and audit, so
 * a past result can be explained without re-running today's code.
 */
enum class ReconciliationRule(
    val code: String,
    val scope: ReconciliationScope,
) {
    SECTION_GROSS("RECON_SECTION_GROSS", ReconciliationScope.SECTION),
    FIXED_INCOME_SUBTOTAL("RECON_FIXED_INCOME_SUBTOTAL", ReconciliationScope.SECTION_SUBTOTAL),
    SUMMARY_ALLOCATION("RECON_SUMMARY_ALLOCATION", ReconciliationScope.CATEGORY),
    DOCUMENT_TOTAL("RECON_DOCUMENT_TOTAL", ReconciliationScope.DOCUMENT),
}

/**
 * How much disagreement a rule tolerates, in minor units, per currency.
 *
 * `FR-RECON-003`: the tolerance is explicit and travels with the result. It
 * absorbs rounding published by the source, and nothing else — widening it to
 * silence a recurring difference would hide a systematic error, which the
 * specification names as the one thing tolerance must not do.
 */
data class ReconciliationTolerance(
    val id: String,
    val byCurrency: Map<CurrencyCode, Long>,
) {
    fun minorUnitsFor(currency: CurrencyCode): Long = byCurrency[currency] ?: 0L

    companion object {
        val DEFAULT =
            ReconciliationTolerance(
                id = "release-0.1/one-minor-unit",
                byCurrency = mapOf(CurrencyCode.BRL to 1L, CurrencyCode.USD to 1L),
            )
    }
}

/**
 * One comparison, with everything needed to explain it later.
 *
 * `INV-018`: the same candidates and the same tolerance policy always produce
 * this same record, so a stored result stays meaningful after the code changes.
 */
data class ReconciliationResult(
    val rule: ReconciliationRule,
    val currency: CurrencyCode,
    val subject: String,
    val amounts: ReconciliationAmounts,
    val outcome: ReconciliationOutcome,
    val reason: ReconciliationReason?,
) {
    val blocks: Boolean
        get() = outcome == ReconciliationOutcome.BLOCKER
}

data class ReconciliationAmounts(
    val declared: Money?,
    val calculated: Money?,
    val differenceMinor: Long?,
    val toleranceMinor: Long,
    val toleranceId: String,
)

/**
 * The reconciliation of one document.
 *
 * `FR-RECON-005`: a single blocker is enough to stop a commit, and
 * [blockingResults] keeps the reason addressable rather than reducing the whole
 * report to a boolean.
 */
data class ReconciliationReport(
    val toleranceId: String,
    val results: List<ReconciliationResult>,
) {
    val blockingResults: List<ReconciliationResult>
        get() = results.filter { result -> result.blocks }

    val hasBlockers: Boolean
        get() = blockingResults.isNotEmpty()
}
