package br.com.vinicius.personalfinance.ingestion

import br.com.vinicius.personalfinance.shared.CurrencyCode
import br.com.vinicius.personalfinance.shared.DecimalRatio
import br.com.vinicius.personalfinance.shared.MarketReferenceDate
import br.com.vinicius.personalfinance.shared.Money
import br.com.vinicius.personalfinance.shared.PositionDate
import br.com.vinicius.personalfinance.shared.Quantity
import br.com.vinicius.personalfinance.shared.ReportGeneratedAt
import br.com.vinicius.personalfinance.shared.UnitPrice
import br.com.vinicius.personalfinance.shared.ValueQuality
import java.time.LocalDate

/**
 * Where a canonical value came from.
 *
 * `FR-EVIDENCE-001..005`: provenance must survive without persisting the PDF or
 * the extracted text, so the page number plus the exact source token is the unit
 * of traceability.
 */
data class FieldEvidence(
    val pageNumber: Int,
    val tokenRaw: String,
)

/**
 * A canonical value bound to the source token that produced it.
 *
 * The value may be [ValueQuality.Unknown] while the evidence still exists: that
 * combination is precisely how the model says "the source printed something here
 * and we refused to interpret it", which `INV-003` requires to stay visible.
 */
data class EvidencedValue<out T : Any>(
    val value: ValueQuality<T>,
    val evidence: FieldEvidence,
)

/** Which part of the report a candidate came from. Not an asset taxonomy. */
enum class PositionCategory {
    TREASURY,
    BRAZILIAN_EQUITY,
    FIXED_INCOME,
    INTERNATIONAL_EQUITY,
    FUNDS,
}

/**
 * Textual hints about what the position is.
 *
 * Deliberately raw and deliberately not an identity: `FR-ASSET-002` forbids
 * treating a name as a strong identifier, and resolving these hints into an
 * `Asset` is a separate step with its own confidence rules.
 */
data class PositionIdentityHints(
    val descriptionRaw: String,
    val assetCodeRaw: String?,
    val indexerRaw: String?,
    val pageNumber: Int,
)

data class PositionHolding(
    val quantity: EvidencedValue<Quantity>?,
    val unitPrice: EvidencedValue<UnitPrice>?,
)

data class PositionTaxes(
    val expectedIof: EvidencedValue<Money>?,
    val expectedIr: EvidencedValue<Money>?,
)

data class PositionAmounts(
    val gross: EvidencedValue<Money>,
    val applied: EvidencedValue<Money>?,
    val net: EvidencedValue<Money>?,
    val market: EvidencedValue<Money>?,
    val redemptionAvailability: EvidencedValue<Money>?,
    val taxes: PositionTaxes?,
)

data class PositionSchedule(
    val applicationDate: EvidencedValue<LocalDate>?,
    val maturityDate: EvidencedValue<LocalDate>?,
    val rate: EvidencedValue<DecimalRatio>?,
)

/**
 * One canonical position candidate.
 *
 * `INV-006`: a candidate is a reading of a document, not portfolio state. It
 * carries no identifier, no account and no snapshot, because nothing here has
 * been confirmed by a human yet.
 */
data class PositionCandidate(
    val category: PositionCategory,
    val currency: CurrencyCode,
    val identity: PositionIdentityHints,
    val holding: PositionHolding,
    val amounts: PositionAmounts,
    val schedule: PositionSchedule?,
)

data class FixedIncomeSubtotal(
    val applied: EvidencedValue<Money>,
    val gross: EvidencedValue<Money>,
    val net: EvidencedValue<Money>,
)

/**
 * A canonical section with the total the source declared for it.
 *
 * [declaredGross] and the candidate amounts stay independent on purpose: the
 * comparison between them is reconciliation, and pre-computing it here would
 * hide the very mismatch reconciliation exists to find (`FR-RECON-008`).
 */
data class CanonicalSection(
    val category: PositionCategory,
    val currency: CurrencyCode,
    /** Index in the parser section list, so every field path agrees. */
    val sourceIndex: Int,
    val pageNumber: Int,
    val declaredGross: EvidencedValue<Money>,
    val declaredSubtotal: FixedIncomeSubtotal?,
    val candidates: List<PositionCandidate>,
)

data class SummaryAllocation(
    val labelRaw: String,
    val currency: CurrencyCode,
    val amount: EvidencedValue<Money>,
)

data class CanonicalSummary(
    val currency: CurrencyCode,
    val pageNumber: Int,
    val declaredTotal: EvidencedValue<Money>,
    val allocations: List<SummaryAllocation>,
)

/**
 * The three distinct dates the release keeps apart (ADR 0031).
 *
 * Not a [br.com.vinicius.personalfinance.shared.FinancialTimeline]: that type
 * requires a known position date and a known generation instant, and a document
 * that omits either must stay representable rather than be completed by a guess.
 */
data class CanonicalTemporality(
    val positionDate: ValueQuality<PositionDate>,
    val generatedAt: ValueQuality<ReportGeneratedAt>,
    val marketReferenceDate: ValueQuality<MarketReferenceDate>,
)

/** A section that survived parsing without any parser claiming to understand it. */
data class UnknownSectionRecord(
    val headingRaw: String,
    val pageNumber: Int,
    val lineCount: Int,
)

data class ParsedDocumentIdentity(
    val descriptor: LayoutDescriptor,
    val parserId: String,
    val parserVersion: String,
)

/**
 * The canonical reading of one position report.
 *
 * [declaredPositionTotal] is preserved exactly as the source stated it. The
 * observed layout prints a single BRL total that already absorbs the USD
 * category without printing the rate it used, so this value may never be
 * recomputed from the sections nor compared against their sum by an implicit
 * conversion (ADR 0034, `INV-002`).
 */
data class CanonicalPositionDocument(
    val identity: ParsedDocumentIdentity,
    val temporality: CanonicalTemporality,
    val declaredPositionTotal: EvidencedValue<Money>?,
    val summaries: List<CanonicalSummary>,
    val sections: List<CanonicalSection>,
    val unknownSections: List<UnknownSectionRecord>,
    val issues: List<NormalizationIssue>,
)
