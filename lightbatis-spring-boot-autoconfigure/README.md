# lightbatis-spring-boot-autoconfigure

Two beans and two settings. That is the entire auto-configuration.

Published: `io.github.lightbatis:lightbatis-spring-boot-autoconfigure`. Most
applications get it through
[`lightbatis-spring-boot-starter`](../lightbatis-spring-boot-starter/README.md)
rather than declaring it directly.

## What it contributes

| Bean | Condition |
|---|---|
| `SQLExceptionTranslator` | `@ConditionalOnMissingBean` — Spring's own default since 6.0, `SQLExceptionSubclassTranslator` |
| `LightBatisSession` | `@ConditionalOnMissingBean` — a `SpringLightBatisSession` over the single `DataSource` |

Plus `LightBatisProperties`, bound from `lightbatis.*`.

Compare with `MybatisAutoConfiguration`, which needs a `SqlSessionFactoryBean`,
a `SqlSessionTemplate`, a `ConfigurationCustomizer` SPI and a mapper-scanner
registrar. None of those has anything to configure here: the mappers are
ordinary classes, and there is no runtime `Configuration` object to customize
because every decision it would hold was made at build time.

## How it fits together

```
@SpringBootApplication
   │  default @ComponentScan finds …
   ▼
LightBatisMapperConfiguration          ← emitted by lightbatis-processor into
   │  one @Bean per mapper, each          your base package when spring-context
   │  asking for a LightBatisSession      is on the build classpath
   ▼
LightBatisAutoConfiguration            ← this module supplies exactly that bean
   │
   ▼
SpringLightBatisSession → DataSourceUtils → the @Transactional connection
```

There is no `@MapperScan` and no `SqlSessionFactoryBean` anywhere in that
picture.

## Details that are load-bearing

**`@ConditionalOnSingleCandidate(DataSource.class)`** is what makes the
multiple-DataSource case safe. With two DataSources and no `@Primary`, *no*
session bean is contributed and the application declares its own — one
`SpringLightBatisSession` per DataSource. Guessing which mapper belongs to
which source is precisely what a generator cannot do.

**`afterName` with two package names**, not `after = DataSourceAutoConfiguration.class`:

```java
@AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",       // Boot 4
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"})      // Boot 3
```

Spring Boot 4 moved `DataSourceAutoConfiguration` into the `spring-boot-jdbc`
module and renamed its package. A class reference compiled against Boot 3
cannot be resolved on Boot 4, and Spring's response is to drop this whole class
from the candidate list — no bean, no warning, no failure until something asks
for a `LightBatisSession`. A *name* that matches nothing is simply ignored, so
listing both is what makes one jar work on both.

**`@AutoConfiguration` implies `proxyBeanMethods = false`**, so no CGLIB
subclass of this class is built at runtime — the same reason the generated
mapper `@Configuration` spells it out.

**Registration is `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.**
`spring.factories` has not been read for auto-configuration since Boot 3.

**The variant thresholds are applied in `afterPropertiesSet()`.** They are
process-wide because the counters they guard are: generated code calls
`LightBatisSql.trackVariants` statically, with no session in reach. Applying
them once when the context refreshes keeps that global out of the session
bean's constructor.

## Properties

```yaml
lightbatis:
  max-sql-variants: 64             # distinct SQL texts per statement before LightBatis complains
  fail-on-unbounded-fragment: false # true = throw instead of one warning
```

Both are about the operational cost of `${}`. Statement caches — the driver's
and the database's — are keyed by SQL text, so a fragment whose value set is
not bounded grows them without limit. Turning the second one on belongs in a
test or staging profile, where it finds the unbounded fragment before it ships;
production should not start failing over a log-worthy trend.

The list is short on purpose: there is no runtime `Configuration` to mirror,
because type handlers, result maps and statement shapes were all decided at
build time.

## Opting out

Take `-autoconfigure` off (or use `lightbatis-spring` alone) and declare the
beans yourself. To keep the auto-configuration but override one bean, just
declare your own — both are `@ConditionalOnMissingBean`.

## How to run

```bash
./gradlew :lightbatis-spring-boot-autoconfigure:test
```

`LightBatisAutoConfigurationTest` uses `ApplicationContextRunner`: the session
bean appears with one DataSource, does not appear with two unqualified ones, a
user-declared bean wins, and the properties reach `LightBatisSql`.

## JPMS

`module-info.java` requires `spring.boot.autoconfigure`, `spring.boot`,
`spring.context` and `spring.beans`, and re-exports `io.github.lightbatis.spring`
transitively. There is no `opens`: `@ConfigurationProperties` binding does
reflect over `LightBatisProperties`, but the class and its setters are public
in an exported package, which is all the binder needs. If a future property
type ever forces an `opens`, that is Spring's container reflecting — never
LightBatis, which reflects nowhere.
