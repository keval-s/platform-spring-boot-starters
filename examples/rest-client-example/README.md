# rest-client-example

Runnable Spring Boot application that demonstrates the `platform-spring-boot-starter-rest-client`
starter by calling the public [JSONPlaceholder](https://jsonplaceholder.typicode.com) API.

No API key or local infrastructure is required — the application works out of the box.

## Running

```bash
cd examples/rest-client-example
mvn spring-boot:run
```

The service starts on `http://localhost:8080`. For local development with ECS-formatted
console logging and request/response logging enabled, activate the `dev` Spring profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Endpoints

Once the application is running, the following endpoints are available:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/posts` | Lists posts (bounded to 10 by the `_limit` default query param) |
| `GET` | `/api/v1/posts/{id}` | Returns a single post by ID |
| `GET` | `/api/v1/users` | Lists all users |
| `GET` | `/api/v1/users/{id}` | Returns a single user by ID |

Swagger UI is available at `http://localhost:8080/swagger-ui.html` and the OpenAPI spec
at `http://localhost:8080/v3/api-docs`.

## What this example demonstrates

| Feature | Where to look |
|---|---|
| Two independent named clients (`posts-client`, `users-client`) | `application.yml` → `platform.rest.client.clients` |
| Static default query parameters (`_limit=10` on `list-posts`) | `application.yml` → `endpoints.list-posts.default-query-params` |
| URI template variable expansion (`/posts/{id}`, `/users/{id}`) | `PostService`, `UserService` — `Map.of("id", id)` |
| Retry with HTTP status triggers (`429`, `503`) on `posts-client` | `application.yml` → `posts-client.retry` |
| Per-client timeouts (`connect-timeout`, `read-timeout`) | `application.yml` → each client block |
| Registry injection and named-client resolution | `PostService(PlatformRestClientRegistry)` constructor |
| Request/response logging (dev profile only) | `application-dev.yml` → `platform.rest.server.logging.enabled` |
| Swagger UI / OpenAPI spec | `http://localhost:8080/swagger-ui.html` |

## Purpose

This module is for demonstration and manual testing only. It is not published as a
library artifact.

## See Also

- [platform-spring-boot-starter-rest-client](../../starters/platform-spring-boot-starter-rest-client/README.md)
- [platform-spring-boot-starter-rest-server](../../starters/platform-spring-boot-starter-rest-server/README.md)
- [Root README](../../README.md)

