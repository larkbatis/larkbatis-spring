# larkbatis-spring

Spring / Spring Boot integration for LarkBatis.

Half of `mybatis-spring` evaporates here, and knowing which half decides what this
repo contains. `mybatis-spring` exists to solve exactly two problems:

1. **A mapper is an interface with no implementation**, so Spring cannot create a bean
   for it — solved with `@MapperScan` → `ClassPathMapperScanner` → `MapperFactoryBean`
   → `MapperProxy`.
2. **`SqlSession` must share its `Connection` with `@Transactional`** — solved with
   `SqlSessionTemplate` + `SqlSessionUtils` + `SpringManagedTransaction`.

Problem 1 **is not a problem here**: `AccountMapper$$Impl` is a real class with a real
constructor, which is an ordinary bean. The scanner, the `FactoryBean` and the
`BeanDefinition` post-processing all disappear, replaced by a generated `@Configuration`
with one `@Bean` method per mapper. Problem 2 **is untouched**, and it is very nearly
this repo's whole content.

| Module | Role |
|---|---|
| [`larkbatis-spring`](larkbatis-spring/README.md) | `SpringLarkBatisSession` — Connections via `DataSourceUtils`, exception translation via `SQLExceptionTranslator` |
| [`larkbatis-spring-boot-autoconfigure`](larkbatis-spring-boot-autoconfigure/README.md) | `LarkBatisAutoConfiguration`, `LarkBatisProperties`, `AutoConfiguration.imports` |
| [`larkbatis-spring-boot-starter`](larkbatis-spring-boot-starter/README.md) | Empty — dependencies only |
| [`larkbatis-spring-sample`](larkbatis-spring-sample/README.md) | Not published: a real Boot context on H2 that proves the claims above |

## Using it

```kotlin
dependencies {
    implementation("io.github.larkbatis:larkbatis-annotations:0.1.0")
    implementation("io.github.larkbatis:larkbatis-spring-boot-starter:0.1.0")
    annotationProcessor("io.github.larkbatis:larkbatis-processor:0.1.0")
}
```

That is the whole setup. There is no `@MapperScan`, no `SqlSessionFactoryBean`, no
`SqlSessionTemplate`:

- the processor emits `LarkBatisMapperConfiguration` into your base package as soon as
  it sees spring-context on the build classpath, so `@SpringBootApplication`'s default
  `@ComponentScan` picks it up;
- the auto-configuration supplies the single `LarkBatisSession` those `@Bean` methods
  ask for.

Inject mappers as beans and call them from `@Transactional` services.

### Properties

```yaml
larkbatis:
  max-sql-variants: 64            # distinct SQL texts per statement before LarkBatis complains
  fail-on-unbounded-fragment: false  # true = throw instead of one warning (good in staging)
```

Both are about the operational cost of `${}`: statement caches are keyed by SQL text, so
a fragment whose value set is not bounded grows them without limit.

### When the defaults do not fit

| Situation | What to do |
|---|---|
| Mappers outside the scanned packages | `-Alarkbatis.springConfigPackage=com.example.app`, or `@Import(LarkBatisMapperConfiguration.class)` |
| You want to declare the mapper beans yourself | `-Alarkbatis.springConfig=false` |
| More than one `DataSource` | Declare one `SpringLarkBatisSession` per `DataSource` and write the mapper `@Bean` methods yourself. `@ConditionalOnSingleCandidate` makes the auto-configuration back off entirely rather than guess, and the generated `@Configuration` takes a single `LarkBatisSession` — mark one `@Primary`, or suppress the class with the option above. Per-mapper `DataSource` selection is **deferred**, per build plan M4: no design without a real service that needs it |

## What runs and what does not

| Scenario | | Why |
|---|---|---|
| `@Transactional` on a service, mapper called inside | works | `DataSourceUtils` returns the connection bound to the transaction |
| `REQUIRES_NEW`, `NESTED`, rollback rules | works | Spring handles all of it; LarkBatis only asks for a connection |
| `readOnly = true` | works | Spring sets the flag on that connection |
| Mapper called outside any transaction | works | Auto-commit; `releaseConnection` closes it immediately |
| A `Stream`-returning mapper method | works | The stream holds a pooled Connection until closed; inside a transaction `release` is a no-op and the transaction keeps it. `try (Stream<T> rows = ...)` either way |
| Sharing a transaction with `JdbcTemplate` or JPA | works | Same `DataSourceUtils`, same `DataSourceTransactionManager` |
| MyBatis `ExecutorType.BATCH` | absent | There is no executor. Batch is a generated mapper method, explicit in the signature |
| MyBatis plugins/interceptors | absent | Dropped in the design. Spring AOP on a mapper bean still works — the mapper is a real bean |

## Spring AOT and native image

`@Bean AccountMapper accountMapper(LarkBatisSession s)` has a static return type, so AOT
treats it like any other bean: no `getObjectType()` at runtime, no proxy hint, no
`reflect-config.json` for the mapper layer. `MapperFactoryBean` is the opposite case —
the bean type is only known at runtime and what it returns is a JDK proxy.

`proxyBeanMethods = false` on the generated `@Configuration` is load-bearing, not style:
the default `true` makes Spring build a CGLIB subclass of that class at runtime, which is
exactly the runtime bytecode generation this project exists to remove.

## Spring Boot 3 and Spring Boot 4

One jar works on both, and that took one deliberate decision. Boot 4 moved
`DataSourceAutoConfiguration` out of `spring-boot-autoconfigure` into the new
`spring-boot-jdbc` module and renamed its package:

| | Boot 3 | Boot 4 |
|---|---|---|
| `DataSourceAutoConfiguration` | `org.springframework.boot.autoconfigure.jdbc` | `org.springframework.boot.jdbc.autoconfigure` |
| `@AutoConfiguration`, `@ConditionalOn*` | `org.springframework.boot.autoconfigure(.condition)` | unchanged |
| `@ConfigurationProperties` | `org.springframework.boot.context.properties` | unchanged |

So `LarkBatisAutoConfiguration` declares its ordering with `afterName` and
lists **both** package names. This is not style. A `after =
DataSourceAutoConfiguration.class` compiled against Boot 3 cannot be resolved
on Boot 4, and Spring's response is to drop the whole auto-configuration from
the candidate list — no bean, no warning, and nothing goes wrong until
something asks for a `LarkBatisSession` and the context fails to start. A name
that matches nothing is simply ignored, which is what makes listing both safe.

Verified by migrating a real Boot 4.1 service; a test asserts both names are
still there, because "simplifying" it back to a class reference reintroduces a
failure with no symptom.

## Not implemented

`log-sql` appears in the design doc's property list and is deliberately absent: every
generated body would have to carry a logging branch, and the generated shape has
none. SQL logging belongs to the driver or the pool (`net.ttddyy:datasource-proxy`,
p6spy) until there is a reason to change that.

## Building

`./gradlew build` (JDK 17 via toolchain). `settings.gradle.kts` already has
`includeBuild("../larkbatis")`, so local changes to the core repo are picked up without
publishing.

All three published modules carry a real JPMS descriptor:
`io.github.larkbatis.spring`, `io.github.larkbatis.spring.boot`,
`io.github.larkbatis.spring.boot.starter`.
