# Contributing to MontageZeit

MontageZeit is a single-module Android app focused on local-first workday tracking. Keep changes small, tested, and aligned with the active product and architecture docs.

## Source of Truth

The binding references for behavior and architecture are:

- `README.md`
- `docs/ARCHITECTURE.md`

When implementation changes make either document stale, update the docs in the same change. Files under `docs/ARCHIVE/` are historical and should not be refreshed unless the task explicitly targets archived material.

## Local Setup

Prerequisites:

- JDK 17
- Android SDK / Platform Tools
- `adb` for device workflows
- `lefthook` for local Git hooks

Recommended first run from the repo root:

```bash
./gradlew assembleDebug
./scripts/setup_hooks.sh
```

If `lefthook` is not installed yet, the setup script prints the required next step.

## Daily Workflow

Core commands:

```bash
./gradlew lint
./gradlew :app:testDebugUnitTest
./gradlew assembleDebug
```

Useful device workflow:

```bash
./scripts/android_debug_run.sh [DEVICE_SERIAL]
```

If exactly one device is connected, the serial argument is optional. For multiple devices, pass a serial explicitly or set `ANDROID_DEVICE_SERIAL`.

## Local Hooks

The repository uses `lefthook` for fast local guardrails:

- `pre-commit`: staged diff sanity checks via `git diff --cached --check`
- `pre-push`: `lint`, `:app:testDebugUnitTest`, and `assembleDebug`

Install once:

```bash
./scripts/setup_hooks.sh
```

To bypass hooks for an exceptional case, use standard Git flags such as `--no-verify`. Do that rarely and only with a clear reason.

## Change Expectations

- Use the Gradle wrapper from the repo root.
- Prefer resource strings over hardcoded UI text.
- Keep package names under `de.montagezeit.app`.
- Avoid machine-specific values in tracked files such as device serials, local paths, or personal IDE settings.
- Treat `AndroidManifest.xml` permission changes as security-sensitive and document the rationale.

Change-specific validation:

- UI / ViewModel changes: relevant screen or view model tests plus `./gradlew :app:testDebugUnitTest`
- Reminder / worker changes: relevant tests under `app/src/test/java/de/montagezeit/app/work/` and notification/receiver coverage as needed
- Export changes: tests under `app/src/test/java/de/montagezeit/app/export/`
- Room schema or migration changes: migration tests and matching doc updates when schema version changes

## Commit Style

Use focused commits with conventional prefixes:

- `feat:`
- `fix:`
- `refactor:`
- `test:`
- `chore:`

Optional scopes are fine, for example `feat(today): ...`.
