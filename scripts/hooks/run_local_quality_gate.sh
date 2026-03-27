#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -x "./gradlew" ]]; then
    echo "gradlew must be executable."
    exit 1
fi

echo "Running local quality gate..."
./gradlew lint :app:testDebugUnitTest assembleDebug
