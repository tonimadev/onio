plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.savings.impl"
}

dependencies {
    implementation(project(":feature:savings:bridge"))
}
