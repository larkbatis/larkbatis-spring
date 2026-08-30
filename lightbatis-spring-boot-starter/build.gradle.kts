// Empty starter — dependencies only (design §10). Splitting the starter from
// the autoconfigure module is Spring Boot's own guidance: the starter carries
// dependencies so an application gets everything with one coordinate, while
// someone who wants to wire it by hand takes -autoconfigure (or neither) and
// declares the beans themselves.
dependencies {
    api(project(":lightbatis-spring"))
    api(project(":lightbatis-spring-boot-autoconfigure"))
    api("io.github.lightbatis:lightbatis-runtime:0.1.0-SNAPSHOT")
    api("org.springframework.boot:spring-boot-starter-jdbc:3.3.5")
}
