# CLAUDE.md

Use `AGENTS.md` as the authoritative repository instructions.

## Required startup sequence

1. Read `AGENTS.md` completely.
2. Read the referenced GitHub issue and its dependencies.
3. Read the product, architecture, ingestion, security, release and ADR documents linked from `AGENTS.md`.
4. Inspect existing code and tests before proposing changes.

## Planning

For non-trivial tasks, produce a short implementation plan covering:

- files/modules affected;
- invariants and architecture boundaries;
- tests required;
- contract/documentation changes;
- security and privacy risks;
- rollback or migration concerns.

Do not start broad implementation while key requirements are ambiguous. Prefer the smallest vertical slice that satisfies the issue.

## Repository-specific cautions

- Financial correctness and provenance are more important than convenience.
- Unknown data remains unknown; do not guess values or classifications.
- Real Banco Inter documents and user financial data must never enter the repository or model context beyond an explicitly private discovery workflow.
- PDF passwords are ephemeral secrets and must be absent from every persistent or observable channel.
- Controllers, tools and UI layers must not own financial rules.
- Release 0.1 is local-first and position-only; do not pull Android, MCP, movements or Open Finance into its critical path.

## Completion report

When finishing a task, report:

- summary of behavior changed;
- files changed;
- tests executed and results;
- assumptions;
- security/privacy review;
- remaining risks or follow-up issues.

Never claim a check passed if it was not executed.
