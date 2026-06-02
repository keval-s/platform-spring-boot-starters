package com.kevshah.platform.starter.rest.client.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kevshah.platform.starter.rest.client.config.LoggingProperties;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestClientLoggingInterceptorTest {

    final RestClientLoggingInterceptor interceptor = new RestClientLoggingInterceptor();

    @Mock
    ClientHttpRequestExecution execution;

    @Mock
    ClientHttpResponse mockResponse;

    // -------------------------------------------------------------------------
    // WhenNoLoggingContext
    // -------------------------------------------------------------------------

    @Nested
    class WhenNoLoggingContext {

        @Test
        void intercept_noAttributePresent_passesThroughWithoutLogging() throws IOException {
            // Given
            var request = new StubHttpRequest(new HashMap<>());
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(result).isSameAs(mockResponse);
            verify(execution).execute(eq(request), any());
        }

        @Test
        void intercept_nullAttributeValue_passesThroughWithoutLogging() throws IOException {
            // Given
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, null);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(result).isSameAs(mockResponse);
        }
    }

    // -------------------------------------------------------------------------
    // WhenLoggingDisabled
    // -------------------------------------------------------------------------

    @Nested
    class WhenLoggingDisabled {

        @Test
        void intercept_enabledFalse_passesThroughWithoutLogging() throws IOException {
            // Given
            var config = new LoggingProperties(false, "INFO", null, null);
            var context = new LoggingContext("svc", "list", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(result).isSameAs(mockResponse);
        }

        @Test
        void intercept_enabledNull_passesThroughWithoutLogging() throws IOException {
            // Given
            var config = new LoggingProperties(null, null, null, null);
            var context = new LoggingContext("svc", "list", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(result).isSameAs(mockResponse);
        }
    }

    // -------------------------------------------------------------------------
    // WhenLoggingEnabled — raw response (no body buffering)
    // -------------------------------------------------------------------------

    @Nested
    class WhenLoggingEnabled {

        Map<String, Object> attributes;
        StubHttpRequest request;

        @BeforeEach
        void setUp() throws IOException {
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("svc", "list", config);
            attributes = new HashMap<>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            request = new StubHttpRequest(attributes);

            when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
            when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);
        }

        @Test
        void intercept_loggingEnabled_returnsRawResponse() throws IOException {
            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(result).isSameAs(mockResponse);
        }

        @Test
        void intercept_loggingEnabled_executionIsInvoked() throws IOException {
            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            verify(execution).execute(eq(request), any());
        }

        @Test
        void intercept_loggingEnabledWithBody_executionReceivesOriginalBody() throws IOException {
            // Given
            var requestBody = "{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8);

            // When
            interceptor.intercept(request, requestBody, execution);

            // Then
            verify(execution).execute(eq(request), eq(requestBody));
        }

        @Test
        void intercept_loggingEnabledNoResponseBodyLogging_responseBodyNotBuffered() throws IOException {
            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then — raw response returned; getBody() never called by the interceptor
            verify(mockResponse, never()).getBody();
            assertThat(result).isSameAs(mockResponse);
        }
    }

    // -------------------------------------------------------------------------
    // WhenResponseBodyLogging — buffered response
    // -------------------------------------------------------------------------

    @Nested
    class WhenResponseBodyLogging {

        Map<String, Object> attributes;
        StubHttpRequest request;

        @BeforeEach
        void setUp() throws IOException {
            var config = new LoggingProperties(true, "INFO", null,
                    new LoggingProperties.ResponseConfig(new LoggingProperties.PayloadConfig(true), null));
            var context = new LoggingContext("svc", "find", config);
            attributes = new HashMap<>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            request = new StubHttpRequest(attributes);

            when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
            when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());
            when(mockResponse.getBody()).thenReturn(
                    new ByteArrayInputStream("{\"id\":\"item-1\"}".getBytes(StandardCharsets.UTF_8)));
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);
        }

        @Test
        void intercept_logResponseBodyTrue_returnsBufferedResponse() throws IOException {
            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then — a wrapping response is returned, not the raw one
            assertThat(result).isNotSameAs(mockResponse);
        }

        @Test
        void intercept_logResponseBodyTrue_bufferedBodyCanBeReadMultipleTimes() throws IOException {
            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then — body can be read twice from the buffered wrapper
            var firstRead = new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8);
            var secondRead = new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(firstRead).isEqualTo("{\"id\":\"item-1\"}");
            assertThat(secondRead).isEqualTo("{\"id\":\"item-1\"}");
        }

        @Test
        void intercept_logResponseBodyTrue_statusCodeIsPreserved() throws IOException {
            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void intercept_logResponseBodyTrue_headersArePreserved() throws IOException {
            // Given
            var responseHeaders = new HttpHeaders();
            responseHeaders.add("X-Request-Id", "req-42");
            when(mockResponse.getHeaders()).thenReturn(responseHeaders);

            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(result.getHeaders().getFirst("X-Request-Id")).isEqualTo("req-42");
        }
    }

    // -------------------------------------------------------------------------
    // WhenRequestBodyLogging
    // -------------------------------------------------------------------------

    @Nested
    class WhenRequestBodyLogging {

        Map<String, Object> attributes;
        StubHttpRequest request;

        @BeforeEach
        void setUp() throws IOException {
            var config = new LoggingProperties(true, "DEBUG",
                    new LoggingProperties.RequestConfig(new LoggingProperties.PayloadConfig(true), null), null);
            var context = new LoggingContext("svc", "create", config);
            attributes = new HashMap<>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            request = new StubHttpRequest(attributes);

            when(mockResponse.getStatusCode()).thenReturn(HttpStatus.CREATED);
            when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);
        }

        @Test
        void intercept_logRequestBodyTrueWithBody_executionIsInvoked() throws IOException {
            // Given
            var body = "{\"name\":\"item\"}".getBytes(StandardCharsets.UTF_8);

            // When
            var result = interceptor.intercept(request, body, execution);

            // Then
            assertThat(result).isSameAs(mockResponse);
            verify(execution).execute(eq(request), eq(body));
        }

        @Test
        void intercept_logRequestBodyTrueWithEmptyBody_executionIsInvoked() throws IOException {
            // Given — empty body should not cause any error even when logRequestBody=true
            var emptyBody = new byte[0];

            // When
            var result = interceptor.intercept(request, emptyBody, execution);

            // Then
            assertThat(result).isSameAs(mockResponse);
            verify(execution).execute(eq(request), eq(emptyBody));
        }
    }

    // -------------------------------------------------------------------------
    // WhenHeaderLogging
    // -------------------------------------------------------------------------

    @Nested
    class WhenHeaderLogging {

        @Test
        void intercept_logHeadersTrue_requestCompletesSuccessfully() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO",
                    new LoggingProperties.RequestConfig(null, new LoggingProperties.HeadersConfig(true, null, null)),
                    new LoggingProperties.ResponseConfig(null, new LoggingProperties.HeadersConfig(true, null, null)));
            var context = new LoggingContext("svc", "list", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
            when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            var result = interceptor.intercept(request, new byte[0], execution);

            // Then — header logging does not affect the returned response
            assertThat(result).isSameAs(mockResponse);
        }
    }

    // -------------------------------------------------------------------------
    // WhenStructuredLogOutput
    // -------------------------------------------------------------------------

    @Nested
    class WhenStructuredLogOutput {

        ch.qos.logback.classic.Logger interceptorLogger;
        ListAppender<ILoggingEvent> listAppender;

        @BeforeEach
        void setUp() throws IOException {
            interceptorLogger = (ch.qos.logback.classic.Logger)
                    LoggerFactory.getLogger(RestClientLoggingInterceptor.class);
            interceptorLogger.setLevel(Level.TRACE);
            listAppender = new ListAppender<>();
            listAppender.start();
            interceptorLogger.addAppender(listAppender);

            when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
            when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());
        }

        @AfterEach
        void tearDown() {
            interceptorLogger.detachAppender(listAppender);
        }

        @Test
        void intercept_loggingEnabled_requestLogEntryEmittedWithBaseKeys() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("payment-service", "listPayments", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "REST client request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = requestEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).containsExactlyInAnyOrder(
                    "platform.rest-client.name",
                    "platform.rest-client.endpoint",
                    "platform.rest-client.http.request.method",
                    "platform.rest-client.http.request.url"
            );
        }

        @Test
        void intercept_loggingEnabled_requestLogEntryHasCorrectValues() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("payment-service", "listPayments", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "REST client request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var kvpMap = toMap(requestEvent.getKeyValuePairs());
            assertThat(kvpMap).containsEntry("platform.rest-client.name", "payment-service")
                    .containsEntry("platform.rest-client.endpoint", "listPayments")
                    .containsEntry("platform.rest-client.http.request.method", "GET")
                    .containsEntry("platform.rest-client.http.request.url", "http://localhost/api/v1/items");
        }

        @Test
        void intercept_loggingEnabled_responseLogEntryEmittedWithBaseKeys() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("payment-service", "listPayments", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "REST client response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = responseEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).contains(
                    "platform.rest-client.name",
                    "platform.rest-client.endpoint",
                    "platform.rest-client.http.request.method",
                    "platform.rest-client.http.request.url",
                    "platform.rest-client.http.response.status",
                    "platform.rest-client.http.duration-ms"
            );
        }

        @Test
        void intercept_loggingEnabled_responseLogEntryHasCorrectStatusValue() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("payment-service", "listPayments", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(mockResponse.getStatusCode()).thenReturn(HttpStatus.CREATED);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "REST client response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var kvpMap = toMap(responseEvent.getKeyValuePairs());
            assertThat(kvpMap).containsEntry("platform.rest-client.http.response.status", 201);
        }

        @Test
        void intercept_debugLevel_bothLogEventsEmittedAtDebugLevel() throws IOException {
            // Given
            var config = new LoggingProperties(true, "DEBUG", null, null);
            var context = new LoggingContext("svc", "op", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(listAppender.list).hasSize(2);
            assertThat(listAppender.list).allMatch(e -> e.getLevel() == Level.DEBUG);
        }

        @Test
        void intercept_infoLevel_bothLogEventsEmittedAtInfoLevel() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("svc", "op", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(listAppender.list).hasSize(2);
            assertThat(listAppender.list).allMatch(e -> e.getLevel() == Level.INFO);
        }

        @Test
        void intercept_logRequestBodyTrueWithNonEmptyBody_requestBodyKeyPresentInRequestLog() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO",
                    new LoggingProperties.RequestConfig(new LoggingProperties.PayloadConfig(true), null), null);
            var context = new LoggingContext("svc", "create", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            var requestBody = "{\"name\":\"item\"}".getBytes(StandardCharsets.UTF_8);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, requestBody, execution);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "REST client request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var kvpMap = toMap(requestEvent.getKeyValuePairs());
            assertThat(kvpMap).containsEntry("platform.rest-client.http.request.body", "{\"name\":\"item\"}");
        }

        @Test
        void intercept_logRequestBodyTrueWithEmptyBody_requestBodyKeyAbsentFromRequestLog() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO",
                    new LoggingProperties.RequestConfig(new LoggingProperties.PayloadConfig(true), null), null);
            var context = new LoggingContext("svc", "create", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "REST client request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = requestEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).doesNotContain("platform.rest-client.http.request.body");
        }

        @Test
        void intercept_logRequestBodyFalse_requestBodyKeyAbsentFromRequestLog() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("svc", "create", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            var requestBody = "{\"name\":\"item\"}".getBytes(StandardCharsets.UTF_8);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, requestBody, execution);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "REST client request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = requestEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).doesNotContain("platform.rest-client.http.request.body");
        }

        @Test
        void intercept_logResponseBodyTrue_responseBodyKeyPresentInResponseLog() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null,
                    new LoggingProperties.ResponseConfig(new LoggingProperties.PayloadConfig(true), null));
            var context = new LoggingContext("svc", "find", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(mockResponse.getBody()).thenReturn(
                    new ByteArrayInputStream("{\"id\":\"item-1\"}".getBytes(StandardCharsets.UTF_8)));
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "REST client response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var kvpMap = toMap(responseEvent.getKeyValuePairs());
            assertThat(kvpMap).containsEntry("platform.rest-client.http.response.body", "{\"id\":\"item-1\"}");
        }

        @Test
        void intercept_logResponseBodyFalse_responseBodyKeyAbsentFromResponseLog() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("svc", "find", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "REST client response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = responseEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).doesNotContain("platform.rest-client.http.response.body");
        }

        @Test
        void intercept_logHeadersTrue_requestHeadersKeyPresentInRequestLog() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO",
                    new LoggingProperties.RequestConfig(null, new LoggingProperties.HeadersConfig(true, null, null)),
                    new LoggingProperties.ResponseConfig(null, new LoggingProperties.HeadersConfig(true, null, null)));
            var context = new LoggingContext("svc", "list", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "REST client request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = requestEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).contains("platform.rest-client.http.request.headers");
        }

        @Test
        void intercept_logHeadersTrue_responseHeadersKeyPresentInResponseLog() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO",
                    new LoggingProperties.RequestConfig(null, new LoggingProperties.HeadersConfig(true, null, null)),
                    new LoggingProperties.ResponseConfig(null, new LoggingProperties.HeadersConfig(true, null, null)));
            var context = new LoggingContext("svc", "list", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "REST client response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = responseEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).contains("platform.rest-client.http.response.headers");
        }

        @Test
        void intercept_logHeadersFalse_headerKeysAbsentFromBothLogEntries() throws IOException {
            // Given
            var config = new LoggingProperties(true, "INFO", null, null);
            var context = new LoggingContext("svc", "list", config);
            var attributes = new HashMap<String, Object>();
            attributes.put(RestClientLoggingInterceptor.ATTRIBUTE_KEY, context);
            var request = new StubHttpRequest(attributes);
            when(execution.execute(eq(request), any())).thenReturn(mockResponse);

            // When
            interceptor.intercept(request, new byte[0], execution);

            // Then
            assertThat(listAppender.list).hasSize(2);
            for (var event : listAppender.list) {
                var keys = event.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
                assertThat(keys).doesNotContain("platform.rest-client.http.request.headers",
                        "platform.rest-client.http.response.headers");
            }
        }

        // Maps a list of SLF4J KeyValuePairs to a plain Map for easy assertion.
        private Map<String, Object> toMap(List<org.slf4j.event.KeyValuePair> pairs) {
            var map = new HashMap<String, Object>();
            if (pairs != null) {
                pairs.forEach(kvp -> map.put(kvp.key, kvp.value));
            }
            return map;
        }
    }

    // -------------------------------------------------------------------------
    // Stub
    // -------------------------------------------------------------------------

    /// Minimal stub implementation of `HttpRequest` for use in interceptor tests.
    @NullMarked
    static final class StubHttpRequest implements org.springframework.http.HttpRequest {

        private final Map<String, Object> attributes;

        StubHttpRequest(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public HttpMethod getMethod() {
            return HttpMethod.GET;
        }

        @Override
        public URI getURI() {
            return URI.create("http://localhost/api/v1/items");
        }

        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }
    }
}



