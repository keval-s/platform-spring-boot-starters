package com.kevshah.platform.starter.rest.client.logging;

import com.kevshah.platform.starter.rest.client.config.LoggingProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

/**
 * {@code ClientHttpRequestInterceptor} that emits structured SLF4J log entries for outbound REST client calls.
 *
 * <p>The interceptor reads a {@link LoggingContext} from the {@code "platform.rest.client.logging-context"} request
 * attribute, set by {@code PlatformRestClient} before each call. When no attribute is present, or {@code enabled} is
 * not {@code true}, the request passes through without any additional overhead.
 *
 * <p>Two structured log entries are emitted per call when logging is enabled:
 *
 * <ul>
 *   <li><b>{@code REST client request}</b> &mdash; logged before the request is sent. Always includes
 *       {@code platform.rest-client.name}, {@code platform.rest-client.http.endpoint},
 *       {@code platform.rest-client.http.method}, and {@code platform.rest-client.http.url}. Optionally includes
 *       {@code platform.rest-client.http.request.headers} (when {@code logHeaders} is {@code true}) and
 *       {@code platform.rest-client.http.request.body} (when {@code logRequestBody} is {@code true} and the body is
 *       non-empty).
 *   <li><b>{@code REST client response}</b> &mdash; logged after the response is received. Always includes
 *       {@code platform.rest-client.name}, {@code platform.rest-client.http.endpoint},
 *       {@code platform.rest-client.http.method}, {@code platform.rest-client.http.url},
 *       {@code platform.rest-client.http.status}, and {@code platform.rest-client.http.duration_ms}. Optionally
 *       includes {@code platform.rest-client.http.response.headers} (when {@code logHeaders} is {@code true}) and
 *       {@code platform.rest-client.http.response.body} (when {@code logResponseBody} is {@code true}).
 * </ul>
 *
 * <p>When {@code logResponseBody} is {@code true}, the entire response body is buffered in memory before it is returned
 * to Spring's {@code RestClient} for deserialization.
 *
 * <p>The log level for both entries is controlled by {@code LoggingProperties#level} and defaults to {@code INFO} when
 * not configured.
 */
public final class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    /** Request attribute key under which {@code PlatformRestClient} stores the {@link LoggingContext}. */
    public static final String ATTRIBUTE_KEY = "platform.rest.client.logging-context";

    private static final Logger log = LoggerFactory.getLogger(RestClientLoggingInterceptor.class);

    /**
     * Intercepts an outbound request, logging request and response details when logging is enabled for the call.
     *
     * @param request the outbound HTTP request
     * @param body the serialized request body bytes
     * @param execution the request execution chain
     * @return the HTTP response, potentially wrapped for body buffering
     * @throws IOException if the request execution fails
     */
    @Override
    @NullMarked
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        var loggingContext = (LoggingContext) request.getAttributes().get(ATTRIBUTE_KEY);
        if (loggingContext == null
                || !Boolean.TRUE.equals(loggingContext.config().enabled())) {
            return execution.execute(request, body);
        }

        var config = loggingContext.config();
        var level = resolveLevel(config.level());

        logRequest(request, body, loggingContext, level);

        long startMs = System.currentTimeMillis();
        var rawResponse = execution.execute(request, body);
        long durationMs = System.currentTimeMillis() - startMs;

        if (isResponsePayloadLoggingEnabled(config)) {
            var buffered = new BufferingClientHttpResponse(rawResponse);
            logResponse(
                    request,
                    buffered.getStatusCode(),
                    buffered.getHeaders(),
                    buffered.getBodyAsString(),
                    loggingContext,
                    durationMs,
                    level);
            return buffered;
        }

        logResponse(
                request,
                rawResponse.getStatusCode(),
                rawResponse.getHeaders(),
                null,
                loggingContext,
                durationMs,
                level);
        return rawResponse;
    }

    // Logs the outgoing request as a structured entry.
    private static void logRequest(HttpRequest request, byte[] body, LoggingContext ctx, Level level) {
        var config = ctx.config();
        var builder = log.atLevel(level)
                .addKeyValue(HttpLoggingKeys.CLIENT_NAME, ctx.clientName())
                .addKeyValue(HttpLoggingKeys.ENDPOINT, ctx.endpointName())
                .addKeyValue(HttpLoggingKeys.METHOD, request.getMethod().name())
                .addKeyValue(HttpLoggingKeys.URL, request.getURI().toString());

        if (isRequestHeadersLoggingEnabled(config)) {
            var headersConfig = config.request().headers();
            var filteredHeaders = filterHeaders(request.getHeaders(), headersConfig);
            builder = builder.addKeyValue(HttpLoggingKeys.REQUEST_HEADERS, filteredHeaders.toString());
        }
        if (isRequestPayloadLoggingEnabled(config) && body != null && body.length > 0) {
            builder = builder.addKeyValue(HttpLoggingKeys.REQUEST_BODY, new String(body, StandardCharsets.UTF_8));
        }
        builder.log("REST client request");
    }

    // Logs the received response as a structured entry.
    private static void logResponse(
            HttpRequest request,
            HttpStatusCode statusCode,
            HttpHeaders headers,
            String responseBody,
            LoggingContext ctx,
            long durationMs,
            Level level) {
        var config = ctx.config();
        var builder = log.atLevel(level)
                .addKeyValue(HttpLoggingKeys.CLIENT_NAME, ctx.clientName())
                .addKeyValue(HttpLoggingKeys.ENDPOINT, ctx.endpointName())
                .addKeyValue(HttpLoggingKeys.METHOD, request.getMethod().name())
                .addKeyValue(HttpLoggingKeys.URL, request.getURI().toString())
                .addKeyValue(HttpLoggingKeys.STATUS, statusCode.value())
                .addKeyValue(HttpLoggingKeys.DURATION_MS, durationMs);

        if (isResponseHeadersLoggingEnabled(config)) {
            var headersConfig = config.response().headers();
            var filteredHeaders = filterHeaders(headers, headersConfig);
            builder = builder.addKeyValue(HttpLoggingKeys.RESPONSE_HEADERS, filteredHeaders.toString());
        }
        if (responseBody != null) {
            builder = builder.addKeyValue(HttpLoggingKeys.RESPONSE_BODY, responseBody);
        }
        builder.log("REST client response");
    }

    // Maps a configured level string to an SLF4J Level, defaulting to INFO.
    private static Level resolveLevel(String level) {
        if (level == null) {
            return Level.INFO;
        }
        return switch (level.toUpperCase()) {
            case "TRACE" -> Level.TRACE;
            case "DEBUG" -> Level.DEBUG;
            case "WARN" -> Level.WARN;
            case "ERROR" -> Level.ERROR;
            default -> Level.INFO;
        };
    }

    // Returns true when the request payload should be included in the request log entry.
    private static boolean isRequestPayloadLoggingEnabled(LoggingProperties config) {
        return config.request() != null
                && config.request().payload() != null
                && config.request().payload().isEnabled();
    }

    // Returns true when the response payload should be buffered and included in the response log entry.
    private static boolean isResponsePayloadLoggingEnabled(LoggingProperties config) {
        return config.response() != null
                && config.response().payload() != null
                && config.response().payload().isEnabled();
    }

    // Returns true when request headers should be included in the request log entry.
    private static boolean isRequestHeadersLoggingEnabled(LoggingProperties config) {
        return config.request() != null
                && config.request().headers() != null
                && config.request().headers().isEnabled();
    }

    // Returns true when response headers should be included in the response log entry.
    private static boolean isResponseHeadersLoggingEnabled(LoggingProperties config) {
        return config.response() != null
                && config.response().headers() != null
                && config.response().headers().isEnabled();
    }

    // Returns a new HttpHeaders containing only the entries that pass the HeadersConfig filter.
    private static HttpHeaders filterHeaders(HttpHeaders source, LoggingProperties.HeadersConfig headersConfig) {
        var filtered = new HttpHeaders();
        source.forEach((name, values) -> {
            if (headersConfig.shouldLogHeader(name)) {
                filtered.addAll(name, values);
            }
        });
        return filtered;
    }

    /**
     * Wraps a {@code ClientHttpResponse} and buffers the entire response body in memory, allowing the body to be read
     * twice &mdash; once here for logging and once by Spring's {@code RestClient} for deserialization.
     */
    @NullMarked
    private static final class BufferingClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final byte[] bufferedBody;

        BufferingClientHttpResponse(ClientHttpResponse delegate) throws IOException {
            this.delegate = delegate;
            this.bufferedBody = StreamUtils.copyToByteArray(delegate.getBody());
        }

        /**
         * Returns the buffered response body as a UTF-8 string.
         *
         * @return the buffered response body as a UTF-8 string
         */
        String getBodyAsString() {
            return new String(bufferedBody, StandardCharsets.UTF_8);
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(bufferedBody);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
