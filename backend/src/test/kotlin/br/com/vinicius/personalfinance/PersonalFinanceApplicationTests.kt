package br.com.vinicius.personalfinance

import br.com.vinicius.personalfinance.database.DatabaseMigrationHealthIndicator
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.health.contributor.Status
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PersonalFinanceApplicationTests
    @Autowired
    constructor(
    private val jdbcTemplate: JdbcTemplate,
    private val flyway: Flyway,
    private val databaseMigrationHealthIndicator: DatabaseMigrationHealthIndicator,
) {
    @Test
    @Order(1)
    fun `application starts with PostgreSQL and valid migrations`() {
        assertTrue(postgres.isRunning)
        assertEquals(Status.UP, databaseMigrationHealthIndicator.health().status)
    }

    @Test
    @Order(2)
    fun `repository connectivity uses the real PostgreSQL container`() {
        val result = jdbcTemplate.queryForObject("select 1", Int::class.java)

        assertEquals(1, result)
    }

    @Test
    @Order(3)
    fun `Flyway applies the baseline to an empty database`() {
        val schemaExists =
            jdbcTemplate.queryForObject(
                "select exists (select 1 from information_schema.schemata where schema_name = 'personal_finance')",
                Boolean::class.java,
            )
        val successfulMigrations =
            jdbcTemplate.queryForObject(
                "select count(*) from personal_finance.flyway_schema_history where success",
                Long::class.java,
            )

        assertEquals(true, schemaExists)
        assertEquals(1L, successfulMigrations)
        assertEquals(Status.UP, databaseMigrationHealthIndicator.health().status)
    }

    @Test
    @Order(4)
    fun `application startup fails when a migration is invalid`() {
        val exception =
            assertThrows<Exception> {
                SpringApplicationBuilder(PersonalFinanceApplication::class.java)
                    .web(WebApplicationType.NONE)
                    .properties(
                        "spring.application.name=broken-migration-test",
                        "spring.datasource.url=${postgres.jdbcUrl}",
                        "spring.datasource.username=${postgres.username}",
                        "spring.datasource.password=${postgres.password}",
                        "spring.flyway.locations=classpath:db/broken-migration",
                        "spring.flyway.default-schema=broken_migration_test",
                        "spring.flyway.schemas=broken_migration_test",
                        "spring.flyway.clean-disabled=true",
                    ).run()
            }

        assertTrue(
            generateSequence<Throwable>(exception) { it.cause }.any { it is FlywayException },
            "Expected application startup failure to contain a FlywayException",
        )
    }

    @Test
    @Order(5)
    fun `clean schema can be rebuilt entirely from migrations`() {
        flyway.clean()
        assertEquals(Status.DOWN, databaseMigrationHealthIndicator.health().status)

        val migrationResult = flyway.migrate()

        assertEquals(1, migrationResult.migrationsExecuted)
        assertEquals(Status.UP, databaseMigrationHealthIndicator.health().status)
    }

    companion object {
        private const val POSTGRES_IMAGE = "postgres:18.1-alpine"

        @Container
        @ServiceConnection
        @JvmField
        val postgres =
            PostgreSQLContainer<Nothing>(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName("personal_finance_test")
                .withUsername("personal_finance_test")
                .withPassword("test-only")

        @DynamicPropertySource
        @JvmStatic
        fun testProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.flyway.clean-disabled") { false }
        }
    }
}
