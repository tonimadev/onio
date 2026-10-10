plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.cart.impl"
}

dependencies {
    implementation(project(":feature:cart:bridge"))
}
