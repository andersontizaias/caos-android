plugins {
    id("org.jetbrains.kotlin.jvm")
    application
    id("com.gradleup.shadow")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.diffplug.spotless")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.github.andersontizaias.caos.lint.MainKt")
}

// By default the `application` plugin's `run` task uses the module's own directory
// (caos-lint/) as workingDir — so `./gradlew :caos-lint:run --args="home.yaml"` would look for
// the file at `caos-lint/home.yaml`, not the repo root, where the user runs `./gradlew` from.
// Discovered by actually running this in CI (see .github/workflows/ci.yml), not obvious upfront.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

// Self-contained fat jar for distribution via GitHub Release (see .github/workflows/release.yml).
// `java -jar caos-lint-<version>-all.jar <file.yaml>` — no Gradle classpath needed.
// Keeps the "-all" classifier (not empty): an empty classifier would make the jar's filename
// collide with the standard `jar` task's output, used by distZip/distTar/startScripts from the
// `application` plugin — Gradle refuses that as an undeclared implicit dependency between tasks.
tasks.shadowJar {
    archiveBaseName.set("caos-lint")
}

dependencies {
    implementation(project(":caos-core"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set("1.5.0")
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("1.5.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

detekt {
    toolVersion = "1.23.7"
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("build/reports/detekt/detekt.xml"))
        html.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

kover {
    reports {
        verify {
            rule {
                minBound(90)
            }
        }
    }
}
