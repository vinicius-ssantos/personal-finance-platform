# Database foundation

## Scope

Release 0.1 uses PostgreSQL as the only source of truth and Flyway as the only schema-change mechanism. This foundation does not define financial entities, import state, snapshots or public contracts; those belong to later issues.

## Managed schema

- database name in local development: `personal_finance`;
- application-owned schema: `personal_finance`;
- Flyway schema history: `personal_finance.flyway_schema_history`;
- no real-data or financial seed;
- no ORM-driven schema creation;
- no automatic destructive migration.

## Migration convention

Versioned SQL migrations live in:

```text
backend/src/main/resources/db/migration/
```

File names follow:

```text
V<positive_integer>__<lower_snake_case_description>.sql
```

Rules:

1. an applied migration is immutable;
2. a correction is a new migration, never a checksum rewrite;
3. one migration has one bounded schema purpose;
4. DDL should be transactional whenever PostgreSQL supports it;
5. migrations cannot contain personal data, real statements or financial seeds;
6. domain tables cannot be introduced before their owning issue;
7. `baseline-on-migrate` stays disabled because Release 0.1 begins from an empty managed schema;
8. Flyway `clean` stays disabled in the application and is enabled only inside ephemeral Testcontainers tests.

## Startup and failure behavior

Application startup requires a configured datasource. The local profile is explicit and loopback-only. Other environments must inject their own datasource properties.

Flyway runs before the application is considered ready. Invalid SQL, checksum differences or unapplied migrations keep startup/readiness closed. The custom `databaseMigration` health contributor validates:

- a live JDBC connection;
- Flyway checksum/state validation;
- absence of pending migrations;
- existence of a current applied migration.

The readiness group combines `readinessState`, `db` and `databaseMigration` at:

```text
/actuator/health/readiness
```

Health details remain hidden.

## Test proof

The integration suite uses an actual PostgreSQL container and proves:

- migration from an empty database;
- JDBC connectivity;
- readiness after successful migration;
- startup failure for intentionally invalid test-only SQL;
- full schema clean and rebuild using only migrations.

## Local reset boundary

`infra/scripts/reset-local-database.sh` is intentionally not a generic database reset tool. It accepts no URL, checks that the active Docker endpoint is local, uses a fixed Compose project and requires the literal confirmation `RESET_PERSONAL_FINANCE_LOCAL` before deleting the local volume.
