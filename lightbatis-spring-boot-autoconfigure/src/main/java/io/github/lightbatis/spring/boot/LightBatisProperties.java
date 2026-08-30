package io.github.lightbatis.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The {@code lightbatis.*} properties (design §10). Deliberately short: there
 * is no runtime {@code Configuration} object to mirror, because everything
 * MyBatis configures at startup — type handlers, result maps, statement
 * shapes — was decided at build time.
 *
 * <p>Both properties are about the same thing, the operational cost of
 * {@code ${}}: how many distinct SQL texts one statement may produce before
 * LightBatis says something, and whether saying something means a warning or
 * a failure.
 */
@ConfigurationProperties("lightbatis")
public class LightBatisProperties {

    /**
     * Distinct SQL texts one statement may produce before LightBatis reports
     * it. Statement caches — the driver's and the database's — are keyed by
     * SQL text, so an unbounded fragment grows them without limit.
     */
    private int maxSqlVariants = 64;

    /**
     * Whether crossing {@code max-sql-variants} throws instead of logging a
     * warning once. Off by default; a test or staging profile is where
     * turning it on finds the unbounded fragment before it ships.
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
