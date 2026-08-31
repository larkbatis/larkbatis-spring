// LarkBatisAutoConfiguration + LarkBatisProperties + AutoConfiguration.imports.
// Kept separate from the starter per Spring Boot guidance: autoconfigure carries
// code, the starter only carries dependencies.

description = "Spring Boot auto-configuration for LarkBatis — supplies the LarkBatisSession the generated @Configuration asks for"

dependencies {
    api(project(":larkbatis-spring"))
    api("org.springframework.boot:spring-boot-autoconfigure:3.3.5")

    // Turns the javadoc on LarkBatisProperties into
    // META-INF/spring-configuration-metadata.json, which is what makes an IDE
    // complete `larkbatis.` in application.yml, show the default and the
    // description, and flag a misspelt key. Without it the properties work but
    // read as unknown in every editor.
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.3.5")

    testImplementation("org.springframework.boot:spring-boot-test:3.3.5")
    testImplementation("org.springframework:spring-test:6.1.14")
    testImplementation("com.h2database:h2:2.3.232")
    // ApplicationContextRunner's assertions are AssertJ ones; spring-boot-test
    // brings the runner, not the matcher library
    testImplementation("org.assertj:assertj-core:3.25.3")
}



// The configuration metadata's descriptions come from the field javadoc and its
// defaults from the field initializers, so the processor only produces them for
// a source file javac was actually handed. An incremental build hands it only
// what changed, and a jar built that way ships property names with no
// description and no default — degraded, still valid, and invisible until
// someone opens application.yml in an editor. Three source files make the
// saving meaningless, so the module always compiles whole.
//
// It also removes the "Implicitly compiled files were not subject to annotation
// processing" warning, which is the same cause seen from javac's side: the
// untouched siblings reached through the -sourcepath Gradle derives from the
// module path.
tasks.named<JavaCompile>("compileJava") {
    options.isIncremental = false
}
