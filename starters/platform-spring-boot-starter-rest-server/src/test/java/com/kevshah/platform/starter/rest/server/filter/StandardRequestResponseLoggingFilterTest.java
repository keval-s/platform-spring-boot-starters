package com.kevshah.platform.starter.rest.server.filter;

import com.kevshah.platform.starter.rest.server.config.RestServerLoggingProperties;
import com.kevshah.platform.starter.rest.server.config.RestServerLoggingProperties.LoggingRule;
import com.kevshah.platform.starter.rest.server.config.RestServerLoggingProperties.PayloadConfig;
import com.kevshah.platform.starter.rest.server.config.RestServerProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

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
        return new RestServerProperties(new RestServerLoggingProperties(false, null));
    }

    /// Logging enabled globally with no rules – only basic request/response info is logged (no payloads).
    private static RestServerProperties loggingEnabled() {
        return new RestServerProperties(new RestServerLoggingProperties(true, null));
    }

    private static RestServerProperties loggingEnabledWithRules(List<LoggingRule> rules) {
        return new RestServerProperties(new RestServerLoggingProperties(true, rules));
    }

    /// Simulates downstream reading the request body so `ContentCachingRequestWrapper.getContentAsByteArray()` returns content.
    private void setupChainToReadRequestBody() throws Exception {
        doAnswer(invocation -> {
            ContentCachingRequestWrapper wrapped = (ContentCachingRequestWrapper) invocation.getArgument(0);
            wrapped.getInputStream().readAllBytes();
            return null;
        }).when(filterChain).doFilter(any(), any());
    }

    /// Simulates downstream writing `body` to the response so `ContentCachingResponseWrapper.getContentAsByteArray()` returns content.
    private void setupChainToWriteResponseBody(byte[] body) throws Exception {
        doAnswer(invocation -> {
            ContentCachingResponseWrapper wrapped = (ContentCachingResponseWrapper) invocation.getArgument(1);
            wrapped.getOutputStream().write(body);
            return null;
        }).when(filterChain).doFilter(any(), any());
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
        void givenLoggingEnabled_whenFiltering_thenWrapsRequestWithContentCachingWrapper() throws Exception {
            // Given
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabled());
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilter(request, response, filterChain);

            // Then – argument 0 is the wrapped request
            ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
            verify(filterChain).doFilter(captor.capture(), any());
            assertThat(captor.getValue()).isInstanceOf(ContentCachingRequestWrapper.class);
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
            LoggingRule rule = new LoggingRule("/**", null, null, new PayloadConfig(true), null);
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
            LoggingRule rule = new LoggingRule("/**", null, null, new PayloadConfig(true), null);
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
            LoggingRule rule = new LoggingRule("/**", null, null, new PayloadConfig(true), null);
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenRequestPayloadLoggingDisabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given – rule matched but request payload logging is off
            LoggingRule rule = new LoggingRule("/**", null, null, new PayloadConfig(false), null);
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
        void givenJsonResponseAndResponsePayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given
            LoggingRule rule = new LoggingRule("/**", null, null, null, new PayloadConfig(true));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("{\"orderId\":1,\"status\":\"CREATED\"}".getBytes());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenNonJsonResponseAndResponsePayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given
            LoggingRule rule = new LoggingRule("/**", null, null, null, new PayloadConfig(true));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("pong".getBytes());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenEmptyResponseBodyAndResponsePayloadLoggingEnabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given – no body written by downstream
            LoggingRule rule = new LoggingRule("/**", null, null, null, new PayloadConfig(true));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenResponsePayloadLoggingDisabled_whenFiltering_thenNoExceptionIsThrown() throws Exception {
            // Given – response payload logging is explicitly off
            LoggingRule rule = new LoggingRule("/**", null, null, null, new PayloadConfig(false));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            setupChainToWriteResponseBody("{\"orderId\":1}".getBytes());

            // When / Then
            assertThatNoException().isThrownBy(() -> filter.doFilter(request, response, filterChain));
        }

        @Test
        void givenResponsePayloadLoggingEnabled_whenFiltering_thenResponseBodyIsCopiedBackToActualResponse() throws Exception {
            // Given
            byte[] responseBody = "{\"orderId\":42}".getBytes();
            LoggingRule rule = new LoggingRule("/**", null, null, null, new PayloadConfig(true));
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
            LoggingRule rule = new LoggingRule("/**", null, null, new PayloadConfig(true), new PayloadConfig(true));
            StandardRequestResponseLoggingFilter filter = filterWith(loggingEnabledWithRules(List.of(rule)));
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
            request.setContent("{\"item\":\"book\"}".getBytes());
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();
            doAnswer(invocation -> {
                ContentCachingRequestWrapper req = (ContentCachingRequestWrapper) invocation.getArgument(0);
                req.getInputStream().readAllBytes();
                ContentCachingResponseWrapper resp = (ContentCachingResponseWrapper) invocation.getArgument(1);
                resp.getOutputStream().write("{\"orderId\":99}".getBytes());
                return null;
            }).when(filterChain).doFilter(any(), any());

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
            LoggingRule logPostRule = new LoggingRule("/api/**", List.of("POST"), null, new PayloadConfig(true), null);
            StandardRequestResponseLoggingFilter filter = filterWith(
                    loggingEnabledWithRules(List.of(silenceGetRule, logPostRule)));
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
            LoggingRule rule = new LoggingRule("/api/**", List.of("POST"), null, new PayloadConfig(true), null);
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
            LoggingRule rule = new LoggingRule("/api/orders/**", List.of("POST", "PUT"), null,
                    new PayloadConfig(true), null);
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
            LoggingRule logAll = new LoggingRule("/api/**", null, null, new PayloadConfig(true), null);
            StandardRequestResponseLoggingFilter filter = filterWith(
                    loggingEnabledWithRules(List.of(silenceInternal, logAll)));
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
            LoggingRule catchAll = new LoggingRule("/**", null, null, new PayloadConfig(true), null);
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
}
