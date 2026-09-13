pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Mines"

include(":app")

// --- Core modules: no feature ever depends "up" into another feature via these ---
include(":core:theme")
include(":core:ui")
include(":core:session")

// --- Feature modules: one Gradle module per product area / team ---
include(":feature:auth")
include(":feature:settings")
include(":feature:store")
include(":feature:wallet")
include(":feature:tournament")
include(":feature:moregames")
include(":feature:userfeedback")
include(":feature:game")
include(":feature:achievements")
include(":feature:celebration")
include(":feature:profile")
include(":feature:home")
