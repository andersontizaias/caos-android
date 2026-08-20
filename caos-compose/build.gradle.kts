import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.diffplug.spotless")
    id("io.gitlab.arturbosch.detekt")
    id("io.github.takahirom.roborazzi")
    id("org.jetbrains.kotlinx.kover")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "io.github.andersontizaias.caos.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Not configuring `publishing { singleVariant("release") {...} }` here: the
    // com.vanniktech.maven.publish plugin already does that on its own for Android modules —
    // declaring it again causes "singleVariant publishing DSL multiple times" during
    // configuration.
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    api(project(":caos-core"))

    implementation(platform("androidx.compose:compose-bom:2026.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.60.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.60.0")
    testImplementation(platform("androidx.compose:compose-bom:2026.05.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test:runner:1.6.2")
    // testImplementation (not debugImplementation): needs to be available to both variants'
    // tests (debug and release), but must never leak into the published AAR — only a test
    // dependency guarantees that.
    testImplementation("androidx.compose.ui:ui-test-manifest")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

ktlint {
    version.set("1.5.0")
    android.set(true)
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

// AGP's own javadoc jar generation for Kotlin Android libraries (the `javaDocReleaseGeneration`
// task, wired in automatically by vanniktech's AndroidSingleVariantLibrary default) delegates to
// a bundled Dokka engine that can't read the `PermittedSubclasses` attribute Kotlin emits for
// sealed classes (like CaosError) when targeting JVM 17+. Confirmed by reproducing locally:
// `com.android.build.gradle.tasks.JavaDocGenerationTask$DokkaWorkAction` throws
// `UnsupportedOperationException: PermittedSubclasses requires ASM9`. Maven Central still
// requires a javadoc artifact to be present on the publication, so we disable AGP's generator
// and attach a minimal empty jar instead — an accepted, common workaround until AGP/Dokka catch
// up with newer Kotlin bytecode.
val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

// Maven Central publishing — credentials come from GitHub Actions secrets
// (see .github/workflows/release.yml), never committed. `publish` only actually runs in CI,
// on a `v*.*.*` tag push.
mavenPublishing {
    configure(
        com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = false,
        ),
    )
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.andersontizaias", "caos-compose", version.toString())
    pom {
        name.set("Caos Compose")
        description.set(
            "Jetpack Compose rendering engine for Caos (Configurable Automated On-demand " +
                "Screens) — CaosStore, CaosScreenView, CaosContainerView. Kotlin port of " +
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

// `publishJavadocJar = false` above means no javadoc artifact is attached automatically — attach
// our empty jar to whatever MavenPublication(s) the plugin creates. `withType(...).configureEach`
// runs for publications created later too (this one is created lazily inside an
// `afterEvaluate` block by AndroidSingleVariantLibrary), so ordering here doesn't matter.
extensions.configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        artifact(emptyJavadocJar)
    }
}
