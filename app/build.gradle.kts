plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "dz.redatest.arabiccolor"
    compileSdk = 35
    defaultConfig {
        applicationId = "dz.redatest.arabiccolor"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.1"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
