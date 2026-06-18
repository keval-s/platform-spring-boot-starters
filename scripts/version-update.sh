#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Get project root directory
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Check if the version argument is provided
if [ -z "$1" ]; then
  echo "Usage: $0 <new_version>"
  exit 1
fi

# Get the new version from the first argument
NEW_VERSION="$1"

# Move into the project root before running commands
cd "$PROJECT_ROOT" || exit 1

# Update versions in all pom.xml files
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DprocessAllModules=true
mvn versions:set-property -pl :platform-spring-boot-starter-dependencies -Dproperty=platform.version -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false