// The Spring integration end to end (build plan §07, M4): a real javac run
// through the annotation processor, a real Spring Boot context, a real H2
// database. The claims of design §10 — mappers are ordinary beans, the
// generated @Configuration replaces @MapperScan, and conn() joins whatever
// transaction is running — are only worth as much as this module proves.
//
// Never published; the starter is consumed here exactly as an application
// would consume it.
dependencies {
    implementation("io.github.lightbatis:lightbatis-annotations:0.1.0-SNAPSHOT")
    implementation(project(":lightbatis-spring-boot-starter"))
    annotationProcessor("io.github.lightbatis:lightbatis-processor:0.1.0-SNAPSHOT")

    runtimeOnly("com.h2database:h2:2.3.232")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.5")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc:3.3.5")
}
