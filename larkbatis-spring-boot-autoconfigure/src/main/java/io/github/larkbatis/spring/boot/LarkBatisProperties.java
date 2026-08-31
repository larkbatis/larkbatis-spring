package io.github.larkbatis.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The {@code larkbatis.*} properties. Deliberately short: there
 * is no runtime {@code Configuration} object to mirror, because everything
 * MyBatis configures at startup — type handlers, result maps, statement
 * shapes — was decided at build time.
 *
 * <p>Both properties are about the same thing, the operational cost of
 * {@code ${}}: how many distinct SQL texts one statement may produce before
 * LarkBatis says something, and whether saying something means a warning or
 * a failure.
 *
 * <p>The javadoc on each field has a second reader: Spring Boot's
 * configuration processor copies it verbatim into
 * {@code META-INF/spring-configuration-metadata.json}, which is the text an
 * IDE shows while completing {@code larkbatis.} in {@code application.yml}.
 * Inline javadoc tags survive that copy as their own source text, so these
 * two comments are written without them.
 */
@ConfigurationProperties("larkbatis")
public class LarkBatisProperties {

    /**
     * Distinct SQL texts one statement may produce before LarkBatis reports
     * it. Statement caches — the driver's and the database's — are keyed by
     * SQL text, so an unbounded fragment grows them without limit.
     */
    private int maxSqlVariants = 64;

    /**
     * Whether crossing max-sql-variants throws instead of logging a warning
     * once. Off by default; a test or staging profile is where turning it on
     * finds the unbounded fragment before it ships.
     */
    private boolean failOnUnboundedFragment;

    public int getMaxSqlVariants() {
        return maxSqlVariants;
    }

    public void setMaxSqlVariants(int maxSqlVariants) {
        this.maxSqlVariants = maxSqlVariants;
    }

    public boolean isFailOnUnboundedFragment() {
        return failOnUnboundedFragment;
    }

    public void setFailOnUnboundedFragment(boolean failOnUnboundedFragment) {
        this.failOnUnboundedFragment = failOnUnboundedFragment;
    }
}
