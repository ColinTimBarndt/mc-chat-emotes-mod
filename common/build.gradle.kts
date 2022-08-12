import java.util.Properties

plugins {
    val loomVersion: String by System.getProperties()
    val kotlinVersion: String by System.getProperties()
    id("java")
    id("fabric-loom") version loomVersion
    kotlin("jvm") version kotlinVersion
}

val config = Properties()
File(rootDir, "build.properties").inputStream().use(config::load)

val modVersion = config.getProperty("mod_version")
val mavenGroup = config.getProperty("maven_group")
version = modVersion
group = mavenGroup

base.archivesName.set(config.getProperty("common.archives_base_name"))

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    minecraft("com.mojang", "minecraft", config.getProperty("minecraft_version"))
    mappings(loom.officialMojangMappings())
}

tasks {
    val javaVersion = JavaVersion.VERSION_17
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
