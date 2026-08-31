import org.gradle.api.JavaVersion.VERSION_25

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.6.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
    // run-paper is applied by :paper, the real server-ready plugin artifact.
    id("xyz.jpenilla.run-paper") version "3.1.0" apply false
}

subprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")
    plugins.apply("com.gradleup.shadow")

    repositories {
        mavenCentral()
        maven("https://redempt.dev") {
            content {
                includeGroup("com.github.Redempt")
            }
        }
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.Redempt")
            }
        }
    }
}

group = "com.notquests"
version = "7.0.0-beta.1"

val minecraftTargetVersion = "26.2"

repositories {
}

dependencies {
    paperweight.paperDevBundle("26.2.build.121-stable")
}

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 11 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

/**
 * Configure NotQuests for shading
 */
val path = "com.notquests"


tasks {
    val collectFinalJars = register<Sync>("collectFinalJars") {
        group = "build"
        description = "Collects final NotQuests platform jars into build/final-jars."

        dependsOn(":paper:shadowJar", ":neoforge:jar")

        into(layout.buildDirectory.dir("final-jars"))

        from(project(":paper").tasks.named("shadowJar").map { it.outputs.files.singleFile }) {
            rename { "notquests-${project.version}-$minecraftTargetVersion-paper.jar" }
        }
        from(project(":neoforge").tasks.named("jar").map { it.outputs.files.singleFile }) {
            rename { "notquests-${project.version}-$minecraftTargetVersion-neoforge.jar" }
        }
    }

    build {
        dependsOn(collectFinalJars)
    }

    jar {
        enabled = false
    }

    shadowJar {
        enabled = false
        archiveClassifier.set("")
    }

    //build {
    //    dependsOn(shadowJar)
    //}
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}
