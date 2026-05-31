# AGENTS.md — Instructions for AI Coding Agents

## 1. Project Overview

A collection of opinionated Spring Boot auto-configuration starters and shared infrastructure
for platform services.

- **Stack:** Java 26, Spring Boot 4.0.x, Maven 3.9+
- **Group ID:** `com.kevshah`
- **Root artifact ID:** `platform-spring-boot-starters`

### Repository layout

```
platform-spring-boot-starters/
├── platform-spring-boot-starter-dependencies/   # BOM — centralised dependency versions
├── platform-spring-boot-starter-parent/          # Consumer-facing parent POM
├── platform-spring-boot-starter-build-parent/    # Internal build parent (used by starters)
├── starters/                                      # Auto-configuration starter modules
│   └── platform-spring-boot-starter-rest-server/
├── examples/                                      # Runnable example applications
│   └── restful-web-service-example/
└── .agents/
    └── skills/                                    # Skill files for AI agents (see §2)
```

---

## 2. Skills

Skills are detailed how-to guides stored under `.agents/skills/`. **Always read the
relevant skill file before performing a task it covers** — they contain exact templates,
checklists, and rules that supersede the summary notes in this file.

| Skill name                         | File path                                                  | When to use                                                                        |
|------------------------------------|------------------------------------------------------------|------------------------------------------------------------------------------------|
| `create-example-module`            | `.agents/skills/create-example-module/SKILL.md`            | Creating a new example application under `examples/`                               |
| `create-starter-module`            | `.agents/skills/create-starter-module/SKILL.md`            | Creating a new starter module under `starters/`                                    |
| `write-tests`                      | `.agents/skills/write-tests/SKILL.md`                      | Writing unit or integration tests                                                  |
| `write-documentation`              | `.agents/skills/write-documentation/SKILL.md`              | Writing or updating any `README.md` or `docs/CONFIGURATIONS.md`                    |
| `update-changelog`                 | `.agents/skills/update-changelog/SKILL.md`                 | Adding entries to `CHANGELOG.md`                                                   |
| `latest-spring-ecosystem-versions` | `.agents/skills/latest-spring-ecosystem-versions/SKILL.md` | Finding the latest stable Spring Boot / Spring Cloud / springdoc / Vaadin versions |
| `update-spring-ecosystem-versions` | `.agents/skills/update-spring-ecosystem-versions/SKILL.md` | Bumping Spring Boot and related ecosystem versions                                 |
| `latest-resilience4j-version`      | `.agents/skills/latest-resilience4j-version/SKILL.md`      | Finding the latest stable Resilience4j version                                     |
| `update-resilience4j-version`      | `.agents/skills/update-resilience4j-version/SKILL.md`      | Bumping the Resilience4j version                                                   |
| `git-commit-message`               | `.agents/skills/git-commit-message/SKILL.md`               | Crafting a concise, informative git commit message for all staged changes           |

---

## 3. Core Build & Run Commands

```bash
# Build everything
mvn clean install

# Build everything including example modules
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

---

## 4. Module & Package Conventions

### Starter naming

All starter artifact IDs and directory names follow the pattern:

```
platform-spring-boot-starter-<short-name>
```

The `<short-name>` is lowercase and hyphen-separated (e.g., `rest-server`, `rest-client`,
`graphql-client`). It should describe the concern, not the technology.

### Java base package

The base package for a starter is derived from its short name by replacing hyphens with
dots and prepending the group namespace:

```
com.kevshah.platform.starter.<short-name-with-hyphens-replaced-by-dots>
```

Example: `rest-server` → `com.kevshah.platform.starter.rest.server`

### Sub-package rules

| Concern                            | Sub-package                  |
|------------------------------------|------------------------------|
| Auto-configuration entry point     | (base package root)          |
| `@ConfigurationProperties` records | `config/`                    |
| JPA entities                       | `entities/`                  |
| Additional feature code            | descriptive sub-package name |

### `@ConfigurationProperties` prefix convention

Every starter's configuration properties must use the prefix:

```
platform.<short-name-with-hyphens-replaced-by-dots>
```

Example: `platform-spring-boot-starter-rest-server` → `platform.rest.server`

**Never** use `spring.*`, `app.*`, or unprefixed names.

---

## 5. Coding Standards

- **Java features:** Use modern Java features — records, sealed classes, pattern matching,
  text blocks, `var`.
- **Auto-configuration:** Use `@AutoConfiguration` (not `@Configuration`) and guard beans
  with the most specific `@ConditionalOn*` annotation available.
- **Mappers:** Use static mapper classes; do not introduce ModelMapper or MapStruct.
- **Error handling:** Use `@ControllerAdvice` for global exception handling. Prefer custom
  `RuntimeException` subclasses over checked exceptions.
- **Dependency versions:** Never hardcode a version on a managed dependency — all versions
  are controlled by the BOM in `platform-spring-boot-starter-dependencies/pom.xml`.
- **No `@SpringBootApplication` in starters:** Starters are libraries; runnable main
  classes belong only in `examples/`.

---

## 6. Documentation Standards

Always use **Markdown Javadoc** (Java 23+ `///` line comments). Do not use `/** ... */`
block comments.

| Element         | Correct               | Incorrect                  |
|-----------------|-----------------------|----------------------------|
| Inline code     | `` `foo` ``           | `{@code foo}`              |
| Links           | `[ClassName#method]`  | `{@link ClassName#method}` |
| Lists           | `- item` or `1. item` | `<ul><li>`                 |
| Paragraph break | blank `///` line      | `<p>`                      |

Every class added to a starter must have a `///` Javadoc comment.

All public API elements (public classes, methods, and fields) must have Javadoc comments that
describe their purpose and behaviour. Comments should be concise but informative, ideally not exceeding 3-4 sentences
per element.

### Javadoc for records

For `record` types, document each component (field) using `@param` tags in the **class-level**
Javadoc comment. Do **not** place a separate Javadoc comment on the individual record components.

```java
/// Holds the timeout configuration for outbound REST calls.
///
/// @param connectTimeoutMs maximum time in milliseconds to establish a connection
/// @param readTimeoutMs    maximum time in milliseconds to wait for a response
public record TimeoutProperties(int connectTimeoutMs, int readTimeoutMs) {}
```

For documentation files (`README.md`, `docs/CONFIGURATIONS.md`), follow the
`write-documentation` skill (§2).

---

## 7. Testing Standards

Read the `write-tests` skill (§2) for the full guide. Key rules are summarised here.

### Frameworks

- **JUnit 6 (Jupiter)** for all tests.
- **AssertJ** for assertions — never use JUnit's `assertEquals` / `assertTrue`.
- **Mockito** for mocking (`when`/`thenReturn`/`verify`) — do not use BDDMockito style.
- **OkHttp `MockWebServer`** for outbound HTTP calls.
- **Testcontainers** for database integration tests — never use H2 or mocked repositories.

### Test method naming

```
<methodOrBehaviour>_<stateOrInput>_<expectedOutcome>
```

### Test structure

Every test body must have `// Given`, `// When`, and `// Then` comment sections
(or a combined `// When/Then` for trivial cases).

### Starter / library modules

- Use `ApplicationContextRunner` (not `@SpringBootTest`) to test `@AutoConfiguration`
  classes and their `@ConditionalOn*` conditions.
- If a full Spring context is genuinely needed, create a package-private `TestApplication`
  class in `src/test/java` and reference it with `@SpringBootTest(classes = TestApplication.class)`.

### Example / runnable application modules

- Use `@SpringBootTest` for integration tests.
- Use `@WebMvcTest` for controller slice tests.

---

## 8. Boundaries & Prohibitions

- **Secrets:** Never commit or suggest hardcoded API keys, passwords, or tokens.
- **Git:** Do not run `git push` or `git merge` unless explicitly asked.
- **Deletions:** Do not delete existing service or repository interfaces without confirming
  with the user.
- **Scope creep:** When creating a new starter, only add dependencies the starter
  genuinely needs — do not copy speculatively from other starters.
