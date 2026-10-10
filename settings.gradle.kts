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

rootProject.name = "onio"
include(":app")
include(":core:model", ":core:ui", ":core:network", ":core:data", ":core:testing")

val features = listOf("home", "search", "product", "cart", "checkout", "orders", "savings", "settings", "onboarding", "rewards")
features.forEach { feature ->
    include(":feature:$feature:bridge", ":feature:$feature:impl")
}
