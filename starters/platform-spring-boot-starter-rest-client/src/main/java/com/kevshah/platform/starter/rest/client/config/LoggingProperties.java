package com.kevshah.platform.starter.rest.client.config;

import java.util.List;

/// Request/response logging configuration for a REST client or a single endpoint.
///
/// All fields are optional (`null` means "not configured"). At the client level, `null`
/// fields are treated as their effective defaults: `enabled` → `false`, `level` → `INFO`,
/// `request` and `response` → no payload or header logging. At the endpoint level, `null`
/// fields inherit the client-level setting via [LoggingProperties#merge].
///
/// @param enabled  Whether to emit request and response log entries for this scope. Defaults
///                 to `false` when `null` at the client level, or inherits the client-level
///                 value at the endpoint level.
/// @param level    SLF4J log level for the emitted entries. Accepted values (case-insensitive):
///                 `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`. Defaults to `INFO` when `null`.
/// @param request  Request logging settings. When `null`, no request payload or headers are
///                 logged beyond the basic structured fields.
/// @param response Response logging settings. When `null`, no response payload or headers are
///                 logged beyond the basic structured fields.
public record LoggingProperties(Boolean enabled, String level, RequestConfig request, ResponseConfig response) {

    /// Merges two `LoggingProperties` instances, with `override` taking precedence over `base`.
    ///
    /// For each scalar field (`enabled`, `level`) a non-`null` value in `override` replaces the
    /// corresponding field in `base`. Nested `RequestConfig` and `ResponseConfig` values are
    /// merged shallowly — a non-`null` nested field in `override` replaces the corresponding
    /// field in `base`. When both inputs are `null`, returns `null`.
    ///
    /// @param base     the client-level configuration; may be `null`
    /// @param override the endpoint-level configuration; may be `null`
    /// @return the merged configuration, or `null` when both inputs are `null`
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

    /// Configuration for request logging settings.
    ///
    /// @param payload Request body logging settings. When `null`, the request body is not logged.
    /// @param headers Request header logging settings. When `null`, no request headers are logged.
    public record RequestConfig(PayloadConfig payload, HeadersConfig headers) {}

    /// Configuration for response logging settings.
    ///
    /// @param payload Response body logging settings. When `null`, the response body is not
    ///                logged. Enabling payload logging buffers the entire response body in memory
    ///                before it is handed to the deserialiser.
    /// @param headers Response header logging settings. When `null`, no response headers are logged.
    public record ResponseConfig(PayloadConfig payload, HeadersConfig headers) {}

    /// Configuration for payload (request or response body) logging.
    ///
    /// @param enabled Set to `true` to capture and include the payload in the log entry.
    ///                When `false` or `null`, the payload is not logged.
    public record PayloadConfig(Boolean enabled) {

        /// Returns `true` when payload logging is switched on.
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    /// Configuration for header logging.
    ///
    /// @param enabled  Set to `true` to capture and include headers in the log entry.
    ///                 When `false` or `null`, no headers are logged.
    /// @param include  List of header names to include in the log entry. When empty or `null`,
    ///                 all headers are included (subject to `enabled`).
    /// @param exclude  List of header names to exclude from the log entry. When empty or `null`,
    ///                 no headers are excluded. If a header appears in both `include` and
    ///                 `exclude`, it is excluded.
    public record HeadersConfig(Boolean enabled, List<String> include, List<String> exclude) {

        /// Returns `true` when header logging is switched on.
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }

        /// Returns `true` when the given header name should be logged according to the `include`
        /// and `exclude` lists.
        ///
        /// Header name matching is case-insensitive. When `include` is empty or `null`, all
        /// headers are included (subject to `enabled`), except those listed in `exclude`.
        ///
        /// @param headerName the HTTP header name to evaluate
        /// @return `true` when the header should appear in the log entry
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
