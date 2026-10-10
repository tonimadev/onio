plugins {
    id("onio.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
