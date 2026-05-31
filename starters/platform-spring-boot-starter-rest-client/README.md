# platform-spring-boot-starter-rest-client

Auto-configures named REST clients backed by Spring's `RestClient`, providing consumers
with a single autowirable `PlatformRestClientRegistry` bean that holds one pre-built
`RestClient` and one `RetryTemplate` per service configured under
`platform.rest.client.clients`.

## Installation

Add the dependency to your `pom.xml` (versions are managed by the BOM):

```xml
<dependency>
    <groupId>com.kevshah</groupId>
    <artifactId>platform-spring-boot-starter-rest-client</artifactId>
</dependency>
```

Your project's parent POM should be `platform-spring-boot-starter-parent` (or import
`platform-spring-boot-starter-dependencies` as a BOM).

## Features

- Declares any number of named REST clients under `platform.rest.client.clients.*`.
- Each client is pre-configured with a base URL, default `Content-Type` / `Accept` headers,
  and optional default query parameters.
- Per-client endpoint catalogue: HTTP method, URL path, per-endpoint header overrides, and
  default query parameters.
- Retry support via Spring Retry — configurable max attempts, fixed or exponential back-off,
  and automatic retry on specified HTTP response status codes (e.g. `429`, `503`).
- SSL support via Spring Boot SSL Bundles — reference any bundle defined under
  `spring.ssl.bundle.*`.
- Custom connection and read timeouts per client.
- Single `PlatformRestClientRegistry` bean registered as `platformRestClientRegistry` —
  no naming collisions with application-defined beans.
- Auto-configured; zero boilerplate required beyond adding the dependency and YAML properties.

## Usage

Inject `PlatformRestClientRegistry` and call `client(name)` to obtain a
`PlatformRestClient` scoped to one configured service. All HTTP-method helpers
automatically apply retry, endpoint-level headers, and default query parameters.

```java
@Service
public class PaymentService {

    private final PlatformRestClient paymentClient;

    public PaymentService(PlatformRestClientRegistry registry) {
        this.paymentClient = registry.client("payment-service");
    }

    public PaymentResponse createPayment(PaymentRequest request) {
        return paymentClient.post("create-payment", request, PaymentResponse.class);
    }

    public PaymentResponse getPayment(String id) {
        return paymentClient.get("get-payment", Map.of("id", id), PaymentResponse.class);
    }

    public List<PaymentResponse> listPayments() {
        // Static default-query-params from YAML (e.g. page=0&size=20) are added automatically
        return paymentClient.get("list-payments", new ParameterizedTypeReference<>() {});
    }
}
```

### Query parameters

There are two ways to attach query parameters — they can be combined freely:

**Static defaults (YAML)** — fixed values appended automatically to every call targeting
that client or endpoint. Configured via `default-query-params` at the client or endpoint
level:

```yaml
platform:
  rest:
    client:
      clients:
        payment-service:
          base-url: https://payment.example.com
          default-query-params:
            api-version: "2"            # every request from this client
          endpoints:
            list-payments:
              method: GET
              path: /api/v1/payments
              default-query-params:
                page: "0"               # every call to this endpoint only
                size: "20"
```

**Dynamic per-call (RFC 6570)** — values known only at call time. Declare them as
RFC 6570 query expansion variables in the endpoint `path`, then pass values in the
`uriVariables` map:

```yaml
endpoints:
  search-payments:
    method: GET
    path: /api/v1/payments{?status,page,size}
```

```java
var results = paymentClient.get(
    "search-payments",
    Map.of("status", "PENDING", "page", "2", "size", "10"),
    new ParameterizedTypeReference<List<PaymentResponse>>() {}
);
// → GET /api/v1/payments?status=PENDING&page=2&size=10&api-version=2
```

See [docs/CONFIGURATIONS.md](docs/CONFIGURATIONS.md#query-parameters) for the full
query parameter reference and precedence rules.

## Configuration

All behaviour is controlled through `application.yml` properties under the
`platform.rest.client` prefix.

See [docs/CONFIGURATIONS.md](docs/CONFIGURATIONS.md) for the full property reference
and example configurations.

## See Also

- [platform-spring-boot-starter-dependencies](../../platform-spring-boot-starter-dependencies/README.md)
- [Root README](../../README.md)

