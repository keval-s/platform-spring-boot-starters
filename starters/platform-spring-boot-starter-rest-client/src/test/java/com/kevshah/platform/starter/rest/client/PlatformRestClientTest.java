package com.kevshah.platform.starter.rest.client;

import com.kevshah.platform.starter.rest.client.config.ClientProperties;
import com.kevshah.platform.starter.rest.client.config.EndpointProperties;
import com.kevshah.platform.starter.rest.client.config.RestClientProperties;
import com.kevshah.platform.starter.rest.client.config.RetryProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.kevshah.platform.starter.rest.client.TestFixtures.ConfigUpdateRequest;
import static com.kevshah.platform.starter.rest.client.TestFixtures.CreateItemRequest;
import static com.kevshah.platform.starter.rest.client.TestFixtures.ItemResponse;
import static com.kevshah.platform.starter.rest.client.TestFixtures.StatusResponse;
import static com.kevshah.platform.starter.rest.client.TestFixtures.ToggleRequest;
import static com.kevshah.platform.starter.rest.client.TestFixtures.UpdateItemRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformRestClientTest {

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

    private PlatformRestClientRegistry registryWithEndpoint(String clientName,
                                                              String endpointName,
                                                              EndpointProperties endpoint) {
        var clientProps = new ClientProperties(baseUrl(), null, null, null, null, null,
                Map.of(endpointName, endpoint), null, null);
        return new PlatformRestClientRegistry(
                new RestClientProperties(Map.of(clientName, clientProps)), null);
    }

    // -------------------------------------------------------------------------
    // PlatformRestClientRegistry#client factory
    // -------------------------------------------------------------------------

    @Nested
    class ClientFactory {

        @Test
        void getPlatformRestClient_knownClientName_returnsPlatformRestClient() {
            // Given
            var registry = new PlatformRestClientRegistry(
                    new RestClientProperties(Map.of(
                            "svc", new ClientProperties(baseUrl(), null, null, null, null, null, null, null, null)
                    )), null);

            // When
            var client = registry.getPlatformRestClient("svc");

            // Then
            assertThat(client).isNotNull();
            assertThat(client.clientName()).isEqualTo("svc");
        }

        @Test
        void getPlatformRestClient_unknownClientName_throwsNoSuchElementException() {
            // Given
            var registry = new PlatformRestClientRegistry(new RestClientProperties(Map.of()), null);

            // When/Then
            assertThatThrownBy(() -> registry.getPlatformRestClient("missing"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("missing");
        }
    }

    // -------------------------------------------------------------------------
    // exchange — uses HTTP method from endpoint config
    // -------------------------------------------------------------------------

    @Nested
    class Exchange {

        @Test
        void exchange_configuredPostEndpoint_sendsPostRequest() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "create",
                    new EndpointProperties("POST", "/api/v1/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .exchange("create", new CreateItemRequest("widget"), ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("POST");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items");
            assertThat(recorded.getBody().readUtf8()).contains("\"name\":\"widget\"");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void exchange_noBodyNoUriVars_sendsGetRequest() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "list",
                    new EndpointProperties("GET", "/api/v1/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").exchange("list", ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("GET");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void exchange_withParameterizedTypeReference_returnsTypedList() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"1\"},{\"id\":\"2\"}]"));
            var registry = registryWithEndpoint("svc", "list",
                    new EndpointProperties("GET", "/api/v1/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").exchange("list",
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ItemResponse::id).containsExactly("1", "2");
        }

        @Test
        void exchange_withBodyAndUriVariables_expandsPathAndForwardsBody() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "update",
                    new EndpointProperties("PUT", "/api/v1/items/{id}", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .exchange("update", new UpdateItemRequest("ACTIVE"), Map.of("id", "pay-1"), ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-1");
            assertThat(recorded.getMethod()).isEqualTo("PUT");
            assertThat(recorded.getBody().readUtf8()).contains("\"status\":\"ACTIVE\"");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void exchange_withBodyUriVariablesAndHeaders_forwardsAdditionalHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "update",
                    new EndpointProperties("PUT", "/api/v1/items/{id}", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-1");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .exchange("update", new UpdateItemRequest("ACTIVE"), Map.of("id", "pay-1"), headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-1");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-1");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void exchange_withBodyAndHeaders_forwardsAdditionalHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "create",
                    new EndpointProperties("POST", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Request-Id", "req-42");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .exchange("create", new CreateItemRequest("widget"), headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Request-Id")).isEqualTo("req-42");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void exchange_withHeaders_forwardsAdditionalHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "list",
                    new EndpointProperties("GET", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Tenant-Id", "tenant-7");

            // When
            var result = registry.getPlatformRestClient("svc").exchange("list", headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Tenant-Id")).isEqualTo("tenant-7");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void exchange_withBodyUriVariablesAndParameterizedTypeRef_returnsTypedResponse() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"1\"}]"));
            var registry = registryWithEndpoint("svc", "list-under",
                    new EndpointProperties("GET", "/api/v1/owners/{ownerId}/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").exchange("list-under",
                    null, Map.of("ownerId", "owner-5"),
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/owners/owner-5/items");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("1");
        }

        @Test
        void exchange_withBodyUriVariablesHeadersAndParameterizedTypeRef_forwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"1\"}]"));
            var registry = registryWithEndpoint("svc", "list-under",
                    new EndpointProperties("GET", "/api/v1/owners/{ownerId}/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-99");

            // When
            var result = registry.getPlatformRestClient("svc").exchange("list-under",
                    null, Map.of("ownerId", "owner-5"), headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-99");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("1");
        }

        @Test
        void exchange_withBodyAndParameterizedTypeRef_returnsTypedResponse() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"new-1\"}]"));
            var registry = registryWithEndpoint("svc", "create",
                    new EndpointProperties("POST", "/api/v1/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").exchange("create",
                    new CreateItemRequest("widget"),
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("new-1");
        }

        @Test
        void exchange_withBodyHeadersAndParameterizedTypeRef_forwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"new-1\"}]"));
            var registry = registryWithEndpoint("svc", "create",
                    new EndpointProperties("POST", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Idempotency-Key", "idem-1");

            // When
            var result = registry.getPlatformRestClient("svc").exchange("create",
                    new CreateItemRequest("widget"), headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Idempotency-Key")).isEqualTo("idem-1");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("new-1");
        }

        @Test
        void exchange_withHeadersAndParameterizedTypeRef_forwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"1\"},{\"id\":\"2\"}]"));
            var registry = registryWithEndpoint("svc", "list",
                    new EndpointProperties("GET", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Tenant-Id", "tenant-3");

            // When
            var result = registry.getPlatformRestClient("svc").exchange("list", headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Tenant-Id")).isEqualTo("tenant-3");
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ItemResponse::id).containsExactly("1", "2");
        }
    }

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @Nested
    class Get {

        @Test
        void get_basicEndpoint_sendsGetRequest() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "find",
                    new EndpointProperties("GET", "/api/v1/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").get("find", ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("GET");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void get_withUriVariables_expandsTemplateInPath() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-42\"}"));
            var registry = registryWithEndpoint("svc", "find-by-id",
                    new EndpointProperties("GET", "/api/v1/items/{id}", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .get("find-by-id", Map.of("id", "pay-42"), ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-42");
            assertThat(result.id()).isEqualTo("pay-42");
        }

        @Test
        void get_withParameterizedTypeReference_returnsTypedList() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"1\"},{\"id\":\"2\"},{\"id\":\"3\"}]"));
            var registry = registryWithEndpoint("svc", "list",
                    new EndpointProperties("GET", "/api/v1/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").get("list",
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(ItemResponse::id).containsExactly("1", "2", "3");
        }

        @Test
        void get_withUriVariablesAndParameterizedTypeReference_expandsTemplateAndReturnsTypedList() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"item-A\"},{\"id\":\"item-B\"}]"));
            var registry = registryWithEndpoint("svc", "list-by-owner",
                    new EndpointProperties("GET", "/api/v1/owners/{ownerId}/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").get("list-by-owner",
                    Map.of("ownerId", "owner-1"),
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/owners/owner-1/items");
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ItemResponse::id).containsExactly("item-A", "item-B");
        }

        @Test
        void get_withHeaders_forwardsAdditionalHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "find",
                    new EndpointProperties("GET", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Tenant-Id", "tenant-5");

            // When
            var result = registry.getPlatformRestClient("svc").get("find", headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Tenant-Id")).isEqualTo("tenant-5");
            assertThat(recorded.getMethod()).isEqualTo("GET");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void get_withUriVariablesAndHeaders_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-10\"}"));
            var registry = registryWithEndpoint("svc", "find-by-id",
                    new EndpointProperties("GET", "/api/v1/items/{id}", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-5");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .get("find-by-id", Map.of("id", "pay-10"), headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-10");
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-5");
            assertThat(result.id()).isEqualTo("pay-10");
        }

        @Test
        void get_withHeadersAndParameterizedTypeRef_forwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"1\"},{\"id\":\"2\"}]"));
            var registry = registryWithEndpoint("svc", "list",
                    new EndpointProperties("GET", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Tenant-Id", "tenant-9");

            // When
            var result = registry.getPlatformRestClient("svc").get("list", headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Tenant-Id")).isEqualTo("tenant-9");
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ItemResponse::id).containsExactly("1", "2");
        }

        @Test
        void get_withUriVariablesHeadersAndParameterizedTypeRef_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"item-X\"},{\"id\":\"item-Y\"}]"));
            var registry = registryWithEndpoint("svc", "list-by-owner",
                    new EndpointProperties("GET", "/api/v1/owners/{ownerId}/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-get-ptr");

            // When
            var result = registry.getPlatformRestClient("svc").get("list-by-owner",
                    Map.of("ownerId", "owner-7"), headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/owners/owner-7/items");
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-get-ptr");
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ItemResponse::id).containsExactly("item-X", "item-Y");
        }
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @Nested
    class Post {

        @Test
        void post_withBody_sendsPostRequestWithBody() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"created-1\"}"));
            var registry = registryWithEndpoint("svc", "create",
                    new EndpointProperties("POST", "/api/v1/items", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .post("create", new CreateItemRequest("widget"), ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("POST");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items");
            assertThat(recorded.getBody().readUtf8()).contains("\"name\":\"widget\"");
            assertThat(result.id()).isEqualTo("created-1");
        }

        @Test
        void post_withBodyAndUriVariables_expandsPathAndSendsBody() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"child-1\"}"));
            var registry = registryWithEndpoint("svc", "create-child",
                    new EndpointProperties("POST", "/api/v1/parents/{parentId}/children", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .post("create-child", new CreateItemRequest("child"), Map.of("parentId", "parent-42"), ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/parents/parent-42/children");
            assertThat(recorded.getBody().readUtf8()).contains("\"name\":\"child\"");
            assertThat(result.id()).isEqualTo("child-1");
        }

        @Test
        void post_withBodyAndHeaders_forwardsAdditionalHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"created-2\"}"));
            var registry = registryWithEndpoint("svc", "create",
                    new EndpointProperties("POST", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Idempotency-Key", "idem-42");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .post("create", new CreateItemRequest("gadget"), headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Idempotency-Key")).isEqualTo("idem-42");
            assertThat(recorded.getMethod()).isEqualTo("POST");
            assertThat(result.id()).isEqualTo("created-2");
        }

        @Test
        void post_withBodyUriVariablesAndHeaders_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"child-2\"}"));
            var registry = registryWithEndpoint("svc", "create-child",
                    new EndpointProperties("POST", "/api/v1/parents/{parentId}/children", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Request-Id", "req-7");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .post("create-child", new CreateItemRequest("child"), Map.of("parentId", "parent-7"), headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/parents/parent-7/children");
            assertThat(recorded.getHeader("X-Request-Id")).isEqualTo("req-7");
            assertThat(result.id()).isEqualTo("child-2");
        }

        @Test
        void post_withBodyAndParameterizedTypeRef_returnsTypedResponse() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"new-1\"},{\"id\":\"new-2\"}]"));
            var registry = registryWithEndpoint("svc", "bulk-create",
                    new EndpointProperties("POST", "/api/v1/items/bulk", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").post("bulk-create",
                    List.of(new CreateItemRequest("a"), new CreateItemRequest("b")),
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getBody().readUtf8()).contains("\"name\":\"a\"").contains("\"name\":\"b\"");
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ItemResponse::id).containsExactly("new-1", "new-2");
        }

        @Test
        void post_withBodyHeadersAndParameterizedTypeRef_forwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"new-1\"}]"));
            var registry = registryWithEndpoint("svc", "create",
                    new EndpointProperties("POST", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Idempotency-Key", "idem-77");

            // When
            var result = registry.getPlatformRestClient("svc").post("create",
                    new CreateItemRequest("widget"), headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Idempotency-Key")).isEqualTo("idem-77");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("new-1");
        }

        @Test
        void post_withBodyUriVariablesAndParameterizedTypeRef_expandsPath() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"child-3\"}]"));
            var registry = registryWithEndpoint("svc", "create-children",
                    new EndpointProperties("POST", "/api/v1/parents/{parentId}/children", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").post("create-children",
                    List.of(new CreateItemRequest("c")), Map.of("parentId", "parent-3"),
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/parents/parent-3/children");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("child-3");
        }

        @Test
        void post_withBodyUriVariablesHeadersAndParameterizedTypeRef_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"child-4\"}]"));
            var registry = registryWithEndpoint("svc", "create-children",
                    new EndpointProperties("POST", "/api/v1/parents/{parentId}/children", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-4");

            // When
            var result = registry.getPlatformRestClient("svc").post("create-children",
                    List.of(new CreateItemRequest("c")), Map.of("parentId", "parent-4"), headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/parents/parent-4/children");
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-4");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("child-4");
        }
    }

    // -------------------------------------------------------------------------
    // PUT
    // -------------------------------------------------------------------------

    @Nested
    class Put {

        @Test
        void put_withBodyAndUriVariables_sendsPutRequestWithBody() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "update",
                    new EndpointProperties("PUT", "/api/v1/items/{id}", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .put("update", new UpdateItemRequest("ACTIVE"), Map.of("id", "pay-1"), ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("PUT");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-1");
            assertThat(recorded.getBody().readUtf8()).contains("\"status\":\"ACTIVE\"");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void put_withBodyOnly_sendsPutRequest() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"status\":\"REPLACED\"}"));
            var registry = registryWithEndpoint("svc", "replace",
                    new EndpointProperties("PUT", "/api/v1/config", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .put("replace", new ConfigUpdateRequest("val"), StatusResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("PUT");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/config");
            assertThat(recorded.getBody().readUtf8()).contains("\"key\":\"val\"");
            assertThat(result.status()).isEqualTo("REPLACED");
        }

        @Test
        void put_withBodyAndHeaders_forwardsAdditionalHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"status\":\"OK\"}"));
            var registry = registryWithEndpoint("svc", "replace",
                    new EndpointProperties("PUT", "/api/v1/config", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-put-1");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .put("replace", new ConfigUpdateRequest("val"), headers, StatusResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-put-1");
            assertThat(recorded.getMethod()).isEqualTo("PUT");
            assertThat(result.status()).isEqualTo("OK");
        }

        @Test
        void put_withBodyUriVariablesAndHeaders_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-2\"}"));
            var registry = registryWithEndpoint("svc", "update",
                    new EndpointProperties("PUT", "/api/v1/items/{id}", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Request-Id", "req-put-2");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .put("update", new UpdateItemRequest("ACTIVE"), Map.of("id", "pay-2"), headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-2");
            assertThat(recorded.getHeader("X-Request-Id")).isEqualTo("req-put-2");
            assertThat(result.id()).isEqualTo("pay-2");
        }

        @Test
        void put_withBodyAndParameterizedTypeRef_returnsTypedResponse() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"status\":\"REPLACED\"}]"));
            var registry = registryWithEndpoint("svc", "replace",
                    new EndpointProperties("PUT", "/api/v1/config", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").put("replace",
                    new ConfigUpdateRequest("val"),
                    new ParameterizedTypeReference<List<StatusResponse>>() {});

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status()).isEqualTo("REPLACED");
        }

        @Test
        void put_withBodyHeadersAndParameterizedTypeRef_forwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"status\":\"OK\"}]"));
            var registry = registryWithEndpoint("svc", "replace",
                    new EndpointProperties("PUT", "/api/v1/config", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-put-ptr");

            // When
            var result = registry.getPlatformRestClient("svc").put("replace",
                    new ConfigUpdateRequest("val"), headers,
                    new ParameterizedTypeReference<List<StatusResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-put-ptr");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status()).isEqualTo("OK");
        }

        @Test
        void put_withBodyUriVariablesAndParameterizedTypeRef_expandsPath() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"pay-3\"}]"));
            var registry = registryWithEndpoint("svc", "update",
                    new EndpointProperties("PUT", "/api/v1/items/{id}", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").put("update",
                    new UpdateItemRequest("ACTIVE"), Map.of("id", "pay-3"),
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-3");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("pay-3");
        }

        @Test
        void put_withBodyUriVariablesHeadersAndParameterizedTypeRef_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"pay-4\"}]"));
            var registry = registryWithEndpoint("svc", "update",
                    new EndpointProperties("PUT", "/api/v1/items/{id}", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-put-all");

            // When
            var result = registry.getPlatformRestClient("svc").put("update",
                    new UpdateItemRequest("ACTIVE"), Map.of("id", "pay-4"), headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-4");
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-put-all");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("pay-4");
        }
    }

    // -------------------------------------------------------------------------
    // PATCH
    // -------------------------------------------------------------------------

    @Nested
    class Patch {

        @Test
        void patch_withBodyAndUriVariables_sendsPatchRequestWithBody() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "partial-update",
                    new EndpointProperties("PATCH", "/api/v1/items/{id}", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .patch("partial-update", new UpdateItemRequest("INACTIVE"), Map.of("id", "pay-1"), ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("PATCH");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-1");
            assertThat(recorded.getBody().readUtf8()).contains("\"status\":\"INACTIVE\"");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void patch_withBodyOnly_sendsPatchRequest() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"status\":\"PATCHED\"}"));
            var registry = registryWithEndpoint("svc", "toggle",
                    new EndpointProperties("PATCH", "/api/v1/config", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc")
                    .patch("toggle", new ToggleRequest(false), StatusResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("PATCH");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/config");
            assertThat(recorded.getBody().readUtf8()).contains("\"enabled\":false");
            assertThat(result.status()).isEqualTo("PATCHED");
        }

        @Test
        void patch_withBodyAndHeaders_forwardsAdditionalHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"status\":\"OK\"}"));
            var registry = registryWithEndpoint("svc", "toggle",
                    new EndpointProperties("PATCH", "/api/v1/config", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-patch-1");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .patch("toggle", new ToggleRequest(false), headers, StatusResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-patch-1");
            assertThat(recorded.getMethod()).isEqualTo("PATCH");
            assertThat(result.status()).isEqualTo("OK");
        }

        @Test
        void patch_withBodyUriVariablesAndHeaders_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-5\"}"));
            var registry = registryWithEndpoint("svc", "partial-update",
                    new EndpointProperties("PATCH", "/api/v1/items/{id}", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Request-Id", "req-patch-5");

            // When
            var result = registry.getPlatformRestClient("svc")
                    .patch("partial-update", new UpdateItemRequest("INACTIVE"), Map.of("id", "pay-5"), headers, ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-5");
            assertThat(recorded.getHeader("X-Request-Id")).isEqualTo("req-patch-5");
            assertThat(result.id()).isEqualTo("pay-5");
        }

        @Test
        void patch_withBodyAndParameterizedTypeRef_returnsTypedResponse() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"status\":\"PATCHED\"}]"));
            var registry = registryWithEndpoint("svc", "toggle",
                    new EndpointProperties("PATCH", "/api/v1/config", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").patch("toggle",
                    new ToggleRequest(false),
                    new ParameterizedTypeReference<List<StatusResponse>>() {});

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status()).isEqualTo("PATCHED");
        }

        @Test
        void patch_withBodyHeadersAndParameterizedTypeRef_forwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"status\":\"OK\"}]"));
            var registry = registryWithEndpoint("svc", "toggle",
                    new EndpointProperties("PATCH", "/api/v1/config", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-patch-ptr");

            // When
            var result = registry.getPlatformRestClient("svc").patch("toggle",
                    new ToggleRequest(false), headers,
                    new ParameterizedTypeReference<List<StatusResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-patch-ptr");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status()).isEqualTo("OK");
        }

        @Test
        void patch_withBodyUriVariablesAndParameterizedTypeRef_expandsPath() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"pay-6\"}]"));
            var registry = registryWithEndpoint("svc", "partial-update",
                    new EndpointProperties("PATCH", "/api/v1/items/{id}", null, null, null));

            // When
            var result = registry.getPlatformRestClient("svc").patch("partial-update",
                    new UpdateItemRequest("INACTIVE"), Map.of("id", "pay-6"),
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-6");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("pay-6");
        }

        @Test
        void patch_withBodyUriVariablesHeadersAndParameterizedTypeRef_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[{\"id\":\"pay-7\"}]"));
            var registry = registryWithEndpoint("svc", "partial-update",
                    new EndpointProperties("PATCH", "/api/v1/items/{id}", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-patch-all");

            // When
            var result = registry.getPlatformRestClient("svc").patch("partial-update",
                    new UpdateItemRequest("INACTIVE"), Map.of("id", "pay-7"), headers,
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-7");
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-patch-all");
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo("pay-7");
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Nested
    class Delete {

        @Test
        void delete_withoutUriVariables_sendsDeleteRequest() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            var registry = registryWithEndpoint("svc", "delete-all",
                    new EndpointProperties("DELETE", "/api/v1/items", null, null, null));

            // When
            registry.getPlatformRestClient("svc").delete("delete-all");

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("DELETE");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items");
        }

        @Test
        void delete_withUriVariables_expandsPathAndSendsDeleteRequest() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            var registry = registryWithEndpoint("svc", "delete-by-id",
                    new EndpointProperties("DELETE", "/api/v1/items/{id}", null, null, null));

            // When
            registry.getPlatformRestClient("svc").delete("delete-by-id", Map.of("id", "pay-99"));

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("DELETE");
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-99");
        }

        @Test
        void delete_withHeaders_forwardsAdditionalHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            var registry = registryWithEndpoint("svc", "delete-all",
                    new EndpointProperties("DELETE", "/api/v1/items", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-del-1");

            // When
            registry.getPlatformRestClient("svc").delete("delete-all", headers);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getMethod()).isEqualTo("DELETE");
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-del-1");
        }

        @Test
        void delete_withUriVariablesAndHeaders_expandsPathAndForwardsHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse().setResponseCode(204));
            var registry = registryWithEndpoint("svc", "delete-by-id",
                    new EndpointProperties("DELETE", "/api/v1/items/{id}", null, null, null));
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", "corr-del-2");

            // When
            registry.getPlatformRestClient("svc").delete("delete-by-id", Map.of("id", "pay-88"), headers);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/api/v1/items/pay-88");
            assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-del-2");
        }
    }

    // -------------------------------------------------------------------------
    // Endpoint-level header overrides
    // -------------------------------------------------------------------------

    @Nested
    class EndpointHeaders {

        @Test
        void get_endpointWithContentTypeAndAccept_requestContainsEndpointHeaders() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/vnd.api+json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var registry = registryWithEndpoint("svc", "find",
                    new EndpointProperties("GET", "/api/v1/items", null,
                            "application/vnd.api+json", "application/vnd.api+json"));

            // When
            var result = registry.getPlatformRestClient("svc").get("find", ItemResponse.class);

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/vnd.api+json");
            assertThat(recorded.getHeader("Accept")).isEqualTo("application/vnd.api+json");
            assertThat(result.id()).isEqualTo("pay-1");
        }

        @Test
        void get_endpointWithNoHeaderOverrides_clientLevelHeadersArePreserved() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));
            var clientProps = new ClientProperties(baseUrl(), "application/json", "application/json",
                    null, null, null,
                    Map.of("find", new EndpointProperties("GET", "/api/v1/items", null, null, null)),
                    null, null);
            var registry = new PlatformRestClientRegistry(
                    new RestClientProperties(Map.of("svc", clientProps)), null);

            // When
            var result = registry.getPlatformRestClient("svc").get("find", ItemResponse.class);

            // Then — client-level headers are still present when no endpoint overrides are set
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json");
            assertThat(recorded.getHeader("Accept")).isEqualTo("application/json");
            assertThat(result.id()).isEqualTo("pay-1");
        }
    }

    // -------------------------------------------------------------------------
    // Endpoint-level default query parameters
    // -------------------------------------------------------------------------

    @Nested
    class EndpointQueryParams {

        @Test
        void get_endpointWithDefaultQueryParams_requestUriContainsEndpointQueryParams() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[]"));
            var registry = registryWithEndpoint("svc", "list",
                    new EndpointProperties("GET", "/api/v1/items",
                            Map.of("page", "0", "size", "10"), null, null));

            // When
            var result = registry.getPlatformRestClient("svc").get("list",
                    new ParameterizedTypeReference<List<ItemResponse>>() {});

            // Then
            var recorded = mockWebServer.takeRequest();
            assertThat(recorded.getPath())
                    .contains("page=0")
                    .contains("size=10");
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Retry integration
    // -------------------------------------------------------------------------

    @Nested
    class RetryIntegration {

        @Test
        void get_retryableStatusCode_retriesAndEventuallySucceeds() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse().setResponseCode(503));
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"id\":\"pay-1\"}"));

            var retryProps = new RetryProperties(2, Duration.ofMillis(10), null, null, List.of(503));
            var clientProps = new ClientProperties(baseUrl(), null, null, null, null, null,
                    Map.of("find", new EndpointProperties("GET", "/api/v1/items", null, null, null)),
                    retryProps, null);
            var registry = new PlatformRestClientRegistry(
                    new RestClientProperties(Map.of("svc", clientProps)), null);

            // When
            var result = registry.getPlatformRestClient("svc").get("find", ItemResponse.class);

            // Then
            assertThat(result.id()).isEqualTo("pay-1");
            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        }
    }

    // -------------------------------------------------------------------------
    // Unknown endpoint guard
    // -------------------------------------------------------------------------

    @Nested
    class UnknownEndpoint {

        @Test
        void get_unknownEndpointName_throwsNoSuchElementException() {
            // Given
            var registry = new PlatformRestClientRegistry(
                    new RestClientProperties(Map.of(
                            "svc", new ClientProperties(baseUrl(), null, null, null, null, null,
                                    Map.of(), null, null)
                    )), null);

            // When/Then
            assertThatThrownBy(() -> registry.getPlatformRestClient("svc").get("nonexistent", ItemResponse.class))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("nonexistent");
        }
    }
}

