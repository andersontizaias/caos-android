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

// Fat jar autocontido pra distribuição via GitHub Release (ver .github/workflows/release.yml).
// `java -jar caos-lint-<versão>-all.jar <arquivo.yaml>` — sem precisar do classpath do Gradle.
// Mantém o classifier "-all" (não vazio): um classifier vazio faria o nome do arquivo colidir
// com o output da task `jar` padrão, usado por distZip/distTar/startScripts do plugin
// `application` — Gradle recusa isso como dependência implícita não declarada entre tasks.
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
