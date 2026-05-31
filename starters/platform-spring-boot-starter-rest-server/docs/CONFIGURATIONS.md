# Configuration Reference — platform-spring-boot-starter-rest-server

## Overview

All configuration for this starter is bound under the `platform.rest.server` prefix via
`RestServerProperties`. The starter activates only in a web application context
(`@ConditionalOnWebApplication`). The `StandardRequestResponseLoggingFilter` bean is
registered only when `platform.rest.server.logging.enabled=true`
(`@ConditionalOnProperty`); omitting or setting it to `false` prevents the bean from
being created entirely.

---

## Properties Reference

### `RestServerProperties`

Top-level configuration record. Prefix: `platform.rest.server`.

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging` | `RestServerLoggingProperties` | — | Nested logging configuration block. |

---

### `RestServerLoggingProperties`

Controls the request/response logging filter. Prefix: `platform.rest.server.logging`.

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging.enabled` | `Boolean` | `false` | Master switch. When `false` (or omitted) the `StandardRequestResponseLoggingFilter` bean is **not registered**. |
| `platform.rest.server.logging.rules` | `List<LoggingRule>` | — | Ordered list of path/method rules. Rules are evaluated top-to-bottom; the **first matching rule wins**. When no rules are configured every request is logged (method + URI + status, no payload). |

---

### `LoggingRule`

A single path/method logging rule. Prefix: `platform.rest.server.logging.rules[*]`.

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging.rules[*].path` | `String` | — | Ant-style URI pattern (e.g. `/api/**`) that this rule applies to. |
| `platform.rest.server.logging.rules[*].methods` | `List<String>` | — | HTTP methods this rule applies to (e.g. `[GET, POST]`). Omit or leave empty to match **all** methods. |
| `platform.rest.server.logging.rules[*].enabled` | `Boolean` | `true` | Set to `false` to silence all logging for requests matching this rule. |
| `platform.rest.server.logging.rules[*].request` | `PayloadConfig` | — | Request-body logging settings. `null` means no request body is captured. |
| `platform.rest.server.logging.rules[*].response` | `PayloadConfig` | — | Response-body logging settings. `null` means no response body is captured. |

---

### `PayloadConfig`

Controls whether a request or response body is included in the log entry.
Prefix: `platform.rest.server.logging.rules[*].request` or `.response`.

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging.rules[*].request.enabled` | `Boolean` | `false` | Set to `true` to capture and log the request body. |
| `platform.rest.server.logging.rules[*].response.enabled` | `Boolean` | `false` | Set to `true` to capture and log the response body. |

---

## Valid Combinations

### Logging disabled globally

Set `platform.rest.server.logging.enabled=false` (or omit the property entirely).
The `StandardRequestResponseLoggingFilter` bean is **not registered** — there is zero
overhead at runtime. All `rules` configuration is ignored.

### Logging enabled, no rules

Set `platform.rest.server.logging.enabled=true` and omit `rules`.
Every request/response is logged at INFO level with method, URI, and status code.
No payload bodies are captured.

### Logging enabled with rules

Rules are evaluated **in declaration order**; the **first matching rule wins**.
A request that matches no rule is still logged (method + URI + status only, no payload).

| Scenario | `enabled` on rule | `request.enabled` | `response.enabled` | Outcome |
|---|---|---|---|---|
| Silence a path | `false` | — | — | No log entries for that path. |
| Log metadata only | `true` (or omit) | `false` (or omit) | `false` (or omit) | Method + URI + status logged; no bodies. |
| Log request body | `true` | `true` | `false` (or omit) | Request body included in log entry. |
| Log response body | `true` | `false` (or omit) | `true` | Response body included in log entry. |
| Log both bodies | `true` | `true` | `true` | Both bodies included in log entries. |

---

## Complete Examples

### Minimal — logging disabled

```yaml
platform:
  rest:
    server:
      logging:
        enabled: false
```

No filter bean is registered; no log output is produced.

---

### Logging enabled, all requests logged (no rules)

```yaml
platform:
  rest:
    server:
      logging:
        enabled: true
```

Every inbound request is logged with its method, URI, and response status.
No payload bodies are captured.

---

### Logging enabled with per-path rules

```yaml
platform:
  rest:
    server:
      logging:
        enabled: true
        rules:
          - path: /actuator/**
            enabled: false                  # silence all actuator traffic
          - path: /api/**
            methods: [POST, PUT, PATCH]
            request:
              enabled: true
            response:
              enabled: true
          - path: /api/**                   # GET / DELETE on /api/** — metadata only
```

Rule evaluation:

1. Requests to `/actuator/**` — skipped entirely (`enabled: false`).
2. `POST /api/orders` — matches rule 2; request and response bodies are logged.
3. `GET /api/orders` — skips rule 2 (wrong method); matches rule 3; metadata only.
4. `GET /health` — no rule matches; metadata only (method + URI + status).

