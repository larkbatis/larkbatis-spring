package io.github.lightbatis.spring.boot;

import io.github.lightbatis.runtime.LightBatisSession;
import io.github.lightbatis.spring.SpringLightBatisSession;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The conditions, which the end-to-end sample cannot reach: what happens with
 * no DataSource, with two of them, and when the application declares its own
 * beans. The two-DataSource case is the one that matters — a generator cannot
 * know which mapper belongs to which source, so backing off entirely is the
     * only honest answer (M4).
 */
class LightBatisAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LightBatisAutoConfiguration.class));

    /**
     * A regression guard for a failure with no symptom. Spring Boot 4 moved
     * {@code DataSourceAutoConfiguration} from {@code spring-boot-autoconfigure}
     * into the {@code spring-boot-jdbc} module and renamed its package. An
     * {@code after = DataSourceAutoConfiguration.class} compiled against Boot 3
     * cannot be resolved on Boot 4, and Spring's response is to drop this whole
     * class from the candidate list — no bean, no warning, nothing until an
     * application asks for a {@code LightBatisSession} and gets
     * NoSuchBeanDefinitionException at startup.
     *
     * <p>Found by migrating a real Boot 4.1 service. Naming both packages is
     * what makes one jar work on both; a name that matches nothing is ignored.
     */
    @Test
    void ordersItselfAfterDataSourceOnBothBootGenerations() {
        AutoConfiguration annotation =
                LightBatisAutoConfiguration.class.getAnnotation(AutoConfiguration.class);
        assertThat(annotation.after())
                .as("a Class reference here is unresolvable on the other Boot generation")
                .isEmpty();
        assertThat(annotation.afterName()).containsExactlyInAnyOrder(
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration");
    }

    @Test
    void contributesASessionForASingleDataSource() {
        runner.withUserConfiguration(OneDataSource.class).run(context -> {
            assertThat(context).hasSingleBean(LightBatisSession.class);
            assertThat(context.getBean(LightBatisSession.class))
                    .isInstanceOf(SpringLightBatisSession.class);
            assertThat(context).hasSingleBean(SQLExceptionTranslator.class);
        });
    }

    @Test
    void backsOffWithoutADataSource() {
        runner.run(context -> assertThat(context).doesNotHaveBean(LightBatisSession.class));
    }

    @Test
    void backsOffWithTwoDataSourcesAndNoPrimary() {
        runner.withUserConfiguration(TwoDataSources.class).run(context -> {
            // the application declares one SpringLightBatisSession per DataSource
            // itself; guessing here would wire mappers to the wrong database
            assertThat(context).doesNotHaveBean(LightBatisSession.class);
            assertThat(context).hasNotFailed();
        });
    }

    @Test
    void backsOffWhenTheApplicationDeclaresItsOwn() {
        runner.withUserConfiguration(OneDataSource.class, OwnSession.class).run(context -> {
            assertThat(context).hasSingleBean(LightBatisSession.class);
            assertThat(context.getBean(LightBatisSession.class))
                    .isSameAs(context.getBean("mySession"));
        });
    }

    @Test
    void appliesTheVariantProperties() {
        runner.withUserConfiguration(OneDataSource.class)
                .withPropertyValues("lightbatis.max-sql-variants=7",
                        "lightbatis.fail-on-unbounded-fragment=true")
                .run(context -> {
                    LightBatisProperties properties = context.getBean(LightBatisProperties.class);
                    assertThat(properties.getMaxSqlVariants()).isEqualTo(7);
                    assertThat(properties.isFailOnUnboundedFragment()).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class OneDataSource {
        @Bean
        DataSource dataSource() {
            return h2("one");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TwoDataSources {
        @Bean
        DataSource main() {
            return h2("main");
        }

        @Bean
        DataSource audit() {
            return h2("audit");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnSession {
        @Bean
        LightBatisSession mySession(DataSource dataSource) {
            return new SpringLightBatisSession(dataSource);
        }
    }

    private static DataSource h2(String name) {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:lbautoconf-" + name + "-" + System.nanoTime());
        ds.setUser("sa");
        return ds;
    }
}
