import java.util.Properties

plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.openjfx.javafxplugin") version "0.0.8"
    application
}

val config = Properties()
File(rootDir, "build.properties").inputStream().use(config::load)

val modVersion = config.getProperty("mod_version")
val mavenGroup = config.getProperty("maven_group")
version = modVersion
group = mavenGroup

base.archivesName.set("emotes-util")

repositories {
    mavenCentral()
    maven("https://nexus.covers1624.net/repository/karmakrafts-releases/")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0-RC")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("io.karma.sliced:sliced:1.1.1.13")
}

application {
    mainClass.set("io.github.colintimbarndt.chat_emotes_util.Main")
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
    getByName<Test>("test") {
        useJUnitPlatform()
    }
}
