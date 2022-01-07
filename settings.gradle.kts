pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

rootProject.name = "notquests"

include(":plugin")
include(":paper")
include(":spigot")
include(":common")

