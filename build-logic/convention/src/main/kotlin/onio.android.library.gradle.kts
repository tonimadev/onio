plugins {
    id("com.android.library")
}

android {
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        // Every module must ship all 10 supported languages.
        error += "MissingTranslation"
    }
}
