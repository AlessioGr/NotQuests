import org.gradle.api.JavaVersion.VERSION_25


group = "com.notquests"
version = rootProject.version

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 11 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    sourceCompatibility = VERSION_25
    targetCompatibility = VERSION_25
}

repositories {
    mavenCentral()
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
}

dependencies {
    api("org.snakeyaml:snakeyaml-engine:3.1.1")
    api("net.kyori:adventure-api:5.2.0")
    api("net.kyori:adventure-text-minimessage:5.2.0")
    api("net.kyori:adventure-text-serializer-gson:5.2.0")
    api("com.github.Redempt:Crunch:2.0.3")
    api("com.zaxxer:HikariCP:7.1.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.53.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("all")
    }
}
