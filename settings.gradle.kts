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
include(":core:network")
include(":core:screenshot-testing")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "Resonote requires JDK 17 or newer. Current JDK: ${JavaVersion.current()}"
}
