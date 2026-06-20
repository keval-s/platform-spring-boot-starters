# Project Overview

**Tech Stack:** Java 26, Spring Boot 4.0.x, Maven 3.9+

A collection of opinionated Spring Boot Starters providing autoconfiguration for RESTful server features, REST/GraphQL
clients,
messaging consumers/producers, etc.

## Modules

- `platform-spring-boot-starters` — Root aggregator module with packaging type `pom`
- `platform-spring-boot-starter-dependencies` — BOM centralising dependency versions
- `platform-spring-boot-starter-parent` — Consumer-facing parent POM
  - Parent: `spring-boot-starter-parent`
- `platform-spring-boot-starter-build-parent` — Internal build parent for starter modules
  - Parent: `spring-boot-starter-parent`
- `starters/platform-spring-boot-starter-rest-server` — Auto-configuration for RESTful server features (e.g. structured
  request/response logging)
  - Parent: `platform-spring-boot-starter-build-parent`
- `starters/platform-spring-boot-starter-rest-client` — Auto-configuration for REST clients with features like endpoint
  catalogues, retry policies, and logging
  - Parent: `platform-spring-boot-starter-build-parent`
- `examples/restful-web-service-example` — Example application demonstrating the REST server starter
  - Parent: `platform-spring-boot-starter-parent`
- `examples/rest-client-example` — Example application demonstrating the REST client starter
  - Parent: `platform-spring-boot-starter-parent`

## Communication and Guardrails

- Skip pleasantries and get straight to the point
- No verbose explanations or commentary
- Keep summaries and descriptions concise and focused on the essentials
- Stop if a task is ambiguous or requires assumptions — ask for clarification
- Do not attempt to fix unrelated issues or make improvements outside the scope of the task without explicit
  instructions
- **Secrets:** Never commit or suggest hardcoded API keys, passwords, or tokens.
- **Deletions:** Do not delete any files or code without confirming with the user.
