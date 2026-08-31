pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "notquests"

include(":core")
project(":core").projectDir = file("src/core")

include(":builtin")
project(":builtin").projectDir = file("src/builtin")

include(":paper")
project(":paper").projectDir = file("src/paper")

include(":neoforge")
project(":neoforge").projectDir = file("src/neoforge")
