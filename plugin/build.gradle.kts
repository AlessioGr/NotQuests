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

import org.gradle.api.JavaVersion.VERSION_21

plugins {

    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
}


group = "rocks.gravili.notquests"
version = rootProject.version

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    sourceCompatibility = VERSION_21
    targetCompatibility = VERSION_21
}

repositories {
    // This module only pulls the Paper platform + PaperLib; no plugin dependencies are declared
    // here, so (like the paper module) it lists no plugin maven repos.
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/"){
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.kyori")
            includeGroup("io.papermc")
        }
    }

    // Mojang libraries (brigadier / authlib / datafixerupper transitives of the dev bundle)
    maven("https://libraries.minecraft.net/"){
        content {
            includeGroup("com.mojang")
        }
    }

    //mavenLocal()

}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

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
    // Run reobfJar on build. The 1.21.x release jar must be uniformly Spigot-mapped so Paper can
    // remap it to its Mojang-mapped runtime on load. Loading the Mojang-mapped shadow jar directly
    // leaves shaded InvUI 1.x inventory-access classes in Spigot mappings (e.g. EntityHuman).
    build {
        dependsOn(reobfJar)
    }
    // Don't emit the thin (un-shaded) plugin jar into build/libs: it has a valid plugin.yml but
    // none of the shaded code, so loading it would crash at enable. NotQuests-<version>.jar
    // (the shadowJar) is the only server-ready artifact.
    jar {
        enabled = false
    }
    shadowJar {
        // DO NOT minimize the jar, since cloud doesnt like it
        // Reference: https://discord.com/channels/766366162388123678/1170254709722984460/1242027222773006376

        // This shaded jar is only the dev/remap input. reobfJar writes the real server-ready artifact
        // as NotQuests-<version>.jar below.
        archiveBaseName.set("NotQuests")
        archiveClassifier.set("dev-all")

        relocate("io.papermc.lib", "$shadowPath.paperlib")

        // InvUI 1.x's inventory-access adapters are Spigot-mapped. On 1.21.x, Paper plugins are
        // assumed Mojang-mapped by default, while Bukkit plugins are remapped when needed. Keep the
        // backport artifact Bukkit-style so Paper can remap the final Spigot-mapped reobf jar.
        exclude("paper-plugin.yml")
    }

    reobfJar {
        outputJar.set(layout.buildDirectory.file("libs/NotQuests-${project.version}.jar"))
    }


    compileJava {
        dependsOn(":common:jar", ":paper:jar", ":paper:build")

        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        exclude("paper-plugin.yml")
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        minecraftVersion("1.21.1")
        pluginJars(reobfJar.flatMap { it.outputJar })
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

runPaper {
    disablePluginJarDetection()
}




bukkit {
    name = "NotQuests"
    version = rootProject.version.toString()
    main = "rocks.gravili.notquests.Main"
    apiVersion = "1.21"
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
        "FancyNpcs",
        "Vault",
        "PlaceholderAPI",
        "MythicMobs",
        "EliteMobs",
        "WorldEdit",
        "Slimefun",
        "LuckPerms",
        "Towny",
        "Jobs",

        "EcoMobs",
        "eco",
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
    apiVersion = "1.21"
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
        register("FancyNpcs") {
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
        register("EcoMobs") {
            required = false
        }
        register("eco") {
            required = false
        }
        register("Floodgate") {
            required = false
        }
    }

    // IMPORTANT: Paper prefers paper-plugin.yml over plugin.yml when both exist, so the permission
    // defaults MUST be declared here too. Without this, notquests.use (default true, which lets every
    // player run /notquests) is never registered, so non-OP players are denied the command.
    permissions {
        register("notquests.admin") {
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to everything in the plugin."
            childrenMap = mapOf(
                "notquests.admin.armorstandeditingitems" to true,
                "notquests.use" to true
            )
        }
        register("notquests.admin.armorstandeditingitems") {
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to use quest editing items for armor stands."
        }
        register("notquests.use") {
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
            description = "Gives the player permission to use the /notquests user command. They can not create new quests or other administrative tasks with just this permission."
        }
        register("notquests.user.profiles") {
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
            description = "Gives the player permission to use the /notquests profiles command, and to create, delete and switch profiles."
        }
    }

}
