buildscript {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.plugin.kotlinGradle)
        classpath(libs.plugin.androidBuild)
        classpath(libs.plugin.detekt)
        classpath(libs.plugin.composeGradle)
    }
}

group = "com.marzec"
version = "1.0"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}