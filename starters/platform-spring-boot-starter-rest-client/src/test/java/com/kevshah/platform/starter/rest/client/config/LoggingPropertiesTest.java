package com.kevshah.platform.starter.rest.client.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LoggingPropertiesTest {

    // -------------------------------------------------------------------------
    // merge
    // -------------------------------------------------------------------------

    @Nested
    class Merge {

        @Test
        void merge_bothNull_returnsNull() {
            // When/Then
            assertThat(LoggingProperties.merge(null, null)).isNull();
        }

        @Test
        void merge_baseNullOverridePresent_returnsOverride() {
            // Given
            var override = new LoggingProperties(true, "DEBUG", null, null);

            // When
            var result = LoggingProperties.merge(null, override);

            // Then
            assertThat(result).isSameAs(override);
        }

        @Test
        void merge_overrideNullBasePresent_returnsBase() {
            // Given
            var base = new LoggingProperties(false, "INFO", null, null);

            // When
            var result = LoggingProperties.merge(base, null);

            // Then
            assertThat(result).isSameAs(base);
        }

        @Test
        void merge_overrideFieldsNonNull_overrideWins() {
            // Given
            var base = new LoggingProperties(
                    false,
                    "INFO",
                    new LoggingProperties.RequestConfig(
                            new LoggingProperties.PayloadConfig(false),
                            new LoggingProperties.HeadersConfig(false, null, null)),
                    new LoggingProperties.ResponseConfig(
                            new LoggingProperties.PayloadConfig(false),
                            new LoggingProperties.HeadersConfig(false, null, null)));
            var override = new LoggingProperties(
                    true,
                    "DEBUG",
                    new LoggingProperties.RequestConfig(
                            new LoggingProperties.PayloadConfig(true),
                            new LoggingProperties.HeadersConfig(true, List.of("X-Request-Id"), null)),
                    new LoggingProperties.ResponseConfig(
                            new LoggingProperties.PayloadConfig(true),
                            new LoggingProperties.HeadersConfig(true, null, List.of("Set-Cookie"))));

            // When
            var result = LoggingProperties.merge(base, override);

            // Then
            assertThat(result.enabled()).isTrue();
            assertThat(result.level()).isEqualTo("DEBUG");
            assertThat(result.request()).isNotNull();
            assertThat(result.response()).isNotNull();
            assertThat(result.request().payload()).isNotNull();
            assertThat(result.request().headers()).isNotNull();
            assertThat(result.request().payload().enabled()).isTrue();
            assertThat(result.request().headers().enabled()).isTrue();
            assertThat(result.request().headers().include()).containsExactly("X-Request-Id");
            assertThat(result.request().headers().exclude()).isNull();
            assertThat(result.response().payload()).isNotNull();
            assertThat(result.response().headers()).isNotNull();
            assertThat(result.response().payload().enabled()).isTrue();
            assertThat(result.response().headers().enabled()).isTrue();
            assertThat(result.response().headers().include()).isNull();
            assertThat(result.response().headers().exclude()).containsExactly("Set-Cookie");
        }

        @Test
        void merge_overrideFieldsNull_baseFallbackUsed() {
            // Given
            var base = new LoggingProperties(
                    true,
                    "WARN",
                    new LoggingProperties.RequestConfig(
                            new LoggingProperties.PayloadConfig(true),
                            new LoggingProperties.HeadersConfig(true, null, null)),
                    new LoggingProperties.ResponseConfig(
                            new LoggingProperties.PayloadConfig(true),
                            new LoggingProperties.HeadersConfig(true, null, null)));
            var override = new LoggingProperties(null, null, null, null);

            // When
            var result = LoggingProperties.merge(base, override);

            // Then
            assertThat(result.enabled()).isTrue();
            assertThat(result.level()).isEqualTo("WARN");
            assertThat(result.request()).isNotNull();
            assertThat(result.response()).isNotNull();
            assertThat(result.request().payload()).isNotNull();
            assertThat(result.request().headers()).isNotNull();
            assertThat(result.request().payload().enabled()).isTrue();
            assertThat(result.request().headers().enabled()).isTrue();
            assertThat(result.request().headers().include()).isNull();
            assertThat(result.request().headers().exclude()).isNull();
            assertThat(result.response().payload()).isNotNull();
            assertThat(result.response().headers()).isNotNull();
            assertThat(result.response().payload().enabled()).isTrue();
            assertThat(result.response().headers().enabled()).isTrue();
            assertThat(result.response().headers().include()).isNull();
            assertThat(result.response().headers().exclude()).isNull();
        }

        //        @Test
        //        void merge_partialOverride_mixesBaseAndOverride() {
        //            // Given
        //            var base = new LoggingProperties(false, true, false, true, "INFO");
        //            var override = new LoggingProperties(true, null, true, null, null);
        //
        //            // When
        //            var result = LoggingProperties.merge(base, override);
        //
        //            // Then
        //            assertThat(result.enabled()).isTrue();         // override wins
        //            assertThat(result.requestBody()).isTrue();  // base fallback
        //            assertThat(result.responseBody()).isTrue(); // override wins
        //            assertThat(result.headers()).isTrue();      // base fallback
        //            assertThat(result.level()).isEqualTo("INFO");  // base fallback
        //        }
    }
}
