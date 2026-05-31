---
name: write-documentation
description: Guide for writing documentation for a project, including README.md, modules documentation, and changelog updates.
---

## Overview

This skill describes how documentation is organised and written across the repository.
Every module ships with its own `README.md`. Starter modules additionally carry a `docs/`
directory that contains deep-dive reference files such as `CONFIGURATIONS.md`.

---

## 1. Repository Layout for Documentation Files

```
platform-spring-boot-starters/          ← repo root
├── README.md                           ← root overview (this doc describes it)
├── CHANGELOG.md
├── platform-spring-boot-starter-dependencies/
│   └── README.md
├── platform-spring-boot-starter-parent/
│   └── README.md
├── platform-spring-boot-starter-build-parent/
│   └── README.md
├── starters/
│   └── platform-spring-boot-starter-<name>/
│       ├── README.md
│       └── docs/
│           └── CONFIGURATIONS.md       ← only when the starter exposes config properties
└── examples/
    └── <example-name>/
        └── README.md
```

Rules:
- Every module **must** have a `README.md` at its root.
- Starter modules that expose `@ConfigurationProperties` **must** have a `docs/CONFIGURATIONS.md`.
- Example modules do **not** need a `docs/` directory.
- Keep all documentation in standard Markdown (`.md`). Do not use AsciiDoc or HTML files.

---

## 2. Root `README.md`

The root `README.md` is the single entry point for anyone discovering the repository.

### Required sections (in order)

1. **Title & one-line description** — `# platform-spring-boot-starters` followed by a
   single sentence describing the repo's purpose.
2. **Prerequisites** — Java version, Maven version, and any other tooling required.
3. **Quick Start** — the minimal steps to clone and build:
   ```bash
   git clone <repo-url>
   cd platform-spring-boot-starters
   mvn clean install
   ```
4. **Repository Structure** — a directory tree annotated with a one-line description of
   every top-level directory and module. Use fenced code blocks for the tree.
5. **Modules** — a Markdown table (or a bullet list with sub-bullets) that lists every
   Maven module, its artifact ID, and a one-sentence description. Group rows under
   **Infrastructure**, **Starters**, and **Examples** sub-headings.
6. **Contributing** — a brief pointer to coding standards (`AGENTS.md`) and the build
   commands from `AGENTS.md §2`.

### Example structure

```markdown
# platform-spring-boot-starters

Opinionated Spring Boot starters and shared infrastructure for platform services.

## Prerequisites

- Java 26+
- Maven 3.9+

## Quick Start

\`\`\`bash
git clone <repo-url>
cd platform-spring-boot-starters
mvn clean install
\`\`\`

## Repository Structure

\`\`\`
platform-spring-boot-starters/
├── platform-spring-boot-starter-dependencies/   # BOM — centralised dependency versions
├── platform-spring-boot-starter-parent/          # Consumer-facing parent POM
├── platform-spring-boot-starter-build-parent/    # Internal build parent POM
├── starters/                                      # Auto-configuration starter modules
│   └── platform-spring-boot-starter-rest-server/ # REST server starter
└── examples/                                      # Runnable example applications
    └── restful-web-service-example/               # Example RESTful web service
\`\`\`

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
```

### Rules

- Do **not** include Maven coordinates, version numbers, or configuration property details
  in the root README — link to the relevant module README or `docs/` file instead.
- Keep module descriptions to one sentence; deeper explanations belong in each module's
  own `README.md`.

---

## 3. Module `README.md`

Each module has its own `README.md` that explains what the module does, how to use it,
and where to find further reference material.

### Required sections (in order)

1. **Title & description** — module artifact ID as the heading; one short paragraph
   describing the module's responsibility.
2. **Installation / Usage** — how a consumer adds the module to their project (Maven
   dependency snippet or parent POM reference).
3. **Features** — unordered list of the capabilities the module provides.
4. **Configuration** *(starters only)* — a short paragraph and a link to
   `docs/CONFIGURATIONS.md`. Do **not** duplicate the full property table here.
5. **See Also** — links to related modules or the root `README.md`.

### Example — starter module

```markdown
# platform-spring-boot-starter-rest-server

Auto-configures common REST server concerns for Spring Boot web applications,
including structured request/response logging with fine-grained per-path rules.

## Installation

Add the dependency to your `pom.xml` (versions are managed by the BOM):

\`\`\`xml
<dependency>
    <groupId>com.kevshah</groupId>
    <artifactId>platform-spring-boot-starter-rest-server</artifactId>
</dependency>
\`\`\`

Your project's parent POM should be `platform-spring-boot-starter-parent` (or import
`platform-spring-boot-starter-dependencies` as a BOM).

## Features

- Structured request and response logging via `StandardRequestResponseLoggingFilter`.
- Per-path, per-method logging rules evaluated in declaration order (first match wins).
- Selective payload capture for request and/or response bodies.
- Master on/off switch (`platform.rest.server.logging.enabled`).
- Auto-configured; zero boilerplate required beyond adding the dependency.
- OpenAPI UI provided by springdoc-openapi.

## Configuration

All behaviour is controlled through `application.yml` properties under the
`platform.rest.server` prefix.

See [docs/CONFIGURATIONS.md](docs/CONFIGURATIONS.md) for the full property reference
and example configurations.

## See Also

- [platform-spring-boot-starter-dependencies](../../platform-spring-boot-starter-dependencies/README.md)
- [restful-web-service-example](../../examples/restful-web-service-example/README.md)
```

### Example — infrastructure POM module

```markdown
# platform-spring-boot-starter-dependencies

Bill of Materials (BOM) that centralizes all dependency versions used across the platform.

## Usage

Import as a BOM in `dependencyManagement`:

\`\`\`xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.kevshah</groupId>
            <artifactId>platform-spring-boot-starter-dependencies</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
\`\`\`

Alternatively, set `platform-spring-boot-starter-parent` as your project's parent POM —
it already imports this BOM.

## Managed versions

| Library | Property |
|---|---|
| Spring Boot | `spring-boot.version` |
| Spring Cloud | `spring-cloud.version` |
| springdoc-openapi | `springdoc.version` |
| Resilience4j | `resilience4j.version` |

## See Also

- [platform-spring-boot-starter-parent](../platform-spring-boot-starter-parent/README.md)
```

### Example — example application module

```markdown
# restful-web-service-example

Runnable Spring Boot application that demonstrates the `platform-spring-boot-starter-rest-server`
starter in a realistic RESTful service context.

## Running

\`\`\`bash
cd examples/restful-web-service-example
mvn spring-boot:run -Pdev
\`\`\`

The service starts on `http://localhost:8080`. The OpenAPI UI is available at
`http://localhost:8080/swagger-ui.html`.

## Purpose

This module is for demonstration and manual testing only. It is not published as a
library artifact.

## See Also

- [platform-spring-boot-starter-rest-server](../../starters/platform-spring-boot-starter-rest-server/README.md)
```

---

## 4. Starter `docs/CONFIGURATIONS.md`

Every starter module that exposes `@ConfigurationProperties` must include
`docs/CONFIGURATIONS.md`. This is the authoritative reference for all supported
properties, their types, defaults, and valid combinations.

### Required sections (in order)

1. **Title** — `# Configuration Reference — <module-name>`.
2. **Overview** — one paragraph describing the overall configuration model (prefix, record
   hierarchy, activation condition).
3. **Properties reference** — one sub-section per `@ConfigurationProperties` record in
   the module. Each sub-section contains:
   - A short description of the record's role.
   - A Markdown table with columns: **Property**, **Type**, **Default**, **Description**.
   - Use the fully-qualified `prefix.field` path in the **Property** column.
4. **Valid combinations** — explicit examples of how properties interact, including any
   conditional activation (`@ConditionalOnProperty`) and rule-priority semantics.
5. **Complete examples** — at least two full YAML snippets showing real-world
   configurations (e.g., "logging disabled", "logging enabled with payload capture").

### Property table format

| Property | Type | Default | Description |
|---|---|---|---|
| `platform.rest.server.logging.enabled` | `Boolean` | `false` | Master switch. When `false` the filter is not registered at all. |

Rules:
- Use the exact property path as it appears in `application.yml` (dot-separated, matching
  the `@ConfigurationProperties` prefix + field names).
- For list-element properties, use the `[*]` wildcard notation:
  `platform.rest.server.logging.rules[*].path`.
- Always document the **default value**. If the field has no default (Spring leaves it
  `null`), write `—` in the Default column.
- Cross-reference other properties in the Description column using inline code
  (`` `platform.rest.server.logging.enabled` ``).

### Valid combinations section

Document every meaningful on/off combination and precedence rule. Follow this format:

```markdown
## Valid Combinations

### Logging disabled globally

Set `platform.rest.server.logging.enabled=false` (or omit the property).
The `StandardRequestResponseLoggingFilter` bean is **not registered** — there is zero
overhead at runtime. All `rules` configuration is ignored.

### Logging enabled, no rules

Set `platform.rest.server.logging.enabled=true` and omit `rules`.
Every request/response is logged at INFO level. No payload bodies are captured.

### Logging enabled with rules

Rules are evaluated **in declaration order**; the **first matching rule wins**.
A request that matches no rule is still logged (method + URI only, no payload).

| Scenario | `enabled` on rule | `request.enabled` | `response.enabled` | Outcome |
|---|---|---|---|---|
| Silence a path | `false` | — | — | No log entries for that path. |
| Log metadata only | `true` (or omit) | `false` (or omit) | `false` (or omit) | Method + URI + status logged; no bodies. |
| Log request body | `true` | `true` | `false` (or omit) | Request body included in log entry. |
| Log response body | `true` | `false` (or omit) | `true` | Response body included in log entry. |
| Log both bodies | `true` | `true` | `true` | Both bodies included in log entries. |
```

### Complete examples section

Provide at least two end-to-end YAML examples:

```markdown
## Complete Examples

### Minimal — logging disabled

\`\`\`yaml
platform:
  rest:
    server:
      logging:
        enabled: false
\`\`\`

No filter is registered; no log output is produced.

---

### Logging enabled with per-path rules

\`\`\`yaml
platform:
  rest:
    server:
      logging:
        enabled: true
        rules:
          - path: /actuator/**
            enabled: false                # silence all actuator traffic
          - path: /api/**
            methods: [POST, PUT, PATCH]
            request:
              enabled: true
            response:
              enabled: true
          - path: /api/**                 # GET / DELETE on /api/** — metadata only
\`\`\`

Rule evaluation:
1. Requests to `/actuator/**` — skipped entirely (`enabled: false`).
2. `POST /api/orders` — matches rule 2; request and response bodies are logged.
3. `GET /api/orders` — skips rule 2 (wrong method); matches rule 3; metadata only.
4. `GET /health` — no rule matches; metadata only (method + URI + status).
```

---

## 5. Changelog Updates

Changelog updates are **out of scope for this skill**. They are governed by the dedicated
`update-changelog` skill.

When writing or reviewing documentation as part of a feature, fix, or version bump, check
whether a `CHANGELOG.md` entry is also required and, if so, follow the `update-changelog`
skill rather than this one.

---

## 6. Quick-Reference Checklist

Before committing documentation, verify:

- [ ] Root `README.md` exists and contains all six required sections.
- [ ] Every module has a `README.md` with the required sections for its type.
- [ ] Every starter that exposes `@ConfigurationProperties` has `docs/CONFIGURATIONS.md`.
- [ ] `CONFIGURATIONS.md` contains a property table, valid-combinations section, and at
  least two complete YAML examples.
- [ ] Property paths in `CONFIGURATIONS.md` match the actual `@ConfigurationProperties`
  prefix + field names (verify against the Java source).
- [ ] The root README module table lists every Maven module declared in the root `pom.xml`.
- [ ] No version numbers or Maven coordinates are hardcoded in prose — they should live in
  the `pom.xml` and be referenced symbolically (e.g., "see the BOM for the current
  version") or pulled from a single source of truth.
- [ ] All Markdown links are relative paths, not absolute URLs, where the target is within
  the same repository.

### Version sync checks

Whenever any documentation mentions a concrete version number, cross-check it against
the single source of truth in the Maven POMs before committing.

| What is documented | Source of truth | Where to verify |
|---|---|---|
| Java version (e.g., "Java 26+") | `<java.version>` property | Root `pom.xml` → `<properties>` |
| Spring Boot version | `spring-boot.version` property | `platform-spring-boot-starter-dependencies/pom.xml` → `<properties>` |
| Spring Cloud version | `spring-cloud.version` property | `platform-spring-boot-starter-dependencies/pom.xml` → `<properties>` |
| springdoc-openapi version | `springdoc.version` property | `platform-spring-boot-starter-dependencies/pom.xml` → `<properties>` |
| Resilience4j version | `resilience4j.version` property | `platform-spring-boot-starter-dependencies/pom.xml` → `<properties>` |
| Project / artifact version | `<version>` on root aggregator | Root `pom.xml` |
| Maven version requirement | Checked manually | `.mvn/wrapper/maven-wrapper.properties` or `AGENTS.md §2` |

Specific rules:

- [ ] The Java version stated in the root `README.md` **Prerequisites** section matches
  `<java.version>` in the root `pom.xml`.
- [ ] The Spring Boot version stated anywhere in documentation matches
  `spring-boot.version` in `platform-spring-boot-starter-dependencies/pom.xml`
  **and** the `<version>` of the `spring-boot-starter-parent` parent in
  `platform-spring-boot-starter-build-parent/pom.xml` (these must always agree).
- [ ] The project version (`1.0.0-SNAPSHOT` or a release tag) stated in any installation
  snippet matches `<version>` in the root `pom.xml`.
- [ ] If a `Managed versions` table appears in an infrastructure module README, every row
  maps to a property that exists in
  `platform-spring-boot-starter-dependencies/pom.xml` — add or remove rows if
  properties are added or removed from that POM.
- [ ] After any version bump (Spring Boot, Java, etc.), grep all `README.md` and
  `CONFIGURATIONS.md` files for hardcoded version strings and update them to match:
  ```bash
  grep -r --include="*.md" '[0-9]\+\.[0-9]' .
  ```
- [ ] The preferred way to avoid stale versions in prose is to **omit the concrete version
  number** and instead tell the reader to check the BOM or the `pom.xml`. Only include a
  concrete version when it is genuinely needed (e.g., a Maven snippet that cannot rely on
  the BOM).

