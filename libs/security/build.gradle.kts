dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    implementation(libs.spring.boot.starter.security)
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("io.projectreactor:reactor-core")
    implementation("org.springframework:spring-webflux")
    implementation(project(":libs:exception"))
    errorprone(libs.errorprone.core)

    testImplementation(libs.spring.boot.starter.test)
}
