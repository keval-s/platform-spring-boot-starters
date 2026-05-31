# platform-spring-boot-starters

Opinionated Spring Boot starters and shared infrastructure for platform services.

## Prerequisites

- Java 26+
- Maven 3.9+

## Quick Start

```bash
git clone https://github.com/keval-s/platform-spring-boot-starters
cd platform-spring-boot-starters
mvn clean install
```

## Repository Structure

```
platform-spring-boot-starters/
├── platform-spring-boot-starter-dependencies/   # BOM — centralised dependency versions
├── platform-spring-boot-starter-parent/          # Consumer-facing parent POM
├── platform-spring-boot-starter-build-parent/    # Internal build parent POM
├── starters/                                      # Auto-configuration starter modules
│   └── platform-spring-boot-starter-rest-server/ # REST server starter
└── examples/                                      # Runnable example applications
    └── restful-web-service-example/               # Example RESTful web service
```

## Modules

### Infrastructure

| Artifact ID | Description |
|---|---|
| `platform-spring-boot-starter-dependencies` | Bill of Materials (BOM). Import this to align all dependency versions. |
| `platform-spring-boot-starter-parent` | Consumer-facing parent POM. Application modules extend this. |
| `platform-spring-boot-starter-build-parent` | Internal build parent used by starter modules. |

### Starters

| Artifact ID | Description |
|---|---|
| `platform-spring-boot-starter-rest-server` | Auto-configures REST server concerns: request/response logging with per-path rules. |

### Examples

| Artifact ID | Description |
|---|---|
| `restful-web-service-example` | Runnable Spring Boot application demonstrating the rest-server starter. |

## Contributing

Follow the coding standards and build commands documented in [AGENTS.md](AGENTS.md).

