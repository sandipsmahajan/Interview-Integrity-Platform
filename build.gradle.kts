import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.spotbugs.snom.SpotBugsExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.errorprone) apply false
    java
    checkstyle
    pmd
    jacoco
}

allprojects {
    group = "com.integrity"
    version = "0.1.0"
}

val checkstyleVersion = libs.versions.checkstyle.get()
val pmdVersion = libs.versions.pmd.get()
val spotbugsVersion = libs.versions.spotbugs.tool.get()
val jacocoVersion = libs.versions.jacoco.get()
val junitPlatformLauncher = libs.junit.platform.launcher.get()

subprojects {
    // Skip virtual container projects (libs/, services/) that group child
    // modules but have no build file of their own.
    if (!project.buildFile.exists()) {
        return@subprojects
    }

    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "net.ltgt.errorprone")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        val errorProneOptions =
            (options as org.gradle.api.plugins.ExtensionAware)
                .extensions
                .getByName("errorprone") as net.ltgt.gradle.errorprone.ErrorProneOptions
        errorProneOptions.disableWarningsInGeneratedCode.set(true)
    }

    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    extensions.configure<SpotlessExtension> {
        java {
            googleJavaFormat()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    checkstyle {
        toolVersion = checkstyleVersion
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    pmd {
        toolVersion = pmdVersion
        ruleSetFiles = files(rootProject.file("config/pmd/ruleset.xml"))
        ruleSets = emptyList()
    }

    tasks.named<org.gradle.api.plugins.quality.Pmd>("pmdTest") {
        ruleSetFiles = files(rootProject.file("config/pmd/ruleset-test.xml"))
        ruleSets = emptyList()
    }

    extensions.configure<SpotBugsExtension> {
        toolVersion.set(spotbugsVersion)
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
        excludeFilter.set(rootProject.layout.projectDirectory.file("config/spotbugs/exclude.xml"))
    }

    tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        maxHeapSize.set("2g")
    }

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.named<JacocoReport>("jacocoTestReport"))
    }

    dependencies {
        testRuntimeOnly(junitPlatformLauncher)
    }

    jacoco {
        toolVersion = jacocoVersion
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}
