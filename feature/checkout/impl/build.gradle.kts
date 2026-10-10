plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.checkout.impl"
}

dependencies {
    implementation(project(":feature:checkout:bridge"))
}
