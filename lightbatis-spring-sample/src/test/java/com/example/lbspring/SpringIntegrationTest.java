package com.example.lbspring;

import com.zaxxer.hikari.HikariDataSource;
import io.github.lightbatis.runtime.LightBatisSession;
import io.github.lightbatis.spring.SpringLightBatisSession;
import io.github.lightbatis.spring.boot.LightBatisProperties;
import java.math.BigDecimal;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Design §10, checked against a running context. The interesting assertions
 * are not "the query works" but "the mapper is on the same connection Spring
 * is" — everything mybatis-spring's {@code SqlSessionTemplate} and
 * {@code SpringManagedTransaction} exist to arrange.
 */
@SpringBootTest
class SpringIntegrationTest {

    @Autowired
    AccountMapper accounts;

    @Autowired
    AccountService service;

    @Autowired
    LightBatisSession session;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    LightBatisProperties properties;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void clean() {
        accounts.deleteAll();
    }

    // --- the beans -----------------------------------------------------------

    @Test
    void mapperIsAnOrdinaryBeanOfTheGeneratedClass() {
        // no JDK proxy, no CGLIB, no FactoryBean: the class the generator wrote
        assertEquals("com.example.lbspring.AccountMapper$$Impl", accounts.getClass().getName());
        assertFalse(java.lang.reflect.Proxy.isProxyClass(accounts.getClass()));
    }

    @Test
    void generatedConfigurationIsNotCglibProxied() {
        Object configuration = context.getBean("lightBatisMapperConfiguration");
        // proxyBeanMethods = false: Spring does not subclass the class at runtime
        assertEquals(LightBatisMapperConfiguration.class, configuration.getClass());
        assertFalse(configuration.getClass().getName().contains("$$SpringCGLIB"));
    }

    @Test
    void propertiesAreBoundFromApplicationYml() {
        assertEquals(32, properties.getMaxSqlVariants());
        assertTrue(properties.isFailOnUnboundedFragment());
    }

    @Test
    void autoConfigurationSuppliedTheSpringSession() {
        assertInstanceOf(SpringLightBatisSession.class, session);
        assertSame(dataSource, ((SpringLightBatisSession) session).dataSource());
    }

    // --- @Transactional: the half of mybatis-spring that does not evaporate ---

    @Test
    void connIsTheTransactionsConnection() {
        tx.executeWithoutResult(status -> {
            Connection bound = DataSourceUtils.getConnection(dataSource);
            try {
                Connection fromSession = session.conn();
                assertSame(bound, fromSession,
                        "conn() must join the running transaction, not open its own");
                session.release(fromSession); // a no-op inside a transaction
                assertFalse(isClosed(fromSession), "release() closed a transactional connection");
            } finally {
                DataSourceUtils.releaseConnection(bound, dataSource);
            }
        });
    }

    @Test
    void rollbackDiscardsTheMapperWrite() {
        assertThrows(IllegalStateException.class,
                () -> service.openThenFail("rolled-back", new BigDecimal("10.00")));
        assertEquals(0, accounts.count(), "the insert survived a rollback — separate connection");
    }

    @Test
    void readsItsOwnUncommittedWrite() {
        assertEquals(1, service.openAndCount("uncommitted", new BigDecimal("1.00")));
    }

    @Test
    void sharesTheTransactionWithJdbcTemplate() {
        tx.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO account (owner, balance) VALUES (?, ?)", "via-jdbc", 5);
            // the mapper sees a row only JdbcTemplate has written, and only this
            // connection can see: same DataSourceTransactionManager, same connection
            assertEquals(1, accounts.count());
            status.setRollbackOnly();
        });
        assertEquals(0, accounts.count());
    }

    @Test
    void generatedKeysComeBackThroughTheTransaction() {
        long id = service.open("with-key", new BigDecimal("42.50"));
        assertTrue(id > 0, "useGeneratedKeys did not fill the id");
        Account found = service.get(id);
        assertNotNull(found);
        assertEquals("with-key", found.getOwner());
        assertEquals(0, new BigDecimal("42.50").compareTo(found.getBalance()));
    }

    // --- outside a transaction ------------------------------------------------

    @Test
    void worksAndReleasesOutsideAnyTransaction() {
        Account account = new Account();
        account.setOwner("no-transaction");
        account.setBalance(new BigDecimal("7.00"));
        accounts.insert(account);
        assertNotNull(accounts.findById(account.getId()));

        // the leak this shape exists to prevent: a generated body that put the
        // Connection in try-with-resources would still pass every assertion
        // above and quietly drain the pool here
        for (int i = 0; i < 50; i++) {
            accounts.findAll();
        }
        assertEquals(0, activeConnections(), "connections were not returned to the pool");
    }

    // --- exception translation -------------------------------------------------

    @Test
    void translatesIntoSpringsHierarchy() {
        service.open("unique-owner", BigDecimal.ONE);
        // owner is UNIQUE: a service already catching DuplicateKeyException from
        // JdbcTemplate keeps catching it after the migration
        assertThrows(DuplicateKeyException.class,
                () -> service.open("unique-owner", BigDecimal.TEN));
        assertNull(accounts.findById(-1));
    }

    private boolean isClosed(Connection c) {
        try {
            return c.isClosed();
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private int activeConnections() {
        return ((HikariDataSource) dataSource).getHikariPoolMXBean().getActiveConnections();
    }
}
