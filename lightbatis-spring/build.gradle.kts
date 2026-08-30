// SpringLightBatisSession: conn() via DataSourceUtils (participates in @Transactional),
// translate() via SQLExceptionTranslator (design §10).
//
// api, not implementation, for both: LightBatisSession is the interface this
// module implements and SQLExceptionTranslator is a public constructor
// parameter, so both are part of this module's compile surface.
dependencies {
    api("io.github.lightbatis:lightbatis-runtime:0.1.0-SNAPSHOT")
    api("org.springframework:spring-jdbc:6.1.14")

    testImplementation("org.springframework:spring-tx:6.1.14")
    testImplementation("com.h2database:h2:2.3.232")
}
