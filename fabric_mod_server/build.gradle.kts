import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    val loomVersion: String by System.getProperties()
    val kotlinVersion: String by System.getProperties()
    id("fabric-loom") version loomVersion
    id("maven-publish")
    kotlin("jvm") version kotlinVersion
}

val side = "server"
val modLoader = "fabric"

val config = Properties()
File(rootDir, "build.properties").inputStream().use(config::load)

base.archivesName.set(String.format(config.getProperty("archives_base_name"), side, modLoader))

val modVersion = config.getProperty("mod_version")
val mavenGroup = config.getProperty("maven_group")
version = modVersion
group = mavenGroup

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    val mcVersion = config.getProperty("minecraft_version")
    minecraft("com.mojang", "minecraft", mcVersion)
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc", "fabric-loader", config.getProperty("fabric.loader_version"))
    modImplementation("net.fabricmc.fabric-api", "fabric-api", "${config.getProperty("fabric.api_version")}+$mcVersion")

    val fabricKotlinVersion = config.getProperty("fabric.fabric_kotlin_version")
    val kotlinVersion: String by System.getProperties()
    modImplementation("net.fabricmc", "fabric-language-kotlin", "${fabricKotlinVersion}+kotlin.${kotlinVersion}")

    implementation(project(":common_server", "namedElements"))
    include(project(":common_server"))
}

tasks {
    val javaVersion = JavaVersion.VERSION_17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
            // Optimization and Features
            val args = freeCompilerArgs.toMutableList()
            // args += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
            args += "-Xinline-classes"
            args += "-Xno-param-assertions"
            args += "-Xno-call-assertions"
            args += "-Xjvm-default=all"
            freeCompilerArgs = args
        }
    }
    jar {
        from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } }
    }
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mutableMapOf(
                "version" to project.version,
                "mod_id" to String.format(config.getProperty("mod_id"), side)
            ))
        }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
    getByName<Test>("test") {
        useJUnitPlatform()
    }
}

// configure the maven publication
publishing {
    publications {
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
