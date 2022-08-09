import java.util.Properties

plugins {
    val kotlinVersion: String by System.getProperties()
    id("java")
    kotlin("jvm") version kotlinVersion
}

val config = Properties()
File(rootDir, "build.properties").inputStream().use(config::load)

val modVersion = config.getProperty("mod_version")
val mavenGroup = config.getProperty("maven_group")
version = modVersion
group = mavenGroup

base.archivesName.set(config.getProperty("common.archives_base_name"))

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}