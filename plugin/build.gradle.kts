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

import org.gradle.api.JavaVersion.VERSION_25

plugins {

    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
}


group = "rocks.gravili.notquests"
version = rootProject.version

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/"){
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

   // maven("https://oss.sonatype.org/content/repositories/snapshots")

    //mavenLocal()

}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.64-stable")

    implementation(project(path= ":common", configuration= "shadowRuntimeElements"))
    implementation(project(path= ":paper", configuration= "shadowRuntimeElements"))

    //implementation(project(":spigot"))
    //implementation(project(":paper"))

    //compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")

    implementation("io.papermc:paperlib:1.0.8")
}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "rocks.gravili.notquests"

/*processResources {
    def props = [version: version]
    inputs.properties props
    filteringCharset 'UTF-8'
    filesMatching('plugin.yml') {
        expand props
    }
}*/


tasks {
    // Run reobfJar on build
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        // DO NOT minimize the jar, since cloud doesnt like it
        // Reference: https://discord.com/channels/766366162388123678/1170254709722984460/1242027222773006376

        archiveClassifier.set("")

        relocate("io.papermc.lib", "$shadowPath.paperlib")
    }


    compileJava {
        dependsOn(":common:jar", ":paper:jar", ":paper:build")

        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
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
        minecraftVersion("26.1.2")
    }

    register<Copy>("copyToServer") {
        val path = System.getenv("PLUGIN_DIR")
        if (path.isNullOrEmpty()) {
            println("No environment variable PLUGIN_DIR set")
            return@register
        }
        from(reobfJar)
        destinationDir = File(path)
    }
}




bukkit {
    name = "NotQuests"
    version = rootProject.version.toString()
    main = "rocks.gravili.notquests.Main"
    apiVersion = "26.1.2"
    authors = listOf("AlessioGr")
    description = "Flexible, open, GUI Quest Plugin for Minecraft"
    website = "https://www.notquests.com"
    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "Citizens",
        "Vault",
        "PlaceholderAPI",
        "MythicMobs",
        "EliteMobs",
        "WorldEdit",
        "Slimefun",
        "LuckPerms",
        "Towny",
        "Jobs",

        "EcoBosses",
        "eco",
        "UltimateJobs",
        "Floodgate"
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

paper {
    name = "NotQuests"
    version = rootProject.version.toString()
    main = "rocks.gravili.notquests.Main"
    apiVersion = "26.1.2"
    authors = listOf("AlessioGr")
    description = "Flexible, open, GUI Quest Plugin for Minecraft"
    website = "https://www.notquests.com"

    serverDependencies {
        register("ProtocolLib") {
            required = false
        }
        register("ProtocolSupport") {
            required = false
        }
        register("ViaVersion") {
            required = false
        }
        register("ViaBackwards") {
            required = false
        }
        register("ViaRewind") {
            required = false
        }
        register("Geyser-Spigot") {
            required = false
        }
        register("Citizens") {
            required = false
        }
        register("Vault") {
            required = false
        }
        register("PlaceholderAPI") {
            required = false
        }
        register("MythicMobs") {
            required = false
        }
        register("EliteMobs") {
            required = false
        }
        register("WorldEdit") {
            required = false
        }
        register("Slimefun") {
            required = false
        }
        register("LuckPerms") {
            required = false
        }
        register("Towny") {
            required = false
        }
        register("Jobs") {
            required = false
        }
        register("EcoBosses") {
            required = false
        }
        register("eco") {
            required = false
        }
        register("UltimateJobs") {
            required = false
        }
        register("Floodgate") {
            required = false
        }
    }

}
