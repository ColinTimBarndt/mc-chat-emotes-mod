rootProject.name = "chat-emotes"

include("util")
include("common_server")
include("fabric_mod_server")

pluginManagement {
    plugins {
        val kotlinVersion: String by System.getProperties()
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion

        val loomVersion: String by System.getProperties()
        id("fabric-loom") version loomVersion
    }
    repositories {
        maven(url = "https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven(url = "https://maven.minecraftforge.net") {
            name = "Forge"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
