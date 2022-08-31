import java.util.Properties

plugins {
    id("java")
    id("fabric-loom")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val side = "server"
val modLoader = "common"

val config = Properties()
File(rootDir, "build.properties").inputStream().use(config::load)

val modVersion = config.getProperty("mod_version")
val mavenGroup = config.getProperty("maven_group")
version = modVersion
group = mavenGroup

base.archivesName.set(String.format(config.getProperty("archives_base_name"), side, modLoader))

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    minecraft("com.mojang", "minecraft", config.getProperty("minecraft_version"))
    mappings(loom.officialMojangMappings())

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0-RC")
    compileOnly("net.luckperms:api:5.4")
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
