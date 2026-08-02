plugins {
    alias(libs.plugins.spring.boot)
}

description = "service registry placeholder"

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
    implementation(project(":libs:api-contract"))
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.springdoc.openapi.starter.webflux.ui)

    errorprone(libs.errorprone.core)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
}
