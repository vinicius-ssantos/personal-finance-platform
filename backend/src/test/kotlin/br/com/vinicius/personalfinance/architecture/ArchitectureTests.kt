package br.com.vinicius.personalfinance.architecture

import architecturefixture.adapter.AdapterFixture
import architecturefixture.domain.IllegalDomainDependency
import br.com.vinicius.personalfinance.PersonalFinanceApplication
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ArchitectureTests {
    private val productionClasses: JavaClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT_PACKAGE)

    @Test
    fun `Spring Modulith model has no cycles or illegal module access`() {
        ApplicationModules.of(PersonalFinanceApplication::class.java).verify()
    }

    @Test
    fun `domain modules do not depend on delivery or persistence adapters`() {
        domainMustNotDependOnAdapters(
            domainPackages = DOMAIN_PACKAGES,
            adapterPackages = ADAPTER_PACKAGES,
        ).check(productionClasses)
    }

    @Test
    fun `API does not bypass application modules to reach persistence`() {
        noClasses()
            .that()
            .resideInAPackage(API_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(PERSISTENCE_PACKAGE)
            .check(productionClasses)
    }

    @Test
    fun `shared remains free from frameworks persistence and providers`() {
        noClasses()
            .that()
            .resideInAPackage(SHARED_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*FORBIDDEN_SHARED_DEPENDENCIES)
            .check(productionClasses)
    }

    @Test
    fun `domain to adapter rule detects an intentional inversion`() {
        val fixtureClasses =
            ClassFileImporter().importClasses(
                IllegalDomainDependency::class.java,
                AdapterFixture::class.java,
            )

        val result =
            domainMustNotDependOnAdapters(
                domainPackages = arrayOf("architecturefixture.domain.."),
                adapterPackages = arrayOf("architecturefixture.adapter.."),
            ).evaluate(fixtureClasses)

        assertTrue(result.hasViolation())
    }

    private fun domainMustNotDependOnAdapters(
        domainPackages: Array<String>,
        adapterPackages: Array<String>,
    ): ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage(*domainPackages)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*adapterPackages)

    private companion object {
        const val ROOT_PACKAGE = "br.com.vinicius.personalfinance"
        const val API_PACKAGE = "..api.."
        const val PERSISTENCE_PACKAGE = "..persistence.."
        const val SHARED_PACKAGE = "..shared.."

        val DOMAIN_PACKAGES =
            arrayOf(
                "..shared..",
                "..audit..",
                "..portfolio..",
                "..ingestion..",
            )

        val ADAPTER_PACKAGES =
            arrayOf(
                API_PACKAGE,
                PERSISTENCE_PACKAGE,
            )

        val FORBIDDEN_SHARED_DEPENDENCIES =
            arrayOf(
                "org.springframework..",
                "org.flywaydb..",
                "org.postgresql..",
                "org.testcontainers..",
                "java.sql..",
                "javax.sql..",
            )
    }
}
