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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
}

group = "rocks.gravili.notquests"
version = rootProject.version

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()

    maven("https://papermc.io/repo/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.kyori")
        }
    }

    maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/") {
        content {
            includeGroup("com.jeff_media")
        }
    }

    maven("https://repo.citizensnpcs.co/") {
        content {
            includeGroup("net.citizensnpcs")
        }
    }

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        content {
            includeGroup("me.clip")
        }
    }

    maven("https://jitpack.io") {
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
            includeGroup("com.willfp")
            includeGroup("com.github.war-systems")
            includeGroup("com.github.MilkBowl")
            includeGroup("com.github.UlrichBR")
            includeGroup("com.github.Slimefun")
        }
        metadataSources {
            artifact()
        }
    }

    maven("https://repo.glaremasters.me/repository/towny/") {
        content {
            includeGroup("com.palmergames.bukkit.towny")
        }
    }

    /*maven("https://repo.minebench.de/"){
        content {
            includeGroup("de.themoep")
        }
    }*/

    maven("https://mvn.lumine.io/repository/maven-public/") {
        content {
            includeGroup("io.lumine.xikage")
            includeGroup("io.lumine")
        }
    }

    maven("https://betonquest.org/nexus/repository/betonquest/") {
        content {
            includeGroup("org.betonquest")
        }
        metadataSources {
            artifact()
        }
    }

    maven("https://maven.enginehub.org/repo/") {
        content {
            includeGroup("com.sk89q.worldedit")
        }
        metadataSources {
            artifact()
        }
    }

    maven("https://oss.sonatype.org/content/repositories/snapshots") {
        content {
            includeGroup("org.incendo.interfaces")
            includeGroup("cloud.commandframework")
        }
    }

    maven("https://repo.thbn.me/snapshots") {
        content {
            includeGroup("org.incendo.interfaces")
        }
    }

    maven("https://libraries.minecraft.net/") {
        content {
            includeGroup("com.mojang")
        }
    }

    maven("https://redempt.dev") {
        content {
            includeGroup("com.github.Redempt")
        }
    }

    maven("https://repo.opencollab.dev/maven-snapshots/") {
        content {
            includeGroup("org.geysermc.floodgate")
            includeGroup("org.geysermc.cumulus")
            includeGroup("org.geysermc")
        }
    }

    //mavenLocal()

}

dependencies {
    implementation(project(path= ":common", configuration= "shadow"))
    paperDevBundle("1.19.3-R0.1-SNAPSHOT")
    //compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT!!")
    //implementation("de.themoep:inventorygui:1.5-SNAPSHOT")

    //compileOnly("net.citizensnpcs:citizens-main:2.0.30-SNAPSHOT")
    compileOnly(files("libs/citizens-2.0.30-8.jar"))

    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")


    compileOnly("io.lumine:Mythic-Dist:5.2.0")
    compileOnly(files("libs/EliteMobs.jar"))
    compileOnly("com.github.UlrichBR:UClansV5-API:4.5")
    compileOnly(files("libs/ProjectKorra-1.10.0.jar"))
    //compileOnly(files("libs/UltimateJobs-0.2.0-SNAPSHOT.jar"))


    compileOnly("org.betonquest:betonquest:2.0.0-SNAPSHOT")

    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT")

    compileOnly("com.github.Slimefun:Slimefun4:RC-32")

    compileOnly("net.luckperms:api:5.4")

    //compileOnly "com.github.NEZNAMY:TAB:2.9.2"
    compileOnly("com.github.TownyAdvanced:Towny:0.98.4.4")

    compileOnly("com.github.Zrips:Jobs:v4.17.2")

    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")




    //Shaded


    implementation("net.kyori:adventure-text-serializer-bungeecord:4.1.2") {
        exclude(group = "net.kyori", module = "adventure-api")
    }

    //CloudCommands
    implementation("cloud.commandframework:cloud-paper:1.8.0-SNAPSHOT") {
        exclude(group = "net.kyori", module = "adventure-api")
    }
    implementation("cloud.commandframework:cloud-minecraft-extras:1.8.0-SNAPSHOT") {
        exclude(group = "net.kyori", module = "adventure-api")
    }
    //Else it errors:
    implementation("io.leangen.geantyref:geantyref:1.3.13")
    //Interfaces
    implementation("org.incendo.interfaces:interfaces-core:1.0.0-SNAPSHOT")

    implementation("org.incendo.interfaces:interfaces-paper:1.0.0-SNAPSHOT") {
        exclude(group = "com.destroystokyo.paper", module = "paper-api")
    }

    //compileOnly("com.mojang:brigadier:1.0.18")


    //implementation 'com.github.retrooper.packetevents:bukkit:2.0-SNAPSHOT'
    implementation("com.github.AlessioGr.packetevents:bukkit:2.0-SNAPSHOT")

    implementation("com.jeff_media:SpigotUpdateChecker:3.0.0")


    //implementation 'commons-io:commons-io:2.11.0'
    //implementation 'org.apache.commons:commons-text:1.9'
    //implementation 'org.apache.commons:commons-lang3:3.12.0'
    //implementation 'org.apache.commons:commons-lang:3.1'

    //implementation("io.netty:netty-all:4.1.74.Final")


    implementation("commons-io:commons-io:2.11.0")

    //compileOnly("com.willfp:EcoBosses:8.0.0")
    compileOnly(files("libs/EcoBosses-v8.78.0.jar"))
    compileOnly("com.willfp:eco:6.38.3")

    compileOnly(files("libs/znpc-servers-3.9.jar"))


    implementation("com.github.Redempt:Crunch:1.1.2")


    implementation("com.fasterxml.jackson.core:jackson-core:2.13.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.3")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")

    implementation("com.zaxxer:HikariCP:5.0.1")

    compileOnly("com.github.war-systems:UltimateJobs:0.3.6")


}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "rocks.gravili.notquests.paper.shadow"
tasks.withType<ShadowJar> {

    minimize()

    //exclude('com.mojang:brigadier')

    //relocate('io.papermc.lib', path.concat('.paper'))
    relocate("cloud.commandframework", "$shadowPath.cloud")
    relocate("io.leangen.geantyref", "$shadowPath.geantyref")
    relocate("de.themoep", "$shadowPath.de.themoep")

    relocate("org.apache.commons.io", "$shadowPath.commons.io")
    //relocate("org.apache.commons.text", path.concat('.commons.text'))
    //relocate("org.apache.commons.lang3", path.concat('.commons.lang'))

    relocate("io.github.retrooper.packetevents", "$shadowPath.packetevents.bukkit")
    relocate("com.github.retrooper.packetevents", "$shadowPath.packetevents.api")

    //Packet Stuff
   // relocate('net.kyori.adventure.text.serializer.bungeecord', path.concat('.kyori.bungeecord'))
    //relocate('net.kyori.adventure.platform.bukkit', path.concat('.kyori.platform-bukkit'))
    relocate("net.kyori.adventure.text.serializer.bungeecord", "$shadowPath.kyori.bungeecord")


    relocate("org.incendo.interfaces", "$shadowPath.interfaces")

    relocate("redempt.crunch", "$shadowPath.crunch")

    relocate("com.fasterxml.jackson", "$shadowPath.jackson")

    relocate("org.apache.http", "$shadowPath.apache.http")

    relocate("com.zaxxer.hikari", "$shadowPath.hikari")

    relocate("com.jeff_media.updatechecker", "$shadowPath.updatechecker")


    dependencies {
        //include(dependency('org.apache.commons:')
        include(dependency("commons-io:commons-io:"))

        //include(dependency('io.papermc:paperlib')
        //include(dependency("de.themoep:inventorygui:1.5-SNAPSHOT"))
        include(dependency("cloud.commandframework:"))
        include(dependency("io.leangen.geantyref:"))
        include(dependency("me.lucko:"))

        include(dependency("com.github.retrooper.packetevents:"))
        //include(dependency('io.github.retrooper.packetevents:')

        include(dependency("com.github.AlessioGr.packetevents:"))
        include(dependency("org.incendo.interfaces:"))

        //include(dependency('net.kyori:adventure-platform-bukkit:')
        include(dependency("net.kyori:adventure-text-serializer-bungeecord:"))

        include(dependency("com.github.Redempt:Crunch:"))

        include(dependency("com.fasterxml.jackson.dataformat:"))
        include(dependency("com.fasterxml.jackson.core:"))

        include(dependency("org.apache.httpcomponents:"))

        include(dependency("com.zaxxer:"))

        include(dependency("com.jeff_media:SpigotUpdateChecker:"))

    }


    //archiveBaseName.set("notquests")
    archiveClassifier.set("")


    //  configurations.forEach { println("E: " + it.toString()) }

    // println("Size: " + configurations.size)


}


tasks {
    // Run reobfJar on build
    //build {
    //    dependsOn(shadowJar)
    //}
    assemble {
        dependsOn(reobfJar)
    }

    build {
        dependsOn(reobfJar)
    }

    /*shadowJar {
        dependsOn(reobfJar)
    }*/

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