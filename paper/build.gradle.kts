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
}

group = "rocks.gravili.notquests"
version = rootProject.version

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    // NOTE: We deliberately do NOT add any maven repository for a *plugin* dependency.
    // Every plugin integration API is vendored locally in paper/libs/ (see the dependencies block),
    // so a relocated/deleted plugin repo can never break our build. Only repos for libraries we
    // actually shade into our jar (or the platform itself) are listed here.
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.kyori")
        }
    }

    // packetevents — shaded library
    maven("https://repo.codemc.io/repository/maven-releases/") {
        content {
            includeGroup("com.github.retrooper")
        }
    }

    // cloud command framework — shaded library (2.x dev line)
    maven("https://oss.sonatype.org/content/repositories/snapshots") {
        content {
            includeGroup("org.incendo")
        }
    }

    // Mojang libraries (brigadier / authlib / datafixerupper transitives)
    maven("https://libraries.minecraft.net/") {
        content {
            includeGroup("com.mojang")
        }
    }

    // Crunch — shaded expression-evaluation library
    maven("https://redempt.dev") {
        content {
            includeGroup("com.github.Redempt")
        }
    }

    // InvUI — shaded GUI library
    maven("https://repo.xenondevs.xyz/releases")
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
    paperweight.paperDevBundle("26.1.2.build.69-stable")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // --- Plugin integration APIs ---
    // ALL vendored locally in paper/libs/ ON PURPOSE: the build must never depend on an external
    // maven repository for a *plugin* (those repos are frequently relocated / deleted / broken).
    // If every one of those repos disappeared, NotQuests would still compile. These are compileOnly
    // because the real plugin provides the classes at runtime. To update one, drop the new jar in
    // paper/libs/ and bump the filename here.
    compileOnly(files("libs/Citizens-2.0.42-SNAPSHOT.jar"))
    compileOnly(files("libs/FancyNpcs-2.10.0.jar"))
    compileOnly(files("libs/PlaceholderAPI-2.12.2.jar"))
    compileOnly(files("libs/VaultAPI-1.7.1.jar"))
    compileOnly(files("libs/Mythic-Dist-5.12.1.jar"))
    compileOnly(files("libs/EliteMobs-10.4.0.jar"))
    compileOnly(files("libs/worldedit-core-7.4.3.jar"))
    compileOnly(files("libs/worldedit-bukkit-7.4.3.jar"))
    compileOnly(files("libs/Slimefun4-RC-37.jar"))
    compileOnly(files("libs/LuckPerms-api-5.5.jar"))
    compileOnly(files("libs/Towny-0.103.0.0.jar"))
    compileOnly(files("libs/Jobs-5.2.6.5.jar"))
    compileOnly(files("libs/floodgate-api-2.2.5-SNAPSHOT.jar"))
    compileOnly(files("libs/EcoMobs-11.7.0.jar"))
    compileOnly(files("libs/eco-7.6.3.jar"))
    // libreforge-loader provides com.willfp.libreforge.loader.configs.RegistrableCategory, which
    // EcoMobs' registry (EcoMobs.INSTANCE) extends; needed on the compile classpath. Vendored like
    // the other eco-ecosystem plugins.
    compileOnly(files("libs/libreforge-loader-5.6.0-all.jar"))


    // --- Shaded libraries (bundled into our jar; fine to resolve from maven) ---

    // Adventure: pinned to Paper 26.1.2's bundled Adventure (adventure-bom 4.26.1). Do NOT move to
    // 5.x — Paper provides 4.x at runtime, so a 5.x compile target would break against the server.
    implementation("net.kyori:adventure-api:4.26.1") {}

    //CloudCommands
    implementation("org.incendo:cloud-paper:2.0.0-SNAPSHOT") {
        exclude(group = "org.incendo.cloud", module = "cloud-bukkit")
    }
    implementation("org.incendo:cloud-minecraft-extras:2.0.0-SNAPSHOT")

    //Else it errors (kept on the 1.3.x line cloud expects):
    implementation("io.leangen.geantyref:geantyref:1.3.16")

    //InvUI
    implementation("xyz.xenondevs.invui:invui:2.1.0")

    implementation("com.github.retrooper:packetevents-spigot:2.12.2")


    implementation("commons-io:commons-io:2.22.0")


    implementation("com.github.Redempt:Crunch:2.0.3")



    implementation("com.zaxxer:HikariCP:7.0.2")


    // --- Testing (JUnit 6 + MockBukkit) ---
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // MockBukkit for Paper 26.1.2 (in-JVM mock server; no real server needed)
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.113.1")
    // MockBukkit does NOT bundle the Bukkit API (it assumes the plugin already provides it).
    // Our paper-api comes from the paperweight dev bundle, which is compileOnly (off the test
    // classpath), so add the regular paper-api + JetBrains annotations for the test compile.
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    testImplementation("org.jetbrains:annotations:26.1.0")

    // Mockito (spies/mocks) — ready for future tests (e.g. failing-Connection DB tests)
    testImplementation("org.mockito:mockito-core:5.23.0")

    // SQLite JDBC driver for deterministic DB-integrity tests (matches the runtime driver)
    testImplementation("org.xerial:sqlite-jdbc:3.53.2.0")
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

        // Strip plugin metadata from shaded libraries. PacketEvents ships its own plugin.yml (it can
        // run as a standalone plugin); if it survives into this shaded jar, the jar masquerades as
        // PacketEvents and Paper tries to load io.github.retrooper.packetevents.PacketEventsPlugin as
        // the main class. The real plugin.yml/paper-plugin.yml is generated by the :plugin module.
        exclude("plugin.yml")
        exclude("paper-plugin.yml")

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
}
