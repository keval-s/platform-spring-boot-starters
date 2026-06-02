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
| `platform.rest.server.logging` | `LoggingProperties` | — | Nested logging configuration block. |

---

### `LoggingProperties`

Controls the request/response logging filter. Prefix: `platform.rest.server.logging`.

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging.enabled` | `Boolean` | `false` | Master switch. When `false` (or omitted) the `StandardRequestResponseLoggingFilter` bean is **not registered**. |
| `platform.rest.server.logging.rules` | `List<LoggingRule>` | — | Ordered list of path/method rules. Rules are evaluated top-to-bottom; the **first matching rule wins**. When no rules are configured every request/response is logged (method + URI + status, no payload or headers). |

---

### `LoggingRule`

A single path/method logging rule. Prefix: `platform.rest.server.logging.rules[*]`.

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging.rules[*].path` | `String` | — | Ant-style URI pattern (e.g. `/api/**`) that this rule applies to. |
| `platform.rest.server.logging.rules[*].methods` | `List<String>` | — | HTTP methods this rule applies to (e.g. `[GET, POST]`). Omit or leave empty to match **all** methods. |
| `platform.rest.server.logging.rules[*].enabled` | `Boolean` | `true` | Set to `false` to silence **all** logging (request and response) for requests matching this rule. |
| `platform.rest.server.logging.rules[*].request` | `RequestConfig` | — | Per-request logging settings. `null` means request log entry is emitted with basic info (method + URI) only. |
| `platform.rest.server.logging.rules[*].response` | `ResponseConfig` | — | Per-response logging settings. `null` means response log entry is emitted with basic info (method + URI + status) only. |

---

### `RequestConfig`

Controls what is included in the request log entry for a matching rule.
Prefix: `platform.rest.server.logging.rules[*].request`.

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging.rules[*].request.enabled` | `Boolean` | `true` | Set to `false` to suppress the request log entry entirely for this rule. |
| `platform.rest.server.logging.rules[*].request.payload` | `PayloadConfig` | — | Request body logging settings. `null` means the request body is not captured. |
| `platform.rest.server.logging.rules[*].request.headers` | `HeadersConfig` | — | Request header logging settings. `null` means no headers are logged. |

---

### `ResponseConfig`

Controls what is included in the response log entry for a matching rule.
Prefix: `platform.rest.server.logging.rules[*].response`.

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging.rules[*].response.enabled` | `Boolean` | `true` | Set to `false` to suppress the response log entry entirely for this rule. |
| `platform.rest.server.logging.rules[*].response.payload` | `PayloadConfig` | — | Response body logging settings. `null` means the response body is not captured. |
| `platform.rest.server.logging.rules[*].response.headers` | `HeadersConfig` | — | Response header logging settings. `null` means no headers are logged. |

---

### `PayloadConfig`

Controls whether the request or response body is included in the log entry.
Prefix: `platform.rest.server.logging.rules[*].request.payload` or `.response.payload`.

| Property | Type | Default | Description |
|---|---|---|---|
| `enabled` | `Boolean` | `false` | Set to `true` to capture and include the body in the log entry. |

---

### `HeadersConfig`

Controls which headers are included in the log entry.
Prefix: `platform.rest.server.logging.rules[*].request.headers` or `.response.headers`.

| Property | Type | Default | Description |
|---|---|---|---|
| `enabled` | `Boolean` | `false` | Set to `true` to include headers in the log entry. |
| `include` | `List<String>` | — | Header names to include (case-insensitive). When empty or `null`, **all** headers are included (subject to `exclude`). |
| `exclude` | `List<String>` | — | Header names to exclude (case-insensitive). Applied after `include`. A header in both lists is excluded. |

---

## Valid Combinations

### Logging disabled globally

Set `platform.rest.server.logging.enabled=false` (or omit the property entirely).
The `StandardRequestResponseLoggingFilter` bean is **not registered** — there is zero
overhead at runtime. All `rules` configuration is ignored.

### Logging enabled, no rules

Set `platform.rest.server.logging.enabled=true` and omit `rules`.
Every request/response is logged at INFO level with method, URI, and status code.
No payload bodies or headers are captured.

### Logging enabled with rules

Rules are evaluated **in declaration order**; the **first matching rule wins**.
A request that matches no rule is still logged (method + URI + status only, no payload or headers).

| Scenario | Rule `enabled` | `request.enabled` | `response.enabled` | `request.payload.enabled` | `response.payload.enabled` | Outcome |
|---|---|---|---|---|---|---|
| Silence a path | `false` | — | — | — | — | No log entries for that path. |
| Log metadata only | `true` (or omit) | `true` (or omit) | `true` (or omit) | `false` (or omit) | `false` (or omit) | Method + URI + status logged; no bodies or headers. |
| Log request body | `true` | `true` | `true` (or omit) | `true` | `false` (or omit) | Request body included in request log entry. |
| Log response body | `true` | `true` (or omit) | `true` | `false` (or omit) | `true` | Response body included in response log entry. |
| Log both bodies | `true` | `true` | `true` | `true` | `true` | Both bodies included in their respective log entries. |
| Suppress request log | `true` | `false` | `true` | — | — | Only the response log entry is emitted. |
| Log request headers | `true` | `true` | `true` (or omit) | — | — | Request headers logged (subject to `include`/`exclude`). |

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
No payload bodies or headers are captured.

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
              payload:
                enabled: true
              headers:
                enabled: true
                exclude: [Authorization, Cookie]
            response:
              enabled: true
              payload:
                enabled: true
          - path: /api/**                   # GET / DELETE on /api/** — metadata only
```

Rule evaluation:

1. Requests to `/actuator/**` — skipped entirely (`enabled: false`).
2. `POST /api/orders` — matches rule 2; request body and selected headers logged, response body logged.
3. `GET /api/orders` — skips rule 2 (wrong method); matches rule 3; metadata only.
4. `GET /health` — no rule matches; metadata only (method + URI + status).
