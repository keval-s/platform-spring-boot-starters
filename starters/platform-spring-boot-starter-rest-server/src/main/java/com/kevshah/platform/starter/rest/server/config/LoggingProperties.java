package com.kevshah.platform.starter.rest.server.config;

import java.util.List;

/**
 * Top-level logging configuration for the REST server starter.
 *
 * <ul>
 *   <li>{@code enabled} &ndash; master switch; when {@code false} no logging is performed at all.
 *   <li>{@code rules} &ndash; ordered list of path/method rules evaluated top-to-bottom. The <b>first</b> matching rule
 *       wins.
 * </ul>
 *
 * <p>Example YAML:
 *
 * <pre>{@code
 * platform:
 *   rest:
 *     server:
 *       logging:
 *         enabled: true
 *         rules:
 *           - path: /api/health
 *             enabled: false             # silence all logging for this path
 *           - path: /api/**
 *             methods: [POST, PUT, PATCH]
 *             request:
 *               enabled: true
 *               payload:
 *                 enabled: true
 *               headers:
 *                 enabled: true
 *                 exclude: [Authorization, Cookie]  # omit sensitive headers
 *             response:
 *               enabled: true
 *               payload:
 *                 enabled: true
 *               headers:
 *                 enabled: true
 *                 include: [Content-Type, X-Request-Id]  # log only these headers
 *           - path: /api/** # all other methods – no payload or header logging
 * }</pre>
 *
 * @param enabled Master switch for REST server logging. When {@code false}, no logging is performed regardless of any
 *     rules.
 * @param rules Ordered list of path/method logging rules. The first matching rule is applied to each request. If no
 *     rules match, no logging is performed for that request.
 */
public record LoggingProperties(Boolean enabled, List<LoggingRule> rules) {

    /**
     * A single path/method logging rule.
     *
     * @param path Ant-style URI pattern (e.g. {@code /api/**}) that this rule applies to.
     * @param methods HTTP methods this rule applies to. Omit or leave empty to match <i>all</i> methods.
     * @param enabled Set to {@code false} to silence <i>all</i> logging for matching requests. Defaults to
     *     {@code true}.
     * @param request Request-payload logging settings; {@code null} means no payload is logged.
     * @param response Response-payload logging settings; {@code null} means no payload is logged.
     */
    public record LoggingRule(
            String path, List<String> methods, Boolean enabled, RequestConfig request, ResponseConfig response) {

        /**
         * Returns {@code true} unless {@code enabled} is explicitly set to {@code false}.
         *
         * @return {@code true} if logging is enabled for this rule, {@code false} otherwise
         */
        public boolean isEnabled() {
            return !Boolean.FALSE.equals(enabled);
        }

        /**
         * Returns {@code true} when this rule has no method restriction.
         *
         * @return {@code true} if this rule applies to all methods
         */
        public boolean matchesAllMethods() {
            return methods == null || methods.isEmpty();
        }

        /**
         * Returns {@code true} when this rule matches the given HTTP method (case-insensitive).
         *
         * @param httpMethod the HTTP method of the incoming request
         * @return {@code true} if the method matches the rule
         */
        public boolean matchesMethod(String httpMethod) {
            return matchesAllMethods() || methods.stream().anyMatch(m -> m.equalsIgnoreCase(httpMethod));
        }
    }

    /**
     * Configuration for request logging settings within a logging rule.
     *
     * @param enabled Set to {@code true} to log the request; {@code false} or {@code null} disables logging
     * @param payload Request body logging settings. When {@code null}, the request body is not logged.
     * @param headers Request header logging settings. When {@code null}, no request headers are logged.
     */
    public record RequestConfig(Boolean enabled, PayloadConfig payload, HeadersConfig headers) {}

    /**
     * Configuration for response logging settings within a logging rule.
     *
     * @param enabled Set to {@code true} to log the response; {@code false} or {@code null} disables logging
     * @param payload Response body logging settings. When {@code null}, the response body is not logged.
     * @param headers Response header logging settings. When {@code null}, no response headers are logged.
     */
    public record ResponseConfig(Boolean enabled, PayloadConfig payload, HeadersConfig headers) {}

    /**
     * Configuration for payload (request or response body) logging.
     *
     * @param enabled Set {@code true} to log the payload; {@code false} or {@code null} disables it.
     */
    public record PayloadConfig(Boolean enabled) {

        /**
         * Returns {@code true} when payload logging is switched on.
         *
         * @return {@code true} if payload logging is enabled
         */
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    /**
     * Configuration for header logging.
     *
     * @param enabled Set to {@code true} to log headers; {@code false} or {@code null} disables header logging.
     * @param include Headers to log. If empty or null, logs all headers (if enabled).
     * @param exclude Headers to exclude (overrides include). Empty/null excludes none.
     */
    public record HeadersConfig(Boolean enabled, List<String> include, List<String> exclude) {

        /**
         * Returns {@code true} when header logging is switched on.
         *
         * @return {@code true} if header logging is enabled
         */
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }

        /**
         * Returns {@code true} when the given header name should be logged according to the {@code include} and
         * {@code exclude} lists.
         *
         * <p>Case-insensitive. Empty/null includes all (if enabled) except excludes.
         *
         * @param headerName the HTTP header name to check
         * @return {@code true} if the header should be logged
         */
        public boolean shouldLogHeader(String headerName) {
            if (!isEnabled()) {
                return false;
            }
            if (exclude != null && exclude.stream().anyMatch(h -> h.equalsIgnoreCase(headerName))) {
                return false;
            }
            if (include != null && !include.isEmpty()) {
                return include.stream().anyMatch(h -> h.equalsIgnoreCase(headerName));
            }
            return true; // log all headers when include is empty or null
        }
    }
}
