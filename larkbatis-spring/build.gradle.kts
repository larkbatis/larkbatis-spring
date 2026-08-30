// SpringLarkBatisSession: conn() via DataSourceUtils (participates in @Transactional),
// translate via SQLExceptionTranslator.
//
// api, not implementation, for both: LarkBatisSession is the interface this
// module implements and SQLExceptionTranslator is a public constructor
// parameter, so both are part of this module's compile surface.

description = "SpringLarkBatisSession — LarkBatis connections and exception translation, wired through Spring's DataSourceUtils"

val larkbatisVersion = providers.gradleProperty("larkbatisVersion").get()

dependencies {
    api("io.github.larkbatis:larkbatis-runtime:$larkbatisVersion")
    api("org.springframework:spring-jdbc:6.1.14")

    testImplementation("org.springframework:spring-tx:6.1.14")
    testImplementation("com.h2database:h2:2.3.232")
}
