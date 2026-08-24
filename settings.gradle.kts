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
include(":core:playback:api")
include(":core:playback:service")
include(":core:datastore-proto")
include(":core:datastore")
include(":core:data")
include(":core:database")
include(":core:media:local")
include(":core:screenshot-testing")
include(":feature:auth:impl")
include(":feature:album:api")
include(":feature:album:impl")
include(":feature:artist:api")
include(":feature:artist:impl")
include(":feature:cloud:api")
include(":feature:cloud:impl")
include(":feature:discover:impl")
include(":feature:home:impl")
include(":feature:history:api")
include(":feature:history:impl")
include(":feature:library:impl")
include(":feature:local:api")
include(":feature:local:impl")
include(":feature:player:impl")
include(":feature:player:api")
include(":feature:playlist:api")
include(":feature:playlist:impl")
include(":feature:ranking:api")
include(":feature:ranking:impl")
include(":feature:risk:impl")
include(":feature:recognition:api")
include(":feature:recognition:impl")
include(":feature:search:api")
include(":feature:search:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:vip:api")
include(":feature:vip:impl")
include(":feature:video:api")
include(":feature:video:impl")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "Resonote requires JDK 17 or newer. Current JDK: ${JavaVersion.current()}"
}
