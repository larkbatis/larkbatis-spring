# Changelog

Notable changes to the LarkBatis Spring integration. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project
follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The release workflow reads the section for the version being tagged out of this
file and uses it verbatim as the GitHub Release body, so a version with no
section here does not get released.

## [Unreleased]

### Added

- **`larkbatis-spring-boot-autoconfigure` ships
  `META-INF/spring-configuration-metadata.json`.** An IDE now completes
  `larkbatis.max-sql-variants` and `larkbatis.fail-on-unbounded-fragment` in
  `application.yml`, shows their defaults and descriptions, and marks a
  misspelt key instead of ignoring it. The properties always worked; they just
  read as unknown in every editor, which is the kind of gap nobody notices from
  inside the build. Produced by `spring-boot-configuration-processor`, and
  guarded by a test that reads the file back off the classpath.

## [0.1.0] - 2026-08-30

First public release.

```kotlin
dependencies {
    implementation("io.github.larkbatis:larkbatis-annotations:0.1.0")
    implementation("io.github.larkbatis:larkbatis-spring-boot-starter:0.1.0")
    annotationProcessor("io.github.larkbatis:larkbatis-processor:0.1.0")
}
```

That is the whole setup. There is no `@MapperScan`, no `SqlSessionFactoryBean`
and no `SqlSessionTemplate`.

### Artifacts

| Coordinate | Role |
|---|---|
| `io.github.larkbatis:larkbatis-spring` | `SpringLarkBatisSession` |
| `io.github.larkbatis:larkbatis-spring-boot-autoconfigure` | `LarkBatisAutoConfiguration`, `LarkBatisProperties` |
| `io.github.larkbatis:larkbatis-spring-boot-starter` | Empty — dependencies only |

### Added

- **`SpringLarkBatisSession`** — connections through `DataSourceUtils`, never
  `dataSource.getConnection()`, so a mapper called inside `@Transactional` gets
  the connection bound to that transaction. Exceptions go through
  `SQLExceptionTranslator`. `REQUIRES_NEW`, `NESTED`, rollback rules and
  `readOnly` all work, because Spring handles all of it and LarkBatis only asks
  for a connection. A transaction shared with `JdbcTemplate` or JPA works for the
  same reason.
- **Auto-configuration** supplying the single `LarkBatisSession` that the
  generated `@Configuration` asks for, registered through
  `META-INF/spring/...AutoConfiguration.imports`. `@ConditionalOnSingleCandidate`
  makes it back off entirely rather than guess when there is more than one
  `DataSource`.
- **Properties:**

  ```yaml
  larkbatis:
    max-sql-variants: 64               # distinct SQL texts per statement before LarkBatis complains
    fail-on-unbounded-fragment: false  # true = throw instead of warn (good in staging)
  ```

  Both are about the operational cost of `${}`: statement caches are keyed by SQL
  text, so a fragment whose value set is not bounded grows them without limit.
- **Half of `mybatis-spring` evaporates.** That project exists to solve two
  problems: a mapper is an interface with no implementation, and a `SqlSession`
  must share its connection with `@Transactional`. The first is not a problem
  here — `AccountMapper$$Impl` is a real class with a real constructor, so it is
  an ordinary bean. The scanner, the `FactoryBean` and the `BeanDefinition`
  post-processing all disappear, replaced by a generated `@Configuration` with
  one `@Bean` method per mapper. The second is untouched, and is very nearly this
  repository's whole content.
- **Spring AOT and native image.** `@Bean AccountMapper accountMapper(...)` has a
  static return type, so AOT treats it like any other bean: no `getObjectType()`
  at runtime, no proxy hint, no `reflect-config.json` for the mapper layer.
  `proxyBeanMethods = false` on the generated `@Configuration` is load-bearing
  rather than style — the default `true` makes Spring build a CGLIB subclass at
  runtime, which is exactly the runtime bytecode generation this project exists
  to remove.
- **One jar for Spring Boot 3 and Spring Boot 4.** Boot 4 moved
  `DataSourceAutoConfiguration` into `spring-boot-jdbc` and renamed its package,
  so `LarkBatisAutoConfiguration` declares its ordering with `afterName` and
  lists *both* package names. A `after = DataSourceAutoConfiguration.class`
  compiled against Boot 3 cannot be resolved on Boot 4, and Spring's response is
  to drop the whole auto-configuration from the candidate list — no bean, no
  warning, and nothing goes wrong until something asks for a `LarkBatisSession`
  and the context fails to start. A name that matches nothing is simply ignored,
  which is what makes listing both safe. Verified by migrating a real Boot 4.1
  service, and a test asserts both names are still there.
- **JPMS.** All three published modules carry real descriptors:
  `io.github.larkbatis.spring`, `io.github.larkbatis.spring.boot`,
  `io.github.larkbatis.spring.boot.starter`.

### Known limitations

- **One `DataSource`.** With more than one, declare a `SpringLarkBatisSession`
  per `DataSource` and write the mapper `@Bean` methods yourself — mark one
  `@Primary`, or suppress the generated `@Configuration` with
  `-Alarkbatis.springConfig=false`. Per-mapper `DataSource` selection
  (`@LarkBatisDataSource`) is deliberately deferred: no design without a real
  service that needs it.
- **`log-sql` is absent**, though the design lists it. Every generated body would
  have to carry a logging branch, and the generated shape has none. SQL logging
  belongs to the driver or the pool (`net.ttddyy:datasource-proxy`, p6spy) until
  there is a reason to change that.
- **MyBatis plugins and interceptors are absent** — dropped in the design. Spring
  AOP on a mapper bean still works, because the mapper is a real bean.
- **`ExecutorType.BATCH` has no equivalent.** There is no executor; a batch is a
  generated mapper method, explicit in the signature.

[Unreleased]: https://github.com/larkbatis/larkbatis-spring/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/larkbatis/larkbatis-spring/releases/tag/v0.1.0
