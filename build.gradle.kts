// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
    id("com.diffplug.spotless") version "7.0.4" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.9" apply false

    // Habilitados nas próximas fases, quando os módulos correspondentes forem adicionados:
    // id("com.android.library") version "8.13.2" apply false      // Fase 2 (caos-compose)
    // id("com.android.application") version "8.13.2" apply false  // Fase 4 (caos-sample)
    // id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    // id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
