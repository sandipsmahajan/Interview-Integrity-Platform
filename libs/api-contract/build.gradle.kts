description = "Shared REST API contract types (error, page, envelope) used by all services"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    implementation(project(":libs:exception"))
    implementation(project(":libs:observability"))
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-webflux")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    errorprone(libs.errorprone.core)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webflux)
}
