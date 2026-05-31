---
name: latest-resilience4j-version
description: Guide for finding the latest stable version of Resilience4j.
---

## Overview

This skill fetches the latest stable version of the Resilience4j library from
[Maven Central](https://central.sonatype.com/).

---

## Steps

### 1. Get the latest Resilience4j version

1. Execute the
   `.agents/skills/latest-resilience4j-version/get-latest-resilience4j-version.sh` shell script.

### 2. Capture the output value

The script prints a single line containing the version string. Store it as:

| Variable | Example value | Description |
|---|---|---|
| `NEW_RESILIENCE4J_VERSION` | `2.3.0` | Latest stable Resilience4j version |
