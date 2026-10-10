plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.settings.impl"
}

dependencies {
    implementation(project(":feature:settings:bridge"))
    implementation(libs.androidx.appcompat)
}
