---
name: create-example-module
description: Guide for creating a new runnable example application under examples/, including all required files, POM wiring, and documentation.
---

## Overview

All example applications live under `examples/` and are runnable Spring Boot applications
whose sole purpose is to demonstrate one or more platform starters in a realistic context.
They are **not** published as library artifacts.

Creating a new example involves:

1. Scaffolding the module directory and source tree.
2. Writing the `pom.xml` following the established template.
3. Creating the `@SpringBootApplication` main class.
4. Adding an `application.yml` (and a profile-specific variant if needed).
5. Registering the module in two places: the root aggregator POM profile and the root `README.md`.
6. Writing module documentation (`README.md`).
7. Adding a changelog entry via the `update-changelog` skill.

---

## 1. Naming Convention

Example artifact IDs and directory names describe what the application demonstrates,
not which starter it uses:

```
<concern>-example
```

Examples: `restful-web-service-example`, `graphql-api-example`, `messaging-consumer-example`.

The name must be lowercase and hyphen-separated.

---

## 2. Directory Structure

Create the following layout under `examples/`:

```
examples/
└── <name>-example/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/kevshah/example/<namewithnohyphens>/
        │   │       ├── Application.java          ← @SpringBootApplication entry point
        │   │       └── <feature>/                ← feature sub-packages (controllers, etc.)
        │   └── resources/
        │       ├── application.yml               ← base config (may be empty or minimal)
        │       └── application-dev.yml           ← dev-profile config with logging, etc.
        └── test/
            └── java/
                └── com/kevshah/example/<namewithnohyphens>/
                    └── (test classes)
```

Rules:
- The Java base package is `com.kevshah.example.<namewithnohyphens>` where `<namewithnohyphens>`
  is the short name with all hyphens removed (e.g., `restful-web-service` →
  `com.kevshah.example.restfulwebservice`).
- **Always** place the `@SpringBootApplication` main class directly in the base package,
  named `Application`.
- Sub-packages follow the same conventions as starter modules: `rest/controllers/`,
  `rest/requests/`, `rest/responses/`, etc.
- Example modules **must** have a `@SpringBootApplication` main class — they are runnable
  applications, not libraries.

---

## 3. `pom.xml`

Model the `pom.xml` exactly on the template below. Only add dependencies that the example
genuinely needs to demonstrate its feature.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.kevshah</groupId>
        <artifactId>platform-spring-boot-starter-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../platform-spring-boot-starter-parent/pom.xml</relativePath>
    </parent>

    <artifactId><name>-example</artifactId>

    <dependencies>
        <!-- The starter(s) this example demonstrates -->
        <dependency>
            <groupId>com.kevshah</groupId>
            <artifactId>platform-spring-boot-starter-<short-name></artifactId>
        </dependency>

        <!--Testing dependencies-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

Key rules:
- **Parent** is always `platform-spring-boot-starter-parent` with `relativePath` pointing
  two directories up (`../../platform-spring-boot-starter-parent/pom.xml`).
- **No `<groupId>` or `<version>`** on the module itself — both are inherited from the
  parent.
- **No `<properties>` block** — Java version and encoding are inherited from the parent.
  Only add a `<properties>` block if the example genuinely needs to override something.
- All dependency versions are managed by the BOM (via `platform-spring-boot-starter-parent`);
  never specify a `<version>` on a managed dependency.
- The `spring-boot-maven-plugin` is **required** — it creates the executable fat JAR and
  enables `mvn spring-boot:run`.
- If the example uses Lombok, add it as an optional dependency and configure
  `maven-compiler-plugin` with an `annotationProcessorPaths` entry (see
  `restful-web-service-example/pom.xml` for the exact snippet).

---

## 4. `Application.java`

Every example must have a `@SpringBootApplication` main class in its base package:

```java
package com.kevshah.example.<namewithnohyphens>;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Rules:
- The class must be named `Application`.
- Place it directly in the base package (`com.kevshah.example.<namewithnohyphens>`), not
  in a sub-package, so that Spring's component scan covers the entire module.
- Do **not** add a `///` Javadoc comment to `Application` — it is boilerplate.

---

## 5. `application.yml` and Profile Configuration

### `application.yml` (base)

A minimal base configuration that sets the application name and suppresses the Spring
Boot banner:

```yaml
spring:
  application:
    name: <name>-example
  main:
    banner-mode: off
```

### `application-dev.yml` (dev profile)

A dev-profile file for local development. Include logging configuration and any
platform starter properties needed to exercise the feature being demonstrated:

```yaml
logging:
  level:
    root: INFO
    com.kevshah.example.<namewithnohyphens>: DEBUG
  structured:
    format:
      console: ecs

# platform starter configuration goes here
# e.g.:
# platform:
#   rest:
#     server:
#       logging:
#         enabled: true
```

Rules:
- Split base and dev config into separate files so that the example can be run cleanly in
  CI (base only) or locally with verbose output (`-Pdev` / `--spring.profiles.active=dev`).
- Never commit secrets, passwords, or API keys to any configuration file.

---

## 6. Wiring the Module into the Repository

Two locations must be updated every time a new example is added.

### 6a. Root aggregator `pom.xml` — `examples` profile

Add a `<module>` entry inside the `<profile>` with `<id>examples</id>` in the root
`pom.xml`. Keep entries in alphabetical order:

```xml
<profiles>
    <profile>
        <id>examples</id>
        <modules>
            <module>examples/<name>-example</module>         <!-- ADD THIS -->
            <module>examples/restful-web-service-example</module>
        </modules>
    </profile>
</profiles>
```

The `examples` profile keeps example modules out of the default build. Activate it when
needed:

```bash
mvn clean install -Pexamples
```

### 6b. Verify the build

After wiring, run a full build including examples to confirm nothing is broken:

```bash
mvn clean install -Pexamples
```

---

## 7. Documentation

Creating a new example requires updates to two documentation locations. Follow the
`write-documentation` skill for the full authoring guide; the requirements are summarised
below.

### 7a. New module `README.md`

Create `examples/<name>-example/README.md` following the **example application module**
template in the `write-documentation` skill (Section 3). Required sections:

1. **Title & description** — artifact ID as the heading; one short paragraph describing
   what the example demonstrates.
2. **Running** — the exact commands to start the application locally:
   ```bash
   cd examples/<name>-example
   mvn spring-boot:run -Pdev
   ```
   Include the default port and any notable URLs (e.g., Swagger UI).
3. **Purpose** — a brief note that this module is for demonstration and manual testing
   only; it is not published as a library artifact.
4. **See Also** — links to the starter(s) the example demonstrates and to the root
   `README.md`.

### 7b. Root `README.md` updates

Update the root `README.md` in two places:

1. **Repository Structure** tree — add a line for the new module under `examples/`:
   ```
   │   └── <name>-example/   # <one-line description>
   ```

2. **Modules → Examples table** — add a row, keeping rows alphabetical by artifact ID:
   ```markdown
   | `<name>-example` | <One-sentence description.> |
   ```

---

## 8. Changelog Entry

Add a changelog entry via the `update-changelog` skill:

- Type: `### Added`
- Group: `#### <name>-example`
- Entry: `Added example application demonstrating <brief description of what is shown>`

---

## 9. Quick-Reference Checklist

- [ ] Directory created at `examples/<name>-example/`.
- [ ] `pom.xml` uses `platform-spring-boot-starter-parent` as parent with the correct
  `relativePath` (`../../platform-spring-boot-starter-parent/pom.xml`).
- [ ] No `<groupId>`, `<version>`, or `<properties>` block unless genuinely needed.
- [ ] `spring-boot-maven-plugin` declared in the `<build>` section.
- [ ] `Application.java` created in `com.kevshah.example.<namewithnohyphens>` with
  `@SpringBootApplication` and a `main` method.
- [ ] `src/main/resources/application.yml` created with application name and banner off.
- [ ] `src/main/resources/application-dev.yml` created with logging and starter config.
- [ ] `<module>` entry added to the `examples` profile in the root `pom.xml`.
- [ ] `mvn clean install -Pexamples` passes from the repo root.
- [ ] `examples/<name>-example/README.md` created with all required sections.
- [ ] Root `README.md` structure tree and Examples table updated.
- [ ] Changelog entry added under `### Added` / `#### <name>-example`.

