---
name: update-spring-ecosystem-versions
description: Guide for updating the Spring Boot version and related Spring ecosystem versions in the project.
---

## Steps

### 1. Gather latest versions

Follow the **latest-spring-ecosystem-versions** skill to obtain the latest stable versions. That skill's shell script returns four values — capture all of them:

- `NEW_BOOT_VERSION` — latest stable Spring Boot version (e.g. `4.0.7`)
- `NEW_CLOUD_VERSION` — compatible Spring Cloud BOM version (e.g. `2025.1.2`)
- `NEW_SPRINGDOC_VERSION` — latest stable springdoc-openapi-bom version (e.g. `3.0.4`)
- `NEW_VAADIN_VERSION` — compatible Vaadin BOM version (e.g. `25.1.6`) — captured for reference; see note below

Also derive `NEW_BOOT_VERSION_MINOR` as the `major.minor` part of `NEW_BOOT_VERSION` (e.g. `4.0`) for prose references.

Read the current values from `platform-spring-boot-starter-dependencies/pom.xml` and store them as:

- `OLD_BOOT_VERSION` — current value of `<spring-boot.version>`
- `OLD_CLOUD_VERSION` — current value of `<spring-cloud.version>`
- `OLD_SPRINGDOC_VERSION` — current value of `<springdoc.version>`

> **Note on Vaadin:** `NEW_VAADIN_VERSION` is captured but not written to any POM because the
> project does not currently manage a `<vaadin.version>` property. If Vaadin is added to the BOM
> in the future, follow the same pattern as the other properties in steps 3 and 4b.

---

### 2. Decide whether to proceed

**Spring Boot version check:**

If `OLD_BOOT_VERSION` equals `NEW_BOOT_VERSION`, the Spring Boot version is already up to date.

Compare the major and minor segments of `OLD_BOOT_VERSION` and `NEW_BOOT_VERSION`:

- If the **major** version has changed (e.g. `3.x.x` → `4.x.x`), ask the user:
  > "The new Spring Boot version (`NEW_BOOT_VERSION`) is a **major** version bump from `OLD_BOOT_VERSION`. Major releases may contain breaking changes. Do you want to proceed?"
- Else if the **minor** version has changed (e.g. `4.0.x` → `4.1.x`), ask the user:
  > "The new Spring Boot version (`NEW_BOOT_VERSION`) is a **minor** version bump from `OLD_BOOT_VERSION`. Minor releases may introduce deprecations or behaviour changes. Do you want to proceed?"

**Stop and do not continue until the user explicitly confirms they want to proceed.**
If the change is patch-only (e.g. `4.0.6` → `4.0.7`), no confirmation is needed — continue directly to step 3.

---

### 3. Update Maven POMs

#### 3a. `platform-spring-boot-starter-dependencies/pom.xml`

This is the canonical source of truth for all ecosystem versions.
Update all three properties as needed:

```xml
<spring-boot.version>NEW_BOOT_VERSION</spring-boot.version>
<spring-cloud.version>NEW_CLOUD_VERSION</spring-cloud.version>
<springdoc.version>NEW_SPRINGDOC_VERSION</springdoc.version>
```

Only update a property if the new value differs from the old value.

#### 3b. `platform-spring-boot-starter-build-parent/pom.xml`

Two places must be changed here. The file itself contains a comment reminding you to keep these in sync with `platform-spring-boot-starter-dependencies/pom.xml`.

1. The `<parent>` block that inherits from `spring-boot-starter-parent`:
   ```xml
   <parent>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-parent</artifactId>
       <version>NEW_BOOT_VERSION</version>
   </parent>
   ```
2. The `<spring-boot.version>` property in `<properties>`:
   ```xml
   <spring-boot.version>NEW_BOOT_VERSION</spring-boot.version>
   ```

#### 3c. `platform-spring-boot-starter-parent/pom.xml`

Update the `<parent>` block that inherits from `spring-boot-starter-parent`. The file also contains a comment reminding you to keep this in sync with `platform-spring-boot-starter-dependencies/pom.xml`:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>NEW_BOOT_VERSION</version>
</parent>
```

---

### 4. Update documentation

#### 4a. `AGENTS.md`

Update the prose Spring Boot version reference in the **Stack** line.
Change the `major.minor` (e.g. `4.0.x`) to match `NEW_BOOT_VERSION_MINOR.x`:

```
- **Stack:** Java 26, Spring Boot NEW_BOOT_VERSION_MINOR.x, Maven 3.9+
```

Only change the `major.minor` portion; leave the `.x` wildcard suffix in place.

#### 4b. `CHANGELOG.md`

Follow the **update-changelog** skill to add a `### Changed` entry for every version that actually changed. Only include lines for versions that changed:

```markdown
### Changed
- Bumped Spring Boot from OLD_BOOT_VERSION to NEW_BOOT_VERSION
- Bumped Spring Cloud from OLD_CLOUD_VERSION to NEW_CLOUD_VERSION
- Bumped springdoc-openapi from OLD_SPRINGDOC_VERSION to NEW_SPRINGDOC_VERSION
```

---

### 5. Verify the build

Run the full build to confirm everything compiles and all tests pass:

```bash
mvn clean install
```

If the build fails, review the error output and address any compatibility issues introduced by the new versions before finishing.
