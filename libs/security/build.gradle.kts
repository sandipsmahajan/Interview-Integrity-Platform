description = "Security conventions: authorities, scopes and principal extraction helpers"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    implementation(libs.spring.boot.starter.security)
    errorprone(libs.errorprone.core)
}
