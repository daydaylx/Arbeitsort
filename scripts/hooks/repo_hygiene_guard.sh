#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

print_section() {
    local title="$1"
    local content="$2"

    if [[ -z "$content" ]]; then
        return
    fi

    echo "$title"
    while IFS= read -r line; do
        [[ -n "$line" ]] && echo "  - $line"
    done <<< "$content"
}

staged_files="$(git diff --cached --name-only --diff-filter=ACMR || true)"
unstaged_tracked_files="$(git diff --name-only || true)"
untracked_files="$(git ls-files --others --exclude-standard || true)"

echo "Repo hygiene summary:"
print_section "Staged files:" "$staged_files"
print_section "Unstaged tracked files:" "$unstaged_tracked_files"
print_section "Untracked files:" "$untracked_files"

if [[ -n "$unstaged_tracked_files" ]]; then
    echo "Warning: unstaged tracked changes are present. Keep commit scope intentional."
fi

denylist_pattern='^(\.claude/|\.clinerules/|\.kilo/|\.vscode/(extensions|settings|launch)\.json$|\.env(\..*)?$|local\.properties$|keystore\.properties$|.*\.(jks|keystore|db|sqlite)$|logs/|debug_artifacts/|build_output\.log$|lint_output\.log$|test_output\.log$)'
banned_staged_files="$(printf '%s\n' "$staged_files" | grep -E "$denylist_pattern" || true)"

if [[ -n "$banned_staged_files" ]]; then
    echo ""
    echo "Blocked staged local-only or machine-specific files:"
    while IFS= read -r line; do
        [[ -n "$line" ]] && echo "  - $line"
    done <<< "$banned_staged_files"
    echo ""
    echo "These files should stay untracked or be removed from the commit."
    exit 1
fi
