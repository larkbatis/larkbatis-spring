/**
 * The Spring half of the integration: one {@code LarkBatisSession} that
 * borrows its connections from {@code DataSourceUtils}.
 *
 * <p>Both {@code requires} are {@code transitive} because both appear in this
 * module's compile surface: the class implements
 * {@code io.github.larkbatis.runtime.LarkBatisSession}, and its public
 * constructor takes a {@code SQLExceptionTranslator}. A consumer naming
 * either type would otherwise have to re-declare the edge itself.
 *
 * <p>{@code spring.jdbc}, {@code spring.tx} and {@code spring.core} are
 * automatic modules — the names come from {@code Automatic-Module-Name} in
 * the Spring jars, so re-check them with {@code jar --describe-module} after
 * a Spring upgrade rather than trusting this file.
 */
// javac warns on `requires transitive` to an automatic module, and it is
// right to: the transitivity of an edge whose name comes from a jar manifest
// is a promise this module cannot enforce. Kept anyway and suppressed here,
// because SQLExceptionTranslator really is in the public constructor — a
// consumer naming it must read spring.jdbc either way, and hiding the edge
// would only move the error into their build.
@SuppressWarnings("requires-transitive-automatic")
module io.github.larkbatis.spring {
    requires transitive io.github.larkbatis.runtime;
    // DataSourceUtils, SQLExceptionTranslator, SQLExceptionSubclassTranslator
    requires transitive spring.jdbc;
    // org.springframework.dao.DataAccessException ships in the spring-tx jar
    requires spring.tx;

    exports io.github.larkbatis.spring;
}
