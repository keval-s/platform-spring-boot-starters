#!/bin/bash

# This script updates the version in all Maven POM files
# and promotes the "Unreleased" section in CHANGELOG.md to the new version with the current date.

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Get project root directory
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Check if the version argument is provided
if [ -z "$1" ]; then
  echo "Usage: $0 <new_version>"
  exit 1
fi

NEW_VERSION="$1"
DATE=$(date +%Y-%m-%d)
CHANGELOG="$PROJECT_ROOT/CHANGELOG.md"

# Move into the project root before running commands
cd "$PROJECT_ROOT" || exit 1

echo "--- Updating Maven POM versions to $NEW_VERSION ---"

# Only update if the current pom version is different
CURRENT_POM_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
if [ "$CURRENT_POM_VERSION" != "$NEW_VERSION" ]; then
    mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DprocessAllModules=true
    mvn versions:set-property -pl :platform-spring-boot-starter-dependencies -Dproperty=platform.version -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false
else
    echo "Version is already set to $NEW_VERSION. Skipping."
fi

echo "--- Promoting Unreleased changes in CHANGELOG.md ---"
# Check if the version is NOT a snapshot version
if [[ "$NEW_VERSION" != *-SNAPSHOT ]]; then
    if [ -f "$CHANGELOG" ]; then
        # Check if the version header already exists
        if grep -q "## \[$NEW_VERSION]" "$CHANGELOG"; then
            echo "Version $NEW_VERSION already exists in CHANGELOG.md. Skipping."
        else
            # Use perl for cross-platform compatibility
            perl -i -pe "s|## \[Unreleased]|## \[Unreleased]\n\n## \[$NEW_VERSION] - $DATE|" "$CHANGELOG"
            echo "Updated CHANGELOG.md with version $NEW_VERSION"
        fi
    else
        echo "CHANGELOG.md not found at $CHANGELOG, skipping."
    fi
else
    echo "Snapshot version detected ($NEW_VERSION). Skipping CHANGELOG update."
fi

echo "--- Version update complete ---"