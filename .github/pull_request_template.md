## Summary

<!-- 1-3 Sätze: was ändert sich und warum. -->

## Changed areas

<!-- z. B. Domain UseCase, Room Migration, Reminder/WorkManager, Compose UI, Agenten-Doku, CI ... -->

## Linked issue/task

<!-- z. B. Closes #NN, oder "kein verlinktes Issue" -->

## Validation

Passende Checks aus [`docs/VALIDATION_MATRIX.md`](../docs/VALIDATION_MATRIX.md)
je Änderungstyp; vor Merge mindestens:

- [ ] `./gradlew detekt`
- [ ] `./gradlew lint`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] `bash scripts/hooks/run_local_quality_gate.sh`

## Documentation Freshness Check

| Bereich                                                                      | Betroffen? | Aktion                     |
| ---------------------------------------------------------------------------- | ---------: | -------------------------- |
| README / Setup                                                               |    ja/nein | aktualisiert / nicht nötig |
| Agenten-Doku (`AGENTS.md`, `docs/CODEMAP.md`, `docs/AGENT_CONTEXT_PACKS.md`) |    ja/nein | aktualisiert / nicht nötig |
| Validation Matrix                                                            |    ja/nein | aktualisiert / nicht nötig |
| Datenmodell / Privacy                                                        |    ja/nein | aktualisiert / nicht nötig |
| UI / Export / Reminder                                                       |    ja/nein | aktualisiert / nicht nötig |

## Risk Check

- [ ] Keine Secrets/Keystores/Logs/lokalen Tooling-Dateien committet
- [ ] Keine unerlaubte Permission-/Privacy-Änderung (siehe [`docs/PRIVACY_CONTEXT.md`](../docs/PRIVACY_CONTEXT.md))
- [ ] Keine Room-Migration ohne separaten, expliziten Auftrag
- [ ] Keine Toolchain-/Dependency-Upgrades ohne separaten Auftrag

## Screenshots

<!-- Nur falls UI betroffen ist. -->

## Manual tests

<!-- Nur falls relevant: welches Szenario aus docs/MANUAL_TEST_SCENARIOS.md wurde geprüft, mit Ergebnis. -->
