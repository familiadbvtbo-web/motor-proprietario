plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.motorproprietario.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.motorproprietario.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 170
        versionName = "V170"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
