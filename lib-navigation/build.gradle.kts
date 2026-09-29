import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("com.android.library")
    id("io.gitlab.arturbosch.detekt")
    id("com.vanniktech.maven.publish") version "0.31.0"
}

kotlin {
    androidTarget()
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.lib)
                api(projects.libCompose)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                implementation(compose.components.resources)
                api(libs.serializationJson)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.coroutineTest)
                implementation(libs.mockkCommon)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.mockkAndroid)
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.mockkJvm)
            }
        }
    }
}

android {
    namespace = "com.marzec.quickmvi.navigation"

    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

detekt {
    source = files(
        "src/main/kotlin"
    )

    config = files("../config/detekt/detekt.yml")
}

mavenPublishing {
    coordinates(
        groupId = rootProject.group.toString(),
        artifactId = "quickmvi-navigation",
        version = rootProject.version.toString()
    )
}