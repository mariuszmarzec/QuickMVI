import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
    id("io.gitlab.arturbosch.detekt")
    id("com.vanniktech.maven.publish") version "0.30.0"
    jacoco
}

jacoco {
    toolVersion = libs.versions.jacoco.get().toString()
}
apply(from = "jacoco.gradle.kts")

group = "com.marzec"
version = "1.0"

kotlin {
    android()
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.coroutineTest)
                implementation(libs.mockk)
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
        groupId = "io.github.mariuszmarzec",
        artifactId = "quickmvi",
        version = "1.0.0"
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("KMP Library for MVI")
        description.set("Library used for providing kotlin multiplatform store for state management based on MVI pattern")
        inceptionYear.set("2025")
        url.set("https://github.com/mariuszmarzec/QuickMVI")

        licenses {
            license {
                name.set("Apache 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        developers {
            developer {
                id.set("mariuszmarzec")
                name.set("Mariusz Marzec")
                email.set("mariusz.marzec00@gmail.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://github.com/mariuszmarzec/QuickMVI")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()
}