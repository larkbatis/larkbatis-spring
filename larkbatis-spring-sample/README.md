# larkbatis-spring-sample

The Spring integration end to end: a real `javac` run through the annotation
processor, a real Spring Boot context, a real H2 database.

Never published — the starter is consumed here exactly as an application would
consume it, and publishing this module would put H2 into a consumer's
dependency graph.

## What it proves

The three claims the design makes about Spring are only worth as much as this
module:

1. **Mappers are ordinary beans.** `AccountMapper` is injected into
   `AccountService` by constructor. Nothing in the mapper says "Spring".
2. **The generated `@Configuration` replaces `@MapperScan`.**
   `SpringSampleApp` is a bare `@SpringBootApplication`: no `@MapperScan`, no
   `SqlSessionFactoryBean`, no `SqlSessionTemplate`. The default
   `@ComponentScan` finds `LarkBatisMapperConfiguration` in this very package
   — a file the processor emitted — and the Boot auto-configuration supplies
   the one `LarkBatisSession` it asks for.
3. **`conn()` joins whatever transaction is running.** LarkBatis never manages
   a transaction; it asks `DataSourceUtils` for a connection and gets whichever
   one Spring already bound.

## The interesting service methods

`AccountService` exists to make failure modes observable:

```java
@Transactional
public void openThenFail(String owner, BigDecimal balance) {
    open(owner, balance);
    throw new IllegalStateException("deliberate failure after the insert");
}
```

If the mapper were opening its own connection, that insert would survive the
rollback — which is the bug this module exists to make impossible.

```java
@Transactional
public int openAndCount(String owner, BigDecimal balance) {
    open(owner, balance);
    return accounts.count();   // reads its own uncommitted write
}
```

Same connection, same transaction. `get(long)` runs under
`@Transactional(readOnly = true)`, and `streamAll()` puts a cursor inside all
of it — inside a transaction, closing the stream gives nothing back because the
transaction still owns the connection; outside one, it returns to the pool.

## Configuration

Everything is in `application.yml` and `schema.sql`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:lbspring;DB_CLOSE_DELAY=-1
  sql:
    init:
      mode: always

larkbatis:
  max-sql-variants: 32
  fail-on-unbounded-fragment: true
```

The `larkbatis.*` block is there to prove the properties bind — this sample
has no `${}` at all, so neither setting does any work.

## How to run

```bash
./gradlew :larkbatis-spring-sample:test    # SpringIntegrationTest
./gradlew :larkbatis-spring-sample:build   # compile + test + jar
```

`SpringIntegrationTest` is a `@SpringBootTest` — it brings up the real context,
so there is nothing to launch separately. The module does not apply the Spring
Boot Gradle plugin, so there is no `bootRun` task.

To read what the processor emitted, look in
`build/generated/sources/annotationProcessor/java/main/com/example/lbspring/`:
`AccountMapper$$Impl.java`, `AccountRow.java`, `LarkBatisMappers.java` and
`LarkBatisMapperConfiguration.java`.

## Cross-repo builds

`settings.gradle.kts` at the repo root `includeBuild("../larkbatis")` when
that directory exists, so a local checkout of the core repo is substituted for
the published `io.github.larkbatis:*` coordinates. It is conditional because
CI checks out one repository at a time; `-PuseLocalCore=false` opts out
explicitly so a release can be built against the published artifacts without
moving the directory away.
