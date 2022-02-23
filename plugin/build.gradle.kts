import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id ("com.github.johnrengelman.shadow")
    id("xyz.jpenilla.run-paper")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "rocks.gravili.notquests"
version = rootProject.version

repositories {
    mavenCentral()

    maven("https://papermc.io/repo/repository/maven-public/"){
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.kyori")
            includeGroup("com.destroystokyo.paper")
            includeGroup("com.destroystokyo")
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

    //mavenLocal()

}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    implementation(project(path= ":paper", configuration= "shadow"))

    //implementation(project(":spigot"))
    //implementation(project(":paper"))

    //compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")

}

/**
 * Configure NotQuests for shading
 */
val shadowPath = "rocks.gravili.notquests"
tasks.withType<ShadowJar> {
    minimize()

    //relocate("rocks.gravili.notquests.spigot", "$shadowPath.spigot")
    //relocate("rocks.gravili.notquests.paper", "$shadowPath.paper")

    dependencies {
        include(dependency(":spigot"))
        include(dependency(":paper"))
    }
    //archiveBaseName.set("notquests")
    archiveClassifier.set("")
    //archiveClassifier.set(null)
}
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
    //build {
    //    dependsOn(shadowJar)
    //}



    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(16)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}

/*publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "rocks.gravili.notquests"
            artifactId = "NotQuests"
            version = "4.0.0-dev"

            from(components["java"])
        }
    }
}*/


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
    apiVersion = "1.16"
    authors = listOf("NoeX")
    description = "Flexible, open, GUI Quest Plugin for Minecraft 1.17 and 1.18"
    website = "quests.notnot.pro"
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
        "UltimateJobs"
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
    }
}
