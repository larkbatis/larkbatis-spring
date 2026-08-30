/**
 * Boot auto-configuration. Two beans, two settings, and the
 * {@code AutoConfiguration.imports} resource that registers the class —
 * {@code spring.factories} has not been read for auto-configuration since
 * Boot 3 (design §10).
 *
 * <p>No {@code opens}: {@code @ConfigurationProperties} binding reflects over
 * {@code LightBatisProperties}, but the class and its setters are public in
 * an exported package, which is all the binder needs. If a future property
 * type ever forces an {@code opens}, that is Spring's container reflecting —
 * never LightBatis, which reflects nowhere (red line #2).
 *
 * <p>The {@code spring.*} names are automatic modules read from each jar's
 * {@code Automatic-Module-Name}; re-check them with
 * {@code jar --describe-module} after a Spring or Boot upgrade.
 */
module io.github.lightbatis.spring.boot {
    requires transitive io.github.lightbatis.spring;
    // @AutoConfiguration, @ConditionalOn*, DataSourceAutoConfiguration
    requires spring.boot.autoconfigure;
    // @ConfigurationProperties, @EnableConfigurationProperties
    requires spring.boot;
    // @Bean
    requires spring.context;
    // InitializingBean
    requires spring.beans;

    exports io.github.lightbatis.spring.boot;
}
