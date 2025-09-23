import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val deps: Map<String, String> by extra
plugins {
    id("com.gradleup.shadow") version "8.3.9"
}

dependencies {
    implementation("org.asynchttpclient:async-http-client:2.12.3")

    implementation("com.fasterxml.jackson.core", "jackson-databind", deps["jackson"])
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", deps["jackson"])
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", deps["jackson"])
    implementation("com.google.guava:guava:30.1.1-jre")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("moderntreasury-client")
}
