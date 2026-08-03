plugins {
    alias(libs.plugins.spring.boot)
}

description = "Eureka service registry (discovery server)"

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(project(":libs:common"))
    implementation(project(":libs:config"))
    implementation(project(":libs:exception"))
    implementation(project(":libs:logging"))
    implementation(project(":libs:observability"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}"))
    implementation(libs.spring.cloud.starter.netflix.eureka.server)
    implementation("org.springframework.boot:spring-boot-starter-web")

    errorprone(libs.errorprone.core)

    testImplementation(libs.spring.boot.starter.test)
}
