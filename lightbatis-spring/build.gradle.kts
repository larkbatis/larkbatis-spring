// SpringLightBatisSession: conn() via DataSourceUtils (participates in @Transactional),
// translate via SQLExceptionTranslator.
//
// api, not implementation, for both: LightBatisSession is the interface this
// module implements and SQLExceptionTranslator is a public constructor
// parameter, so both are part of this module's compile surface.

description = "SpringLightBatisSession — LightBatis connections and exception translation, wired through Spring's DataSourceUtils"

val lightbatisVersion = providers.gradleProperty("lightbatisVersion").get()

dependencies {
    api("io.github.lightbatis:lightbatis-runtime:$lightbatisVersion")
    api("org.springframework:spring-jdbc:6.1.14")

    testImplementation("org.springframework:spring-tx:6.1.14")
    testImplementation("com.h2database:h2:2.3.232")
}
