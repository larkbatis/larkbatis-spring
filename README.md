# lightbatis-spring

Spring / Spring Boot integration for LightBatis (design §10).

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
| `lightbatis-spring` | `SpringLightBatisSession` — Connections via `DataSourceUtils`, exception translation via `SQLExceptionTranslator` |
| `lightbatis-spring-boot-autoconfigure` | `LightBatisAutoConfiguration`, `LightBatisProperties`, `AutoConfiguration.imports` |
| `lightbatis-spring-boot-starter` | Empty — dependencies only |
| `lightbatis-spring-sample` | Not published: a real Boot context on H2 that proves the claims above |

## Using it

```kotlin
dependencies {
    implementation("io.github.lightbatis:lightbatis-annotations:0.1.0-SNAPSHOT")
    implementation("io.github.lightbatis:lightbatis-spring-boot-starter:0.1.0-SNAPSHOT")
    annotationProcessor("io.github.lightbatis:lightbatis-processor:0.1.0-SNAPSHOT")
}
```

That is the whole setup. There is no `@MapperScan`, no `SqlSessionFactoryBean`, no
`SqlSessionTemplate`:

- the processor emits `LightBatisMapperConfiguration` into your base package as soon as
  it sees spring-context on the build classpath, so `@SpringBootApplication`'s default
  `@ComponentScan` picks it up;
- the auto-configuration supplies the single `LightBatisSession` those `@Bean` methods
  ask for.

Inject mappers as beans and call them from `@Transactional` services.

### Properties

```yaml
lightbatis:
  max-sql-variants: 64            # distinct SQL texts per statement before LightBatis complains
  fail-on-unbounded-fragment: false  # true = throw instead of one warning (good in staging)
```

Both are about the operational cost of `${}`: statement caches are keyed by SQL text, so
a fragment whose value set is not bounded grows them without limit.

### When the defaults do not fit

| Situation | What to do |
|---|---|
| Mappers outside the scanned packages | `-Alightbatis.springConfigPackage=com.example.app`, or `@Import(LightBatisMapperConfiguration.class)` |
| You want to declare the mapper beans yourself | `-Alightbatis.springConfig=false` |
| More than one `DataSource` | Declare one `SpringLightBatisSession` per `DataSource` and write the mapper `@Bean` methods yourself. `@ConditionalOnSingleCandidate` makes the auto-configuration back off entirely rather than guess, and the generated `@Configuration` takes a single `LightBatisSession` — mark one `@Primary`, or suppress the class with the option above. Per-mapper `DataSource` selection is **deferred**, per build plan M4: no design without a real service that needs it |

## What runs and what does not

| Scenario | | Why |
|---|---|---|
| `@Transactional` on a service, mapper called inside | works | `DataSourceUtils` returns the connection bound to the transaction |
| `REQUIRES_NEW`, `NESTED`, rollback rules | works | Spring handles all of it; LightBatis only asks for a connection |
| `readOnly = true` | works | Spring sets the flag on that connection |
| Mapper called outside any transaction | works | Auto-commit; `releaseConnection` closes it immediately |
| Sharing a transaction with `JdbcTemplate` or JPA | works | Same `DataSourceUtils`, same `DataSourceTransactionManager` |
| MyBatis `ExecutorType.BATCH` | absent | There is no executor. Batch is a generated mapper method, explicit in the signature (design §07) |
| MyBatis plugins/interceptors | absent | Dropped in design §08. Spring AOP on a mapper bean still works — the mapper is a real bean |

## Spring AOT and native image

`@Bean AccountMapper accountMapper(LightBatisSession s)` has a static return type, so AOT
treats it like any other bean: no `getObjectType()` at runtime, no proxy hint, no
`reflect-config.json` for the mapper layer. `MapperFactoryBean` is the opposite case —
the bean type is only known at runtime and what it returns is a JDK proxy.

`proxyBeanMethods = false` on the generated `@Configuration` is load-bearing, not style:
the default `true` makes Spring build a CGLIB subclass of that class at runtime, which is
exactly the runtime bytecode generation this project exists to remove.

## Not implemented

`log-sql` appears in the design doc's property list and is deliberately absent: every
generated body would have to carry a logging branch, and the §04 generated shape has
none. SQL logging belongs to the driver or the pool (`net.ttddyy:datasource-proxy`,
p6spy) until there is a reason to change that.

## Building

`./gradlew build` (JDK 17 via toolchain). `settings.gradle.kts` already has
`includeBuild("../lightbatis")`, so local changes to the core repo are picked up without
publishing.

All three published modules carry a real JPMS descriptor:
`io.github.lightbatis.spring`, `io.github.lightbatis.spring.boot`,
`io.github.lightbatis.spring.boot.starter`.
