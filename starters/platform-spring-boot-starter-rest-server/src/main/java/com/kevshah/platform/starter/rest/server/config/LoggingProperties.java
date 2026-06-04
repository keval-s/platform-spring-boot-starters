package com.kevshah.platform.starter.rest.server.config;

import java.util.List;

/// Top-level logging configuration for the REST server starter.
///
/// - `enabled` – master switch; when `false` no logging is performed at all.
/// - `rules` – ordered list of path/method rules evaluated top-to-bottom.
///   The **first** matching rule wins.
///
/// Example YAML:
/// ```yaml
/// platform:
///   rest:
///     server:
///       logging:
///         enabled: true
///         rules:
///           - path: /api/health
///             enabled: false             # silence all logging for this path
///           - path: /api/**
///             methods: [POST, PUT, PATCH]
///             request:
///               enabled: true
///               payload:
///                 enabled: true
///               headers:
///                 enabled: true
///                 exclude: [Authorization, Cookie]  # omit sensitive headers
///             response:
///               enabled: true
///               payload:
///                 enabled: true
///               headers:
///                 enabled: true
///                 include: [Content-Type, X-Request-Id]  # log only these headers
///           - path: /api/**              # all other methods – no payload or header logging
/// ```
///
/// @param enabled Master switch for REST server logging. When `false`, no logging is performed regardless of any rules.
/// @param rules   Ordered list of path/method logging rules. The first matching rule is applied to each request. If no
/// rules match, no logging is performed for that request.
public record LoggingProperties(Boolean enabled, List<LoggingRule> rules) {

    /// A single path/method logging rule.
    ///
    /// @param path     Ant-style URI pattern (e.g. `/api/**`) that this rule applies to.
    /// @param methods  HTTP methods this rule applies to. Omit or leave empty to match _all_ methods.
    /// @param enabled  Set to `false` to silence _all_ logging for matching requests. Defaults to `true`.
    /// @param request  Request-payload logging settings; `null` means no payload is logged.
    /// @param response Response-payload logging settings; `null` means no payload is logged.
    public record LoggingRule(
            String path, List<String> methods, Boolean enabled, RequestConfig request, ResponseConfig response) {

        /// Returns `true` unless `enabled` is explicitly set to `false`.
        public boolean isEnabled() {
            return !Boolean.FALSE.equals(enabled);
        }

        /// Returns `true` when this rule has no method restriction.
        public boolean matchesAllMethods() {
            return methods == null || methods.isEmpty();
        }

        /// Returns `true` when this rule matches the given HTTP method (case-insensitive).
        public boolean matchesMethod(String httpMethod) {
            return matchesAllMethods() || methods.stream().anyMatch(m -> m.equalsIgnoreCase(httpMethod));
        }
    }

    /// Configuration for request logging settings within a logging rule.
    ///
    /// @param enabled Set to `true` to log the request; `false` or `null` disables logging
    /// @param payload Request body logging settings. When `null`, the request body is not logged.
    /// @param headers Request header logging settings. When `null`, no request headers are logged.
    public record RequestConfig(Boolean enabled, PayloadConfig payload, HeadersConfig headers) {}

    /// Configuration for response logging settings within a logging rule.
    ///
    /// @param enabled Set to `true` to log the response; `false` or `null` disables logging
    /// @param payload Response body logging settings. When `null`, the response body is not logged.
    /// @param headers Response header logging settings. When `null`, no response headers are logged.
    public record ResponseConfig(Boolean enabled, PayloadConfig payload, HeadersConfig headers) {}

    /// Configuration for payload (request or response body) logging.
    ///
    /// @param enabled Set `true` to log the payload; `false` or `null` disables it.
    public record PayloadConfig(Boolean enabled) {

        /// Returns `true` when payload logging is switched on.
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    /// Configuration for header logging.
    ///
    /// @param enabled Set to `true` to log headers; `false` or `null` disables header logging.
    /// @param include Headers to log. If empty or null, logs all headers (if enabled).
    /// @param exclude Headers to exclude (overrides include). Empty/null excludes none.
    public record HeadersConfig(Boolean enabled, List<String> include, List<String> exclude) {

        /// Returns `true` when header logging is switched on.
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }

        /// Returns `true` when the given header name should be logged according to the `include` and `exclude` lists.
        /// Case-insensitive. Empty/null includes all (if enabled) except excludes.
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
