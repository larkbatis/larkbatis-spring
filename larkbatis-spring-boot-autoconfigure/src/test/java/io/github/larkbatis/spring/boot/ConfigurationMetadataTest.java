package io.github.larkbatis.spring.boot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The jar ships {@code META-INF/spring-configuration-metadata.json}, so an IDE
 * completes {@code larkbatis.} in {@code application.yml} and flags a misspelt
 * key instead of ignoring it.
 *
 * <p>A regression guard for a build file, not for code: the file is produced by
 * {@code spring-boot-configuration-processor} on the {@code annotationProcessor}
 * configuration. Drop that one line and everything still compiles, every test
 * still passes, and the only symptom is an editor that has never heard of these
 * properties — which nobody notices from inside the build.
 */
class ConfigurationMetadataTest {

    private static String metadata() throws IOException {
        try (InputStream in = ConfigurationMetadataTest.class
                .getResourceAsStream("/META-INF/spring-configuration-metadata.json")) {
            assertThat(in)
                    .describedAs("spring-configuration-metadata.json is missing — is "
                            + "spring-boot-configuration-processor still on the "
                            + "annotationProcessor configuration?")
                    .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void describesEveryProperty() throws IOException {
        String json = metadata();
        assertThat(json).contains("\"larkbatis.max-sql-variants\"");
        assertThat(json).contains("\"larkbatis.fail-on-unbounded-fragment\"");
    }

    /**
     * The defaults are read out of the field initializers, and they are what the
     * IDE shows next to the key. A default that drifts from the documented one
     * is worse than none.
     */
    @Test
    void carriesTheDefaults() throws IOException {
        assertThat(metadata()).contains("\"defaultValue\": 64");
    }

    /**
     * The descriptions are the field javadoc, copied verbatim — inline javadoc
     * tags included. A {@code @code} tag written there reaches the user as its
     * own source text in a completion popup.
     */
    @Test
    void carriesDescriptionsWithoutJavadocTags() throws IOException {
        assertThat(metadata()).contains("Distinct SQL texts one statement may produce");
        assertThat(metadata()).doesNotContain("{@");
    }
}
