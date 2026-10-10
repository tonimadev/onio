plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    buildFeatures {
        compose = true
    }
}

dependencies {
    "implementation"(platform(lib("androidx-compose-bom")))
    "implementation"(lib("androidx-compose-ui"))
    "implementation"(lib("androidx-compose-ui-graphics"))
    "implementation"(lib("androidx-compose-foundation"))
    "implementation"(lib("androidx-compose-material3"))
    "implementation"(lib("androidx-compose-ui-tooling-preview"))
    "implementation"(lib("androidx-lifecycle-runtime-compose"))
    "implementation"(lib("kotlinx-collections-immutable"))
    "debugImplementation"(lib("androidx-compose-ui-tooling"))
}
