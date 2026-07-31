plugins {
    java
}

description = "Common primitives (time provider, id factories) shared by all services"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    errorprone(libs.errorprone.core)
}
