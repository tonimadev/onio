plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.home.impl"
}

dependencies {
    implementation(project(":feature:home:bridge"))
}
