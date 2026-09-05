# Financial primitive contracts

## Purpose

The Release 0.1 domain uses a small set of framework-free types in the `shared` module. They prevent currency, scale, quality and time semantics from being represented by ambiguous primitive values.

## Money and currency

`Money` stores:

- `amountMinor: Long`;
- `currency: CurrencyCode`;
- two fraction digits for the currently supported BRL and USD currencies.

Addition, subtraction and ordering require equal currencies. Conversion requires a directional `ExchangeRate` and an explicit `RoundingPolicy`. There is no operation that implicitly combines or converts currencies.

Major-unit input uses `BigDecimal`. The default policy is `EXACT`; values that require rounding fail unless the caller selects a rounding policy deliberately.

## Decimal values

Canonical decimal policies are:

| Type | Scale | Default rounding |
|---|---:|---|
| `Quantity` | 12 | exact |
| `UnitPrice` | 8 | exact |
| `Rate` | 12 | exact |
| `DecimalRatio` | 12 | exact |

`UnitPrice` cannot be negative. Quantity and rate signs remain domain-specific and are not silently restricted by the shared primitive.

`DecimalRatio` follows ADR 0036: `0.30` represents 30%. Boundary code may use `fromPercentagePoints(30)` and `toPercentagePoints()`, but the canonical stored value remains the decimal ratio.

`Float` and `Double` are not accepted or returned by the financial primitives. A reflection-based test protects this constraint.

## Value quality

`ValueQuality<T>` has three explicit states:

- `Unknown` — no value is available;
- `Estimated(value)` — a value exists but is not exact;
- `Exact(value)` — the source or deterministic rule establishes the value.

Unknown is not represented by zero. Exact and estimated zero remain valid, distinguishable values.

## Financial time

The domain uses distinct types for distinct temporal concepts:

- `PositionDate(LocalDate)`;
- `ReportGeneratedAt(Instant)`;
- `MarketReferenceDate(LocalDate)`.

`FinancialTimeline` keeps the three concepts in separate fields, and market reference date may carry explicit quality. Technical time is obtained through `DomainClock`, which can wrap a system UTC clock or a fixed test clock.

## Typed IDs

The first shared identifiers are:

- `ImportBatchId`;
- `FinancialAccountId`;
- `AssetId`;
- `PositionSnapshotId`.

Each wraps a UUID but remains a separate Kotlin type. APIs and domain services must use the typed ID rather than raw UUIDs or strings.

## Verification

The test suite includes:

- same-currency and cross-currency arithmetic cases;
- explicit exchange-rate conversion;
- exact and rounded major-unit conversion;
- deterministic scale and rounding checks;
- seeded property tests for money identity, commutativity, associativity and ordering;
- seeded property tests for decimal normalization idempotence and ordering;
- unknown-versus-zero checks;
- fixed-clock and temporal-type checks;
- reflection checks that prohibit floating-point signatures.

The seeded property loops are deterministic and report the same failing case on every CI run.
