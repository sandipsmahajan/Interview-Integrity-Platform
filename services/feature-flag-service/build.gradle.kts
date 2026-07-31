plugins {
    alias(libs.plugins.spring.boot)
}

description = "feature flag service: flags, rollouts, experiments"

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(project(":libs:common"))
    implementation(project(":libs:config"))
    implementation(project(":libs:exception"))
    implementation(project(":libs:logging"))
    implementation(project(":libs:observability"))
    implementation(project(":libs:security"))
    implementation(libs.spring.boot.starter.data.r2dbc)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.r2dbc.postgresql)
    implementation(libs.postgresql)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.reactor.kafka)
    implementation(libs.springdoc.openapi.starter.webflux.ui)
    implementation(project(":libs:api-contract"))
    implementation(project(":libs:dto"))
    implementation(project(":libs:event"))
    implementation(project(":libs:validation"))

    errorprone(libs.errorprone.core)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
}
