description = "Observability conventions: metric and tag names, correlation id propagation"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    implementation(project(":libs:logging"))
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.projectreactor:reactor-core")
    implementation("io.micrometer:context-propagation")
    errorprone(libs.errorprone.core)

    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.boot.starter.test)
}
