plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.product.impl"
}

dependencies {
    implementation(project(":feature:product:bridge"))
}
