plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.orders.impl"
}

dependencies {
    implementation(project(":feature:orders:bridge"))
}
