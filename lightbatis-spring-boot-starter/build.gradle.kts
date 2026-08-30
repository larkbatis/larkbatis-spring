// Empty starter — dependencies only. Splitting the starter from
// the autoconfigure module is Spring Boot's own guidance: the starter carries
// dependencies so an application gets everything with one coordinate, while
// someone who wants to wire it by hand takes -autoconfigure (or neither) and
// declares the beans themselves.

description = "Spring Boot starter for LightBatis — dependencies only"

val lightbatisVersion = providers.gradleProperty("lightbatisVersion").get()

dependencies {
    api(project(":lightbatis-spring"))
    api(project(":lightbatis-spring-boot-autoconfigure"))
    api("io.github.lightbatis:lightbatis-runtime:$lightbatisVersion")
    api("org.springframework.boot:spring-boot-starter-jdbc:3.3.5")
}

// This module has no classes — `module-info.java` is the only source file — and
// javadoc fails outright on "No public or protected classes found to document".
// Maven Central still requires a javadoc artifact, so the jar carries a note
// saying why it is empty instead of a doc tree that cannot exist.

val javadocPlaceholder by tasks.registering {
    description = "A note explaining why this module's javadoc jar has no documentation in it"
    val outputFile = layout.buildDirectory.file("javadoc-placeholder/README.txt")
    outputs.file(outputFile)
    val text = """
        lightbatis-spring-boot-starter declares dependencies and nothing else, which is
        Spring Boot's own guidance for starters: the code lives in
        lightbatis-spring-boot-autoconfigure and lightbatis-spring, and those two
        modules carry the javadoc.

        https://github.com/lightbatis/lightbatis-spring
    """.trimIndent() + "\n"
    doLast {
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(text)
        }
    }
}

tasks.named<Javadoc>("javadoc") {
    enabled = false
}

tasks.named<Jar>("javadocJar") {
    from(javadocPlaceholder)
}
