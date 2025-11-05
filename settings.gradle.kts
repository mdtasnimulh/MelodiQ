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
        maven("https://oss.jfrog.org/libs-snapshot")
        maven("https://www.jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jitpack.io")
        maven("https://oss.jfrog.org/libs-snapshot")
    }
}

rootProject.name = "MelodiQ"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")

/*** Core Modules ***/
include(":core:common")
include(":core:data")
include(":core:database")
include(":core:design-system")
include(":core:di")
include(":core:domain")
include(":core:model:api-response")
include(":core:model:entity")
include(":core:ui")
include(":core:notifications")
include(":core:datastore")
/*** Core Modules ***/

/*** Feature Modules ***/
include(":feature:home")
include(":feature:albums")
include(":feature:songs")
include(":feature:playlists")
include(":feature:eqalizer")
include(":feature-player")
include(":feature-queue")
include(":feature-favourite")
include(":feature-tools:feature-about")
include(":feature-tools:feature-feedback")
include(":feature-tools:settings")
include(":fetaure-playlist:playlist-details")
/*** Feature Modules ***/