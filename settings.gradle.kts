pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "interview-integrity-platform"

val libs = listOf(
    "api-contract",
    "common",
    "config",
    "dto",
    "event",
    "exception",
    "logging",
    "observability",
    "security",
    "validation"
)

val services = listOf(
    "api-gateway",
    "identity-service",
    "organization-service",
    "recruiter-service",
    "candidate-service",
    "interview-service",
    "desktop-client-service",
    "telemetry-service",
    "policy-engine-service",
    "report-service",
    "notification-service",
    "analytics-service",
    "audit-service",
    "storage-service",
    "feature-flag-service",
    "scheduler-service",
    "integration-service",
    "configuration-service",
    "discovery-service"
)

libs.forEach { include(":libs:$it") }
services.forEach { include(":services:$it") }
