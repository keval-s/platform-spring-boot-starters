package com.kevshah.platform.starter.rest.server.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingPropertiesTest {

    // -------------------------------------------------------------------------
    // HeaderConfig Tests
    // -------------------------------------------------------------------------

    @Nested
    class HeaderConfigTest {

        @ParameterizedTest
        @ValueSource(strings = {"Content-Type", "X-Request-Id"})
        void givenNullIncludeAndExclude_thenShouldLogAllHeaders(String header) {
            // Given
            var config = new LoggingProperties.HeadersConfig(
                    true,
                    null,
                    null
            );

            // When
            var result = config.shouldLogHeader(header);

            // Then
            assertThat(result).isTrue();
        }


        @Test
        void givenExcludeList_thenShouldNotLogExcludedHeaders() {
            // Given
            var config = new LoggingProperties.HeadersConfig(
                    true,
                    null,
                    List.of("Authorization", "Cookie")
            );

            // When
            var shouldLogAuth = config.shouldLogHeader("Authorization");
            var shouldLogCookie = config.shouldLogHeader("Cookie");
            var shouldLogContentType = config.shouldLogHeader("Content-Type");

            // Then
            assertThat(shouldLogAuth).isFalse();
            assertThat(shouldLogCookie).isFalse();
            assertThat(shouldLogContentType).isTrue();
        }


        @Test
        void givenIncludeList_thenShouldLogHeaders() {
            // Given
            var config = new LoggingProperties.HeadersConfig(
                    true,
                    List.of("Content-Type", "X-Request-Id"),
                    null
            );

            // When
            var shouldLogContentType = config.shouldLogHeader("Content-Type");
            var shouldLogRequestId = config.shouldLogHeader("X-Request-Id");
            var shouldLogAuthorization = config.shouldLogHeader("Authorization");

            // Then
            assertThat(shouldLogContentType).isTrue();
            assertThat(shouldLogRequestId).isTrue();
            assertThat(shouldLogAuthorization).isFalse();
        }


        @Test
        void givenIncludeAndExclude_thenShouldLogOnlyIncludedAndNotExcludedHeaders() {
            // Given
            var config = new LoggingProperties.HeadersConfig(
                    true,
                    List.of("Content-Type", "X-Request-Id"),
                    List.of("Authorization")
            );

            // When
            var shouldLogContentType = config.shouldLogHeader("Content-Type");
            var shouldLogRequestId = config.shouldLogHeader("X-Request-Id");
            var shouldLogAuthorization = config.shouldLogHeader("Authorization");
            var shouldLogCookie = config.shouldLogHeader("Cookie");

            // Then
            assertThat(shouldLogContentType).isTrue();
            assertThat(shouldLogRequestId).isTrue();
            assertThat(shouldLogAuthorization).isFalse();  // Excluded even though it's in the include list
            assertThat(shouldLogCookie).isFalse();         // Not included and not excluded, so defaults to false
        }


        @Test
        void givenDisabled_thenShouldNotLogAnyHeaders() {
            // Given
            var config = new LoggingProperties.HeadersConfig(
                    false,
                    List.of("Content-Type", "X-Request-Id"),
                    List.of("Authorization")
            );

            // When
            var shouldLogContentType = config.shouldLogHeader("Content-Type");
            var shouldLogRequestId = config.shouldLogHeader("X-Request-Id");
            var shouldLogAuthorization = config.shouldLogHeader("Authorization");

            // Then
            assertThat(shouldLogContentType).isFalse();
            assertThat(shouldLogRequestId).isFalse();
            assertThat(shouldLogAuthorization).isFalse();
        }


    }


}
