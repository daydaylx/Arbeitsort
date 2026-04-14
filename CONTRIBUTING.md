# Contributing to MontageZeit

MontageZeit is a single-module Android app focused on local-first workday tracking. Keep changes small, tested, and aligned with the active product and architecture docs.

## Source of Truth

The binding references for behavior and architecture are:

- `README.md`
- `docs/ARCHITECTURE.md`

When implementation changes make either document stale, update the docs in the same change. Files under `docs/ARCHIVE/` are historical and should not be refreshed unless the task explicitly targets archived material.

For AI-assisted work:

- `AGENTS.md` is the canonical repo instruction set for Codex-style agents.
- `CLAUDE.md` and `.claude/*` are compatibility surfaces and should stay thinner than `AGENTS.md`.
- Repo-local Codex plugin and skills live under `plugins/montagezeit-android/`.
- Prefer GitHub as the only external connector for PR and CI workflows; keep Android implementation work local and shell-first.

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

- `pre-commit`: fast staged diff and repo hygiene checks
- `pre-push`: `lint`, `:app:testDebugUnitTest`, and `assembleDebug`

Install once:

```bash
./scripts/setup_hooks.sh
```

To bypass hooks for an exceptional case, use standard Git flags such as `--no-verify`. Do that rarely and only with a clear reason.

## Tracked vs Local-Only Tooling Files

Keep repo-tracked tooling intentionally small:

- Tracked: `.agents/plugins/marketplace.json`, `.vscode/tasks.json`
- Local-only: `.claude/`, `.clinerules/`, `.kilo/`, personal VS Code settings or extension recommendations, generated tool worktrees and caches

The pre-commit hook blocks staged local-only or machine-specific files such as assistant workspaces, personal IDE config, local secrets, and debug/log artifacts. It also prints staged, unstaged tracked, and untracked files so mixed worktrees stay visible before commit.

## Change Expectations

- Use the Gradle wrapper from the repo root.
- Prefer resource strings over hardcoded UI text.
- Keep package names under `de.montagezeit.app`.
- Avoid machine-specific values in tracked files such as device serials, local paths, or personal IDE settings.
- Treat `AndroidManifest.xml` permission changes as security-sensitive and document the rationale.
- Expect the local workflow to stay split: fast hygiene checks at `git commit`, then `lint`, `:app:testDebugUnitTest`, and `assembleDebug` at `git push`.

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
