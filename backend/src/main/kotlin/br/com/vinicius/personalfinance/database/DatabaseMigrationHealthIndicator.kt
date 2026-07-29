package br.com.vinicius.personalfinance.database

import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("databaseMigration")
class DatabaseMigrationHealthIndicator(
    private val dataSource: DataSource,
    private val flyway: Flyway,
) : HealthIndicator {
    override fun health(): Health =
        runCatching {
            dataSource.connection.use { connection ->
                check(connection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS)) {
                    "Database connection validation failed"
                }
            }

            val validation = flyway.validateWithResult()
            check(validation.validationSuccessful) {
                "Flyway validation failed"
            }

            val migrationInfo = flyway.info()
            check(migrationInfo.pending().isEmpty()) {
                "Pending Flyway migrations detected"
            }

            val currentMigration = checkNotNull(migrationInfo.current()) {
                "No Flyway migration has been applied"
            }

            Health
                .up()
                .withDetail("migrationVersion", currentMigration.version?.version ?: "unversioned")
                .build()
        }.getOrElse {
            Health
                .down()
                .withDetail("reason", "database_or_migration_not_ready")
                .build()
        }

    private companion object {
        const val CONNECTION_VALIDATION_TIMEOUT_SECONDS = 2
    }
}
