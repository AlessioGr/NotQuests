/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
}

group = "rocks.gravili.notquests"
version = rootProject.version

repositories {
    mavenCentral()

    maven(" https://repo.papermc.io/repository/maven-public/"){
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.kyori")
            includeGroup("io.papermc")
        }
    }

    maven("https://repo.citizensnpcs.co/"){
        content {
            includeGroup("net.citizensnpcs")
        }
    }

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/"){
        content {
            includeGroup("me.clip")
        }
    }

    maven("https://jitpack.io"){
        content {
            includeGroup("com.github.MilkBowl")
            includeGroup("com.github.TheBusyBiscuit")
            includeGroup("com.github.retrooper")
            includeGroup("com.github.retrooper.packetevents")
            includeGroup("io.github.retrooper")
            includeGroup("com.github.AlessioGr")
            includeGroup("com.github.AlessioGr.packetevents")
            includeGroup("com.github.TownyAdvanced")
            includeGroup("com.github.Zrips")
        }
        metadataSources {
            artifact()
        }
    }

    maven("https://repo.minebench.de/"){
        content {
            includeGroup("de.themoep")
        }
    }

    maven("https://mvn.lumine.io/repository/maven-public/"){
        content {
            includeGroup("io.lumine.xikage")
        }
    }

    maven("https://betonquest.org/nexus/repository/betonquest/"){
        content {
            includeGroup("org.betonquest")
        }
        metadataSources {
            artifact()
        }
    }

    maven("https://maven.enginehub.org/repo/"){
        content {
            includeGroup("com.sk89q.worldedit")
        }
        metadataSources {
            artifact()
        }
    }

    maven("https://repo.incendo.org/content/repositories/snapshots"){
        content {
            includeGroup("org.incendo.interfaces")
        }
    }

    maven("https://libraries.minecraft.net/"){
        content {
            includeGroup("com.mojang")
        }
    }
}

dependencies {
    paperweight.paperDevBundle("1.19.4-R0.1-SNAPSHOT")

    implementation(project(path= ":common", configuration= "shadow"))
    implementation(project(path= ":paper", configuration= "shadow"))


    implementation("io.papermc:paperlib:1.0.8")
}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "rocks.gravili.notquests"


tasks {
    shadowJar {
        relocate("io.papermc.lib", "$shadowPath.paperlib")

        archiveClassifier.set("")
    }

    build {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.19")
    }

    register<Copy>("copyToServer") {
        val path = System.getenv("PLUGIN_DIR")
        if (path.toString().isEmpty()) {
            println("No environment variable PLUGIN_DIR set")
            return@register
        }
        from(reobfJar)
        destinationDir = File(path.toString())
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/alessiogr/NotQuests")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}


bukkit {
    name = "NotQuests"
    version = rootProject.version.toString()
    main = "rocks.gravili.notquests.Main"
    apiVersion = "1.18"
    authors = listOf("AlessioGr")
    description = "Flexible, open, GUI Quest Plugin for Minecraft 1.19"
    website = "https://www.notquests.com"
    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "ViaVersion",
        "Geyser-Spigot",
        "Citizens",
        "Vault",
        "PlaceholderAPI",
        "MythicMobs",
        "EliteMobs",
        "BetonQuest",
        "WorldEdit",
        "Slimefun",
        "LuckPerms",
        "UltimateClans",
        "Towny",
        "Jobs",
        "ProjectKorra",
        "EcoBosses",
        "eco",
        "UltimateJobs",
        "Floodgate",
        "ZNPCs",
    )

    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    permissions {
        register("notquests.admin"){
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to everything in the plugin."
            childrenMap = mapOf(
                "notquests.admin.armorstandeditingitems" to true,
                "notquests.use" to true
            )
        }
        register("notquests.admin.armorstandeditingitems"){
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to use quest editing items for armor stands."
        }
        register("notquests.use"){
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
            description = "Gives the player permission to use the /notquests user command. They can not create new quests or other administrative tasks with just this permission."
        }
        register("notquests.user.profiles"){
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to use the /notquests profiles command, and to create, delete and switch profiles."
        }
    }
}
