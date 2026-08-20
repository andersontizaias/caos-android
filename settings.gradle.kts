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

// Habilitados nas próximas fases (ver PLAN_ANDROID.md):
// include(":caos-lint")     // Fase 3
// include(":caos-sample")   // Fase 4
