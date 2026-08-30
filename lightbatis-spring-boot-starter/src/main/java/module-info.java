/**
 * The starter as a module: an aggregator with no packages of its own, whose
 * only content is {@code requires transitive} edges. Depending on it on the
 * module path therefore reads exactly what a LightBatis application needs.
 *
 * <p>{@code spring-boot-starter-jdbc} is deliberately absent. It is an empty
 * pom-style jar with no {@code Automatic-Module-Name}, so its module name
 * would be derived from a file name — a name that changes with a version
 * bump. Aggregating Spring's own jars is a build-tool job (the {@code api}
 * dependencies in this module's build file), and the module graph gets them
 * through {@code io.github.lightbatis.spring} instead.
 */
module io.github.lightbatis.spring.boot.starter {
    requires transitive io.github.lightbatis.runtime;
    requires transitive io.github.lightbatis.spring;
    requires transitive io.github.lightbatis.spring.boot;
}
