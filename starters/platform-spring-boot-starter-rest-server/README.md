# platform-spring-boot-starter-rest-server

Auto-configures common REST server concerns for Spring Boot web applications,
including structured request/response logging with fine-grained per-path rules
and an OpenAPI UI powered by springdoc-openapi.

## Installation

Add the dependency to your `pom.xml` (versions are managed by the BOM):

```xml
<dependency>
    <groupId>com.kevshah</groupId>
    <artifactId>platform-spring-boot-starter-rest-server</artifactId>
</dependency>
```

Your project's parent POM should be `platform-spring-boot-starter-parent` (or import
`platform-spring-boot-starter-dependencies` as a BOM).

## Features

- Structured request and response logging via `StandardRequestResponseLoggingFilter`.
- Per-path, per-method logging rules evaluated in declaration order (first match wins).
- Selective payload capture for request and/or response bodies.
- Master on/off switch (`platform.rest.server.logging.enabled`) — when `false` the filter
  bean is not registered at all, adding zero runtime overhead.
- Auto-configured; zero boilerplate required beyond adding the dependency.
- OpenAPI UI provided by springdoc-openapi (available at `/swagger-ui.html` by default).

## Configuration

All behaviour is controlled through `application.yml` properties under the
`platform.rest.server` prefix.

See [docs/CONFIGURATIONS.md](docs/CONFIGURATIONS.md) for the full property reference
and example configurations.

## See Also

- [platform-spring-boot-starter-dependencies](../../platform-spring-boot-starter-dependencies/README.md)
- [platform-spring-boot-starter-rest-client](../platform-spring-boot-starter-rest-client/README.md)
- [restful-web-service-example](../../examples/restful-web-service-example/README.md)

