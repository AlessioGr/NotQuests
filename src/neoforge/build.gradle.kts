import org.gradle.api.JavaVersion.VERSION_25

plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

group = "com.notquests"
version = rootProject.version

val minecraftTargetVersion = "26.1.2"
val neoForgeVersion = "26.1.2.76"
val adventureVersion = "4.26.1"
val examinationVersion = "1.3.0"
val hikariVersion = "7.0.2"
val sqliteVersion = "3.53.2.0"
val mysqlVersion = "9.6.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
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
    maven("https://libraries.minecraft.net/") {
        content {
            includeGroup("com.mojang")
        }
    }
}

neoForge {
    version = neoForgeVersion

    runs {
        create("server") {
            server()
            gameDirectory = layout.projectDirectory.dir("run").asFile
            programArgument("--nogui")
        }
    }

    mods {
        create("notquests") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":builtin"))

    jarJar(project(":core"))
    jarJar(project(":builtin"))

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("com.mysql:mysql-connector-j:$mysqlVersion")
    jarJar("com.zaxxer:HikariCP:$hikariVersion") {
        version {
            strictly("[$hikariVersion,8.0.0)")
            prefer(hikariVersion)
        }
    }
    jarJar("org.xerial:sqlite-jdbc:$sqliteVersion") {
        version {
            strictly("[$sqliteVersion,4.0.0)")
            prefer(sqliteVersion)
        }
    }
    jarJar("com.mysql:mysql-connector-j:$mysqlVersion") {
        version {
            strictly("[$mysqlVersion,10.0.0)")
            prefer(mysqlVersion)
        }
    }

    implementation("org.snakeyaml:snakeyaml-engine:3.0.1")
    jarJar("org.snakeyaml:snakeyaml-engine:3.0.1") {
        version {
            strictly("[3.0.1,4.0.0)")
            prefer("3.0.1")
        }
    }

    implementation("com.github.Redempt:Crunch:2.0.3")
    jarJar("com.github.Redempt:Crunch:2.0.3") {
        version {
            strictly("[2.0.3,3.0.0)")
            prefer("2.0.3")
        }
    }

    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    for (artifact in listOf(
            "adventure-api",
            "adventure-key",
            "adventure-text-minimessage",
            "adventure-text-serializer-gson",
            "adventure-text-serializer-json"
    )) {
        jarJar("net.kyori:$artifact:$adventureVersion") {
            version {
                strictly("[$adventureVersion,5.0.0)")
                prefer(adventureVersion)
            }
        }
    }
    for (artifact in listOf("examination-api", "examination-string")) {
        jarJar("net.kyori:$artifact:$examinationVersion") {
            version {
                strictly("[$examinationVersion,2.0.0)")
                prefer(examinationVersion)
            }
        }
    }
    jarJar("net.kyori:option:1.1.0") {
        version {
            strictly("[1.1.0,2.0.0)")
            prefer("1.1.0")
        }
    }

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.mojang:brigadier:1.3.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    shadowJar {
        enabled = false
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(
                mapOf(
                    "version" to project.version,
                    "minecraftVersion" to minecraftTargetVersion,
                    "neoForgeVersion" to neoForgeVersion
                )
            )
        }
    }

    jar {
        archiveFileName.set("notquests-${project.version}-$minecraftTargetVersion-neoforge.jar")
        dependsOn(":core:processResources")
        from(project(":core").layout.buildDirectory.dir("resources/main"))
        manifest {
            attributes(
                "Implementation-Title" to "NotQuests NeoForge",
                "Implementation-Version" to project.version
            )
        }
    }

    test {
        useJUnitPlatform()
    }
}
