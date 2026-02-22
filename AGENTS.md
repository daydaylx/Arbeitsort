# Repository Guidelines

## Project Structure & Module Organization
This is a single-module Android project (`:app`) built with Kotlin and Jetpack Compose.
- App code: `app/src/main/java/de/montagezeit/app/`
- Unit tests: `app/src/test/java/de/montagezeit/app/`
- Instrumented tests: `app/src/androidTest/java/de/montagezeit/app/`
- Resources/manifest: `app/src/main/res/`, `app/src/main/AndroidManifest.xml`
- Architecture and product docs: `README.md`, `docs/ARCHITECTURE.md` (primary references)
- CI workflows: `.github/workflows/`

Code is organized by layer and feature (`ui/`, `domain/usecase/`, `data/`, `work/`, `notification/`, `export/`, `di/`).

## Build, Test, and Development Commands
Use the Gradle wrapper from repo root:
- `./gradlew clean`: remove old build outputs.
- `./gradlew lint`: run Android lint checks.
- `./gradlew :app:testDebugUnitTest`: run the main local unit-test suite (CI-aligned).
- `./gradlew test`: run all unit tests (all variants).
- `./gradlew assembleDebug`: build debug APK.
- `./gradlew assembleRelease`: build release APK (minification enabled).
- `./gradlew connectedDebugAndroidTest`: run instrumentation tests on device/emulator.

## Coding Style & Naming Conventions
- Follow idiomatic Kotlin style with 4-space indentation and descriptive names.
- Keep package paths under `de.montagezeit.app`.
- Naming: screens use `*Screen` (for example, `HistoryScreen`).
- Naming: view models use `*ViewModel`.
- Naming: use cases are verb-focused (for example, `RecordEveningCheckIn`).
- Naming: tests use `*Test`.
- Prefer resource strings in `app/src/main/res/values/strings.xml` over hardcoded UI text.

## Testing Guidelines
Current stack includes JUnit4, MockK, Coroutines Test, Robolectric, and AndroidX/Compose test libs.
- Add or update tests for behavior changes in `domain`, `work`, and `export` logic.
- Use behavior-focused test names (for example, ``fun `invoke creates new morning snapshot entry`()``).
- Keep tests deterministic and avoid real network/device dependencies in unit tests.

## Commit & Pull Request Guidelines
Commit history follows conventional prefixes: `feat:`, `fix:`, `refactor:`, `test:`, `chore:` (optionally scoped, for example `feat(today): ...`).
- Keep commits focused and in imperative style.
- PRs should include: summary, linked issue/task, commands run, and screenshots for UI changes.
- Before opening a PR, run at least: `./gradlew lint`, `./gradlew :app:testDebugUnitTest`, `./gradlew assembleDebug`.

## Security & Configuration Tips
- Do not commit API keys, keystores, or local debug artifacts/log exports.
- Treat permission changes in `AndroidManifest.xml` as security-sensitive and document rationale in PRs.
