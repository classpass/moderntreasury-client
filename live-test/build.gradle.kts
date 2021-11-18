group = "com.classpass.moderntreasury"

tasks.withType<Test> {
    onlyIf {
        project.hasProperty("liveTests")
    }
}

dependencies {
    testImplementation(project(":client"))
}
