group = "com.classpass.moderntreasury"

tasks {
    test {
        onlyIf {
            project.hasProperty("liveTests")
        }
    }
}

dependencies {
    testImplementation(project(":client"))
}