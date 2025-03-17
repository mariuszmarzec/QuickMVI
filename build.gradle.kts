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

group = "io.github.mariuszmarzec"
version = "1.1.0-RC1"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}