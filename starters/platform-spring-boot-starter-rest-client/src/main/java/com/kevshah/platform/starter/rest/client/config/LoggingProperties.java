package com.kevshah.platform.starter.rest.client.config;

import java.util.List;

/**
 * Request/response logging configuration for a REST client or a single endpoint.
 *
 * <p>All fields are optional ({@code null} means "not configured"). At the client level, {@code null} fields are
 * treated as their effective defaults: {@code enabled} &rarr; {@code false}, {@code level} &rarr; {@code INFO},
 * {@code request} and {@code response} &rarr; no payload or header logging. At the endpoint level, {@code null} fields
 * inherit the client-level setting via {@link LoggingProperties#merge}.
 *
 * @param enabled Whether to emit request and response log entries for this scope. Defaults to {@code false} when
 *     {@code null} at the client level, or inherits the client-level value at the endpoint level.
 * @param level SLF4J log level for the emitted entries. Accepted values (case-insensitive): {@code TRACE},
 *     {@code DEBUG}, {@code INFO}, {@code WARN}, {@code ERROR}. Defaults to {@code INFO} when {@code null}.
 * @param request Request logging settings. When {@code null}, no request payload or headers are logged beyond the basic
 *     structured fields.
 * @param response Response logging settings. When {@code null}, no response payload or headers are logged beyond the
 *     basic structured fields.
 */
public record LoggingProperties(Boolean enabled, String level, RequestConfig request, ResponseConfig response) {

    /**
     * Merges two {@code LoggingProperties} instances, with {@code override} taking precedence over {@code base}.
     *
     * <p>For each scalar field ({@code enabled}, {@code level}) a non-{@code null} value in {@code override} replaces
     * the corresponding field in {@code base}. Nested {@code RequestConfig} and {@code ResponseConfig} values are
     * merged shallowly &mdash; a non-{@code null} nested field in {@code override} replaces the corresponding field in
     * {@code base}. When both inputs are {@code null}, returns {@code null}.
     *
     * @param base the client-level configuration; may be {@code null}
     * @param override the endpoint-level configuration; may be {@code null}
     * @return the merged configuration, or {@code null} when both inputs are {@code null}
     */
    public static LoggingProperties merge(LoggingProperties base, LoggingProperties override) {
        if (base == null && override == null) {
            return null;
        }
        if (base == null) {
            return override;
        }
        if (override == null) {
            return base;
        }
        return new LoggingProperties(
                override.enabled() != null ? override.enabled() : base.enabled(),
                override.level() != null ? override.level() : base.level(),
                mergeRequestConfig(base.request(), override.request()),
                mergeResponseConfig(base.response(), override.response()));
    }

    // Merges two RequestConfig values, preferring non-null fields from override.
    private static RequestConfig mergeRequestConfig(RequestConfig base, RequestConfig override) {
        if (base == null && override == null) {
            return null;
        }
        if (base == null) {
            return override;
        }
        if (override == null) {
            return base;
        }
        return new RequestConfig(
                override.payload() != null ? override.payload() : base.payload(),
                override.headers() != null ? override.headers() : base.headers());
    }

    // Merges two ResponseConfig values, preferring non-null fields from override.
    private static ResponseConfig mergeResponseConfig(ResponseConfig base, ResponseConfig override) {
        if (base == null && override == null) {
            return null;
        }
        if (base == null) {
            return override;
        }
        if (override == null) {
            return base;
        }
        return new ResponseConfig(
                override.payload() != null ? override.payload() : base.payload(),
                override.headers() != null ? override.headers() : base.headers());
    }

    /**
     * Configuration for request logging settings.
     *
     * @param payload Request body logging settings. When {@code null}, the request body is not logged.
     * @param headers Request header logging settings. When {@code null}, no request headers are logged.
     */
    public record RequestConfig(PayloadConfig payload, HeadersConfig headers) {}

    /**
     * Configuration for response logging settings.
     *
     * @param payload Response body logging settings. When {@code null}, the response body is not logged. Enabling
     *     payload logging buffers the entire response body in memory before it is handed to the deserializer.
     * @param headers Response header logging settings. When {@code null}, no response headers are logged.
     */
    public record ResponseConfig(PayloadConfig payload, HeadersConfig headers) {}

    /**
     * Configuration for payload (request or response body) logging.
     *
     * @param enabled Set to {@code true} to capture and include the payload in the log entry. When {@code false} or
     *     {@code null}, the payload is not logged.
     */
    public record PayloadConfig(Boolean enabled) {

        /**
         * Returns {@code true} when payload logging is switched on.
         *
         * @return {@code true} if enabled
         */
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    /**
     * Configuration for header logging.
     *
     * @param enabled Set to {@code true} to capture and include headers in the log entry. When {@code false} or
     *     {@code null}, no headers are logged.
     * @param include List of header names to include in the log entry. When empty or {@code null}, all headers are
     *     included (subject to {@code enabled}).
     * @param exclude List of header names to exclude from the log entry. When empty or {@code null}, no headers are
     *     excluded. If a header appears in both {@code include} and {@code exclude}, it is excluded.
     */
    public record HeadersConfig(Boolean enabled, List<String> include, List<String> exclude) {

        /**
         * Returns {@code true} when header logging is switched on.
         *
         * @return {@code true} if enabled
         */
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }

        /**
         * Returns {@code true} when the given header name should be logged according to the {@code include} and
         * {@code exclude} lists.
         *
         * <p>Header name matching is case-insensitive. When {@code include} is empty or {@code null}, all headers are
         * included (subject to {@code enabled}), except those listed in {@code exclude}.
         *
         * @param headerName the HTTP header name to evaluate
         * @return {@code true} when the header should appear in the log entry
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
            return true;
        }
    }
}
