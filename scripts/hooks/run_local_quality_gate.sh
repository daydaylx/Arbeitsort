#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -x "./gradlew" ]]; then
    echo "gradlew must be executable."
    exit 1
fi

echo "Running local quality gate..."
echo ""
echo "=== Detekt (static analysis) ==="
./gradlew detekt
echo ""
echo "=== Android Lint + Unit Tests + Debug Build ==="
./gradlew lint :app:testDebugUnitTest assembleDebug
echo ""
echo "Quality gate passed."
