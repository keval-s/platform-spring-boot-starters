# AGENTS.md

## Project Overview

**Tech Stack:** Java 26, Spring Boot 4.0.x, Maven 3.9+

A collection of opinionated Spring Boot Starters providing autoconfiguration for RESTful server features, REST/GraphQL
clients,
messaging consumers/producers, etc.

### Modules

- `platform-spring-boot-starters` — Root aggregator module with packaging type `pom`
- `platform-spring-boot-starter-dependencies` — BOM centralising dependency versions
- `platform-spring-boot-starter-parent` — Consumer-facing parent POM
- `platform-spring-boot-starter-build-parent` — Internal build parent for starter modules
- `starters/platform-spring-boot-starter-rest-server` — Auto-configuration for RESTful server features (e.g. structured
  request/response logging)
- `starters/platform-spring-boot-starter-rest-client` — Auto-configuration for REST clients with features like endpoint
  catalogues, retry policies, and logging
- `examples/restful-web-service-example` — Example application demonstrating the REST server starter
- `examples/rest-client-example` — Example application demonstrating the REST client starter

---

## 1. Communication Guidelines

- Skip pleasantries and get straight to the point.
- Be concise but informative.
- Stop if a task is ambiguous or requires assumptions — ask for clarification.
- Be direct, not diplomatic — say what you mean without softening or hedging.

---

## 2. Boundaries & Prohibitions

- **Secrets:** Never commit or suggest hardcoded API keys, passwords, or tokens.
- **Git:** Do not run `git push` or `git merge` unless explicitly asked.
- **Deletions:** Do not delete any files or code without confirming with the user.

---

## 3. Core Build & Run Commands

```bash
# Build everything (excluding example modules)
mvn clean install

# Build everything (including example modules)
mvn clean install -Pexamples

# Run an example application
mvn spring-boot:run

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ClassName

# Check for dependency updates
mvn versions:display-dependency-updates
```
