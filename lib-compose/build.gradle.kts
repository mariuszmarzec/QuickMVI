import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
        alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("com.android.library")
    id("io.gitlab.arturbosch.detekt")
    id("com.vanniktech.maven.publish") version "0.31.0"
    jacoco
}

apply(from = "../gradle/jacoco.gradle.kts")
jacoco {
    toolVersion = libs.versions.jacoco.get()
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
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
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

            }
        }
        val desktopTest by getting {
            dependencies {
            }
        }
    }
}

android {
    namespace = "com.marzec.quickmvi"

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
        groupId = project.group.toString(),
        artifactId = "quickmvi-compose",
        version = project.version.toString()
    )

    pom {
        name.set("KMP Library with compose tools for QuickMVI")
        description.set("Library used for providing compose tools for QuickMVI library")
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()
}
