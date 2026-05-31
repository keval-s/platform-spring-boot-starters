# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
#### Cross-cutting
- Added `AGENTS.md` documenting project conventions, coding standards, and AI agent instructions
- Added AI agent skills under `.agents/skills/` covering module creation, Spring and Resilience4j version management, test writing, and documentation writing
- Added root `README.md`
- Added `platform-spring-boot-starter-dependencies` BOM centralising Spring Boot, Spring Cloud, Resilience4j, and springdoc-openapi dependency versions
- Added `platform-spring-boot-starter-build-parent` internal build parent POM for starter modules
- Added `platform-spring-boot-starter-parent` consumer-facing parent POM for downstream service modules

#### platform-spring-boot-starter-rest-server
- Added `RestServerAutoConfiguration` providing structured request/response logging via `StandardRequestResponseLoggingFilter`
- Added `platform.rest.server` configuration properties for enabling and customising logging rules

#### restful-web-service-example
- Added example application demonstrating the `platform-spring-boot-starter-rest-server` starter with an Orders REST API

### Changed
#### Cross-cutting
- Bumped Java from 25 to 26 across all POMs and `.sdkmanrc`
- Added Spring Boot 4.0.6 as parent POM of `platform-spring-boot-starter-build-parent`
- Added `.editorconfig` defining IDE-independent code style rules
- Added Lombok as a provided dependency to `platform-spring-boot-starter-build-parent`
- Configured `maven-compiler-plugin` in `platform-spring-boot-starter-build-parent` with Lombok and `spring-boot-configuration-processor` annotation processors
- Updated `.gitignore` to exclude HTTP Client response files from version control
- Registered `restful-web-service-example` in the `examples` Maven profile of the root aggregator `pom.xml`

#### platform-spring-boot-starter-rest-server
- Configured `maven-surefire-plugin` with Mockito `-javaagent` to suppress self-attach warning on modern JDKs

