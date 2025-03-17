pluginManagement {
    repositories {
        google()
        jcenter()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    
}
rootProject.name = "QuickMVI"


include(":android")
include(":desktop")
include(":common")
include(":lib")
include(":lib-compose")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

