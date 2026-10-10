plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.search.impl"
}

dependencies {
    implementation(project(":feature:search:bridge"))
}
