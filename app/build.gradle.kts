plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.sanchitsinghal630_afk.simplecalc"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.sanchitsinghal630_afk.simplecalc"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // Keeps the build reproducible and free of Google-signed dependency blobs (F-Droid friendly)
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
// No third-party dependencies: nothing proprietary, nothing that tracks.
