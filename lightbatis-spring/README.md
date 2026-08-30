# lightbatis-spring

One class: `SpringLightBatisSession`. That really is the whole Spring
integration.

Published: `io.github.lightbatis:lightbatis-spring`.

## Why it is one class

`mybatis-spring` exists to solve two problems. The first — *a mapper is an
interface with no implementation, so Spring cannot create a bean for it* — is
solved by `@MapperScan` → `ClassPathMapperScanner` → `MapperFactoryBean` →
`MapperProxy`. That problem does not exist here: `AccountMapper$$Impl` is a
real class with a real constructor, so the scanner, the `FactoryBean` and the
`BeanDefinition` post-processing all disappear, replaced by a generated
`@Configuration` that `lightbatis-processor` emits.

The second problem — *the session must share its `Connection` with
`@Transactional`* — never evaporates, and it is what this module is.

## The one rule

`conn()` must **never** call `dataSource.getConnection()`.

```java
@Override
public Connection conn() {
    return DataSourceUtils.getConnection(dataSource);   // joins the running transaction
}

@Override
public void release(Connection c) {
    DataSourceUtils.releaseConnection(c, dataSource);   // no-op inside a transaction
}
```

`DataSourceUtils` hands back the connection Spring already bound to the running
transaction, and opens a fresh one only when there is none. `release` is its
mirror: a no-op inside a transaction, a real close outside one.

That asymmetry is exactly why generated mapper bodies keep the Connection out
of try-with-resources and call `s.release(c)` in a `finally` instead — closing
a transaction's connection would be wrong, and only `release` knows.

## Exception translation

`translate(SQLException, String)` goes through Spring's
`SQLExceptionTranslator`, so a LightBatis mapper fails the same way a
`JdbcTemplate` call does — the `DuplicateKeyException` a service is already
catching. The default is `SQLExceptionSubclassTranslator`, Spring's own default
since 6.0, which reads the standard `SQLException` subclass tree rather than a
per-vendor error-code table.

A translator may return `null` when it recognises nothing. `LightBatisException`
is the floor under that case — a raw `SQLException` never escapes unchecked.

## Using it

Normally you do not construct it: `lightbatis-spring-boot-autoconfigure`
contributes the bean. Wire it by hand when Boot is not in play, or when there
is more than one `DataSource`:

```java
@Bean
LightBatisSession ordersSession(@Qualifier("ordersDataSource") DataSource ds) {
    return new SpringLightBatisSession(ds);
}
```

One session per `DataSource`. The class holds no state beyond its two
collaborators and is safe to share across threads.

## How to run

```bash
./gradlew :lightbatis-spring:test
```

`SpringLightBatisSessionTest` runs against H2 with a real
`DataSourceTransactionManager`: the assertions are that the connection inside a
transaction is the bound one, that `release` does not close it, and that
outside a transaction both statements hold.

The claims end-to-end — mappers as ordinary beans, `@Transactional` rollback
covering a mapper write — are proven in
[`lightbatis-spring-sample`](../lightbatis-spring-sample/README.md).

## JPMS

```java
module io.github.lightbatis.spring {
    requires transitive io.github.lightbatis.runtime;   // implements LightBatisSession
    requires transitive spring.jdbc;                    // SQLExceptionTranslator is a ctor param
    requires spring.tx;                                 // DataAccessException lives in spring-tx
    exports io.github.lightbatis.spring;
}
```

Both `transitive` edges are in the public compile surface. `spring.jdbc` is an
*automatic* module, and javac warns about `requires transitive` to one — rightly,
since the name comes from a jar manifest. The warning is suppressed and the edge
kept, because `SQLExceptionTranslator` really is in the public constructor:
hiding the edge would only move the error into a consumer's build. Re-check the
`spring.*` names with `jar --describe-module` after a Spring upgrade.
