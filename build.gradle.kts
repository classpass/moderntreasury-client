import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    application
    id("org.jmailen.kotlinter") version "3.4.0"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

group = "com.classpass.moderntreasury"

repositories {
    maven {
        url = uri("https://classpassengineering.jfrog.io/classpassengineering/libs-release")
        credentials {
            username = project.properties["com.classpass.artifactory.username"].toString()
            password = project.properties["com.classpass.artifactory.apiKey"].toString()
        }
        authentication { create<BasicAuthentication>("basic") }
    }
}

val deps by extra {
    mapOf(
        "jade" to "4.5.2",
        "jackson" to "2.12.2",
        "junit" to "5.7.1"
    )
}
dependencies {
    implementation("org.asynchttpclient:async-http-client:2.12.3")
    api("com.classpass.jade", "async-http-client-tools", deps["jade"])
    implementation("com.classpass.jade", "util", deps["jade"])

    implementation("com.fasterxml.jackson.core", "jackson-databind", deps["jackson"])
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", deps["jackson"])
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", deps["jackson"])
    implementation("com.google.guava:guava:30.1.1-jre")

    testImplementation("com.classpass.jade", "test-support", deps["jade"])
    testImplementation("org.junit.jupiter:junit-jupiter-api:${deps["junit"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${deps["junit"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${deps["junit"]}")
    testImplementation("com.github.tomakehurst", "wiremock", "2.25.1")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${deps["junit"]}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${deps["junit"]}")

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

task("sandbox-test", JavaExec::class) {
    main="com.classpass.moderntreasury.SandboxTest"
    classpath = sourceSets["main"].runtimeClasspath
}

application {
    mainClassName = "MainKt"
}
apply(plugin = "maven-publish")

java {
    withSourcesJar()
}

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