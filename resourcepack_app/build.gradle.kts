plugins {
    kotlin("plugin.serialization")
    id("org.openjfx.javafxplugin") version "0.0.8"
    application
}

base.archivesName.set("resourcepack_app")

repositories {
    maven("https://nexus.covers1624.net/repository/karmakrafts-releases/")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.5")
}

application {
    mainClass.set("io.github.colintimbarndt.chat_emotes_util.Main")
    applicationName = "Chat Emotes Utility"
}

val javaVersion = JavaVersion.VERSION_17

javafx {
    version = javaVersion.toString()
    modules = arrayListOf("javafx.controls", "javafx.fxml", "javafx.swing")
}
tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    jar {
        manifest {
            attributes["Implementation-Title"] = application.applicationName
            attributes["Implementation-Version"] = project.version
            attributes["Main-Class"] = application.mainClass
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
    getByName<Test>("test") {
        useJUnitPlatform()
    }
}
