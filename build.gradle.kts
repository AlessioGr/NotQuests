import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id ("com.github.johnrengelman.shadow") version "7.1.1"
}


group = "rocks.gravili.notquests"
version = "3.2.4"

repositories {
}

dependencies {

}

/**
 * Configure NotQuests for shading
 */
val path = "rocks.gravili.notquests.shadow"
tasks.withType<ShadowJar> {


}

tasks {
    //build {
    //    dependsOn(shadowJar)
    //}
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
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "rocks.gravili.notquests"
            artifactId = "NotQuests"
            version = "3.2.4"

            from(components["java"])
        }
    }
}