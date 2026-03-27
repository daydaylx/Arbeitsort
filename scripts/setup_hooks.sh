#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v lefthook >/dev/null 2>&1; then
    cat >&2 <<'EOF'
lefthook is not installed.

Install it first, then rerun:
  ./scripts/setup_hooks.sh

Common install option on macOS:
  brew install lefthook
EOF
    exit 1
fi

lefthook install
echo "Lefthook installed for this repository."
