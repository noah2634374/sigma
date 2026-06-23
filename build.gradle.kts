plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("java")
}

base {
    archivesName.set(project.property("archives_base_name") as String)
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
    maven("https://maven.meteordev.org/releases")
    maven("https://maven.meteordev.org/snapshots")
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    modImplementation(libs.meteor)
}

loom {
    accessWidenerPath.set(file("src/main/resources/spawnerutils.accesswidener"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

tasks.jar {
    from("LICENSE")
}
