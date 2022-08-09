pluginManagement {
    repositories {
        maven(url = "https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        val loomVersion: String by System.getProperties()
        val kotlinVersion: String by System.getProperties()
        id("fabric-loom") version loomVersion
        kotlin("jvm") version kotlinVersion
    }
}
