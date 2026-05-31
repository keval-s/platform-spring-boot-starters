---
name: update-changelog
description: Guide for updating the CHANGELOG.md in accordance with the Keep a Changelog format.
---

## Overview

`CHANGELOG.md` follows the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) specification and
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

All changes are recorded under one of the seven standard **change-type headings** inside a version
section. The topmost version section is always `## [Unreleased]` and collects work that has not yet
been tagged and released.

---

## Change-Type Headings

Use only these headings, in the order shown, and only include a heading when at least one entry
belongs to it:

| Heading | When to use |
|---|---|
| `### Added` | New features or capabilities |
| `### Changed` | Changes to existing functionality (including dependency bumps) |
| `### Deprecated` | Features that will be removed in a future release |
| `### Removed` | Features removed in this release |
| `### Fixed` | Bug fixes |
| `### Security` | Security fixes or vulnerability patches |

---

## Entry Grouping Within a Change-Type Section

Within each `### <Type>` heading, entries are further grouped using `####` sub-headings to
separate **cross-cutting** changes from **module-specific** changes.

### `#### Cross-cutting`

Use this sub-heading for changes that affect the whole repository or are not tied to any
single module:

- Dependency / BOM version bumps (Spring Boot, Spring Cloud, Java, Resilience4j, etc.)
- Changes to `platform-spring-boot-starter-dependencies` or either parent POM
- Tooling, CI, or build changes (Maven plugin versions, `.editorconfig`, etc.)
- Repository-level documentation (root `README.md`, `AGENTS.md`, `CHANGELOG.md` itself)

### `#### <module-artifact-id>`

Use the Maven artifact ID as the sub-heading for changes scoped to a single module:

```
#### platform-spring-boot-starter-rest-server
#### restful-web-service-example
```

### Ordering within a `### <Type>` section

1. `#### Cross-cutting` always comes first.
2. Module sub-headings follow in alphabetical order by artifact ID.
3. Omit a sub-heading entirely when it would have no entries.

### Example

```markdown
## [Unreleased]

### Added
#### platform-spring-boot-starter-rest-server
- Added `platform.rest.server.logging.rules[*].methods` to restrict logging rules by HTTP method

### Changed
#### Cross-cutting
- Bumped Spring Boot from 4.0.3 to 4.0.6
- Bumped Spring Cloud from 2025.1.0 to 2025.1.1

#### platform-spring-boot-starter-rest-server
- `StandardRequestResponseLoggingFilter` now logs response time in milliseconds

### Fixed
#### platform-spring-boot-starter-rest-server
- Corrected null-pointer exception when `rules` list is omitted from configuration
```

---



### 1. Collect the full set of unrecorded changes

Run the following commands to collect **all** changes that may not yet be reflected in
`CHANGELOG.md` — both changes that have already been committed and changes that are staged but
not yet committed. This approach is squash-safe and rebase-safe because it anchors to the last
commit that touched `CHANGELOG.md` rather than to a specific commit hash.

```bash
# Find the commit that last modified CHANGELOG.md
LAST=$(git --no-pager log -1 --format="%H" -- CHANGELOG.md)

# 1a. Diff of all commits since that point, excluding CHANGELOG.md and test sources
git --no-pager diff ${LAST}..HEAD -- . ':(exclude)CHANGELOG.md' ':(exclude)*/src/test/*'

# 1b. Diff of changes that are staged but not yet committed, excluding CHANGELOG.md and test sources
git --no-pager diff --staged -- . ':(exclude)CHANGELOG.md' ':(exclude)*/src/test/*'
```

Use the **union** of both diffs as your input for writing changelog entries. If `LAST` is empty
(i.e. `CHANGELOG.md` has never been committed), fall back to diffing from the very first commit:

```bash
git --no-pager diff $(git --no-pager rev-list --max-parents=0 HEAD)..HEAD -- . ':(exclude)CHANGELOG.md' ':(exclude)*/src/test/*'
```

Then read the current `CHANGELOG.md` and compare the combined diff against the existing entries
under `## [Unreleased]`. **Do not add an entry if an equivalent one already exists.** An entry is
considered equivalent when it describes the same change to the same module, even if the wording
differs slightly. If all changes from the diff are already recorded, inform the user and stop —
do not modify the file.

---

### 2. Identify the correct change type and group

Decide which heading the new entry belongs to (see the table above) **and** which group
sub-heading to place it under:

- Dependency version bumps → `### Changed` / `#### Cross-cutting`
- New public API or feature in a specific starter → `### Added` / `#### <artifact-id>`
- Behaviour change without API break in a specific starter → `### Changed` / `#### <artifact-id>`
- CVE / vulnerability fix → `### Security` / `#### Cross-cutting` (if BOM-level) or `#### <artifact-id>` (if isolated to one module)
- Breaking removal → `### Removed` / `#### <artifact-id>`
- Build, CI, or tooling changes → `### Changed` / `#### Cross-cutting`

---

### 3. Write the entry

Entries are unordered Markdown list items (`-`). Keep them short and written in the **past tense**
or **imperative mood** (both are acceptable; pick one and stay consistent within a section).

Good examples:

```markdown
### Changed
#### Cross-cutting
- Bumped Spring Boot from 4.0.3 to 4.0.7

### Fixed
#### platform-spring-boot-starter-rest-server
- Corrected null-pointer exception in `StandardRequestResponseLoggingFilter` when input is empty

### Security
#### Cross-cutting
- Upgraded `com.example:lib` from 1.2.0 to 1.3.1 to resolve CVE-2026-12345
```

Rules:
- Do **not** add a date, author, or ticket number inline — those belong in git commit messages.
- Reference the old **and** new version for any dependency bump.
- Reference CVE identifiers for security fixes.
- Do **not** repeat the module name inside the entry text — the `####` sub-heading already
  provides that context.

---

### 4. Add the entry under `## [Unreleased]`

Open `CHANGELOG.md`. Locate the `## [Unreleased]` section.

**Locating the right `### <Type>` heading:**
If the relevant `### <Type>` heading already exists under `[Unreleased]`, use it.
If it does not yet exist, insert it in the canonical order defined in the table above.

**Locating the right `#### <group>` sub-heading:**
Within the `### <Type>` heading, find the matching `####` sub-heading (`Cross-cutting` or
the module artifact ID). If it does not yet exist, insert it:
- `#### Cross-cutting` always goes first, before any module sub-headings.
- Module sub-headings are ordered alphabetically by artifact ID after `#### Cross-cutting`.

Append the new bullet below the last existing bullet in the located `####` sub-heading.

Example — before:

```markdown
## [Unreleased]

### Changed
#### Cross-cutting
- Bumped Spring Boot from 4.0.2 to 4.0.3
```

After adding a `Fixed` entry for a specific module:

```markdown
## [Unreleased]

### Changed
#### Cross-cutting
- Bumped Spring Boot from 4.0.2 to 4.0.3

### Fixed
#### platform-spring-boot-starter-rest-server
- Corrected null-pointer exception in `StandardRequestResponseLoggingFilter` when input is empty
```

---

### 5. Promoting `[Unreleased]` to a versioned release (release-time only)

Perform this step **only** when a new version of the project is being tagged and released.

1. Determine the new version number (`RELEASE_VERSION`, e.g. `1.2.0`) following Semantic Versioning:
   - **Patch** (`x.y.Z`): only `Fixed` or `Security` entries since the last release.
   - **Minor** (`x.Y.0`): at least one `Added` or `Changed` entry; no breaking removals.
   - **Major** (`X.0.0`): at least one `Removed` or breaking `Changed` entry.

2. Determine `RELEASE_DATE` as today's date in `YYYY-MM-DD` format.

3. Rename the `## [Unreleased]` heading to `## [RELEASE_VERSION] - RELEASE_DATE`:

   ```markdown
   ## [1.2.0] - 2026-05-30
   ```

4. Insert a fresh, empty `## [Unreleased]` section above it:

   ```markdown
   ## [Unreleased]


   ## [1.2.0] - 2026-05-30
   ```

5. Update the comparison links at the bottom of the file (see Step 6).

---

### 6. Maintain the comparison links

The bottom of `CHANGELOG.md` contains diff links for each version. Keep them up to date.

#### Pattern

```markdown
[unreleased]: https://github.com/ORG/REPO/compare/vLAST_RELEASE...HEAD
[RELEASE_VERSION]: https://github.com/ORG/REPO/compare/vPREV_VERSION...vRELEASE_VERSION
```

#### When adding entries to `[Unreleased]` only

No link changes are needed unless the `[unreleased]` link is missing entirely, in which case add
it pointing to the last known tag:

```markdown
[unreleased]: https://github.com/ORG/REPO/compare/vLAST_RELEASE...HEAD
```

#### When promoting `[Unreleased]` to a versioned release

1. Update the `[unreleased]` link so its base is the new release tag:

   ```markdown
   [unreleased]: https://github.com/ORG/REPO/compare/vRELEASE_VERSION...HEAD
   ```

2. Add a new link for the released version:

   ```markdown
   [RELEASE_VERSION]: https://github.com/ORG/REPO/compare/vPREV_VERSION...vRELEASE_VERSION
   ```

Insert the new versioned link directly below the `[unreleased]` line, keeping links in
descending version order.

---

### 7. Final check

Review the updated `CHANGELOG.md` and confirm:

- [ ] Both diff sources were consulted: committed changes since the last `CHANGELOG.md` commit **and** currently staged changes.
- [ ] The new entry is under `## [Unreleased]` (or the correct versioned section for a release).
- [ ] The correct `### <Type>` heading is used.
- [ ] The entry is placed under the correct `####` sub-heading (`Cross-cutting` or the module artifact ID).
- [ ] `#### Cross-cutting` appears before all module sub-headings within its `### <Type>` section.
- [ ] Module sub-headings within a `### <Type>` section are in alphabetical order by artifact ID.
- [ ] The entry is a plain Markdown list item with no trailing punctuation or extra metadata.
- [ ] The module name is **not** repeated inside the entry text (the sub-heading provides the context).
- [ ] All comparison links at the bottom are consistent and point to the correct tags.
- [ ] No existing entries have been removed or modified.
- [ ] No duplicate entries have been introduced — each change appears exactly once under `## [Unreleased]`.
