package com.kevshah.platform.starter.rest.server.filter;

import com.kevshah.platform.starter.rest.server.config.RestServerLoggingProperties.LoggingRule;
import com.kevshah.platform.starter.rest.server.config.RestServerProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
public class StandardRequestResponseLoggingFilter extends OncePerRequestFilter {

    private final RestServerProperties properties;
    private final JsonMapper jsonMapper;

    public StandardRequestResponseLoggingFilter(RestServerProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

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

        boolean logRequestPayload = matchedRule
                .map(r -> r.request() != null && r.request().isEnabled())
                .orElse(false);

        boolean logResponsePayload = matchedRule
                .map(r -> r.response() != null && r.response().isEnabled())
                .orElse(false);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, Integer.MAX_VALUE);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            // Pass wrapped request/response down the chain so bodies are consumed and cached
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            logRequest(wrappedRequest, logRequestPayload);
            logResponse(wrappedRequest, wrappedResponse, logResponsePayload);
        } finally {
            // Always copy the cached response body back to the actual response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, boolean logPayload) {
        var builder = log.atInfo();
        if (logPayload) {
            String raw = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (!raw.isBlank()) {
                builder = addPayload(builder, raw, "platform.request.payload");
            }
        }
        builder.addKeyValue("platform.method", request.getMethod())
                .addKeyValue("platform.uri", request.getRequestURI())
                .log("Incoming HTTP request");
    }

    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, boolean logPayload) {
        var builder = log.atInfo();
        if (logPayload) {
            byte[] body = response.getContentAsByteArray();
            if (body.length > 0) {
                String raw = new String(body, StandardCharsets.UTF_8);
                if (!raw.isBlank()) {
                    builder = addPayload(builder, raw, "platform.response.payload");
                }
            }
        }
        builder.addKeyValue("platform.method", request.getMethod())
                .addKeyValue("platform.uri", request.getRequestURI())
                .addKeyValue("platform.status", response.getStatus())
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
}
