package io.github.larkbatis.spring.boot;

import io.github.larkbatis.runtime.LarkBatisSession;
import io.github.larkbatis.runtime.LarkBatisSql;
import io.github.larkbatis.spring.SpringLarkBatisSession;
import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * Two beans and two settings — the whole auto-configuration.
 * {@code MybatisAutoConfiguration} needs a {@code SqlSessionFactoryBean}, a
 * {@code SqlSessionTemplate}, a {@code ConfigurationCustomizer} SPI and a
 * mapper scanner registrar; none of those has anything to configure here,
 * because the mappers are ordinary classes and there is no runtime
 * {@code Configuration} to customize.
 *
 * <p>{@code @ConditionalOnSingleCandidate} is what makes the
 * multiple-DataSource case safe: with two DataSources and no {@code @Primary},
 * no session bean is contributed at all and the application declares its own
 * — one {@link SpringLarkBatisSession} per DataSource. Guessing which mapper
 * belongs to which source is exactly what a generator cannot do.
 *
 * <p>{@code @AutoConfiguration} implies {@code proxyBeanMethods = false}, so
 * no CGLIB subclass of this class is built at runtime — the same reason the
 * generated mapper {@code @Configuration} spells it out.
 *
 * <p>The ordering is declared by {@code afterName}, with both packages listed,
 * and that is not a style choice. Spring Boot 4 moved
 * {@code DataSourceAutoConfiguration} out of {@code spring-boot-autoconfigure}
 * into the {@code spring-boot-jdbc} module and renamed its package; a
 * {@code after = DataSourceAutoConfiguration.class} compiled against Boot 3
 * cannot be resolved on Boot 4, and Spring's response is to drop this whole
 * class from the candidate list — no bean, no warning, no failure until
 * something asks for a {@code LarkBatisSession}. A name that matches nothing
 * is simply ignored, so listing both is what makes one jar work on both.
 */
@AutoConfiguration(afterName = {
        // Boot 4: spring-boot-jdbc
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        // Boot 3: spring-boot-autoconfigure
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"})
@ConditionalOnClass({DataSource.class, LarkBatisSession.class})
@ConditionalOnSingleCandidate(DataSource.class)
@EnableConfigurationProperties(LarkBatisProperties.class)
public class LarkBatisAutoConfiguration implements InitializingBean {

    private final LarkBatisProperties properties;

    public LarkBatisAutoConfiguration(LarkBatisProperties properties) {
        this.properties = properties;
    }

    /**
     * The variant thresholds are process-wide because the counters they guard
     * are (generated code calls {@code LarkBatisSql.trackVariants} statically
     * — no session in reach). Applying them here, once the context refreshes,
     * keeps that global out of the session bean's constructor.
     */
    @Override
    public void afterPropertiesSet() {
        LarkBatisSql.maxSqlVariants(properties.getMaxSqlVariants());
        LarkBatisSql.failOnUnboundedVariants(properties.isFailOnUnboundedFragment());
    }

    /** Spring's own default since 6.0: the standard SQLException subclass tree. */
    @Bean
    @ConditionalOnMissingBean
    SQLExceptionTranslator larkBatisSqlExceptionTranslator() {
        return new SQLExceptionSubclassTranslator();
    }

    @Bean
    @ConditionalOnMissingBean
    LarkBatisSession larkBatisSession(DataSource dataSource, SQLExceptionTranslator translator) {
        return new SpringLarkBatisSession(dataSource, translator);
    }
}
