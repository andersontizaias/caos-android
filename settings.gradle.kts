pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "caos-android"

include(":caos-core")
include(":caos-compose")
include(":caos-lint")

// Habilitado na Fase 4 (ver PLAN_ANDROID.md):
// include(":caos-sample")
