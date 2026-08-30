// LarkBatisAutoConfiguration + LarkBatisProperties + AutoConfiguration.imports.
// Kept separate from the starter per Spring Boot guidance: autoconfigure carries
// code, the starter only carries dependencies.

description = "Spring Boot auto-configuration for LarkBatis — supplies the LarkBatisSession the generated @Configuration asks for"

dependencies {
    api(project(":larkbatis-spring"))
    api("org.springframework.boot:spring-boot-autoconfigure:3.3.5")

    testImplementation("org.springframework.boot:spring-boot-test:3.3.5")
    testImplementation("org.springframework:spring-test:6.1.14")
    testImplementation("com.h2database:h2:2.3.232")
    // ApplicationContextRunner's assertions are AssertJ ones; spring-boot-test
    // brings the runner, not the matcher library
    testImplementation("org.assertj:assertj-core:3.25.3")
}
