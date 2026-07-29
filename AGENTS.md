# AGENTS.md

## Mission

Implement the Personal Finance Platform incrementally, preserving financial correctness, privacy, auditability and the architecture documented in this repository.

## Read first

Before changing files, read:

1. `README.md`;
2. `docs/product/PRODUCT.md`;
3. `docs/architecture/ARCHITECTURE.md`;
4. `docs/architecture/INGESTION.md`;
5. `docs/security/SECURITY-MODEL.md`;
6. `docs/roadmap/RELEASE-0.1.md`;
7. `docs/adr/README.md`;
8. the referenced GitHub issue.

## Non-negotiable rules

- Never commit real financial documents or personal financial data.
- Never store, log, trace, measure or echo a PDF password.
- Never use `float` or `double` for financial values.
- Never turn missing data into zero.
- Never let parsing directly mutate confirmed portfolio state.
- Never overwrite confirmed financial history silently.
- Never share backend persistence entities with mobile.
- Never add money movement capabilities to MCP.
- Never use an LLM as the authority for financial parsing or calculations.
- Never broaden Release 0.1 without an approved issue and ADR when applicable.

## Work process

1. Confirm the issue scope, dependencies and acceptance criteria.
2. Inspect relevant files and existing ADRs.
3. Propose the smallest coherent change.
4. Add or update tests before considering the task complete.
5. Update contracts and documentation in the same PR.
6. Run all applicable checks.
7. Report changed files, tests, risks, assumptions and remaining work.

Do not merge, publish, deploy or modify protected branches unless explicitly requested and allowed by repository policy.

## Architecture boundaries

- Domain modules define ports; adapters implement them.
- Controllers, MCP tools and mobile endpoints contain no financial business rules.
- `portfolio` does not depend on ingestion or delivery adapters.
- `analytics` reads through domain/application ports, not database tables.
- REST and MCP call the same application services.
- `shared` remains small: Money, IDs, Clock and basic errors only.
- PostgreSQL is the source of truth.

## Kotlin conventions

- Constructor injection.
- Explicit nullability.
- Avoid `!!`; isolate unavoidable Java interop.
- Use value classes and sealed interfaces for constrained concepts.
- Use data classes for values and DTOs, not automatically for mutable entities.
- Use minor units for money and `BigDecimal` for quantities/rates.
- Store percentages as decimal ratios: `0.30` means 30%.
- Use coroutines only when the complete call chain benefits.
- Treat warnings as failures where configured.

## Import and parser changes

Every parser change requires synthetic fixtures and expected artifacts:

- extracted text;
- raw parser DTO;
- canonical representation;
- reconciliation result;
- evidence and confidence;
- expected issues/errors;
- parser/layout version impact.

Keep real discovery material outside the repository. Unknown layouts must fail safely instead of guessing.

## Security checks

Verify that changes do not expose:

- financial values in logs/metrics;
- PII in errors or fixtures;
- raw PDF/text through REST or MCP;
- tokens or secrets in source/config;
- decrypted temporary files after completion;
- remote access in the local-first profile.

## Testing expectations

- Unit tests for invariants, normalization and calculations.
- Property tests for financial primitives and monotonic rules.
- Testcontainers for PostgreSQL/Flyway and transactional behavior.
- Golden/regression tests for parsers.
- Contract tests for OpenAPI and MCP schemas.
- Security tests for upload, password lifecycle, redaction and authorization.
- E2E tests for the release flow.

## Definition of complete

A task is complete only when the issue criteria pass, tests are green, documentation and contracts match behavior, no privacy regression exists, and the change stays within the approved release scope.
