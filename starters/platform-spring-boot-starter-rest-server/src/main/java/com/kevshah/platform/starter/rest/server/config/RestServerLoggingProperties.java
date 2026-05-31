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
/// platform.rest.server.logging:
///   enabled: true
///   rules:
///     - path: /api/health
///       enabled: false             # silence all logging for this path
///     - path: /api/**
///       methods: [POST, PUT, PATCH]
///       request:
///         enabled: true
///       response:
///         enabled: true
///     - path: /api/**              # all other methods – no payload logging
/// ```
public record RestServerLoggingProperties(
        Boolean enabled,
        List<LoggingRule> rules
) {

    /// A single path/method logging rule.
    ///
    /// - `path` – Ant-style URI pattern (e.g. `/api/**`).
    /// - `methods` – HTTP methods this rule applies to. Omit or leave empty to match _all_ methods.
    /// - `enabled` – set to `false` to silence _all_ logging for matching requests. Defaults to `true`.
    /// - `request` – request-payload logging settings; `null` means no payload is logged.
    /// - `response` – response-payload logging settings; `null` means no payload is logged.
    public record LoggingRule(
            String path,
            List<String> methods,
            Boolean enabled,
            PayloadConfig request,
            PayloadConfig response
    ) {

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
            return matchesAllMethods() || methods.stream()
                    .anyMatch(m -> m.equalsIgnoreCase(httpMethod));
        }
    }

    /// Configuration for payload (request or response body) logging.
    ///
    /// - `enabled` – when `true`, the payload is captured and included in the log entry.
    public record PayloadConfig(Boolean enabled) {

        /// Returns `true` when payload logging is switched on.
        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }
}
