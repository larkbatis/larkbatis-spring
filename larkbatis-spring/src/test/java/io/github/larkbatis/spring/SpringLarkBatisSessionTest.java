package io.github.larkbatis.spring;

import io.github.larkbatis.runtime.LarkBatisException;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The session on its own, without a Spring Boot context: the two behaviours
 * a generated body depends on (a connection that joins the transaction, a
 * release that does not close it) and the one nothing else pins down — what
 * happens when the translator recognises nothing.
 */
class SpringLarkBatisSessionTest {

    private final JdbcDataSource dataSource = h2();

    @AfterEach
    void shutdown() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.createStatement().execute("SHUTDOWN");
        }
    }

    @Test
    void connOpensAndReleaseClosesOutsideATransaction() throws SQLException {
        SpringLarkBatisSession session = new SpringLarkBatisSession(dataSource);
        Connection first = session.conn();
        assertFalse(first.isClosed());
        session.release(first);
        assertTrue(first.isClosed(), "outside a transaction release() must really close");
    }

    @Test
    void connJoinsTheRunningTransactionAndReleaseLeavesItOpen() {
        SpringLarkBatisSession session = new SpringLarkBatisSession(dataSource);
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        tx.executeWithoutResult((TransactionStatus status) -> {
            Connection a = session.conn();
            Connection b = session.conn();
            assertSame(a, b, "two conn() calls in one transaction must be the same connection");
            session.release(a);
            try {
                assertFalse(a.isClosed(), "release() closed a transactional connection");
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            session.release(b);
        });
    }

    @Test
    void translatesThroughSpringsHierarchy() {
        SpringLarkBatisSession session = new SpringLarkBatisSession(dataSource);
        SQLException duplicate = new SQLException("dup", "23505", 23505);
        assertInstanceOf(DuplicateKeyException.class,
                session.translate(duplicate, "INSERT INTO t VALUES (1)"));
    }

    @Test
    void fallsBackToTheLarkBatisExceptionWhenTheTranslatorDeclines() {
        // Spring's contract allows a translator to return null for anything it
        // does not recognise; a raw SQLException must never escape past that.
        SQLExceptionTranslator declines = (task, sql, e) -> null;
        SpringLarkBatisSession session = new SpringLarkBatisSession(dataSource, declines);
        SQLException failure = new SQLException("something the translator ignores");
        RuntimeException thrown = session.translate(failure, "SELECT 1");

        LarkBatisException larkBatis = assertInstanceOf(LarkBatisException.class, thrown);
        assertEquals("SELECT 1", larkBatis.sql());
        assertSame(failure, larkBatis.getCause());
        assertFalse(thrown instanceof DataAccessException);
    }

    private static JdbcDataSource h2() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:lbspring-unit-" + System.nanoTime());
        ds.setUser("sa");
        return ds;
    }
}
