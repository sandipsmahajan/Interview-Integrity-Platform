plugins {
    alias(libs.plugins.spring.boot)
}

description = "API gateway: routing, authentication, rate limiting, correlation ids"

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
    runtimeOnly(libs.caffeine)
    implementation(project(":libs:logging"))
    implementation(project(":libs:observability"))
    implementation(project(":libs:security"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}"))
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation(libs.spring.cloud.starter.gateway.server.webflux)
    implementation(libs.spring.cloud.starter.circuitbreaker.reactor.resilience4j)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.data.redis.reactive)
    implementation(libs.spring.boot.starter.webflux)
    implementation(project(":libs:api-contract"))

    errorprone(libs.errorprone.core)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
}
