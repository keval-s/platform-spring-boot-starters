---
name: git-commit-message
description: Guide for crafting a concise and informative git commit message that follows best practices.
---

## Overview

A good commit message tells reviewers **what** changed and **why** — not how (the diff already shows that).
This skill covers:

1. Verifying that everything intended is staged.
2. Inspecting the staged diff.
3. Composing the message following the Conventional Commits format.
4. Delivering the final message.

---

## 1. Confirm Staged Changes With the User

**Before reading any diff, ask the user:**

> "Have you staged all the changes you want included in this commit? I'll base the message
> only on what is currently in the index (`git diff --staged`)."

Wait for confirmation before proceeding. If the user says they need to stage more files,
stop and let them do so — do not guess or proceed speculatively.

---

## 2. Inspect the Staged Diff

Once the user confirms, run:

```bash
# Summary of what is staged (file-level overview)
git --no-pager diff --staged --stat

# Full staged diff (content detail)
git --no-pager diff --staged
```

Read **both** outputs carefully before writing any message:

- The `--stat` output gives you the scope (which files / modules changed).
- The full diff gives you the precise nature of each change.

---

## 3. Commit Message Format

Use the **Conventional Commits** specification
([conventionalcommits.org](https://www.conventionalcommits.org/en/v1.0.0/)).

### Structure

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

### Subject line rules

| Rule | Detail |
|------|--------|
| Max length | **72 characters** |
| Case | lowercase — never sentence-case or ALL-CAPS |
| Tense | imperative mood (`add`, `fix`, `bump`, not `added`, `fixes`) |
| No period | do not end the subject line with `.` |
| Scope | use the Maven artifact ID short name (e.g. `rest-server`, `dependencies`, `build-parent`) or omit if the change is truly cross-cutting |

### Allowed types

| Type | When to use |
|------|-------------|
| `feat` | a new feature or capability |
| `fix` | a bug fix |
| `chore` | maintenance — version bumps, build changes, tooling |
| `docs` | documentation only |
| `test` | adding or correcting tests |
| `refactor` | code restructuring with no behaviour change |
| `perf` | performance improvement |
| `ci` | CI/CD pipeline changes |
| `revert` | reverts a previous commit |

### Body (optional)

- Separate from the subject with **one blank line**.
- Wrap at **72 characters** per line.
- Explain **why** the change was made, or what problem it solves — not what the diff shows.
- Use bullet points (`-`) for multi-point explanations.

### Footer (optional)

- `BREAKING CHANGE: <description>` — required when the change breaks a public API.
- `Closes #<issue>` — reference related issues or tickets.
- `Co-authored-by: Name <email>` — for pair or mob work.

---

## 4. Scope Determination

Derive the scope from the staged files:

| Staged files are in… | Scope to use |
|----------------------|--------------|
| `starters/platform-spring-boot-starter-<name>/` | `<name>` (e.g. `rest-server`) |
| `platform-spring-boot-starter-dependencies/` | `dependencies` |
| `platform-spring-boot-starter-parent/` | `parent` |
| `platform-spring-boot-starter-build-parent/` | `build-parent` |
| `examples/<example-name>/` | `<example-name>` (e.g. `restful-web-service-example`) |
| Root files only (`README.md`, `AGENTS.md`, `CHANGELOG.md`, `pom.xml`) | omit scope |
| Multiple modules with no single focus | omit scope and widen the subject |

---

## 5. Examples

### Single-module feature
```
feat(rest-server): add per-rule HTTP method filtering to logging
```

### Dependency bump
```
chore(dependencies): bump spring boot from 4.0.3 to 4.0.7
```

### Bug fix with body
```
fix(rest-server): prevent NPE in logging filter when rules list is absent

The `StandardRequestResponseLoggingFilter` threw a NullPointerException
when the `platform.rest.server.logging.rules` list was omitted entirely
from the application configuration. Added a null-safe guard and defaulted
to an empty list on startup.
```

### Breaking change
```
feat(rest-server): replace HeaderLoggingMode enum with sealed interface

BREAKING CHANGE: `HeaderLoggingMode` is now a sealed interface with
`record` implementations. Consumers that referenced the enum constants
directly must migrate to the new types.
```

### Docs-only change
```
docs: update rest-server README with logging configuration examples
```

### Cross-cutting refactor
```
refactor: migrate all Javadoc to markdown triple-slash style
```

---

## 6. Composing the Message — Step by Step

1. **Identify the dominant change type** from the diff.  
   If the diff mixes types (e.g. `feat` + `docs`), choose the type with the highest user
   impact; mention the secondary changes in the body.

2. **Determine the scope** using the table in §4.

3. **Draft the subject line** in imperative mood, ≤ 72 characters.

4. **Decide if a body is needed:**
   - Skip the body for trivial, self-explanatory changes.
   - Add a body when the *why* is not obvious from the subject alone, or when the change
     spans multiple logical concerns.

5. **Add footers** only if there is a breaking change or a linked issue.

6. **Present the final message** to the user in a fenced code block so it is easy to copy:

   ````
   ```
   feat(rest-server): add per-rule HTTP method filtering to logging
   ```
   ````

---

## 7. Final Checklist

Before presenting the message, verify:

- [ ] The user confirmed that all intended changes are staged.
- [ ] Both `--stat` and the full diff were read.
- [ ] The type accurately reflects the dominant nature of the change.
- [ ] The scope matches the affected Maven module(s) or is omitted when cross-cutting.
- [ ] The subject is ≤ 72 characters, lowercase, imperative mood, no trailing period.
- [ ] A body is present whenever the *why* is non-obvious.
- [ ] `BREAKING CHANGE:` footer is present if a public API is broken.
- [ ] No sensitive information (secrets, tokens, passwords) appears anywhere in the message.
