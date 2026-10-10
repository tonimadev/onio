plugins {
    id("onio.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    "api"(lib("androidx-navigation3-runtime"))
    "api"(lib("kotlinx-serialization-json"))
}
