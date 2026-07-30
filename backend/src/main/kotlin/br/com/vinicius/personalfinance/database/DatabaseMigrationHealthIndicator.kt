package br.com.vinicius.personalfinance.database

@Deprecated(
    message = "Database adapters belong to the persistence module",
    level = DeprecationLevel.HIDDEN,
)
internal typealias DatabaseMigrationHealthIndicator =
    br.com.vinicius.personalfinance.persistence.health.DatabaseMigrationHealthIndicator
