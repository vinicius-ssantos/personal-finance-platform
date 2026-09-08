import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "br.com.vinicius"
version = "0.1.0-SNAPSHOT"
description = "Local-first personal finance platform backend"

kotlin {
    jvmToolchain(25)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        javaParameters.set(true)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation(platform(libs.spring.modulith.bom))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.modulith.api)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.pdfbox)
    implementation(kotlin("reflect"))

    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(platform(libs.spring.modulith.bom))
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // The goldens live outside the source sets, so without declaring them Gradle
    // keeps `test` up to date after a golden changed and reports a stale green.
    inputs
        .files(fileTree(layout.projectDirectory.dir("../fixtures")))
        .withPropertyName("repositoryFixtures")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    // Lets the fixture goldens be rewritten with -DupdateGoldenFiles=true; the
    // resulting diff is reviewed like any other change.
    systemProperty(
        "updateGoldenFiles",
        providers.systemProperty("updateGoldenFiles").getOrElse("false"),
    )

    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.named("check") {
    dependsOn("ktlintCheck", "detekt")
}

// The ktlint format tasks rewrite sources as a side effect the build cache does
// not restore, so a cached run reports success while formatting nothing.
tasks.matching { task -> task.name.startsWith("ktlint") && task.name.endsWith("Format") }.configureEach {
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }
}
