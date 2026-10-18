# Repository Guidelines

## Source Priority

`AGENTS.md` is the canonical repo instruction set for Codex-style agents. When sources disagree, trust them in this order:

1. Executable sources: Gradle files, scripts, workflow YAML, manifest, and typed config
2. `AGENTS.md`
3. Active shared docs: `README.md`, `docs/ARCHITECTURE.md`, `CONTRIBUTING.md`, `README_ANDROID_DEV.md`
4. Compatibility files such as `CLAUDE.md`, `.claude/*`, `.codex`, and repo-local plugin metadata

Keep compatibility files thin. Do not maintain separate architecture, version, or command inventories there when `AGENTS.md` or executable sources already cover them.

## Pflichtlektüre je Aufgabentyp

1. `AGENTS.md` (dieses Dokument) — immer zuerst.
2. `README.md` + `docs/ARCHITECTURE.md` — verbindliche Source of Truth für
   Produktverhalten und Architektur.
3. `docs/CODEMAP.md` — schnelle Datei-Orientierung bei Einstieg in einen
   unbekannten Bereich.
4. `docs/AGENT_CONTEXT_PACKS.md` — das passende Pack **vor** jeder
   Implementierung lesen (Zweck, Pflichtlektüre, Risiken, Mindestchecks, No-Gos
   je Aufgabenbereich).
5. `docs/VALIDATION_MATRIX.md` — vor jedem Handoff/PR, um die korrekten
   Checks für den jeweiligen Änderungstyp zu bestimmen.

Für Datenmodellfragen zusätzlich `docs/DATA_MODEL.md`, für Datenschutz-/
Permission-Fragen `docs/PRIVACY_CONTEXT.md`.

## Agent Workflow

- Repo-local Codex surfaces live under `plugins/montagezeit-android/` and `.agents/plugins/marketplace.json`.
- Keep tracked editor/tooling surfaces minimal: `.agents/plugins/marketplace.json` and `.vscode/tasks.json` are intentional, while local assistant or IDE state such as `.claude/`, `.clinerules/`, `.kilo/`, and personal VS Code settings/extensions should remain untracked.
- Prefer the repo-local skill `montagezeit-android-maintainer` for Android, Compose, Room, WorkManager, export, and verification work in this repo.
- Prefer the official GitHub plugin or connector for pull requests, review threads, labels, comments, and CI triage. Fall back to local `git` and `gh` only when the connector path is insufficient.
- Keep external tooling narrow. Do not reach for Drive, Notion, Cloudflare, or unrelated connectors unless the task explicitly needs them.
- For Android development work, stay shell-first: `rg`, `git`, `./gradlew`, `adb`, and repo scripts are the primary tools.

## Project Structure & Module Organization

This is a single-module Android project (`:app`) built with Kotlin and Jetpack Compose.

- App code: `app/src/main/java/de/montagezeit/app/`
- Unit tests: `app/src/test/java/de/montagezeit/app/`
- Instrumented tests: `app/src/androidTest/java/de/montagezeit/app/`
- Resources/manifest: `app/src/main/res/`, `app/src/main/AndroidManifest.xml`
- Architecture and product docs: `README.md`, `docs/ARCHITECTURE.md`
- CI workflows: `.github/workflows/`

Code is organized by layer and feature (`ui/`, `domain/usecase/`, `data/`, `work/`, `notification/`, `export/`, `di/`).

## Build, Test, and Development Commands

Use the Gradle wrapper from repo root:

- `./gradlew clean`: remove old build outputs
- `./gradlew detekt`: Kotlin static analysis
- `./gradlew lint`: Android lint checks
- `./gradlew :app:testDebugUnitTest`: CI-aligned local unit-test suite
- `./gradlew test`: all unit tests
- `./gradlew assembleDebug`: build debug APK
- `./gradlew assembleRelease`: build release APK
- `./gradlew connectedDebugAndroidTest`: run instrumentation tests on device/emulator
- `./gradlew jacocoTestDebugUnitTestReport`: generate unit-test coverage report
- `bash scripts/hooks/run_local_quality_gate.sh`: detekt + lint + unit tests + debug build
- `./scripts/setup_hooks.sh`: install the repository `lefthook` hooks
- `./scripts/android_debug_run.sh [DEVICE_SERIAL]`: build, install, launch, and tail logcat for a connected device

Use the smallest relevant verification set first. Escalate to the full local quality gate when a change spans multiple layers or before handoff on broad refactors.

## Coding Style & Naming Conventions

- Follow idiomatic Kotlin style with 4-space indentation and descriptive names.
- Keep package paths under `de.montagezeit.app`.
- Screens use `*Screen` names.
- View models use `*ViewModel`.
- Use cases are verb-focused, for example `RecordCheckIn`.
- Tests use `*Test`.
- Prefer resource strings in `app/src/main/res/values/strings.xml` over hardcoded UI text.
- Avoid committing machine-specific values such as device serials, local absolute paths, or personal IDE settings.

## Testing Guidelines

Current stack includes JUnit4, MockK, Coroutines Test, Robolectric, and AndroidX/Compose test libs.

- Add or update tests for behavior changes in `domain`, `work`, `notification`, and `export`.
- Use behavior-focused test names such as ``fun `invoke creates new morning snapshot entry`()``.
- Keep tests deterministic and avoid real network or device dependencies in unit tests.
- Room schema or migration changes must update migration coverage and any doc references to schema versions.
- Reminder, notification, and receiver changes should keep the corresponding worker, notification, and receiver tests green.
- UI or ViewModel work should run the closest targeted test first, then `./gradlew :app:testDebugUnitTest` when the behavior change is broader than a single screen.

## Commit & Pull Request Guidelines

Commit history follows conventional prefixes: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`. Optional scopes are fine, for example `feat(today): ...`.

- Keep commits focused and imperative.
- PRs should include a summary, linked issue or task, commands run, and screenshots for UI changes.
- Before opening a PR, run at least `./gradlew lint`, `./gradlew :app:testDebugUnitTest`, and `./gradlew assembleDebug`.
- Use `.github/pull_request_template.md` (auto-populated by GitHub) — fill in
  Validation, Documentation Freshness Check, and Risk Check, do not skip them.
- For agent-to-agent or agent-to-user handoff, copy the structure from
  `docs/AGENT_HANDOFF_TEMPLATE.md`.
- CI (`.github/workflows/ci.yml`, "Quality Gate") runs detekt + lint + unit
  tests + debug build on every push/PR to `main` and uploads lint/test
  reports as artifacts. It mirrors `bash scripts/hooks/run_local_quality_gate.sh`
  — run that locally before opening a PR so CI does not surprise you.
- **Documentation Freshness:** if a change touches product behavior, the data
  model, permissions, or developer workflow, update the matching doc in the
  same change (`README.md`, `docs/ARCHITECTURE.md`, `docs/DATA_MODEL.md`,
  `docs/PRIVACY_CONTEXT.md`, or the relevant context pack). Do not let docs
  drift from code.

## Security & Configuration Tips

- Do not commit API keys, keystores, or local debug artifacts or log exports.
- Treat permission changes in `AndroidManifest.xml` as security-sensitive and document the rationale.
- Keep repo automation and workflow files intentional; if you change hooks, scripts, or contributor docs, keep them in sync.

## No-Gos

- Keine Standortberechtigung, kein GPS — Tagesort bleibt manueller Text
  (siehe `docs/PRIVACY_CONTEXT.md`).
- Kein Cloud-Sync, kein Backend, kein Login.
- Keine Secrets, Keystores oder Logs committen.
- Keine Toolchain-/Dependency-Upgrades ohne separaten, expliziten Auftrag.
- Keine Room-Migration ohne separaten, expliziten Auftrag — auch nicht
  „vorsorglich".

## Branch Protection (empfohlen für `main`)

GitHub-Branch-Protection ist für dieses Repo aktuell nicht per Code gesetzt.
Empfohlene manuelle Einstellung für `main`, bis das jemand mit Repo-Admin-
Rechten in den GitHub-Settings konfiguriert:

- Pull Request vor jedem Merge (kein Direkt-Push auf `main`)
- Required Status Check: „Quality Gate" (`.github/workflows/ci.yml`)
- Kein ungeprüfter Direkt-Merge bei rotem Check
- Optional: Branch vor Merge aktuell halten ("Require branches to be up to date")
