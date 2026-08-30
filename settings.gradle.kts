rootProject.name = "lightbatis-spring-root"

include("lightbatis-spring")
include("lightbatis-spring-boot-autoconfigure")
include("lightbatis-spring-boot-starter")
include("lightbatis-spring-sample")

// Local development: substitutes io.github.lightbatis:* dependencies with the core repo.
includeBuild("../lightbatis")
