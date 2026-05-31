---
name: update-resilience4j-version
description: Guide for updating the Resilience4j version in the project.
---

## Steps

### 1. Gather latest version

Follow the **latest-resilience4j-version** skill to obtain the latest stable version and capture:

- `NEW_RESILIENCE4J_VERSION` — latest stable Resilience4j version (e.g. `2.3.0`)

Read the current value from `platform-spring-boot-starter-dependencies/pom.xml` and store it as:

- `OLD_RESILIENCE4J_VERSION` — current value of `<resilience4j.version>`

---

### 2. Decide whether to proceed

If `OLD_RESILIENCE4J_VERSION` equals `NEW_RESILIENCE4J_VERSION`, the version is already up to date —
stop here and inform the user.

Compare the major and minor segments of `OLD_RESILIENCE4J_VERSION` and `NEW_RESILIENCE4J_VERSION`:

- If the **major** version has changed, ask the user:
  > "The new Resilience4j version (`NEW_RESILIENCE4J_VERSION`) is a **major** version bump from
  > `OLD_RESILIENCE4J_VERSION`. Major releases may contain breaking changes. Do you want to proceed?"
- Else if the **minor** version has changed, ask the user:
  > "The new Resilience4j version (`NEW_RESILIENCE4J_VERSION`) is a **minor** version bump from
  > `OLD_RESILIENCE4J_VERSION`. Minor releases may introduce deprecations or behaviour changes.
  > Do you want to proceed?"

**Stop and do not continue until the user explicitly confirms they want to proceed.**
If the change is patch-only (e.g. `2.3.0` → `2.3.1`), no confirmation is needed — continue
directly to step 3.

---

### 3. Update Maven POM

#### `platform-spring-boot-starter-dependencies/pom.xml`

Update the `<resilience4j.version>` property:

```xml
<resilience4j.version>NEW_RESILIENCE4J_VERSION</resilience4j.version>
```

---

### 4. Update documentation

#### `CHANGELOG.md`

Follow the **update-changelog** skill to add a `### Changed` entry:

```markdown
### Changed
- Bumped Resilience4j from OLD_RESILIENCE4J_VERSION to NEW_RESILIENCE4J_VERSION
```

---

### 5. Verify the build

Run the full build to confirm everything compiles and all tests pass:

```bash
mvn clean install
```

If the build fails, review the error output and address any compatibility issues introduced by the
new version before finishing.

