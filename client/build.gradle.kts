val deps: Map<String, String> by extra
plugins {
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

dependencies {
    implementation("org.asynchttpclient:async-http-client:2.12.3")

    implementation("com.fasterxml.jackson.core", "jackson-databind", deps["jackson"])
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", deps["jackson"])
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", deps["jackson"])
    implementation("com.google.guava:guava:30.1.1-jre")
}

tasks {
    shadowJar {
        archiveBaseName.set("moderntreasury-client")
    }
}

apply(plugin = "maven-publish")
configure<PublishingExtension> {
    findProperty("com.classpass.jenkins.localRepo")?.toString()?.let { localRepoPath ->
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = "client"
                from(components["java"])
            }
        }

        // if we're publishing artifacts, add the repo to write to
        repositories {
            maven {
                name = "jenkinsLocal"
                url = uri(localRepoPath)
            }
        }
    }
}
