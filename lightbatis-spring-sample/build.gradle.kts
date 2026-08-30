// The Spring integration end to end: a real javac run through the annotation
// processor, a real Spring Boot context, a real H2 database. Three claims —
// mappers are ordinary beans, the generated @Configuration replaces
// @MapperScan, and conn() joins whatever transaction is running — are only
// worth as much as this module proves.
//
// Never published; the starter is consumed here exactly as an application
// would consume it.

val lightbatisVersion = providers.gradleProperty("lightbatisVersion").get()

dependencies {
    implementation("io.github.lightbatis:lightbatis-annotations:$lightbatisVersion")
    implementation(project(":lightbatis-spring-boot-starter"))
    annotationProcessor("io.github.lightbatis:lightbatis-processor:$lightbatisVersion")

    runtimeOnly("com.h2database:h2:2.3.232")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.5")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc:3.3.5")
}
