plugins {
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("com.android.application")
    kotlin("android")
    id("io.gitlab.arturbosch.detekt")
}

group = "com.marzec"
version = rootProject.version.toString()

repositories {
    jcenter()
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidActivityX)
    implementation(libs.viewModel)
}

android {
    namespace = "com.marzec.quickmvi.sample"

    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "com.marzec.quickmvi.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
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

detekt {
    source = files(
        "src/main/kotlin"
    )

    config = files("../config/detekt/detekt.yml")
}
