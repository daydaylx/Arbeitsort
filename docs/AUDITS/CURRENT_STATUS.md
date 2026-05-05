# Aktueller Audit- und UI-Status

Stand: 2026-05-05

Diese Datei ersetzt die alten Audit-Reports, Weekly-Reviews, UI-Planungsberichte und Archivnotizen. Fuer den aktuellen Produktstand gelten weiterhin Code, `README.md` und `docs/ARCHITECTURE.md`.

## Offene Punkte

| ID | Prioritaet | Bereich | Aufgabe |
| --- | --- | --- | --- |
| O01 | Hoch | Datenfluss | `HistoryViewModel.applyBatchEdit` fachlich als atomaren Range-UseCase absichern oder dokumentiert auf bestehende Repository-Serialisierung begrenzen. |
| O02 | Mittel | Fachlogik | Entscheidung fuer `COMP_TIME` mit Travel treffen und danach Statistik, Ueberstunden und UI konsistent machen. |
| O03 | Mittel | Migration | Version-15-Fixture in `AppDatabaseSchemaMigrationTest` an echte v15-Spaltenbreite angleichen oder Abweichung dokumentieren. |
| O04 | Mittel | Receiver | Echten `scheduleAll()`-Pfad fuer `BootReceiver` und `TimeChangeReceiver` testen, nicht nur Intent-Filter spiegeln. |
| O05 | Niedrig | Accessibility | Verbleibende dekorative `contentDescription = null`-Stellen pruefen und bei interaktiven Icons sprechende Labels ergaenzen. |
| O06 | Niedrig | UI QA | Travel-Modus-Selektor auf Geraet/Emulator fuer alle Modi mit Daten-Roundtrip testen. |

## Zusammengefasste erledigte UI-Arbeiten

- UI-Reduktion: redundante Hero-/Dashboard-Elemente, WeekOverview, Quick-Link-Panels, alte History-Kalenderoptionen und mehrere doppelte Aktionspfade wurden entfernt oder in ruhigere Nebenpfade verschoben.
- Usability-Verbesserung: Today, Editor, History, Settings und Overview wurden staerker auf Alltagspfade ausgerichtet; Travel-Erfassung nutzt einen Modus-Selektor; Editor-Formularreihenfolge und Fehleranzeige wurden vereinfacht.
- Visueller System-Cleanup: Glassmorphism, Glow-Orbs, starke Verlaeufe und transparente Panels wurden durch solide Dark-Surfaces, dezente Borders, kompaktere Radien und klarere Aktionshierarchie ersetzt.

## Letzte Verifikation

- `./gradlew :app:compileDebugKotlin`: erfolgreich.
- `./gradlew lint`: erfolgreich.
- `./gradlew assembleDebug`: erfolgreich.
- `./gradlew test`: fehlgeschlagen in `HistoryViewModelTest`, Test `applyBatchEdit reports no changes when request produces no updates`.
  - Ursache laut Stacktrace: `Dispatchers.Main` bzw. `android.os.Looper.getMainLooper` wird im Unit-Test-Kontext initialisiert, ausgelöst aus `HistoryViewModel.uiState`.
  - Betroffene Datei: `app/src/test/java/de/montagezeit/app/ui/screen/history/HistoryViewModelTest.kt`.

## Entfernte historische Quellen

Die folgenden Dokumenttypen wurden in diese Statusdatei zusammengefuehrt und geloescht:

- alte Audit-Vollberichte und Verifikationsberichte
- Weekly-Reviews
- UI-Planungs- und Umsetzungsberichte
- sshterm-/UI-Migrationsnotizen
- altes Archivmaterial und veraltete Checklisten
- exportierte Jira-/Backlog-Zwischenlisten
