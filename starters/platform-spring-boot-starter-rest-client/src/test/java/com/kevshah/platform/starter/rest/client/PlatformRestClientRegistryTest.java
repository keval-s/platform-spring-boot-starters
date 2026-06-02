package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.ClientProperties;
import com.kevshah.platform.starter.rest.client.config.EndpointProperties;
import com.kevshah.platform.starter.rest.client.config.LoggingProperties;
import com.kevshah.platform.starter.rest.client.config.RestClientProperties;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformRestClientRegistryTest {

    MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private String baseUrl() {
        return mockWebServer.url("/").toString();
    }

    private PlatformRestClientRegistry registryWithClient(String name, ClientProperties props) {
        return new PlatformRestClientRegistry(
                new RestClientProperties(Map.of(name, props)),
                null);
    }

    // -------------------------------------------------------------------------
    // getPlatformRestClient
    // -------------------------------------------------------------------------

    @Nested
    class GetPlatformRestClient {

        @Test
        void getPlatformRestClient_knownClientName_returnsPlatformRestClient() {
            // Given
            var registry = registryWithClient("svc",
                    new ClientProperties(baseUrl(), null, null, null, null, null, null, null, null, null));

            // When
            var client = registry.getPlatformRestClient("svc");

            // Then
            assertThat(client).isNotNull();
            assertThat(client.clientName()).isEqualTo("svc");
        }

        @Test
        void getPlatformRestClient_unknownClientName_throwsNoSuchElementException() {
            // Given
            var registry = new PlatformRestClientRegistry(
                    new RestClientProperties(Map.of()), null);

            // When/Then
            assertThatThrownBy(() -> registry.getPlatformRestClient("missing"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("missing");
        }
    }

    // -------------------------------------------------------------------------
    // getEffectiveLogging
    // -------------------------------------------------------------------------

    @Nested
    class GetEffectiveLogging {

        @Test
        void getEffectiveLogging_noLoggingOnClientOrEndpoint_returnsNull() {
            // Given
            var endpoint = new EndpointProperties("GET", "/api/items", null, null, null, null);
            var registry = registryWithClient("svc",
                    new ClientProperties(baseUrl(), null, null, null, null, null,
                            Map.of("list", endpoint), null, null, null));

            // When
            var result = registry.getEffectiveLogging("svc", "list");

            // Then
            assertThat(result).isNull();
        }

        @Test
        void getEffectiveLogging_clientLoggingOnly_returnsClientLogging() {
            // Given
            var logging = new LoggingProperties(true, "DEBUG", null, null);
            var endpoint = new EndpointProperties("GET", "/api/items", null, null, null, null);
            var registry = registryWithClient("svc",
                    new ClientProperties(baseUrl(), null, null, null, null, null,
                            Map.of("list", endpoint), null, null, logging));

            // When
            var result = registry.getEffectiveLogging("svc", "list");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.enabled()).isTrue();
            assertThat(result.level()).isEqualTo("DEBUG");
        }

        @Test
        void getEffectiveLogging_endpointLoggingOverridesClient_mergedResult() {
            // Given
            var clientLogging = new LoggingProperties(false, "DEBUG",
                    new LoggingProperties.RequestConfig(
                            new LoggingProperties.PayloadConfig(false), null),
                    new LoggingProperties.ResponseConfig(
                            new LoggingProperties.PayloadConfig(false), null)
            );
            var endpointLogging = new LoggingProperties(true, "INFO",
                    new LoggingProperties.RequestConfig(
                            new LoggingProperties.PayloadConfig(true),
                            new LoggingProperties.HeadersConfig(true, List.of("X-Request-Id"), null)),
                    new LoggingProperties.ResponseConfig(
                            new LoggingProperties.PayloadConfig(true),
                            new LoggingProperties.HeadersConfig(true, null, List.of("Set-Cookie")))
            );
            var endpoint = new EndpointProperties("GET", "/api/items/{id}", null, null, null, endpointLogging);
            var registry = registryWithClient("svc",
                    new ClientProperties(baseUrl(), null, null, null, null, null,
                            Map.of("get-item", endpoint), null, null, clientLogging));

            // When
            var result = registry.getEffectiveLogging("svc", "get-item");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.enabled()).isTrue();
            assertThat(result.level()).isEqualTo("INFO");
            assertThat(result.request().payload().enabled()).isTrue();
            assertThat(result.request().headers().enabled()).isTrue();
            assertThat(result.request().headers().include()).containsExactly("X-Request-Id");
            assertThat(result.request().headers().exclude()).isNull();
            assertThat(result.response().payload().enabled()).isTrue();
            assertThat(result.response().headers().enabled()).isTrue();
            assertThat(result.response().headers().include()).isNull();
            assertThat(result.response().headers().exclude()).containsExactly("Set-Cookie");
        }

        @Test
        void getEffectiveLogging_endpointDisablesClientLogging_overrideWins() {
            // Given
            var clientLogging = new LoggingProperties(true, "INFO", null, null);
            var endpointLogging = new LoggingProperties(false, null, null, null);
            var endpoint = new EndpointProperties("GET", "/api/health", null, null, null, endpointLogging);
            var registry = registryWithClient("svc",
                    new ClientProperties(baseUrl(), null, null, null, null, null,
                            Map.of("health-check", endpoint), null, null, clientLogging));

            // When
            var result = registry.getEffectiveLogging("svc", "health-check");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.enabled()).isFalse(); // endpoint disables logging
            assertThat(result.level()).isEqualTo("INFO"); // inherited from client
        }

        @Test
        void getEffectiveLogging_unknownClient_returnsNull() {
            // Given
            var registry = new PlatformRestClientRegistry(new RestClientProperties(Map.of()), null);

            // When
            var result = registry.getEffectiveLogging("unknown", "list");

            // Then
            assertThat(result).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // getClientNames
    // -------------------------------------------------------------------------

    @Nested
    class GetClientNames {

        @Test
        void getClientNames_noClientsConfigured_returnsEmptySet() {
            // Given
            var registry = new PlatformRestClientRegistry(
                    new RestClientProperties(null), null);

            // When/Then
            assertThat(registry.getClientNames()).isEmpty();
        }

        @Test
        void getClientNames_multipleClients_returnsAllNames() {
            // Given
            var props = new RestClientProperties(Map.of(
                    "svc-a", new ClientProperties(baseUrl(), null, null, null, null, null, null, null, null, null),
                    "svc-b", new ClientProperties(baseUrl(), null, null, null, null, null, null, null, null, null)
            ));
            var registry = new PlatformRestClientRegistry(props, null);

            // When/Then
            assertThat(registry.getClientNames()).containsExactlyInAnyOrder("svc-a", "svc-b");
        }
    }
}
