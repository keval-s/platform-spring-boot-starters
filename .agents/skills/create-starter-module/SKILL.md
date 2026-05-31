---
name: create-starter-module
description: Guide for creating a new Spring Boot starter module in the starters/ directory, including all required files, POM wiring, and documentation.
---

## Overview

All starter modules live under `starters/` and follow a strict, consistent layout.
Creating a new starter involves:

1. Scaffolding the module directory and source tree.
2. Writing the `pom.xml` following the established template.
3. Creating the auto-configuration class and registering it.
4. Optionally wiring `@ConfigurationProperties` and metadata.
5. Registering the module in three places: the root aggregator POM, the BOM, and the
   root `README.md`.
6. Writing module documentation (`README.md` and, where applicable, `docs/CONFIGURATIONS.md`).
7. Adding a changelog entry via the `update-changelog` skill.

---

## 1. Naming Convention

Artifact IDs and directory names always follow the pattern:

```
platform-spring-boot-starter-<short-name>
```

Examples: `platform-spring-boot-starter-rest-server`, `platform-spring-boot-starter-rest-client`,
`platform-spring-boot-starter-graphql-client`, `platform-spring-boot-starter-openapi`,
`platform-spring-boot-starter-rabbitmq-messaging`.

The `<short-name>` should be lowercase, hyphen-separated, and describe the concern the
starter addresses (not a technology name where possible).

---

## 2. Directory Structure

Create the following layout under `starters/`:

```
starters/
└── platform-spring-boot-starter-<name>/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/kevshah/platform/starter/<name>/
        │   │       ├── <Name>AutoConfiguration.java        ← required
        │   │       ├── config/                             ← if @ConfigurationProperties are needed
        │   │       │   └── <Name>Properties.java
        │   │       └── <feature>/                          ← additional sub-packages as needed
        └── resources/
            └── META-INF/
                ├── spring/
                │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
                └── additional-spring-configuration-metadata.json   ← only if @ConfigurationProperties
    └── test/
        └── java/
            └── com/kevshah/platform/starter/<name>/
                └── (test classes)
```

Rules:
- The Java base package is always `com.kevshah.platform.starter.<name>` where `<name>`
  is the short name with hyphens replaced by dots
  (e.g., `rest-server` → `com.kevshah.platform.starter.rest.server`).
- Sub-packages follow the conventions in `AGENTS.md`: entities in `entities/`,
  configuration classes in `config/`, etc.
- Never place a `@SpringBootApplication` class in `src/main/java` — starters are
  libraries, not runnable applications.

---

## 3. `pom.xml`

Model the `pom.xml` exactly on the template below. Only add dependencies that the starter
genuinely needs; do not copy dependencies from other starters speculatively.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.kevshah</groupId>
        <artifactId>platform-spring-boot-starter-build-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../platform-spring-boot-starter-build-parent</relativePath>
    </parent>

    <artifactId>platform-spring-boot-starter-<name></artifactId>
    <description><!-- one sentence describing the starter --></description>

    <dependencies>
        <!-- Required Spring Boot starters for this module's feature set -->

        <!--
            Always include these two optional annotation-processor dependencies
            when the starter exposes @ConfigurationProperties.
            Omit them entirely if there are no @ConfigurationProperties classes.
        -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!--Test Dependencies-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <!--Prevents Mockito self-attach warning on modern JDKs-->
                    <argLine>
                        -javaagent:${settings.localRepository}/org/mockito/mockito-core/${mockito.version}/mockito-core-${mockito.version}.jar
                        -Xshare:off
                    </argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

Key rules:
- **Parent** is always `platform-spring-boot-starter-build-parent` with `relativePath`
  pointing two directories up (`../../platform-spring-boot-starter-build-parent`).
- **No `<groupId>` or `<version>`** on the module itself — both are inherited from the
  parent.
- **No `<properties>` block** — Java version and encoding are inherited from the parent.
  Only add a `<properties>` block if the starter genuinely overrides a parent property.
- All dependency versions are managed by the BOM via the build-parent; never specify a
  `<version>` on a managed dependency.
- `spring-boot-configuration-processor` and `spring-boot-autoconfigure-processor` are
  `<optional>true</optional>` compile-time tools — include them only when the module has
  `@ConfigurationProperties` classes.

---

## 4. Auto-Configuration Class

Every starter must have exactly one `@AutoConfiguration` class as its entry point.

```java
package com.kevshah.platform.starter.<name>;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/// Auto-configuration for the `platform-spring-boot-starter-<name>` starter.
@AutoConfiguration
@ConditionalOnWebApplication   // add appropriate @ConditionalOn* annotations
public class <Name>AutoConfiguration {

    @Bean
    public MyFeatureBean myFeatureBean() {
        return new MyFeatureBean();
    }
}
```

Rules:
- Use `@AutoConfiguration` (not `@Configuration`) — this is the Spring Boot 4 idiom.
- Guard beans with the most specific `@ConditionalOn*` annotation possible
  (`@ConditionalOnWebApplication`, `@ConditionalOnProperty`, `@ConditionalOnClass`, etc.).
- Use `@ConditionalOnProperty` with `havingValue` to gate optional beans behind a config
  flag (see the rest-server starter for an example).
- Keep the auto-configuration class lean — delegate complexity to collaborating classes.
- Document the class with Markdown Javadoc (`///`) following `AGENTS.md §5`.

---

## 5. Registering the Auto-Configuration

Create the file:

```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Its content must be the fully-qualified class name of the auto-configuration class,
one per line:

```
com.kevshah.platform.starter.<name>.<Name>AutoConfiguration
```

This file is how Spring Boot discovers auto-configurations at runtime. Without it the
starter will be silently ignored.

---

## 6. `@ConfigurationProperties` (when needed)

### Properties prefix convention

Every starter's configuration properties **must** use the following prefix:

```
platform.<short-name-with-dots>
```

The prefix is derived mechanically from the artifact ID:

1. Strip the `platform-spring-boot-starter-` prefix.
2. Replace every remaining hyphen (`-`) with a dot (`.`).
3. Prepend `platform.`.

| Artifact ID | Derived prefix |
|---|---|
| `platform-spring-boot-starter-rest-server` | `platform.rest.server` |
| `platform-spring-boot-starter-rest-client` | `platform.rest.client` |
| `platform-spring-boot-starter-graphql-client` | `platform.graphql.client` |
| `platform-spring-boot-starter-openapi` | `platform.openapi` |
| `platform-spring-boot-starter-rabbitmq-messaging` | `platform.rabbitmq.messaging` |

This convention guarantees that all platform properties are:
- Grouped together under the `platform.*` namespace in any application's `application.yml`.
- Non-overlapping across starters (each short name is unique within the repo).
- Predictable — a reader can infer the prefix from the dependency name alone.

**Never** use a prefix outside this pattern (e.g., do not use `spring.*`, `app.*`, or an
unprefixed property name).

### Creating the properties record

When the starter needs user-configurable behaviour, create a `@ConfigurationProperties`
record in the `config/` sub-package.

```java
package com.kevshah.platform.starter.<name>.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Top-level configuration properties for `platform-spring-boot-starter-<name>`.
///
/// All properties are nested under the `platform.<short-name-with-dots>` prefix.
@ConfigurationProperties(prefix = "platform.<short-name-with-dots>")
public record <Name>Properties(
        Boolean enabled
        // add fields as needed
) {}
```

Then enable it in the auto-configuration class:

```java
@AutoConfiguration
@EnableConfigurationProperties(<Name>Properties.class)
public class <Name>AutoConfiguration { ... }
```

### `additional-spring-configuration-metadata.json`

For any properties not picked up automatically by the annotation processor (e.g. list
element sub-properties), create:

```
src/main/resources/META-INF/additional-spring-configuration-metadata.json
```

Follow the structure already used in `platform-spring-boot-starter-rest-server` —
`groups`, `properties`, and `hints` arrays.

---

## 7. Wiring the Module into the Repository

Three files must be updated every time a new starter is added.

### 7a. Root aggregator `pom.xml`

Add a `<module>` entry in `pom.xml` (repo root) inside `<modules>`, keeping starter
entries in alphabetical order below the infrastructure modules:

```xml
<modules>
    <module>platform-spring-boot-starter-dependencies</module>
    <module>platform-spring-boot-starter-parent</module>
    <module>platform-spring-boot-starter-build-parent</module>

    <module>starters/platform-spring-boot-starter-<name></module>   <!-- ADD THIS -->
    <module>starters/platform-spring-boot-starter-rest-server</module>
</modules>
```

### 7b. BOM — `platform-spring-boot-starter-dependencies/pom.xml`

Add a `<dependency>` entry in the `<!--Custom starters-->` block of
`platform-spring-boot-starter-dependencies/pom.xml`, keeping entries in alphabetical
order by artifact ID:

```xml
<!--Custom starters-->
<dependency>
    <groupId>com.kevshah</groupId>
    <artifactId>platform-spring-boot-starter-<name></artifactId>
    <version>${platform.version}</version>
</dependency>
```

This makes the new starter available to consumers without specifying a version, as long as
they import the BOM or use `platform-spring-boot-starter-parent`.

### 7c. Verify the build

After wiring, run a full build from the repo root to confirm nothing is broken:

```bash
mvn clean install
```

---

## 8. Documentation

Creating a new starter requires updates to three documentation locations. Follow the
`write-documentation` skill for the full authoring guide; the requirements are summarised
below.

### 8a. New module `README.md`

Create `starters/platform-spring-boot-starter-<name>/README.md` following the
**starter module** template in the `write-documentation` skill (Section 3). Required
sections:

1. Title & description
2. Installation (Maven dependency snippet — no version, relies on BOM)
3. Features (bullet list)
4. Configuration — link to `docs/CONFIGURATIONS.md` (only if `@ConfigurationProperties` are present)
5. See Also — link to the root `README.md` and the BOM module

### 8b. New `docs/CONFIGURATIONS.md` (only when `@ConfigurationProperties` are present)

Create `starters/platform-spring-boot-starter-<name>/docs/CONFIGURATIONS.md` following
Section 4 of the `write-documentation` skill. Required sections:

1. Title
2. Overview (prefix, record hierarchy, activation condition)
3. Properties reference table
4. Valid combinations
5. Complete YAML examples (at least two)

### 8c. Root `README.md` updates

Update the root `README.md` in two places:

1. **Repository Structure** tree — add a line for the new module under `starters/`:
   ```
   │   └── platform-spring-boot-starter-<name>/   # <one-line description>
   ```

2. **Modules → Starters table** — add a row, keeping rows alphabetical by artifact ID:
   ```markdown
   | `platform-spring-boot-starter-<name>` | <One-sentence description.> |
   ```

---

## 9. Changelog Entry

Add a changelog entry via the `update-changelog` skill:

- Type: `### Added`
- Group: `#### platform-spring-boot-starter-<name>`
- Entry: `Added <Name> starter with <brief description of what it auto-configures>`

---

## 10. Quick-Reference Checklist

- [ ] Directory created at `starters/platform-spring-boot-starter-<name>/`.
- [ ] `pom.xml` uses `platform-spring-boot-starter-build-parent` as parent with the
  correct `relativePath`.
- [ ] No `<groupId>`, `<version>`, or `<properties>` block unless genuinely needed.
- [ ] `spring-boot-configuration-processor` and `spring-boot-autoconfigure-processor`
  included as `<optional>true</optional>` **only** when `@ConfigurationProperties` exist.
- [ ] `maven-surefire-plugin` configured with the Mockito `-javaagent` `argLine`.
- [ ] `@AutoConfiguration` class created in `com.kevshah.platform.starter.<name>`.
- [ ] `org.springframework.boot.autoconfigure.AutoConfiguration.imports` file created and
  contains the fully-qualified auto-configuration class name.
- [ ] `@ConfigurationProperties` record created in `config/` sub-package (if needed).
- [ ] `@ConfigurationProperties` prefix follows the convention `platform.<short-name-with-dots>`
  derived from the artifact ID (strip `platform-spring-boot-starter-`, replace `-` with `.`,
  prepend `platform.`).
- [ ] `additional-spring-configuration-metadata.json` created (if list/nested properties
  are not picked up automatically).
- [ ] `<module>` entry added to root `pom.xml` in alphabetical order.
- [ ] `<dependency>` entry added to `platform-spring-boot-starter-dependencies/pom.xml`
  in the `<!--Custom starters-->` block, in alphabetical order.
- [ ] `mvn clean install` passes from the repo root.
- [ ] `starters/platform-spring-boot-starter-<name>/README.md` created.
- [ ] `starters/platform-spring-boot-starter-<name>/docs/CONFIGURATIONS.md` created
  (only when `@ConfigurationProperties` are present).
- [ ] Root `README.md` structure tree and Starters table updated.
- [ ] Changelog entry added under `### Added` / `#### platform-spring-boot-starter-<name>`.


