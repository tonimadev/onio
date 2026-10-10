plugins {
    id("onio.android.library")
    id("onio.android.compose")
}

android {
    namespace = "com.ipirangatech.onio.core.ui"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
}
