import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32" apply false
    application
    id("org.jmailen.kotlinter") version "3.4.0"
}

group = "com.classpass.moderntreasury"


subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jmailen.kotlinter")

    group = "com.classpass.moderntreasury"
    description = "Modern Treasury Client"

    repositories {
        maven {
            url = uri("https://classpassengineering.jfrog.io/classpassengineering/libs-release")
            credentials {
                username = project.properties["com.classpass.artifactory.username"].toString()
                password = project.properties["com.classpass.artifactory.apiKey"].toString()
            }
            authentication { create<BasicAuthentication>("basic") }
        }
        mavenLocal()
    }

    val deps by extra {
        mapOf(
            "jade" to "4.5.2",
            "jackson" to "2.12.2",
            "junit" to "5.7.1"
        )
    }

    dependencies {
        testImplementation("com.classpass.jade", "test-support", deps["jade"])
        testImplementation("org.junit.jupiter:junit-jupiter-api:${deps["junit"]}")
        testImplementation("org.junit.jupiter:junit-jupiter-params:${deps["junit"]}")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:${deps["junit"]}")
        testImplementation("com.github.tomakehurst", "wiremock", "2.25.1")
        testImplementation(kotlin("test-junit5"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:${deps["junit"]}")
        testImplementation("org.mockito.kotlin:mockito-kotlin:3.1.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${deps["junit"]}")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23.1")

        implementation("org.slf4j:slf4j-api:1.7.30")
        implementation("ch.qos.logback:logback-classic:1.2.3")
        implementation("ch.qos.logback:logback-core:1.2.3")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        withSourcesJar()
    }

    tasks.withType<KotlinCompile>() {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.test {
        useJUnitPlatform()
    }
}



task("sandbox-test", JavaExec::class) {
    main = "com.classpass.moderntreasury.SandboxTest"
    classpath = sourceSets["main"].runtimeClasspath
}

application {
    mainClassName = "MainKt"
}
