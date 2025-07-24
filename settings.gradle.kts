pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {

    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ERDMT"
include(":app")
    versionCatalogs { create("libs") { from(files("gradle/libs.versions.toml")) } }
