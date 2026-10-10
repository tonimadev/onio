plugins {
    id("onio.android.library")
    id("onio.android.compose")
    id("onio.android.hilt")
}

dependencies {
    "implementation"(project(":core:model"))
    "implementation"(project(":core:ui"))
    "implementation"(lib("androidx-lifecycle-viewmodel-compose"))
    "implementation"(lib("androidx-hilt-navigation-compose"))
    "implementation"(lib("kotlinx-coroutines-android"))
    "implementation"(lib("androidx-compose-material-icons-extended"))
    "testImplementation"(lib("junit"))
    "testImplementation"(lib("kotlinx-coroutines-test"))
    "testImplementation"(lib("turbine"))
}
