import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val loomVersion: String by System.getProperties()
    val kotlinVersion: String by System.getProperties()
    id("fabric-loom") version loomVersion
    id("maven-publish")
    kotlin("jvm") version kotlinVersion
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}

val modVersion: String by project
val mavenGroup: String by project
version = modVersion

group = mavenGroup
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // To change the versions see the gradle.properties file
    val minecraftVersion: String by project
    val loaderVersion: String by project
    minecraft("com.mojang", "minecraft", minecraftVersion)
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc", "fabric-loader", loaderVersion)

    val fabricVersion: String by project
    val fabricKotlinVersion: String by project
    val kotlinVersion: String by System.getProperties()
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabricVersion)
    modImplementation("net.fabricmc", "fabric-language-kotlin", "${fabricKotlinVersion}+kotlin.${kotlinVersion}")
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
        //sourceCompatibility = javaVersion.toString()
        //targetCompatibility = javaVersion.toString()
    }
    jar {
        from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } }
    }
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mutableMapOf("version" to project.version))
        }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
    test {
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
