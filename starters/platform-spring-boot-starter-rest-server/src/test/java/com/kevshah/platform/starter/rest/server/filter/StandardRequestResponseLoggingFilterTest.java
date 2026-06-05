package com.kevshah.platform.starter.rest.server.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties.HeadersConfig;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties.LoggingRule;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties.PayloadConfig;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties.RequestConfig;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties.ResponseConfig;
import com.kevshah.platform.starter.rest.server.config.RestServerProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class StandardRequestResponseLoggingFilterTest {

    @Mock
    private FilterChain filterChain;

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private StandardRequestResponseLoggingFilter filterWith(RestServerProperties properties) {
        return new StandardRequestResponseLoggingFilter(properties, jsonMapper);
    }

    private static RestServerProperties loggingDisabled() {
        return new RestServerProperties(new LoggingProperties(false, null));
    }

    /** Logging enabled globally with no rules &mdash; only basic request/response info is logged (no payloads). */
    private static RestServerProperties loggingEnabled() {
        return new RestServerProperties(new LoggingProperties(true, null));
    }

    private static RestServerProperties loggingEnabledWithRules(List<LoggingRule> rules) {
        return new RestServerProperties(new LoggingProperties(true, rules));
    }

    /**
     * Simulates downstream reading the request body &mdash; with the new {@code CachedBodyRequestWrapper} the body is
     * pre-buffered, so the chain just reads from the replayable stream.
     */
    private void setupChainToReadRequestBody() throws Exception {
        doAnswer(invocation -> {
                    HttpServletRequest req = (HttpServletRequest) invocation.getArgument(0);
                    req.getInputStream().readAllBytes();
                    return null;
                })
                .when(filterChain)
                .doFilter(any(), any());
    }

    /**
     * Simulates downstream writing {@code body} to the response so
     * {@code ContentCachingResponseWrapper.getContentAsByteArray()} returns content.
     */
    private void setupChainToWriteResponseBody(byte[] body) throws Exception {
        doAnswer(invocation -> {
                    ContentCachingResponseWrapper wrapped = (ContentCachingResponseWrapper) invocation.getArgument(1);
                    wrapped.getOutputStream().write(body);
                    return null;
                })
                .when(filterChain)
                .doFilter(any(), any());
    }

    // -------------------------------------------------------------------------
    // Routing: original request/response vs. wrapped wrappers
    // -------------------------------------------------------------------------

    @Nested
    class RequestRoutingTests {

        @Test
        void givenNullLoggingConfig_whenFiltering_thenPassesOriginalRequestToChain() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(new RestServerProperties(null));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(captor.capture(), eq(response));
            assertThat(captor.getValue()).isSameAs(request);
        }

        @Test
        void givenLoggingDisabled_whenFiltering_thenPassesOriginalRequestToChain() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingDisabled());
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(captor.capture(), eq(response));
            assertThat(captor.getValue()).isSameAs(request);
        }

        @Test
        void givenLoggingEnabledWithNoPayloadRule_whenFiltering_thenOriginalRequestPassedToChain() throws Exception {
            // Given – logging enabled but no rules → logRequestPayload=false → no wrapping needed
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabled());
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then – the original request is passed through unchanged (no body buffering needed)
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(captor.capture(), any());
            assertThat(captor.getValue()).isSameAs(request);
        }

        @Test
        void givenLoggingEnabledWithRequestPayloadRule_whenFiltering_thenRequestWrappedForBodyBuffering()
                throws Exception {
            // Given – a rule enables request payload logging → CachedBodyRequestWrapper is used
            LoggingRule rule =
                    new LoggingRule("/**", null, null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then – request is wrapped in a HttpServletRequestWrapper for body buffering and replay
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(captor.capture(), any());
            assertThat(captor.getValue()).isInstanceOf(HttpServletRequestWrapper.class);
        }

        @Test
        void givenLoggingEnabled_whenFiltering_thenWrapsResponseWithContentCachingWrapper() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabled());
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then – argument 1 is the wrapped response
            ArgumentCaptor<HttpServletResponse> captor = ArgumentCaptor.forClass(HttpServletResponse.class);
            verify(filterChain).doFilter(any(), captor.capture());
            assertThat(captor.getValue()).isInstanceOf(ContentCachingResponseWrapper.class);
        }
    }

    // -------------------------------------------------------------------------
    // Filter chain is always called exactly once
    // -------------------------------------------------------------------------

    @Nested
    class FilterChainInvocationTests {

        @Test
        void givenLoggingDisabled_whenFiltering_thenFilterChainCalledExactlyOnce() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingDisabled());
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        void givenLoggingEnabled_whenFiltering_thenFilterChainCalledExactlyOnce() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabled());
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Request payload body handling
    // -------------------------------------------------------------------------

    @Nested
    class RequestPayloadBodyTests {

        @Test
        void givenJsonBodyAndRequestPayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given – catch-all rule with request payload logging on
            LoggingRule rule =
                    new LoggingRule("/**", null, null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\",\"qty\":2}".getBytes());
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToReadRequestBody();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenNonJsonBodyAndRequestPayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given
            LoggingRule rule =
                    new LoggingRule("/**", null, null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
            request.setContent("plain text payload".getBytes());
            request.setContentType("text/plain");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToReadRequestBody();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenEmptyBodyAndRequestPayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given
            LoggingRule rule =
                    new LoggingRule("/**", null, null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenRequestPayloadLoggingDisabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given – rule matched but request payload logging is off
            LoggingRule rule =
                    new LoggingRule("/**", null, null, new RequestConfig(true, new PayloadConfig(false), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }
    }

    // -------------------------------------------------------------------------
    // Response payload body handling
    // -------------------------------------------------------------------------

    @Nested
    class ResponsePayloadBodyTests {

        @Test
        void givenJsonResponseAndResponsePayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown()
                throws Exception {
            // Given
            LoggingRule rule =
                    new LoggingRule("/**", null, null, null, new ResponseConfig(true, new PayloadConfig(true), null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("{\"orderId\":1,\"status\":\"CREATED\"}".getBytes());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenNonJsonResponseAndResponsePayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown()
                throws Exception {
            // Given
            LoggingRule rule =
                    new LoggingRule("/**", null, null, null, new ResponseConfig(true, new PayloadConfig(true), null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("pong".getBytes());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenEmptyResponseBodyAndResponsePayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown()
                throws Exception {
            // Given – no body written by downstream
            LoggingRule rule =
                    new LoggingRule("/**", null, null, null, new ResponseConfig(true, new PayloadConfig(true), null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenResponsePayloadLoggingDisabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given – response payload logging is explicitly off
            LoggingRule rule =
                    new LoggingRule("/**", null, null, null, new ResponseConfig(true, new PayloadConfig(false), null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("{\"orderId\":1}".getBytes());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenResponsePayloadLoggingEnabled_whenFiltering_thenResponseBodyIsCopiedBackToActualResponse()
                throws Exception {
            // Given
            byte[] responseBody = "{\"orderId\":42}".getBytes();
            LoggingRule rule =
                    new LoggingRule("/**", null, null, null, new ResponseConfig(true, new PayloadConfig(true), null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/42");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody(responseBody);

            // When
            filter.doFilter(request, response, filterChain);

            // Then – the body is still present on the actual response after the filter runs
            assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
        }

        @Test
        void givenBothRequestAndResponsePayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given
            LoggingRule rule = new LoggingRule(
                    "/**",
                    null,
                    null,
                    new RequestConfig(true, new PayloadConfig(true), null),
                    new ResponseConfig(true, new PayloadConfig(true), null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\"}".getBytes());
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();
            doAnswer(invocation -> {
                        HttpServletRequest req = (HttpServletRequest) invocation.getArgument(0);
                        req.getInputStream().readAllBytes();
                        ContentCachingResponseWrapper resp = (ContentCachingResponseWrapper) invocation.getArgument(1);
                        resp.getOutputStream().write("{\"orderId\":99}".getBytes());
                        return null;
                    })
                    .when(filterChain)
                    .doFilter(any(), any());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }
    }

    // -------------------------------------------------------------------------
    // Rule matching: enabled flag, ordering, method restrictions
    // -------------------------------------------------------------------------

    @Nested
    class RuleMatchingTests {

        @Test
        void givenRuleWithEnabledFalse_whenRequestMatches_thenOriginalRequestPassedToChain() throws Exception {
            // Given – rule disables all logging for /api/health
            LoggingRule silenceRule = new LoggingRule("/api/health", null, false, null, null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(silenceRule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
            request.setContent("{\"status\":\"UP\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then – original (unwrapped) request is passed to the chain
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(captor.capture(), eq(response));
            assertThat(captor.getValue()).isSameAs(request);
        }

        @Test
        void givenMethodScopedSilenceRule_whenMethodDoesNotMatch_thenFilterChainIsStillCalled() throws Exception {
            // Given – silence only GET /api/**; POST should continue normally
            LoggingRule silenceGetRule = new LoggingRule("/api/**", List.of("GET"), false, null, null);
            LoggingRule logPostRule = new LoggingRule(
                    "/api/**", List.of("POST"), null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter =
                    filterWith(loggingEnabledWithRules(List.of(silenceGetRule, logPostRule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\"}".getBytes());
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        void givenRequestMatchesRule_whenPayloadLoggingEnabled_thenNoExceptionIsThrown() throws Exception {
            // Given
            LoggingRule rule = new LoggingRule(
                    "/api/**", List.of("POST"), null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\"}".getBytes());
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        void givenRequestDoesNotMatchAnyRule_whenFiltering_thenBasicLoggingOccursWithoutPayload() throws Exception {
            // Given – rule only covers POST/PUT; a GET request matches no rule
            LoggingRule rule = new LoggingRule(
                    "/api/orders/**",
                    List.of("POST", "PUT"),
                    null,
                    new RequestConfig(true, new PayloadConfig(true), null),
                    null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then – no exception; chain still called once
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        void givenMoreSpecificSilenceRuleBeforeWildcard_whenRequestMatchesSilenceRule_thenOriginalRequestPassed()
                throws Exception {
            // Given – /api/internal/** is silenced; broader /api/** enables payload logging.
            //         Rule ordering means the silence rule is checked first.
            LoggingRule silenceInternal = new LoggingRule("/api/internal/**", null, false, null, null);
            LoggingRule logAll = new LoggingRule(
                    "/api/**", null, null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter =
                    filterWith(loggingEnabledWithRules(List.of(silenceInternal, logAll)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/internal/config");
            request.setContent("{\"secret\":\"value\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then – original (unwrapped) request is passed to the chain
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(captor.capture(), eq(response));
            assertThat(captor.getValue()).isSameAs(request);
        }

        @Test
        void givenCatchAllRule_whenAnyPathRequested_thenPayloadLoggingApplies() throws Exception {
            // Given – a single catch-all rule enables payload logging for everything
            LoggingRule catchAll =
                    new LoggingRule("/**", null, null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(catchAll)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/anything/at/all");
            request.setContent("{\"data\":true}".getBytes());
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
            verify(filterChain).doFilter(any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Per-direction enabled flag (RequestConfig.enabled / ResponseConfig.enabled)
    // -------------------------------------------------------------------------

    @Nested
    class PerDirectionEnabledTests {

        @Test
        void givenRequestConfigEnabledFalse_whenFiltering_thenOriginalRequestPassedToChainAndFilterChainCalled()
                throws Exception {
            // Given – rule suppresses the request log entry only; response is still logged
            LoggingRule rule = new LoggingRule("/**", null, null, new RequestConfig(false, null, null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then – original (unwrapped) request passed; chain called exactly once
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(captor.capture(), any());
            assertThat(captor.getValue()).isSameAs(request);
        }

        @Test
        void givenResponseConfigEnabledFalse_whenFiltering_thenFilterChainCalledAndNoExceptionIsThrown()
                throws Exception {
            // Given – rule suppresses the response log entry only; request is still logged
            LoggingRule rule = new LoggingRule("/**", null, null, null, new ResponseConfig(false, null, null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("{\"count\":5}".getBytes());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        void givenBothDirectionsDisabled_whenFiltering_thenResponseBodyStillCopiedBack() throws Exception {
            // Given – both request and response log entries suppressed
            LoggingRule rule = new LoggingRule(
                    "/**", null, null, new RequestConfig(false, null, null), new ResponseConfig(false, null, null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            byte[] body = "{\"ok\":true}".getBytes();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/status");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody(body);

            // When
            filter.doFilter(request, response, filterChain);

            // Then – response body still copied back even though logging was suppressed
            assertThat(response.getContentAsByteArray()).isEqualTo(body);
        }
    }

    // -------------------------------------------------------------------------
    // Header logging
    // -------------------------------------------------------------------------

    @Nested
    class HeaderLoggingTests {

        @Test
        void givenRequestHeaderLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given
            HeadersConfig headersConfig = new HeadersConfig(true, null, null);
            LoggingRule rule = new LoggingRule("/**", null, null, new RequestConfig(true, null, headersConfig), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            request.addHeader("X-Correlation-Id", "abc-123");
            request.addHeader("Accept", "application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenRequestHeaderLoggingWithExcludeList_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given – Authorization header is excluded
            HeadersConfig headersConfig = new HeadersConfig(true, null, List.of("Authorization"));
            LoggingRule rule = new LoggingRule("/**", null, null, new RequestConfig(true, null, headersConfig), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.addHeader("Authorization", "Bearer secret");
            request.addHeader("Content-Type", "application/json");
            request.setContent("{\"item\":\"book\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenResponseHeaderLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given
            HeadersConfig headersConfig = new HeadersConfig(true, null, null);
            LoggingRule rule = new LoggingRule("/**", null, null, null, new ResponseConfig(true, null, headersConfig));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            doAnswer(invocation -> {
                        ContentCachingResponseWrapper resp = (ContentCachingResponseWrapper) invocation.getArgument(1);
                        resp.addHeader("X-Request-Id", "req-42");
                        resp.getOutputStream().write("{\"orderId\":1}".getBytes());
                        return null;
                    })
                    .when(filterChain)
                    .doFilter(any(), any());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenRequestHeaderLoggingDisabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given – headers config present but disabled
            HeadersConfig headersConfig = new HeadersConfig(false, null, null);
            LoggingRule rule = new LoggingRule("/**", null, null, new RequestConfig(true, null, headersConfig), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            request.addHeader("Accept", "application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }
    }

    // -------------------------------------------------------------------------
    // Structured log key assertions
    // -------------------------------------------------------------------------

    @Nested
    class WhenStructuredLogOutput {

        ch.qos.logback.classic.Logger filterLogger;
        ListAppender<ILoggingEvent> listAppender;

        @BeforeEach
        void setUp() {
            filterLogger =
                    (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(StandardRequestResponseLoggingFilter.class);
            filterLogger.setLevel(Level.DEBUG);
            listAppender = new ListAppender<>();
            listAppender.start();
            filterLogger.addAppender(listAppender);
        }

        @AfterEach
        void tearDown() {
            filterLogger.detachAppender(listAppender);
        }

        @Test
        void doFilter_loggingEnabled_requestLogEntryEmittedWithBaseKeys() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabled());
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "Incoming HTTP request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys =
                    requestEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys)
                    .containsExactlyInAnyOrder(
                            "platform.rest-server.http.request.method", "platform.rest-server.http.request.url");
        }

        @Test
        void doFilter_loggingEnabled_requestLogEntryHasCorrectValues() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabled());
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "Incoming HTTP request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var kvpMap = toMap(requestEvent.getKeyValuePairs());
            assertThat(kvpMap)
                    .containsEntry("platform.rest-server.http.request.method", "POST")
                    .containsEntry("platform.rest-server.http.request.url", "/api/orders");
        }

        @Test
        void doFilter_loggingEnabled_responseLogEntryEmittedWithBaseKeys() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabled());
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "Outgoing HTTP response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = responseEvent.getKeyValuePairs().stream()
                    .map(kvp -> kvp.key)
                    .toList();
            assertThat(keys)
                    .containsExactlyInAnyOrder(
                            "platform.rest-server.http.request.method",
                            "platform.rest-server.http.request.url",
                            "platform.rest-server.http.response.status");
        }

        @Test
        void doFilter_loggingEnabled_responseLogEntryHasCorrectStatusValue() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabled());
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(201);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "Outgoing HTTP response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var kvpMap = toMap(responseEvent.getKeyValuePairs());
            assertThat(kvpMap).containsEntry("platform.rest-server.http.response.status", 201);
        }

        @Test
        void doFilter_requestPayloadLoggingEnabled_requestBodyKeyPresentInRequestLog() throws Exception {
            // Given
            LoggingRule rule =
                    new LoggingRule("/**", null, null, new RequestConfig(true, new PayloadConfig(true), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\"}".getBytes());
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToReadRequestBody();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "Incoming HTTP request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys =
                    requestEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).contains("platform.rest-server.http.request.body");
        }

        @Test
        void doFilter_requestPayloadLoggingDisabled_requestBodyKeyAbsentFromRequestLog() throws Exception {
            // Given
            LoggingRule rule =
                    new LoggingRule("/**", null, null, new RequestConfig(true, new PayloadConfig(false), null), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "Incoming HTTP request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys =
                    requestEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).doesNotContain("platform.rest-server.http.request.body");
        }

        @Test
        void doFilter_responsePayloadLoggingEnabled_responseBodyKeyPresentInResponseLog() throws Exception {
            // Given
            LoggingRule rule =
                    new LoggingRule("/**", null, null, null, new ResponseConfig(true, new PayloadConfig(true), null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("{\"orderId\":1}".getBytes());

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "Outgoing HTTP response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = responseEvent.getKeyValuePairs().stream()
                    .map(kvp -> kvp.key)
                    .toList();
            assertThat(keys).contains("platform.rest-server.http.response.body");
        }

        @Test
        void doFilter_responsePayloadLoggingDisabled_responseBodyKeyAbsentFromResponseLog() throws Exception {
            // Given
            LoggingRule rule =
                    new LoggingRule("/**", null, null, null, new ResponseConfig(true, new PayloadConfig(false), null));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("{\"orderId\":1}".getBytes());

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "Outgoing HTTP response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = responseEvent.getKeyValuePairs().stream()
                    .map(kvp -> kvp.key)
                    .toList();
            assertThat(keys).doesNotContain("platform.rest-server.http.response.body");
        }

        @Test
        void doFilter_requestHeaderLoggingEnabled_requestHeadersKeyPresentInRequestLog() throws Exception {
            // Given
            HeadersConfig headersConfig = new HeadersConfig(true, null, null);
            LoggingRule rule = new LoggingRule("/**", null, null, new RequestConfig(true, null, headersConfig), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            request.addHeader("X-Correlation-Id", "abc-123");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var requestEvent = listAppender.list.stream()
                    .filter(e -> "Incoming HTTP request".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys =
                    requestEvent.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
            assertThat(keys).contains("platform.rest-server.http.request.headers");
        }

        @Test
        void doFilter_responseHeaderLoggingEnabled_responseHeadersKeyPresentInResponseLog() throws Exception {
            // Given
            HeadersConfig headersConfig = new HeadersConfig(true, null, null);
            LoggingRule rule = new LoggingRule("/**", null, null, null, new ResponseConfig(true, null, headersConfig));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            doAnswer(invocation -> {
                        ContentCachingResponseWrapper resp = (ContentCachingResponseWrapper) invocation.getArgument(1);
                        resp.addHeader("X-Request-Id", "req-42");
                        return null;
                    })
                    .when(filterChain)
                    .doFilter(any(), any());

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            var responseEvent = listAppender.list.stream()
                    .filter(e -> "Outgoing HTTP response".equals(e.getMessage()))
                    .findFirst()
                    .orElseThrow();
            var keys = responseEvent.getKeyValuePairs().stream()
                    .map(kvp -> kvp.key)
                    .toList();
            assertThat(keys).contains("platform.rest-server.http.response.headers");
        }

        @Test
        void doFilter_headerLoggingDisabled_headerKeysAbsentFromBothLogEntries() throws Exception {
            // Given
            LoggingRule rule = new LoggingRule(
                    "/**",
                    null,
                    null,
                    new RequestConfig(true, null, new HeadersConfig(false, null, null)),
                    new ResponseConfig(true, null, new HeadersConfig(false, null, null)));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            request.addHeader("Accept", "application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(listAppender.list).hasSize(2);
            for (var event : listAppender.list) {
                var keys = event.getKeyValuePairs().stream().map(kvp -> kvp.key).toList();
                assertThat(keys)
                        .doesNotContain(
                                "platform.rest-server.http.request.headers",
                                "platform.rest-server.http.response.headers");
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
}
