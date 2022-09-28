import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.OffsetDateTime
import java.time.ZoneOffset

plugins {
    kotlin("jvm") version "1.6.21" apply false
    application
    id("org.jmailen.kotlinter") version "3.4.0"
    id("com.github.hierynomus.license") version "0.16.1" apply false
}

group = "com.classpass.moderntreasury"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jmailen.kotlinter")
    apply(plugin = "com.github.hierynomus.license")

    group = "com.classpass.moderntreasury"
    description = "Modern Treasury Client"

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
}

application {
    mainClass.set("MainKt")
}
