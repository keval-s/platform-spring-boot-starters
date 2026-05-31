package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.ClientProperties;
import com.kevshah.platform.starter.rest.client.config.RestClientProperties;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
                    new ClientProperties(baseUrl(), null, null, null, null, null, null, null, null));

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
                    "svc-a", new ClientProperties(baseUrl(), null, null, null, null, null, null, null, null),
                    "svc-b", new ClientProperties(baseUrl(), null, null, null, null, null, null, null, null)
            ));
            var registry = new PlatformRestClientRegistry(props, null);

            // When/Then
            assertThat(registry.getClientNames()).containsExactlyInAnyOrder("svc-a", "svc-b");
        }
    }
}
