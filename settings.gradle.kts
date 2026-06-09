@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "strix"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Modules are added phase by phase as they are implemented, so every commit builds.
include(":app")
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:database")
include(":core:network")
include(":core:data")
include(":core:player")
include(":core:designsystem")
include(":feature:channels")
include(":feature:onboarding")
include(":feature:epg")
