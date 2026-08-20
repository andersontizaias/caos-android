plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.diffplug.spotless")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
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

// Maven Central publishing — credentials come from GitHub Actions secrets
// (see .github/workflows/release.yml), never committed. `publish` only actually runs in CI,
// on a `v*.*.*` tag push.
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.andersontizaias", "caos-core", version.toString())
    pom {
        name.set("Caos Core")
        description.set(
            "Schema-only core of Caos (Configurable Automated On-demand Screens) — YAML v1 " +
                "parser and typed models, zero third-party dependencies. Kotlin port of " +
                "github.com/andersontizaias/Caos.",
        )
        url.set("https://github.com/andersontizaias/caos-android")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("andersontizaias")
                name.set("Anderson Tiago Izaias")
            }
        }
        scm {
            url.set("https://github.com/andersontizaias/caos-android")
            connection.set("scm:git:https://github.com/andersontizaias/caos-android.git")
        }
    }
}
