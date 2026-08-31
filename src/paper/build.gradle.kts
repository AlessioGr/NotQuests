import org.gradle.api.JavaVersion.VERSION_25
import org.gradle.api.file.DuplicatesStrategy


plugins {
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
}

group = "com.notquests"
version = rootProject.version

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    // NOTE: We deliberately do NOT add any maven repository for a *plugin* dependency.
    // Every plugin integration API is vendored locally in src/paper/libs/ (see the dependencies block),
    // so a relocated/deleted plugin repo can never break our build. Only repos for libraries we
    // actually shade into our jar (or the platform itself) are listed here.
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("io.papermc")
            includeGroup("net.kyori")
        }
    }

    // packetevents — shaded library
    maven("https://repo.codemc.io/repository/maven-releases/") {
        content {
            includeGroup("com.github.retrooper")
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
    maven("https://jitpack.io") {
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
    // Bukkit implementation provider conflict.
    // See https://docs.mockbukkit.org/docs/en/user_guide/advanced/paperweight
    addServerDependencyTo.set(configurations.named("compileOnly").map { setOf(it) })
}

dependencies {
    implementation(project(path = ":core", configuration = "runtimeElements"))
    implementation(project(path = ":builtin", configuration = "runtimeElements"))
    paperweight.paperDevBundle("26.2.build.121-stable")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // --- Plugin integration APIs ---
    // ALL vendored locally in src/paper/libs/ ON PURPOSE: the build must never depend on an external
    // maven repository for a *plugin* (those repos are frequently relocated / deleted / broken).
    // If every one of those repos disappeared, NotQuests would still compile. These are compileOnly
    // because the real plugin provides the classes at runtime. To update one, drop the new jar in
    // src/paper/libs/ and bump the filename here.
    compileOnly(files("libs/citizens-main-2.0.43-SNAPSHOT.jar", "libs/citizensapi-2.0.43-SNAPSHOT.jar"))
    compileOnly(files("libs/FancyNpcs-2.11.0.jar"))
    compileOnly(files("libs/PlaceholderAPI-2.12.3.jar"))
    compileOnly(files("libs/VaultAPI-1.7.1.jar"))
    compileOnly(files("libs/MythicMobs-5.13.0.jar"))
    compileOnly(files("libs/EliteMobs-10.8.1.jar"))
    compileOnly(files("libs/worldedit-core-7.4.5.jar"))
    compileOnly(files("libs/worldedit-bukkit-7.4.5.jar"))
    compileOnly(files("libs/Slimefun4-RC-37.jar"))
    compileOnly(files("libs/LuckPerms-api-5.5.jar"))
    compileOnly(files("libs/Towny-0.103.2.0.jar"))
    compileOnly(files("libs/Jobs-5.2.6.6.jar"))
    compileOnly(files("libs/floodgate-api-2.2.5-SNAPSHOT.jar"))
    compileOnly(files("libs/EcoMobs-2026.35.1.jar"))
    compileOnly(files("libs/eco-2026.35.jar"))
    compileOnly(files("libs/BetonQuest-3.2.0.jar"))
    // libreforge-loader provides com.willfp.libreforge.loader.configs.RegistrableCategory, which
    // EcoMobs' registry (EcoMobs.INSTANCE) extends; needed on the compile classpath. Vendored like
    // the other eco-ecosystem plugins.
    compileOnly(files("libs/libreforge-loader-2026.35.1-all.jar"))


    // --- Shaded libraries (bundled into our jar; fine to resolve from maven) ---

    // Adventure is pinned to Paper 26.2's bundled Adventure BOM.
    implementation("net.kyori:adventure-api:5.2.0") {}

    //InvUI
    implementation("xyz.xenondevs.invui:invui:2.3.1")

    implementation("com.github.retrooper:packetevents-spigot:2.13.0")


    implementation("commons-io:commons-io:2.22.0")


    implementation("com.github.Redempt:Crunch:2.0.3")



    implementation("io.papermc:paperlib:1.0.8")


    // --- Testing (JUnit 6 + MockBukkit) ---
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // MockBukkit for Paper 26.2 (in-JVM mock server; no real server needed)
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    // MockBukkit does NOT bundle the Bukkit API (it assumes the plugin already provides it).
    // Our paper-api comes from the paperweight dev bundle, which is compileOnly (off the test
    // classpath), so add the regular paper-api + JetBrains annotations for the test compile.
    testImplementation("io.papermc.paper:paper-api:26.2.build.121-stable")
    testImplementation("org.jetbrains:annotations:26.1.0")

    // Mockito (spies/mocks) — ready for future tests (e.g. failing-Connection DB tests)
    testImplementation("org.mockito:mockito-core:5.23.0")

    // SQLite JDBC driver for deterministic DB-integrity tests (matches the runtime driver)
    testImplementation("org.xerial:sqlite-jdbc:3.53.4.0")
}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "com.notquests.paper.shadow"
val minecraftTargetVersion = "26.2"


tasks {
    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }

    shadowJar {
        dependsOn(":core:classes", ":builtin:classes")
        from(project(":core").layout.buildDirectory.dir("classes/java/main"))
        from(project(":core").layout.buildDirectory.dir("resources/main"))
        from(project(":builtin").layout.buildDirectory.dir("classes/java/main"))
        from(project(":builtin").layout.buildDirectory.dir("resources/main"))

        // DO NOT minimize the jar, since cloud doesnt like it
        // Reference: https://discord.com/channels/766366162388123678/1170254709722984460/1242027222773006376

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

        relocate("io.papermc.lib", "$shadowPath.paperlib")

        relocate("org.snakeyaml.engine", "$shadowPath.snakeyaml.engine")

        //relocate("com.jeff_media.updatechecker", "$shadowPath.updatechecker")


        dependencies {
            include(dependency("commons-io:commons-io:.*"))
            include(dependency("xyz.xenondevs.invui:.*:.*"))

            include(dependency("me.lucko:.*:.*"))

            include(dependency("com.github.retrooper:.*:.*"))
            include(dependency("io.github.retrooper:.*:.*"))

            include(dependency("net.kyori:adventure-text-serializer-bungeecord:.*"))

            include(dependency("com.github.Redempt:.*:.*"))

            include(dependency("com.fasterxml.jackson.dataformat:.*:.*"))
            include(dependency("com.fasterxml.jackson.core:.*:.*"))

            include(dependency("org.apache.httpcomponents:.*:.*"))

            include(dependency("com.zaxxer:.*:.*"))

            include(dependency("io.papermc:paperlib:.*"))

            include(dependency("org.snakeyaml:snakeyaml-engine:.*"))

        }

        // Keep this module's generated plugin.yml/paper-plugin.yml if shaded dependencies also carry
        // plugin metadata (PacketEvents can run standalone and ships its own plugin.yml).
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        archiveFileName.set("notquests-${project.version}-$minecraftTargetVersion-paper.jar")
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
        dependsOn(":core:jar", ":builtin:jar")

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
        minecraftVersion(minecraftTargetVersion)
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
    main = "com.notquests.Main"
    apiVersion = "26.2"
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
        "Floodgate",
        "BetonQuest"
    )

    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD

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

paper {
    name = "NotQuests"
    version = rootProject.version.toString()
    main = "com.notquests.Main"
    apiVersion = "26.2"
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
        register("BetonQuest") {
            required = false
            load = net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }

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
