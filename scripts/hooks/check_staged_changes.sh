#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -x "./gradlew" ]]; then
    echo "gradlew must be executable."
    exit 1
fi

if git diff --cached --quiet; then
    exit 0
fi

echo "Running staged diff checks..."
git diff --cached --check

echo "Running repo hygiene checks..."
bash scripts/hooks/repo_hygiene_guard.sh
