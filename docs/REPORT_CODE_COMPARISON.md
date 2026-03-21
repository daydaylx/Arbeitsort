# Vergleich: Deep-Research-Report vs. aktueller Code

Stand: 2026-03-21

Quelle des Vergleichs:
- Report: `/home/d/Schreibtisch/deep-research-report.md`
- Repo-Stand: lokaler Arbeitsbaum dieses Repos

Methodik:
- Verglichen wurden nur technisch pruefbare Aussagen aus dem Report.
- Source of truth ist der aktuelle Code, nicht der Report, nicht der Changelog.
- Status `zutreffend` bedeutet: Die Kernaussage wird durch den aktuellen Code gedeckt.
- Status `teilweise zutreffend` bedeutet: Der technische Kern ist sichtbar, aber der Report mischt alte Details, starke Interpretationen oder unvollstaendige Ableitungen hinein.
- Status `veraltet` bedeutet: Der aktuelle Code widerspricht dem Report.
- Status `nicht belegbar` bedeutet: Der lokale Repo-Stand liefert dafuer keine belastbare Evidenz.

## Vergleichsmatrix

| Report-Befund | Status | Code-Evidenz | Einordnung / Korrektur |
|---|---|---|---|
| Viele Flow-Collections im Root von `TodayScreenV2` fuehren zu breiten Recomposition-Flaechen. | teilweise zutreffend | `app/src/main/java/de/montagezeit/app/ui/screen/today/TodayScreenV2.kt:63-87` sammelt viele einzelne Flows direkt im Root-Composable. | Die strukturelle Beobachtung ist korrekt. Der Report belegt damit aber noch keinen gemessenen Runtime-Engpass; er beschreibt ein plausibles Risiko, keinen nachgewiesenen Performance-Defekt. |
| In `TodayViewModel.observeEntryUpdates()` behauptet ein Kommentar Debounce, der Code hat aber keines. | zutreffend | `app/src/main/java/de/montagezeit/app/ui/screen/today/TodayViewModel.kt:237-245` kommentiert `A04: debounce`, ruft aber schlicht `selectedEntry.collect { loadStatistics(); loadWeekOverview() }` auf. | Der Report trifft den Ist-Zustand genau: kein `debounce`, kein `distinctUntilChanged`, kein `collectLatest`. |
| `_todayDate` kann bei ueber Nacht offen gelassener App stale bleiben. | teilweise zutreffend | `_todayDate` wird in `TodayViewModel.kt:115-124` fuer `todayEntry` verwendet und in `TodayViewModel.kt:247-253` nur bei `loadStatistics()` aktualisiert. | Die Risikobeschreibung ist plausibel. Ein echter Fehlerfall entsteht nur dann, wenn die App ueber Mitternacht offen bleibt und kein weiterer Trigger `loadStatistics()` ausloest. Das ist ein Edge Case, aber nicht widerlegt. |
| Ein frueherer Audit-Fund zu unbegrenzt wachsenden Reminder-Flags ist bereits behoben. | zutreffend | `app/src/main/java/de/montagezeit/app/data/preferences/ReminderFlagsStore.kt:37-40` definiert `RETENTION_DAYS = 7`; `ReminderFlagsStore.kt:116-140` bereinigt alte Eintraege ueber `cleanupOldDates(...)`. | Der Report bewertet diesen Alt-Fund korrekt als bereits erledigt. |
| `WorkEntryDao.readModifyWrite()` existiert, wird aber nicht konsequent von allen schreibenden Use Cases genutzt. | zutreffend | `app/src/main/java/de/montagezeit/app/data/local/dao/WorkEntryDao.kt:73-77` definiert das transaktionale Muster. `RecordDailyManualCheckIn.kt:46-83` nutzt es bereits, waehrend z.B. `ConfirmWorkDay.kt:26-65`, `ConfirmOffDay.kt:36-52` und `RecordMorningCheckIn.kt:15-21` weiterhin `getByDate(...)` plus `upsert(...)` in zwei Schritten ausfuehren. | Die Report-Empfehlung ist inhaltlich richtig. Der Punkt ist kein Totalbefund gegen das ganze Repo, sondern ein Konsistenzproblem zwischen einzelnen Use Cases. |
| CSV-Export ist gegen Formula Injection noch nicht gehaertet. | teilweise zutreffend | `app/src/main/java/de/montagezeit/app/export/CsvExporter.kt:27-30` quotet nur bei `;`, `"` oder Zeilenumbruechen. `CsvExporter.kt:102` und `CsvExporter.kt:126` schreiben `dayLocationLabel` und `note` ohne Prefix-Haertung fuer fuehrende `=`, `+`, `-`, `@`. | Das Formula-Injection-Risiko ist real. Der Report mischt hier aber eine veraltete Nebenannahme hinein: Der aktuelle Exporter entfernt CR/LF nicht, sondern quotet solche Felder. Der Changelog (`CHANGELOG.md:61-66`) und `CsvExporterLogicTest.kt:24-65` spiegeln aeltere Sanitizing-Logik, die aktuelle Implementierung jedoch nicht. |
| Globales Lint-Disable `NewApi` maskiert potenzielle API-Risiken. | zutreffend | `app/build.gradle.kts:89-96` enthaelt `disable += "NewApi"`. | Die Codeaussage ist korrekt. Ob das im Projektkontext akzeptabel ist, bleibt eine Engineering-Entscheidung; die globale Deaktivierung existiert aber tatsaechlich. |
| Zwei GitHub-Workflows verursachen einen redundanten Doppelbuild. | zutreffend | `.github/workflows/android.yml:3-26` und `.github/workflows/ci.yml:3-52` triggern beide auf Push und Pull Request gegen `main`. | Der Report trifft den Repo-Stand. `ci.yml` ist der staerkere Quality-Gate-Workflow; `android.yml` fuegt im aktuellen Zustand kaum eigenstaendigen Wert hinzu. |
| CI fuehrt keine Instrumentation-Tests aus. | zutreffend | `.github/workflows/ci.yml:31-38` fuehrt nur `lint`, `testDebugUnitTest` und `assembleDebug` aus; `.github/workflows/android.yml:25-26` fuehrt nur `./gradlew build` aus. `README.md:125-127` nennt `connectedDebugAndroidTest` nur als optionalen lokalen Schritt. | Der Report beschreibt den Ist-Zustand korrekt. Instrumentation-Test-Struktur existiert im Repo, ist aber nicht im CI-Gate eingebunden. |
| Coverage-/Quality-Metriken fehlen im Build/CI. | zutreffend | Repo-Suche nach `kover`, `jacoco`, `coverage` und `codecov` in `build.gradle.kts`, `app/build.gradle.kts` und `.github/workflows/` liefert keine Konfiguration. | Der Befund ist als Repo-Beobachtung korrekt. Der Report kann daraus fehlende Messung ableiten, nicht aber automatisch fehlende Testqualitaet. |
| Es gibt eine Versionsdrift: Changelog 1.0.2, Gradle 1.0.1. | veraltet | `app/build.gradle.kts:28-33` setzt `versionName = "1.0.2"` und `versionCode = 3`; `CHANGELOG.md:16-16` fuehrt ebenfalls `## [1.0.2]`. | Diese Aussage ist im aktuellen Repo-Stand ueberholt. Die Versionierung ist derzeit konsistent. |
| Der Foreground-Service-Typ `specialUse` ist korrekt deklariert; der Punkt ist eher ein Compliance-Check als ein Bug. | zutreffend | `app/src/main/AndroidManifest.xml:43-52` deklariert `CheckInActionService` mit `android:foregroundServiceType="specialUse"` und `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; `app/build.gradle.kts:28-33` setzt `targetSdk = 34`. | Die technische Beobachtung des Reports passt zum Manifest. Aus dem lokalen Code laesst sich aber keine Aussage darueber ableiten, ob ein Store-/Policy-Review die Begruendung akzeptiert. |
| Der `CsvExporter` existiert, ist aber nicht an den sichtbaren UI-/Settings-Flow angebunden. | zutreffend | `README.md:65-73` dokumentiert den Zustand. Repo-Suche nach `CsvExporter` und `exportToCsv(` findet nur die Exporter-Klasse selbst, Tests, Doku und Audit-Dateien, aber keinen sichtbaren Screen-/ViewModel-Aufruf. | Der Report beschreibt den aktuellen Integrationsgrad korrekt: CSV-Code ist vorhanden, aber kein aktiver Endnutzer-Flow ist im sichtbaren App-Pfad verdrahtet. |

## Wichtige Abweichungen zwischen Report, Code und Begleitdoku

1. Der CSV-Teil ist unsauber synchronisiert.
   - Der Changelog sagt, Notizfelder wuerden CR/LF entfernen (`CHANGELOG.md:61-66`).
   - `CsvExporterLogicTest.kt:24-65` testet ebenfalls eine Sanitizing-Variante mit `replace("\n", " ")` und `replace("\r", "")`.
   - Der aktuelle `CsvExporter.kt:27-30` tut das nicht, sondern quotet Felder mit Zeilenumbruechen.

2. Der Report trifft mehrere Architektur- und CI-Befunde sauber, zieht bei Performance aber bewusst probabilistische Schluesse.
   - Beispiel: Viele Root-Collects und fehlendes Debounce sind im Code direkt sichtbar.
   - Nicht direkt sichtbar sind gemessene Jank-Werte, DB-Lastprofile oder Nutzerfolgen auf echten Geraeten.

3. Der Punkt "Version drift" ist inzwischen ueberholt.
   - Das war vermutlich beim Recherchestand korrekt oder aus einem aelteren Snapshot abgeleitet.
   - Im aktuellen Repo ist der Widerspruch nicht mehr vorhanden.

## Kurzfazit

Der Report ist fuer die Today-/State-/CI-Themen insgesamt belastbar: Die meisten Kernbefunde sind im aktuellen Code nachvollziehbar. Die staerksten Abweichungen liegen nicht bei den grossen Architekturpunkten, sondern in den Details rund um CSV: Dort sind Report, Changelog, Tests und Implementierung nicht mehr voll synchron. Der klar veraltete Report-Punkt ist die behauptete Versionsdrift zwischen Changelog und Gradle; dieser Befund trifft auf den aktuellen Stand nicht mehr zu.
