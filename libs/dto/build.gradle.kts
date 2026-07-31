description = "Shared data transfer objects defining the cross-service wire contract"

dependencies {
    implementation(project(":libs:common"))
    errorprone(libs.errorprone.core)
}
