#!/bin/sh

# Fetch the latest default (stable) Spring Boot version
BOOT_VERSION=$(curl -s -H 'Accept: application/json' https://start.spring.io | jq -r '.bootVersion.default')

# Strip any trailing qualifier (e.g. ".RELEASE", ".M1") so the version is
# in the plain "X.Y.Z" format that the /dependencies endpoint requires.
BOOT_VERSION_PLAIN=$(echo "$BOOT_VERSION" | sed 's/\.\(RELEASE\|SNAPSHOT\|M[0-9]*\|RC[0-9]*\)$//')

# Fetch the dependencies payload once and cache it
DEPS_JSON=$(curl -s -H 'Accept: application/json' "https://start.spring.io/dependencies?bootVersion=${BOOT_VERSION_PLAIN}")

# Extract the compatible Spring Cloud BOM version from the cached response
CLOUD_VERSION=$(echo "$DEPS_JSON" | jq -r '.boms["spring-cloud"].version')

# Extract the compatible SpringDoc OpenAPI version from the cached response
SPRINGDOC_VERSION=$(echo "$DEPS_JSON" | jq -r '.dependencies["springdoc-openapi"].version')

# Extract the compatible Vaadin version from the cached response
VAADIN_VERSION=$(echo "$DEPS_JSON" | jq -r '.boms["vaadin"].version')

# Print the results
echo "Latest Spring Boot version: $BOOT_VERSION"
echo "Latest compatible spring-cloud version: $CLOUD_VERSION"
echo "Latest compatible springdoc-openapi version: $SPRINGDOC_VERSION"
echo "Latest compatible Vaadin version: $VAADIN_VERSION"
