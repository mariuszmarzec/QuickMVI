plugins {
    id("org.jetbrains.compose") version "1.4.0"
    id("com.android.application")
    kotlin("android")
}

group = "com.marzec"
version = "1.0"

repositories {
    jcenter()
}

dependencies {
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.4.0")
}

android {
    compileSdkVersion(33)
    defaultConfig {
        applicationId = "com.marzec.android"
        minSdkVersion(30)
        targetSdkVersion(33)
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}