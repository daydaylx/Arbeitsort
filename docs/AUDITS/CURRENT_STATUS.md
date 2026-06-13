# Aktueller Audit- und UI-Status

Stand: 2026-06-13 (zuletzt geaendert: 2026-06-13)

Diese Datei ersetzt die alten Audit-Reports, Weekly-Reviews, UI-Planungsberichte und Archivnotizen. Fuer den aktuellen Produktstand gelten weiterhin Code, `README.md` und `docs/ARCHITECTURE.md`.

## Offene Punkte

| ID      | Prioritaet | Bereich        | Aufgabe                                                                                                                                                                                                                                              |
| ------- | ---------- | -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ~~O01~~ | ~~Hoch~~   | ~~Datenfluss~~ | ~~`HistoryViewModel.applyBatchEdit` fachlich als atomaren Range-UseCase absichern oder dokumentiert auf bestehende Repository-Serialisierung begrenzen.~~ Erledigt: Limitation in Code dokumentiert (Kommentar in `applyBatchEdit`).                 |
| ~~O02~~ | ~~Mittel~~ | ~~Fachlogik~~  | ~~Entscheidung fuer `COMP_TIME` mit Travel treffen und danach Statistik, Ueberstunden und UI konsistent machen.~~ Erledigt: Reisezeit an Nicht-Arbeitstagen wird ignoriert und nur bei `WORK` gewertet. Dokumentiert in `CalculateOvertimeForRange`. |
| O03     | Mittel     | Migration      | Version-15-Fixture in `AppDatabaseSchemaMigrationTest` an echte v15-Spaltenbreite angleichen oder Abweichung dokumentieren.                                                                                                                          |
| O04     | Mittel     | Receiver       | Echten `scheduleAll()`-Pfad fuer `BootReceiver` und `TimeChangeReceiver` testen, nicht nur Intent-Filter spiegeln.                                                                                                                                   |
| O05     | Niedrig    | Accessibility  | Verbleibende dekorative `contentDescription = null`-Stellen pruefen und bei interaktiven Icons sprechende Labels ergaenzen.                                                                                                                          |
| O06     | Niedrig    | UI QA          | Travel-Modus-Selektor auf Geraet/Emulator fuer alle Modi mit Daten-Roundtrip testen.                                                                                                                                                                 |

## Zusammengefasste erledigte UI-Arbeiten

- UI-Reduktion: redundante Hero-/Dashboard-Elemente, WeekOverview, Quick-Link-Panels, alte History-Kalenderoptionen und mehrere doppelte Aktionspfade wurden entfernt oder in ruhigere Nebenpfade verschoben.
- Usability-Verbesserung: Today, Editor, History, Settings und Overview wurden staerker auf Alltagspfade ausgerichtet; Travel-Erfassung nutzt einen Modus-Selektor; Editor-Formularreihenfolge und Fehleranzeige wurden vereinfacht.
- Visueller System-Cleanup: Glassmorphism, Glow-Orbs, starke Verlaeufe und transparente Panels wurden durch solide Dark-Surfaces, dezente Borders, kompaktere Radien und klarere Aktionshierarchie ersetzt.

## Letzte Verifikation

Stand 2026-05-05 (vor DB-v17):

- `./gradlew :app:compileDebugKotlin`: erfolgreich.
- `./gradlew lint`: erfolgreich.
- `./gradlew assembleDebug`: erfolgreich.
- `./gradlew test`: erfolgreich. Alle Unit-Tests gruen (inkl. `HistoryViewModelTest`).

Neuere Aenderungen (Commit `55c07ca`, DB-Version 17) noch nicht vollstaendig in dieser Datei verifikationsbestaetigt:

- `DatabaseBackupManager` eingefuehrt, aufgerufen via `DatabaseModule.backupIfVersionMismatch()`.
- DB-Migration v16→17: SCHULUNG/LEHRGANG → WORK. Tests: `AppDatabaseMigrationTest`, `DatabaseBackupManagerTest`.
- Agent-Workflow-Dokumentation hinzugefuegt (CODEMAP.md, DATENMODELL.md, MANUELLE_TESTS.md, PRUEFMATRIX.md, ADRs, Context-Packs).

## Entfernte historische Quellen

Die folgenden Dokumenttypen wurden in diese Statusdatei zusammengefuehrt und geloescht:

- alte Audit-Vollberichte und Verifikationsberichte
- Weekly-Reviews
- UI-Planungs- und Umsetzungsberichte
- sshterm-/UI-Migrationsnotizen
- altes Archivmaterial und veraltete Checklisten
- exportierte Jira-/Backlog-Zwischenlisten
