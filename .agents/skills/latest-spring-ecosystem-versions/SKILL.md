---
name: latest-spring-ecosystem-versions
description: Guide for finding the latest stable version of Spring Boot and related Spring ecosystem versions (Spring Cloud, springdoc-openapi, and Vaadin).
---

## Overview

This skill fetches the latest stable Spring Boot version from [start.spring.io](https://start.spring.io)
and the compatible versions of the Spring Cloud BOM, springdoc-openapi BOM, and Vaadin BOM.

---

## Steps

### 1. Get the latest Spring Boot ecosystem versions

1. Execute the
   `.agents/skills/latest-spring-ecosystem-versions/get-latest-spring-ecosystem-versions.sh` shell
   script.

### 2. Capture the output values

The script prints four lines. Store each value for use in downstream steps:

| Variable | Example value | Description |
|---|---|---|
| `NEW_BOOT_VERSION` | `4.0.6.RELEASE` | Latest stable Spring Boot version (may include a `.RELEASE` qualifier) |
| `NEW_CLOUD_VERSION` | `2025.1.1` | Compatible Spring Cloud BOM version |
| `NEW_SPRINGDOC_VERSION` | `3.0.2` | Compatible springdoc-openapi BOM version |
| `NEW_VAADIN_VERSION` | `25.1.6` | Compatible Vaadin BOM version |

> **Note:** `NEW_BOOT_VERSION` may contain a trailing qualifier such as `.RELEASE`. Strip it to
> obtain the plain `X.Y.Z` form when writing it to a Maven POM.
