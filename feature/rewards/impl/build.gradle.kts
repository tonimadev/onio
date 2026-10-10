plugins {
    id("onio.android.feature.impl")
}

android {
    namespace = "com.ipirangatech.onio.feature.rewards.impl"
}

dependencies {
    implementation(project(":feature:rewards:bridge"))
}
