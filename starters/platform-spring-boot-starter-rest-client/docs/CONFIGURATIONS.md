# Configuration Reference — platform-spring-boot-starter-rest-client

## Overview

All properties live under the `platform.rest.client` prefix, which is derived mechanically
from the artifact ID (`platform-spring-boot-starter-rest-client` → strip prefix, replace
`-` with `.`, prepend `platform.`).

The configuration model is a map of named clients:

```
platform.rest.client.clients.<client-name>.*
```

Each `<client-name>` key creates one `RestClient` and one `RetryTemplate` instance inside
the auto-configured `PlatformRestClientRegistry` bean (`platformRestClientRegistry`).

The auto-configuration activates whenever `spring-web` (i.e. `RestClient`) is on the
classpath, regardless of whether any clients are configured.

---

## Properties Reference

### `RestClientProperties` — `platform.rest.client`

Top-level record.

| Property                       | Type                            | Default | Description                                                         |
|--------------------------------|---------------------------------|---------|---------------------------------------------------------------------|
| `platform.rest.client.clients` | `Map<String, ClientProperties>` | `—`     | Named REST client definitions. Each key is the logical client name. |

---

### `ClientProperties` — `platform.rest.client.clients.<name>`

Per-client configuration.

| Property                                               | Type                              | Default | Description                                                                      |
|--------------------------------------------------------|-----------------------------------|---------|----------------------------------------------------------------------------------|
| `platform.rest.client.clients[*].base-url`             | `String`                          | `—`     | Base URL of the external service (e.g. `https://payment.example.com`).           |
| `platform.rest.client.clients[*].default-content-type` | `String`                          | `—`     | Default `Content-Type` header applied to every request from this client.         |
| `platform.rest.client.clients[*].default-accept`       | `String`                          | `—`     | Default `Accept` header applied to every request from this client.               |
| `platform.rest.client.clients[*].default-query-params` | `Map<String, String>`             | `—`     | Default query parameters appended to every request from this client.             |
| `platform.rest.client.clients[*].connect-timeout`      | `Duration`                        | `—`     | TCP connection timeout (e.g. `5s`, `500ms`).                                     |
| `platform.rest.client.clients[*].read-timeout`         | `Duration`                        | `—`     | Response read timeout (e.g. `30s`).                                              |
| `platform.rest.client.clients[*].endpoints`            | `Map<String, EndpointProperties>` | `—`     | Named endpoint definitions for this client.                                      |
| `platform.rest.client.clients[*].retry.*`              | `RetryProperties`                 | `—`     | Retry configuration. When absent, requests are attempted exactly once.           |
| `platform.rest.client.clients[*].ssl.*`                | `SslProperties`                   | `—`     | SSL bundle reference. When absent, the JVM's default SSL context is used.        |
| `platform.rest.client.clients[*].logging.*`            | `LoggingProperties`               | `—`     | Client-level logging configuration. When absent, no structured logs are emitted. |

---

### `EndpointProperties` — `platform.rest.client.clients.<name>.endpoints.<endpoint-name>`

Metadata for a single logical operation on a client.

| Property                                                            | Type                  | Default | Description                                                                                                                                                                   |
|---------------------------------------------------------------------|-----------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `platform.rest.client.clients[*].endpoints[*].method`               | `String`              | `—`     | HTTP method (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`).                                                                                                     |
| `platform.rest.client.clients[*].endpoints[*].path`                 | `String`              | `—`     | URL path relative to `base-url`. May contain URI template variables (e.g. `/api/v1/payments/{id}`) and RFC 6570 query expansion (e.g. `/api/v1/payments{?status,page,size}`). |
| `platform.rest.client.clients[*].endpoints[*].default-query-params` | `Map<String, String>` | `—`     | Endpoint-level default query parameters, merged on top of client-level `default-query-params`.                                                                                |
| `platform.rest.client.clients[*].endpoints[*].content-type`         | `String`              | `—`     | Per-endpoint `Content-Type` override. Takes precedence over `default-content-type`.                                                                                           |
| `platform.rest.client.clients[*].endpoints[*].accept`               | `String`              | `—`     | Per-endpoint `Accept` override. Takes precedence over `default-accept`.                                                                                                       |
| `platform.rest.client.clients[*].endpoints[*].logging.*`            | `LoggingProperties`   | `—`     | Endpoint-level logging overrides. Non-`null` fields take precedence over the client-level `logging.*` settings.                                                               |

---

### `RetryProperties` — `platform.rest.client.clients.<name>.retry`

Controls retry behaviour for a client.

| Property                                                           | Type            | Default | Description                                                                              |
|--------------------------------------------------------------------|-----------------|---------|------------------------------------------------------------------------------------------|
| `platform.rest.client.clients[*].retry.max-attempts`               | `Integer`       | `3`     | Total number of attempts (first attempt + retries).                                      |
| `platform.rest.client.clients[*].retry.wait-duration`              | `Duration`      | `500ms` | Base wait duration between retry attempts.                                               |
| `platform.rest.client.clients[*].retry.multiplier`                 | `Double`        | `1.0`   | Back-off multiplier. Values greater than `1.0` enable exponential back-off.              |
| `platform.rest.client.clients[*].retry.max-wait-duration`          | `Duration`      | `—`     | Maximum wait cap when exponential back-off is active. Ignored when `multiplier` ≤ `1.0`. |
| `platform.rest.client.clients[*].retry.retry-on-response-statuses` | `List<Integer>` | `—`     | HTTP response status codes that trigger a retry (e.g. `429`, `503`, `504`).              |

---

### `SslProperties` — `platform.rest.client.clients.<name>.ssl`

References a named SSL bundle for mutual TLS or custom trust stores.

| Property                                     | Type     | Default | Description                                                |
|----------------------------------------------|----------|---------|------------------------------------------------------------|
| `platform.rest.client.clients[*].ssl.bundle` | `String` | `—`     | Name of an SSL bundle defined under `spring.ssl.bundle.*`. |

---

### `LoggingProperties` — `platform.rest.client.clients.<name>.logging` / `…endpoints.<endpoint-name>.logging`

Controls structured request/response logging for a client or a single endpoint. All fields
are optional. At the endpoint level, only the fields that are explicitly set override the
corresponding client-level values — unset fields inherit from the client.

| Property                            | Type           | Default | Description                                                                                                                                             |
|-------------------------------------|----------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `…logging.enabled`                  | `Boolean`      | `false` | Enables structured log entries for every request and response in this scope.                                                                            |
| `…logging.level`                    | `String`       | `INFO`  | SLF4J log level for the emitted entries. Accepted values (case-insensitive): `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`.                                 |
| `…logging.request.payload.enabled`  | `Boolean`      | `false` | Includes the serialized request body in the request log entry.                                                                                          |
| `…logging.request.headers.enabled`  | `Boolean`      | `false` | Includes request headers in the request log entry.                                                                                                      |
| `…logging.request.headers.include`  | `List<String>` | `—`     | Allowlist of request header names to log. When absent or empty, all headers are logged (subject to `exclude`).                                          |
| `…logging.request.headers.exclude`  | `List<String>` | `—`     | Denylist of request header names to suppress. Takes precedence over `include`.                                                                          |
| `…logging.response.payload.enabled` | `Boolean`      | `false` | Includes the response body in the response log entry. Enabling this buffers the entire response body in memory before it is handed to the deserializer. |
| `…logging.response.headers.enabled` | `Boolean`      | `false` | Includes response headers in the response log entry.                                                                                                    |
| `…logging.response.headers.include` | `List<String>` | `—`     | Allowlist of response header names to log. When absent or empty, all headers are logged (subject to `exclude`).                                         |
| `…logging.response.headers.exclude` | `List<String>` | `—`     | Denylist of response header names to suppress. Takes precedence over `include`.                                                                         |

When `logging` is configured at both the client level and the endpoint level, the values are
merged: each non-`null` field in the endpoint configuration takes precedence over the
corresponding client-level field.

---

## Valid Combinations

### Query parameters

There are two independent mechanisms for attaching query parameters to requests.

#### Static default query parameters (configured in YAML)

Both `ClientProperties` and `EndpointProperties` accept a `default-query-params` map.
These parameters are appended automatically to every request — no call-site code is
required.

Precedence and merging:

| Level                                          | Scope                          | Behaviour                                                 |
|------------------------------------------------|--------------------------------|-----------------------------------------------------------|
| `clients[*].default-query-params`              | Every request from that client | Applied first                                             |
| `clients[*].endpoints[*].default-query-params` | Every request to that endpoint | Applied on top; same key overrides the client-level value |

Example — client-level param always present, endpoint-level param adds pagination
defaults:

```yaml
platform:
  rest:
    client:
      clients:
        inventory-service:
          base-url: https://inventory.example.com
          default-query-params:
            api-version: "2"           # sent with every request from this client
          endpoints:
            list-items:
              method: GET
              path: /api/v1/items
              default-query-params:
                page: "0"              # default pagination, only on this endpoint
                size: "20"
            get-item:
              method: GET
              path: /api/v1/items/{id} # no extra query params — only api-version added
```

#### Dynamic per-call query parameters (RFC 6570 query expansion in `path`)

When a query parameter value is only known at call time, declare it as an RFC 6570
query expansion variable in the endpoint `path` and supply its value via the
`uriVariables` map on `PlatformRestClient`:

```yaml
endpoints:
  search-items:
    method: GET
    path: /api/v1/items{?query,page,size}  # RFC 6570 query expansion
```

```java
var results = inventoryClient.get(
        "search-items",
        Map.of("query", "widget", "page", "2", "size", "10"),
        new ParameterizedTypeReference<List<Item>>() {
        }
);
// → GET /api/v1/items?query=widget&page=2&size=10
```

Static `default-query-params` and dynamic RFC 6570 variables can be combined freely.
The static defaults are appended first; the dynamic variables are then expanded by
Spring's `UriBuilder`.

---

### No retry configured

When `retry` is absent for a client, requests are attempted exactly once. Any HTTP error
status not handled by the application propagates normally.

### Logging

When `logging.enabled` is `true` for a client, two structured log entries are emitted
per outbound call via SLF4J:

- **`REST client request`** — before the request is sent. Always includes `http.client`,
  `http.endpoint`, `http.method`, and `http.url`. Optionally includes
  `http.request.headers` and `http.request.body`.
- **`REST client response`** — after the response is received. Always includes
  `http.client`, `http.endpoint`, `http.method`, `http.url`, `http.status`, and
  `http.duration_ms`. Optionally includes `http.response.headers` and
  `http.response.body`.

Endpoint-level `logging` settings override only the fields that are explicitly set;
unset fields fall back to the client-level values. This makes it straightforward to
enable verbose logging for a single noisy endpoint while keeping other endpoints quiet,
or to silence a chatty health-check endpoint without touching the rest of the client.

```yaml
platform:
  rest:
    client:
      clients:
        inventory-service:
          base-url: https://inventory.example.com
          logging:
            enabled: true          # apply to all endpoints of this client
            level: DEBUG
          endpoints:
            list-items:
              method: GET
              path: /api/v1/items
            get-item:
              method: GET
              path: /api/v1/items/{id}
              logging:
                response:
                  payload:
                    enabled: true   # also buffer and log the response body for this endpoint
            health-check:
              method: GET
              path: /actuator/health
              logging:
                enabled: false            # suppress logs for the health-check endpoint
```

> **Memory note:** When `response.payload.enabled` is `true`, the entire response body is
> buffered in memory before it is handed to the deserializer. Avoid enabling this for
> endpoints that return large payloads.

### Retry on specific status codes

When `retry.retry-on-response-statuses` is set, the `RestClient` built for that client
automatically throws `PlatformHttpStatusRetryException` for the listed codes. The
`RetryTemplate` re-executes the callback up to `max-attempts` times.

```
Sequence: attempt 1 → 503 → wait → attempt 2 → 503 → wait → attempt 3 → 200 ✓
```

### Fixed vs. exponential back-off

| `multiplier`    | Back-off strategy                                                                  |
|-----------------|------------------------------------------------------------------------------------|
| absent or `1.0` | Fixed — every wait is exactly `wait-duration`.                                     |
| `> 1.0`         | Exponential — each wait multiplied by `multiplier`, capped at `max-wait-duration`. |

### SSL bundle

When `ssl.bundle` is set, the `ClientHttpRequestFactory` is built with the named SSL
bundle via Spring Boot's `ClientHttpRequestFactorySettings`. The bundle must be declared
under `spring.ssl.bundle.*` in the application configuration.

---

## Complete Examples

### Query parameters — static defaults and dynamic expansion

```yaml
platform:
  rest:
    client:
      clients:
        inventory-service:
          base-url: https://inventory.example.com
          default-content-type: application/json
          default-accept: application/json
          default-query-params:
            api-version: "2"             # appended to every request from this client
          endpoints:
            list-items:
              method: GET
              path: /api/v1/items
              default-query-params:
                page: "0"                # default pagination applied on every call
                size: "20"
            search-items:
              method: GET
              path: /api/v1/items{?query,page,size}   # dynamic params via RFC 6570
            get-item:
              method: GET
              path: /api/v1/items/{id}
```

```java
// Static defaults — no extra code needed
var page = inventoryClient.get("list-items", new ParameterizedTypeReference<List<Item>>() {
        });
// → GET /api/v1/items?api-version=2&page=0&size=20

// Dynamic query params via uriVariables
var results = inventoryClient.get(
        "search-items",
        Map.of("query", "widget", "page", "3", "size", "5"),
        new ParameterizedTypeReference<List<Item>>() {
        }
);
// → GET /api/v1/items?query=widget&page=3&size=5&api-version=2

// Path variable only
var item = inventoryClient.get("get-item", Map.of("id", "item-42"), Item.class);
// → GET /api/v1/items/item-42?api-version=2
```

---

### Minimal — single client, no retry, no SSL

```yaml
platform:
  rest:
    client:
      clients:
        payment-service:
          base-url: https://payment.example.com
          default-content-type: application/json
          default-accept: application/json
          connect-timeout: 5s
          read-timeout: 30s
          endpoints:
            create-payment:
              method: POST
              path: /api/v1/payments
            get-payment:
              method: GET
              path: /api/v1/payments/{id}
            list-payments:
              method: GET
              path: /api/v1/payments
              default-query-params:
                page: "0"
                size: "20"
```

---

### Full — retry with exponential back-off and SSL bundle

```yaml
spring:
  ssl:
    bundle:
      jks:
        payment-service-ssl:
          keystore:
            location: classpath:keystore/payment-service.p12
            password: changeit
            type: PKCS12

platform:
  rest:
    client:
      clients:
        payment-service:
          base-url: https://payment.example.com
          default-content-type: application/json
          default-accept: application/json
          connect-timeout: 5s
          read-timeout: 30s
          default-query-params:
            correlation-id: "platform-client"
          endpoints:
            create-payment:
              method: POST
              path: /api/v1/payments
            get-payment:
              method: GET
              path: /api/v1/payments/{id}
          retry:
            max-attempts: 4
            wait-duration: 500ms
            multiplier: 2.0
            max-wait-duration: 8s
            retry-on-response-statuses:
              - 429
              - 503
              - 504
          ssl:
            bundle: payment-service-ssl

        order-service:
          base-url: https://order.example.com
          default-content-type: application/json
          default-accept: application/json
          endpoints:
            create-order:
              method: POST
              path: /api/v1/orders
            cancel-order:
              method: DELETE
              path: /api/v1/orders/{orderId}
          retry:
            max-attempts: 3
            wait-duration: 1s
            retry-on-response-statuses:
              - 503
```

---

### Logging — client-level with per-endpoint overrides

```yaml
platform:
  rest:
    client:
      clients:
        inventory-service:
          base-url: https://inventory.example.com
          default-content-type: application/json
          default-accept: application/json
          logging:
            enabled: true          # emit request + response logs for all endpoints
            level: DEBUG           # at DEBUG level
          endpoints:
            list-items:
              method: GET
              path: /api/v1/items
            get-item:
              method: GET
              path: /api/v1/items/{id}
              logging:
                response:
                  payload:
                    enabled: true  # additionally buffer and log the response body
            create-item:
              method: POST
              path: /api/v1/items
              logging:
                request:
                  payload:
                    enabled: true   # additionally log the outbound request body
                response:
                  payload:
                    enabled: true
            health-check:
              method: GET
              path: /actuator/health
              logging:
                enabled: false           # suppress logs for the health-check endpoint
``````
