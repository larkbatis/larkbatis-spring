# larkbatis-spring-boot-starter

Dependencies and nothing else. No classes — `module-info.java` is the only
source file in the module.

Published: `io.github.larkbatis:larkbatis-spring-boot-starter`.

## What it pulls in

| Coordinate | Why |
|---|---|
| `larkbatis-runtime` | What generated mapper bodies call |
| `larkbatis-spring` | `SpringLarkBatisSession` |
| `larkbatis-spring-boot-autoconfigure` | The two beans and the `larkbatis.*` properties |
| `spring-boot-starter-jdbc` | `DataSource`, `spring-jdbc`, `spring-tx` |

Splitting the starter from the autoconfigure module is Spring Boot's own
guidance: the starter carries dependencies so an application gets everything
from one coordinate, while someone who wants to wire it by hand takes
`-autoconfigure` (or neither) and declares the beans themselves.

## Using it

```kotlin
dependencies {
    implementation("io.github.larkbatis:larkbatis-annotations:0.1.0-SNAPSHOT")
    implementation("io.github.larkbatis:larkbatis-spring-boot-starter:0.1.0-SNAPSHOT")
    annotationProcessor("io.github.larkbatis:larkbatis-processor:0.1.0-SNAPSHOT")
}
```

That is the whole setup — no `@MapperScan`, no `SqlSessionFactoryBean`, no
`SqlSessionTemplate`. The processor emits `LarkBatisMapperConfiguration` into
your base package as soon as it sees spring-context on the build classpath, so
`@SpringBootApplication`'s default `@ComponentScan` picks it up, and the
auto-configuration supplies the single `LarkBatisSession` those `@Bean`
methods ask for.

`larkbatis-annotations` and `larkbatis-processor` are **not** here on
purpose: the annotations are a compile-time-only edge, and the processor is
build-only and must never reach a runtime classpath.

## JPMS

An aggregator with no packages of its own, whose only content is `requires
transitive` edges — so depending on it on the module path reads exactly what a
LarkBatis application needs:

```java
module io.github.larkbatis.spring.boot.starter {
    requires transitive io.github.larkbatis.runtime;
    requires transitive io.github.larkbatis.spring;
    requires transitive io.github.larkbatis.spring.boot;
}
```

`spring-boot-starter-jdbc` is deliberately absent from the descriptor. It is an
empty pom-style jar with no `Automatic-Module-Name`, so its module name would
be derived from a file name — a name that changes with a version bump.
Aggregating Spring's own jars is a build-tool job (the `api` dependencies in
the build file), and the module graph reaches them through
`io.github.larkbatis.spring` instead.

## Build notes

There is nothing to test here, and `javadoc` is **disabled**: it fails outright
on "No public or protected classes found to document". Maven Central still
requires a javadoc artifact, so the jar carries a short note explaining why it
is empty instead of a doc tree that cannot exist.

```bash
./gradlew :larkbatis-spring-boot-starter:build
```
