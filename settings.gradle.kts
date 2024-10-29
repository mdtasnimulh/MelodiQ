@file:Suppress("UnstableApiUsage")

include(":feature:home")

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

//Core module
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
include(":core:shared-preference")

//Feature module
include(":feature:home")
include(":feature:settings")
