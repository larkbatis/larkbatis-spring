package io.github.lightbatis.spring;

import io.github.lightbatis.runtime.LightBatisException;
import io.github.lightbatis.runtime.LightBatisSession;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * The whole of the Spring integration (design §10). Half of
 * {@code mybatis-spring} evaporates because a mapper is a real class with a
 * constructor — no scanner, no {@code MapperFactoryBean}, no
 * {@code SqlSessionTemplate}. What is left is the half that never evaporates:
 * sharing a {@link Connection} with {@code @Transactional}.
 *
 * <p>The one rule: {@link #conn()} must never call
 * {@code dataSource.getConnection()}. It asks {@link DataSourceUtils}, which
 * hands back the connection already bound to the running transaction, and
 * opens a fresh one only when there is none. {@link #release} is its mirror —
 * a no-op inside a transaction, a real close outside one. That is why
 * generated bodies keep the Connection out of try-with-resources.
 *
 * <p>This class holds no state beyond its two collaborators and is safe to
 * share across threads; one bean per {@link DataSource}.
 */
public final class SpringLightBatisSession implements LightBatisSession {

    /** The {@code task} label Spring puts in front of every translated message. */
    private static final String TASK = "LightBatis";

    private final DataSource dataSource;
    private final SQLExceptionTranslator translator;

    /**
     * With Spring's default translator since 6.0,
     * {@link SQLExceptionSubclassTranslator} — it reads the standard
     * {@code SQLException} subclass tree instead of a per-vendor error-code
     * table.
     */
    public SpringLightBatisSession(DataSource dataSource) {
        this(dataSource, new SQLExceptionSubclassTranslator());
    }

    public SpringLightBatisSession(DataSource dataSource, SQLExceptionTranslator translator) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    /** The DataSource this session borrows from; one session per DataSource. */
    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public Connection conn() {
        return DataSourceUtils.getConnection(dataSource); // joins the running transaction
    }

    @Override
    public void release(Connection c) {
        DataSourceUtils.releaseConnection(c, dataSource); // no-op if c belongs to a transaction
    }

    /**
     * Into Spring's {@code DataAccessException} hierarchy, so a LightBatis
     * mapper fails the same way a {@code JdbcTemplate} call does — the same
     * {@code DuplicateKeyException} a service is already catching.
     *
     * <p>A translator may return null when it recognises nothing; the
     * LightBatis exception is the floor under that case, never a raw
     * {@code SQLException} escaping unchecked.
     */
    @Override
    public RuntimeException translate(SQLException e, String sql) {
        DataAccessException translated = translator.translate(TASK, sql, e);
        if (translated != null) {
            return translated;
        }
        return new LightBatisException(TASK + " failed: " + e.getMessage()
                + " — while executing: " + sql, sql, e);
    }
}
