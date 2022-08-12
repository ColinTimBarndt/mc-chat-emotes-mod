rootProject.name = "chat-emotes"

include("common_server")
include("fabric_mod_server")

pluginManagement {
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
