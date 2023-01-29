import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

dependencies {
    implementation("org.asynchttpclient:async-http-client:2.12.3")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.2")
    implementation("com.google.guava:guava:30.1.1-jre")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("moderntreasury-client")
}
