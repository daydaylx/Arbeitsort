# CLAUDE.md

## Source Priority

When sources disagree, trust them in this order:

1. Executable sources: Gradle files, scripts, workflow YAML, manifest, typed config
2. `AGENTS.md`
3. Active shared docs: `README.md`, `docs/ARCHITECTURE.md`, `CONTRIBUTING.md`, `README_ANDROID_DEV.md`
4. This file and `.claude/*`

`AGENTS.md` is the canonical repo instruction set. Keep this file as a thin compatibility layer.

## Quick Commands

```bash
./gradlew detekt
./gradlew lint
./gradlew :app:testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew connectedDebugAndroidTest
./gradlew jacocoTestDebugUnitTestReport
bash scripts/hooks/run_local_quality_gate.sh
./scripts/android_debug_run.sh [DEVICE_SERIAL]
```

## Repo Focus

- Single-module Android app (`:app`) with Kotlin, Compose, Room, WorkManager, Hilt, and DataStore
- Shell-first workflow: `rg`, `git`, `./gradlew`, `adb`, and repo scripts
- Repo-local Codex workflow surfaces live under `plugins/montagezeit-android/`
- Prefer the GitHub plugin or connector for PR, review, and CI work when available

## Working Rules

- Read `AGENTS.md` first for repo rules and validation expectations.
- Use the smallest relevant verification set first and escalate to the local quality gate for broader changes.
- Keep compatibility files thin; do not duplicate architecture facts or long command inventories outside `AGENTS.md` and active docs.
