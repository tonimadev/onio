plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.onboarding.impl"
}

dependencies {
    implementation(project(":feature:onboarding:bridge"))
}
