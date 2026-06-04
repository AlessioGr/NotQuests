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
}

group = "rocks.gravili.notquests"
version = rootProject.version

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.kyori")
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

    maven("https://repo.codemc.io/repository/maven-releases/") {
        content {
            includeGroup("com.github.retrooper")
        }
    }

    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.MilkBowl")
            includeGroup("com.github.TheBusyBiscuit")
            includeGroup("com.github.TownyAdvanced")
            includeGroup("com.github.Zrips")
            includeGroup("com.willfp")
            includeGroup("com.github.war-systems")
            includeGroup("com.github.UlrichBR")
            includeGroup("com.github.Slimefun")
            includeGroup("net.citizensnpcs")
            includeGroup("com.github.Redempt")
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

    maven("https://mvn.lumine.io/repository/maven-public/") {
        content {
            includeGroup("io.lumine.xikage")
            includeGroup("io.lumine")
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
            includeGroup("org.incendo")
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

    maven("https://repo.opencollab.dev/main/") {
        content {
            includeGroup("org.geysermc.floodgate")
            includeGroup("org.geysermc.cumulus")
            includeGroup("org.geysermc")
            includeGroup("org.geysermc.event")
            includeGroup("org.geysermc.geyser")
        }
    }

    maven("https://repo.xenondevs.xyz/releases")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://repo.magmaguy.com/releases")
    //mavenLocal()

}

paperweight {
    // Keep the Paper dev bundle (NMS / Mojang-mapped server) on the COMPILE classpath only,
    // so it is NOT on the test runtime classpath where it conflicts with MockBukkit's own
    // Bukkit implementation ("two service providers" / "Bukkit not initialized").
    // See https://docs.mockbukkit.org/docs/en/user_guide/advanced/paperweight
    addServerDependencyTo.set(configurations.named("compileOnly").map { setOf(it) })
}

dependencies {
    implementation(project(path = ":common", configuration = "shadow"))
    paperweight.paperDevBundle("26.1.2.build.64-stable")

    compileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor("org.projectlombok:lombok:1.18.44")

    compileOnly("net.citizensnpcs:citizens-main:2.0.42-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }

    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")


    compileOnly("io.lumine:Mythic-Dist:5.3.0-SNAPSHOT")
    compileOnly("com.magmaguy:EliteMobs:9.1.9")
    compileOnly(files("libs/EliteMobs-8.7.11.jar"))


    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT")

    compileOnly("com.github.Slimefun:Slimefun4:RC-37")

    compileOnly("net.luckperms:api:5.4")

    compileOnly("com.github.TownyAdvanced:Towny:0.98.4.4")

    compileOnly("com.github.Zrips:Jobs:v4.17.2")

    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")


    //Shaded


    implementation("net.kyori:adventure-api:4.18.0") {}

    //CloudCommands
    implementation("org.incendo:cloud-paper:2.0.0-SNAPSHOT") {
        exclude(group = "org.incendo.cloud", module = "cloud-bukkit")
    }
    implementation("org.incendo:cloud-minecraft-extras:2.0.0-SNAPSHOT")

    //Else it errors:
    implementation("io.leangen.geantyref:geantyref:1.3.13")

    //InvUI
    implementation("xyz.xenondevs.invui:invui:2.1.0")

    implementation("com.github.retrooper:packetevents-spigot:2.12.0")


    implementation("commons-io:commons-io:2.11.0")

    //compileOnly("com.willfp:EcoBosses:8.0.0")
    compileOnly(files("libs/EcoBosses-v8.78.0.jar"))
    compileOnly("com.willfp:eco:6.38.3")

    compileOnly(files("libs/znpcs-4.8.jar"))


    implementation("com.github.Redempt:Crunch:2.0.3")



    implementation("com.zaxxer:HikariCP:7.0.2")

    compileOnly("com.github.war-systems:UltimateJobs:0.3.6")


    // --- Testing (JUnit 6 + MockBukkit) ---
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // MockBukkit for Paper 26.1.2 (in-JVM mock server; no real server needed)
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.113.1")
    // MockBukkit does NOT bundle the Bukkit API (it assumes the plugin already provides it).
    // Our paper-api comes from the paperweight dev bundle, which is compileOnly (off the test
    // classpath), so add the regular paper-api + JetBrains annotations for the test compile.
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.64-stable")
    testImplementation("org.jetbrains:annotations:26.1.0")

    // Mockito (spies/mocks) — ready for future tests (e.g. failing-Connection DB tests)
    testImplementation("org.mockito:mockito-core:5.23.0")

    // SQLite JDBC driver for deterministic DB-integrity tests (matches the runtime driver)
    testImplementation("org.xerial:sqlite-jdbc:3.53.1.0")
}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "rocks.gravili.notquests.paper.shadow"


tasks {

    shadowJar {
        // DO NOT minimize the jar, since cloud doesnt like it
        // Reference: https://discord.com/channels/766366162388123678/1170254709722984460/1242027222773006376

        relocate("cloud.commandframework", "$shadowPath.cloud")
        relocate("cloud.commandframework.bukkit.internal", "$shadowPath.cloud.bukkit.internal")
        relocate("io.leangen.geantyref", "$shadowPath.geantyref")
        relocate("de.themoep", "$shadowPath.de.themoep")

        relocate("org.apache.commons.io", "$shadowPath.commons.io")

        relocate("io.github.retrooper.packetevents", "$shadowPath.packetevents.bukkit")
        relocate("com.github.retrooper.packetevents", "$shadowPath.packetevents.api")

        relocate("net.kyori.adventure.text.serializer.bungeecord", "$shadowPath.kyori.bungeecord")

        relocate("xyz.xenondevs.invui", "$shadowPath.invui")

        relocate("redempt.crunch", "$shadowPath.crunch")

        relocate("com.fasterxml.jackson", "$shadowPath.jackson")

        relocate("org.apache.http", "$shadowPath.apache.http")

        relocate("com.zaxxer.hikari", "$shadowPath.hikari")

        //relocate("com.jeff_media.updatechecker", "$shadowPath.updatechecker")


        dependencies {
            include(dependency("commons-io:commons-io:.*"))
            include(dependency("xyz.xenondevs.invui:.*:.*"))

            include(dependency("org.incendo:.*:.*"))
            include(dependency("io.leangen.geantyref:.*:.*"))
            include(dependency("me.lucko:.*:.*"))

            include(dependency("com.github.retrooper:.*:.*"))
            include(dependency("io.github.retrooper:.*:.*"))

            include(dependency("net.kyori:adventure-text-serializer-bungeecord:.*"))

            include(dependency("com.github.Redempt:.*:.*"))

            include(dependency("com.fasterxml.jackson.dataformat:.*:.*"))
            include(dependency("com.fasterxml.jackson.core:.*:.*"))

            include(dependency("org.apache.httpcomponents:.*:.*"))

            include(dependency("com.zaxxer:.*:.*"))
        }


        archiveClassifier.set("")

    }

    test {
        useJUnitPlatform()
        // Quiet Mockito's self-attaching agent on JDK 25+ and allow MockBukkit's reflection.
        jvmArgs("-XX:+EnableDynamicAgentLoading", "--add-opens", "java.base/java.lang=ALL-UNNAMED")
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    compileJava {
        mustRunAfter(":common:jar")

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
        minecraftVersion("26.1.2")
    }
}
