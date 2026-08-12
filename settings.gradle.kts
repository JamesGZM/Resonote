import org.gradle.api.JavaVersion

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
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Resonote"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":app-resonote-catalog")
include(":core:designsystem")
include(":core:navigation")
include(":core:model")
include(":core:network")
include(":core:datastore-proto")
include(":core:datastore")
include(":core:data")
include(":core:screenshot-testing")
include(":feature:home:impl")
include(":feature:player:impl")
include(":feature:playlist:api")
include(":feature:playlist:impl")
include(":feature:search:api")
include(":feature:search:impl")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "Resonote requires JDK 17 or newer. Current JDK: ${JavaVersion.current()}"
}
