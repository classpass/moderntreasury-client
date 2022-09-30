group = "com.classpass.oss.moderntreasury"

tasks.withType<Test> {
    onlyIf {
        project.hasProperty("liveTests")
    }
}

dependencies {
    testImplementation(project(":client"))
}
