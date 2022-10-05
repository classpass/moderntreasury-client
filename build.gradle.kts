import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.Duration
import java.net.URI


plugins {
    java
    kotlin("jvm") version "1.6.21" apply false
    application
    id("org.jmailen.kotlinter") version "3.4.0"
    id("org.jetbrains.dokka") version "1.4.32" apply false
    id("com.github.hierynomus.license") version "0.16.1" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("net.researchgate.release") version "2.8.1"
}

group = "com.classpass.oss.moderntreasury"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jmailen.kotlinter")
    apply(plugin = "com.github.hierynomus.license")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")

    group = "com.classpass.oss.moderntreasury"
    description = "Modern Treasury Client"

    tasks {
        register<Jar>("docJar") {
            from(project.tasks["dokkaHtml"])
            archiveClassifier.set("javadoc")
        }
    }

    repositories {
        mavenCentral()
    }

    val deps by extra {
        mapOf(
            "jackson" to "2.12.2",
            "junit" to "5.7.1"
        )
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:${deps["junit"]}")
        testImplementation("org.junit.jupiter:junit-jupiter-params:${deps["junit"]}")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:${deps["junit"]}")
        testImplementation("com.github.tomakehurst", "wiremock", "2.25.1")
        testImplementation(kotlin("test-junit5"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:${deps["junit"]}")
        testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${deps["junit"]}")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23.1")

        implementation("org.slf4j:slf4j-api:1.7.30")
        implementation("ch.qos.logback:logback-classic:1.2.3")
        implementation("ch.qos.logback:logback-core:1.2.3")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        withSourcesJar()
    }

    configure<nl.javadude.gradle.plugins.license.LicenseExtension> {
        header = rootProject.file("LICENSE-header")
    }

    tasks {
        // enable ${year} substitution in licenseFormat
        OffsetDateTime.now(ZoneOffset.UTC).let { now ->
            withType<com.hierynomus.gradle.license.tasks.LicenseFormat> {
                extra["year"] = now.year.toString()
            }
            withType<com.hierynomus.gradle.license.tasks.LicenseCheck> {
                extra["year"] = now.year.toString()
            }
        }

        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }

        test {
            useJUnitPlatform()
        }
    }

    configure<PublishingExtension> {
        publications {
            register<MavenPublication>("sonatype") {
                from(components["java"])
                artifact(tasks["docJar"])
                // sonatype required pom elements
                pom {
                    name.set("${project.group}:${project.name}")
                    description.set(name)
                    url.set("https://github.com/classpass/moderntreasury-client")
                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.html")
                        }
                    }
                    developers {
                        developer {
                            id.set("jstafford406")
                            name.set("Jon Stafford")
                            email.set("38865369+jstafford406@users.noreply.github.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/classpass/moderntreasury-client")
                        developerConnection.set("scm:git:https://github.com/classpass/moderntreasury-client.git")
                        url.set("https://github.com/classpass/moderntreasury-client")
                    }
                }
            }
        }

        // A safe throw-away place to publish to:
        // ./gradlew publishSonatypePublicationToLocalDebugRepository -Pversion=foo
        repositories {
            maven {
                name = "localDebug"
                url = URI.create("file:///${project.buildDir}/repos/localDebug")
            }
        }
    }

    // don't barf for devs without signing set up
    if (project.hasProperty("signing.keyId")) {
        configure<SigningExtension> {
            sign(project.extensions.getByType<PublishingExtension>().publications["sonatype"])
        }
    }

    // releasing should publish
    val provider = provider { project.tasks.named("publishToSonatype") }
    rootProject.tasks.afterReleaseBuild {
        dependsOn(provider)
    }
}


nexusPublishing {
    repositories {
        sonatype {
            // sonatypeUsername and sonatypePassword properties are used automatically
            stagingProfileId.set("1f02cf06b7d4cd") // com.classpass.oss
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
    // these are not strictly required. The default timeouts are set to 1 minute. But Sonatype can be really slow.
    // If you get the error "java.net.SocketTimeoutException: timeout", these lines will help.
    connectTimeout.set(Duration.ofMinutes(3))
    clientTimeout.set(Duration.ofMinutes(3))
}

application {
    mainClass.set("MainKt")
}

release {
    // work around lack of proper kotlin DSL support
    (getProperty("git") as net.researchgate.release.GitAdapter.GitConfig).apply {
        requireBranch = "js/relpub"
    }
}
