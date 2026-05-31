#!/bin/sh

# Fetch the latest version of resilience4j from Maven Central
LATEST_VERSION=$(curl -s "https://search.maven.org/solrsearch/select?q=g:%22io.github.resilience4j%22+AND+a:%22resilience4j%22&rows=1&wt=json" | jq -r '.response.docs[0].latestVersion')

# Output the latest version
echo "$LATEST_VERSION"