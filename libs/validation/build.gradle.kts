description = "Reusable validation utilities shared by all services"

dependencies {
    implementation(project(":libs:exception"))
    errorprone(libs.errorprone.core)
}
