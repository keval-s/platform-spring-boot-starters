# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

#### platform-spring-boot-starter-rest-client

- Added `RestClientAutoConfiguration` registering a `PlatformRestClientRegistry` bean (`platformRestClientRegistry`)
  that holds one pre-built `RestClient` and `RetryTemplate` per named client
- Added `PlatformRestClient`, a typed endpoint-aware client obtained from
  `PlatformRestClientRegistry#getPlatformRestClient` that provides HTTP-method helpers (`get`, `post`, `put`, `patch`,
  `delete`) and an `exchange` method that uses the HTTP method configured on the endpoint; all calls are automatically
  wrapped in the client's retry policy with endpoint-level headers and query parameters applied
- Added `platform.rest.client` configuration properties for base URL, endpoints (HTTP method + path), default headers,
  default query parameters, timeouts, retry (max attempts, back-off, retryable status codes), and SSL bundle references
- Added `PlatformHttpStatusRetryException` thrown by the built-in status handler when a configured retryable HTTP status
  code is received
- Added `DefaultQueryParamsInterceptor` that appends client-level default query parameters to every outbound request

#### Cross-cutting

- Added `AGENTS.md` documenting project conventions, coding standards, and AI agent instructions
- Added AI agent skills under `.agents/skills/` covering module creation, Spring and Resilience4j version management,
  test writing, and documentation writing
- Added root `README.md`
- Added `platform-spring-boot-starter-dependencies` BOM centralising Spring Boot, Spring Cloud, Resilience4j, and
  springdoc-openapi dependency versions
- Added `platform-spring-boot-starter-build-parent` internal build parent POM for starter modules
- Added `platform-spring-boot-starter-parent` consumer-facing parent POM for downstream service modules
- Added `.editorconfig` defining IDE-independent code style rules
- Added Spring Boot 4.0.6 as parent POM of `platform-spring-boot-starter-build-parent`
- Registered `restful-web-service-example` in the `examples` Maven profile of the root aggregator `pom.xml`
- Configured `maven-compiler-plugin` in `platform-spring-boot-starter-build-parent` with Lombok and
  `spring-boot-configuration-processor` annotation processors
- Added Lombok as a provided dependency to `platform-spring-boot-starter-build-parent`
- Added `.sdkmanrc` with Java 26 + updated across all POMs

#### platform-spring-boot-starter-rest-server

- Added `RestServerAutoConfiguration` providing structured request/response logging via
  `StandardRequestResponseLoggingFilter`
- Added `platform.rest.server` configuration properties for enabling and customising logging rules
- Configured `maven-surefire-plugin` with Mockito `-javaagent` to suppress self-attach warning on modern JDKs

#### restful-web-service-example

- Added example application demonstrating the `platform-spring-boot-starter-rest-server` starter with an Orders REST API

