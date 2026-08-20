// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
    id("com.diffplug.spotless") version "7.0.4" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.9" apply false
    id("com.android.library") version "8.13.2" apply false
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("io.github.takahirom.roborazzi") version "1.60.0" apply false
    id("com.vanniktech.maven.publish") version "0.33.0" apply false
    // 8.3.x, não 9.x: a linha 9.x do Shadow exige uma API do Gradle mais nova que a 8.14.1
    // usada aqui (NoSuchMethodError em AdhocComponentWithVariants.addVariantsFromConfiguration).
    id("com.gradleup.shadow") version "8.3.11" apply false
}

// Fonte única de verdade pra versão do projeto — mesmo padrão do repo Swift (`version.txt` +
// release-please). Todos os módulos compartilham essa versão.
val projectVersion = file("version.txt").readText().trim()

allprojects {
    version = projectVersion
}
