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

    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.named("check") {
    dependsOn("ktlintCheck", "detekt")
}
