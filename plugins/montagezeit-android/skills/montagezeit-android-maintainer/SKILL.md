---
name: montagezeit-android-maintainer
description: Repo-local workflow for MontageZeit Android maintenance; use for Compose screens, ViewModels, Room queries and migrations, WorkManager reminders, export flows, and local Gradle or device verification.
metadata:
  short-description: MontageZeit Android maintenance workflow
---

# MontageZeit Android Maintainer

Use this skill for implementation and maintenance work in the MontageZeit Android repo.

## Workflow

1. Read `AGENTS.md` first, then the active docs `README.md` and `docs/ARCHITECTURE.md` when behavior or architecture matters.
2. Stay shell-first for Android work: use `rg`, `git`, `./gradlew`, `adb`, and the scripts under `scripts/`.
3. Prefer the official GitHub plugin or connector for PR, review, and CI work. Do not introduce unrelated external connectors unless the task explicitly requires them.
4. Search and inspect the smallest relevant code slice before editing. Respect the repo structure under `ui/`, `domain/usecase/`, `data/`, `work/`, `notification/`, `receiver/`, and `export/`.
5. Validate changes with the smallest relevant command set first, then escalate as needed:
   - UI or ViewModel work: targeted tests, then `./gradlew :app:testDebugUnitTest`
   - Domain, export, reminder, notification, or receiver work: matching tests plus `./gradlew :app:testDebugUnitTest`
   - Room schema or migration work: migration coverage, doc updates, and the relevant unit tests
   - Broad refactors or handoff-ready changes: `bash scripts/hooks/run_local_quality_gate.sh`
6. Use `./scripts/android_debug_run.sh [DEVICE_SERIAL]` only when a real device or emulator check adds value.

## Rules

- Treat `AGENTS.md` as canonical over compatibility files.
- Keep changes small and aligned with the repo's offline-first Android architecture.
- Update active docs in the same change when behavior, schema version, or developer workflow changes.
- Do not rely on non-GitHub connectors for routine Android development in this repo.
