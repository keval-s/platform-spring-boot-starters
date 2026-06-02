package com.kevshah.platform.starter.rest.server.filter;

import com.kevshah.platform.starter.rest.server.config.LoggingProperties;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties.LoggingRule;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties.RequestConfig;
import com.kevshah.platform.starter.rest.server.config.LoggingProperties.ResponseConfig;
import com.kevshah.platform.starter.rest.server.config.RestServerProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/// Servlet filter that logs incoming HTTP requests and outgoing HTTP responses.
///
/// Logging behaviour is governed by [RestServerProperties] — it can be disabled globally or
/// fine-tuned per path and HTTP method via [LoggingProperties.LoggingRule] entries.
///
/// Each rule may independently suppress or enable request/response logging via
/// [LoggingProperties.RequestConfig] and [LoggingProperties.ResponseConfig], and can
/// optionally include the payload body ([LoggingProperties.PayloadConfig]) or selected
/// headers ([LoggingProperties.HeadersConfig]) in the log entry.
@Slf4j
public class StandardRequestResponseLoggingFilter extends OncePerRequestFilter {

    private final RestServerProperties properties;
    private final JsonMapper jsonMapper;

    /// Creates a new filter with the given server properties and JSON mapper.
    ///
    /// @param properties server-level configuration including the logging rules
    /// @param jsonMapper mapper used to parse payload bodies into structured JSON for log output
    public StandardRequestResponseLoggingFilter(RestServerProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    /// Applies request/response logging around the filter chain for the current request.
    ///
    /// The "Incoming HTTP request" entry is logged **before** `filterChain.doFilter` is called
    /// so that it always precedes any log entries executed during controller/service execution (e.g. REST client calls).
    ///
    /// When request body logging is enabled the body is eagerly buffered by a
    /// [CachedBodyRequestWrapper] so it can be included in that pre-chain log entry and later
    /// replayed to downstream handlers (e.g. `@RequestBody` deserialisation).
    ///
    /// @param request     the current HTTP request
    /// @param response    the current HTTP response
    /// @param filterChain the remaining filter chain
    /// @throws ServletException if a servlet error occurs during filter processing
    /// @throws IOException      if an I/O error occurs during filter processing
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (properties.logging() == null || !Boolean.TRUE.equals(properties.logging().enabled())) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<LoggingRule> matchedRule = findFirstMatchingRule(request.getRequestURI(), request.getMethod());

        // A matched rule with enabled=false means: skip all logging for this request
        if (matchedRule.isPresent() && !matchedRule.get().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        var requestConfig = matchedRule.map(LoggingRule::request).orElse(null);
        var responseConfig = matchedRule.map(LoggingRule::response).orElse(null);

        // Log the request/response unless the per-direction enabled flag is explicitly false.
        // When no rule matches (requestConfig == null) we default to logging basic info.
        boolean logRequestEnabled = requestConfig == null || !Boolean.FALSE.equals(requestConfig.enabled());
        boolean logResponseEnabled = responseConfig == null || !Boolean.FALSE.equals(responseConfig.enabled());

        boolean logRequestPayload = logRequestEnabled && requestConfig != null
                && requestConfig.payload() != null && requestConfig.payload().isEnabled();
        boolean logResponsePayload = logResponseEnabled && responseConfig != null
                && responseConfig.payload() != null && responseConfig.payload().isEnabled();

        // When request body logging is enabled, eagerly buffer the body so it is available
        // for the pre-chain log entry and can be replayed for downstream handlers.
        HttpServletRequest requestToProcess = logRequestPayload
                ? new CachedBodyRequestWrapper(request)
                : request;

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Log the incoming request BEFORE the chain so this entry always precedes any
        // logs emitted during controller/service execution (e.g. REST client calls). The response is logged AFTER the chain so that status,
        // headers and body reflect the final values set by the controller rather than the defaults at the start of the request.
        if (logRequestEnabled) {
            logRequest(requestToProcess, logRequestPayload, requestConfig);
        }

        try {
            filterChain.doFilter(requestToProcess, wrappedResponse);
            if (logResponseEnabled) {
                logResponse(requestToProcess, wrappedResponse, logResponsePayload, responseConfig);
            }
        } finally {
            // Always copy the cached response body back to the actual response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(HttpServletRequest request, boolean logPayload, RequestConfig requestConfig) {
        var builder = log.atInfo();
        if (logPayload && request instanceof CachedBodyRequestWrapper cached) {
            String raw = new String(cached.getCachedBody(), StandardCharsets.UTF_8);
            if (!raw.isBlank()) {
                builder = addPayload(builder, raw, HttpLoggingKeys.REQUEST_BODY);
            }
        }
        if (requestConfig != null && requestConfig.headers() != null && requestConfig.headers().isEnabled()) {
            builder = addRequestHeaders(builder, request, requestConfig.headers());
        }
        builder.addKeyValue(HttpLoggingKeys.METHOD, request.getMethod())
                .addKeyValue(HttpLoggingKeys.URL, request.getRequestURI())
                .log("Incoming HTTP request");
    }

    private void logResponse(HttpServletRequest request, ContentCachingResponseWrapper response, boolean logPayload, ResponseConfig responseConfig) {
        var builder = log.atInfo();
        if (logPayload) {
            byte[] body = response.getContentAsByteArray();
            if (body.length > 0) {
                String raw = new String(body, StandardCharsets.UTF_8);
                if (!raw.isBlank()) {
                    builder = addPayload(builder, raw, HttpLoggingKeys.RESPONSE_BODY);
                }
            }
        }
        if (responseConfig != null && responseConfig.headers() != null && responseConfig.headers().isEnabled()) {
            builder = addResponseHeaders(builder, response, responseConfig.headers());
        }
        builder.addKeyValue(HttpLoggingKeys.METHOD, request.getMethod())
                .addKeyValue(HttpLoggingKeys.URL, request.getRequestURI())
                .addKeyValue(HttpLoggingKeys.STATUS, response.getStatus())
                .log("Outgoing HTTP response");
    }

    /// Parses `raw` as JSON when possible and attaches it under `key`; falls back to the raw
    /// string for non-JSON bodies (plain text, form data, etc.).
    private org.slf4j.spi.LoggingEventBuilder addPayload(org.slf4j.spi.LoggingEventBuilder builder, String raw, String key) {
        try {
            return builder.addKeyValue(key, jsonMapper.readValue(raw, Object.class));
        } catch (JacksonException e) {
            return builder.addKeyValue(key, raw);
        }
    }

    /// Collects request headers that pass the [LoggingProperties.HeadersConfig] filter and
    /// attaches them as a map under [HttpLoggingKeys.REQUEST_HEADERS].
    /// Returns `builder` unchanged when no headers pass the filter or enumeration is unavailable.
    private LoggingEventBuilder addRequestHeaders(
            LoggingEventBuilder builder,
            HttpServletRequest request,
            LoggingProperties.HeadersConfig headersConfig) {
        var headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return builder;
        }
        var headers = new LinkedHashMap<String, String>();
        Collections.list(headerNames).forEach(name -> {
            if (headersConfig.shouldLogHeader(name)) {
                headers.put(name, request.getHeader(name));
            }
        });
        return headers.isEmpty() ? builder : builder.addKeyValue(HttpLoggingKeys.REQUEST_HEADERS, headers);
    }

    /// Collects response headers that pass the [LoggingProperties.HeadersConfig] filter and
    /// attaches them as a map under [HttpLoggingKeys.RESPONSE_HEADERS].
    /// Returns `builder` unchanged when no headers pass the filter.
    private LoggingEventBuilder addResponseHeaders(
            LoggingEventBuilder builder,
            ContentCachingResponseWrapper response,
            LoggingProperties.HeadersConfig headersConfig) {
        var headers = new LinkedHashMap<String, String>();
        response.getHeaderNames().forEach(name -> {
            if (headersConfig.shouldLogHeader(name)) {
                headers.put(name, response.getHeader(name));
            }
        });
        return headers.isEmpty() ? builder : builder.addKeyValue(HttpLoggingKeys.RESPONSE_HEADERS, headers);
    }

    /// Finds the first [LoggingRule] whose `path` and `methods` match the given URI and HTTP method.
    ///
    /// Rules are evaluated in declaration order; the first match wins.
    /// Returns an empty `Optional` when no rule matches.
    private Optional<LoggingRule> findFirstMatchingRule(String uri, String method) {
        List<LoggingRule> rules = properties.logging().rules();
        if (rules == null || rules.isEmpty()) {
            return Optional.empty();
        }
        PathContainer path = PathContainer.parsePath(uri);
        PathPatternParser parser = PathPatternParser.defaultInstance;
        return rules.stream()
                .filter(rule -> {
                    PathPattern compiled = parser.parse(rule.path());
                    return compiled.matches(path) && rule.matchesMethod(method);
                })
                .findFirst();
    }

    /// `HttpServletRequestWrapper` that eagerly reads the entire request body into an in-memory
    /// buffer on construction, then replays it from the buffer on every subsequent
    /// `getInputStream` or `getReader` call.
    ///
    /// This allows the body to be logged before the filter chain runs while still being
    /// fully available to downstream handlers such as `@RequestBody` deserialisation.
    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        /// Returns the eagerly buffered request body bytes.
        byte[] getCachedBody() {
            return body;
        }

        /// Returns a fresh `ServletInputStream` backed by the buffered body on every call,
        /// allowing the body to be read any number of times.
        @Override
        public ServletInputStream getInputStream() {
            var stream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return stream.read();
                }

                @Override
                public boolean isFinished() {
                    return stream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("setReadListener is not supported");
                }
            };
        }

        /// Returns a `BufferedReader` over the buffered body, respecting the request character encoding.
        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
