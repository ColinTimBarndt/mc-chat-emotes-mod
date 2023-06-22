plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.openjfx.javafxplugin") version "0.0.8"
    application
}

version = project.properties["mod_version"] as String
group = project.properties["maven_group"] as String

base.archivesName.set("resourcepack_app")

sourceSets {
    main {
        resources.srcDir("${rootProject.rootDir}/shared_resources")
    }
}

repositories {
    mavenCentral()
    maven("https://nexus.covers1624.net/repository/karmakrafts-releases/")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    implementation(kotlin("stdlib"))
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
    getByName<Test>("test") {
        useJUnitPlatform()
    }
}
