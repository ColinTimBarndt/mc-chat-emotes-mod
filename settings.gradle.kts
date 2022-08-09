rootProject.name = "chat-emotes"

include("common")
include("fabric_mod_server")

pluginManagement {
    repositories {
        maven(url = "https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
