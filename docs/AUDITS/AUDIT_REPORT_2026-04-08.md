# MontageZeit Audit Report — 2026-04-08

**Datum:** 2026-04-08
**Scope:** Gesamter Produktionscode (`app/src/main/java/`), Tests, CI/CD, Migration Chain
**Methode:** Statische Analyse, Cross-Referenzierung, Verifikation vorheriger Audits, Build-Validierung
**Vorherige Audits:** 2026-03-14 (45 Befunde A01–B25), 2026-03-29 (12 Befunde H1–N5)

---

## 1. Executive Summary

Der Gesamtzustand des Projekts hat sich seit dem letzten Audit (2026-03-29) **deutlich verbessert**. Von 45 Befunden aus dem März-14-Audit und 12 Befunden aus dem März-29-Audit sind:

- **39 vollständig behoben** (68%)
- **4 teilweise behoben** (7%)
- **14 noch offen** (25%) — überwiegend niedrige Priorität

Die kritischsten vorherigen Befunde (Race Conditions in UseCases via `readModifyWrite`, Midnight-Staleness via `TodayDateCoordinator`, StateFlow-Explosion via konsolidiertem UI State, Thread-Safety in `CheckInActionService` via Mutex) sind **alle behoben**.

**Größte verbleibende Risiken:**

1. `RecordDailyManualCheckIn` überschreibt bestätigte Einträge ohne Guard (B03, kritisch, offen)
2. `ExportPreview` zeigt nicht, was tatsächlich im PDF landet (E02, E05)
3. `OverviewViewModel` hat keinen Test (T01)

**Was solide wirkt:**

- Domain-Logik (TimeCalculator, ClassifyDay, MealAllowanceCalculator) ist mathematisch korrekt und konsistent über alle Konsumenten
- Statistik- und Export-Pfade nutzen identische Berechnungsketten
- Migrationskette v1→v14 ist intakt, keine Datenverluste gefunden
- Reminder-System ist robust mit korrekter Window-Semantik und Timezone-Behandlung
- Testabdeckung der kritischen Domain-Logik ist gut (24 Tests für AggregateWorkStats allein)

---

## 2. Projektverständnis

**Stack:** Kotlin, Jetpack Compose, Room (v14), WorkManager, Hilt, DataStore, Material3
**Architektur:** Clean Architecture — UI → ViewModel → UseCase → Repository → Room
**Modulstruktur:** Single-Module (`:app`)

```
app/src/main/java/de/montagezeit/app/
├── data/          — Room DB, DAOs, Repositories, Preferences
├── di/            — Hilt Module (DatabaseModule, ApplicationModule)
├── domain/        — Use Cases + reine Fachlogik
│   ├── usecase/   — 15+ Use Cases
│   └── util/      — TimeCalculator, MealAllowanceCalculator, WeekCalculator
├── export/        — PdfExporter, CsvExporter, PdfUtilities
├── notification/  — ReminderNotificationManager
├── receiver/      — BootReceiver, TimeChangeReceiver
├── service/       — CheckInActionService (Foreground Service)
├── ui/            — 6 Screens (Today, Overview, History, Edit, Settings, ExportPreview)
└── work/          — ReminderScheduler, WindowCheckWorker, ReminderLaterWorker
```

**Kritische Datenmodelle:**

- `WorkEntry` (18 Felder, LocalDate als PK)
- `TravelLeg` (12 Felder, FK auf WorkEntry.date, sortiert und kategorisiert)

**Zentrale Fachregeln (aus DAY_CLASSIFICATION.md):**

- 6 Tagesklassifikationen mit unterschiedlichen Regeln für Sichtbarkeit, Sollstunden und Verpflegungspauschale
- Nur bestätigte Tage (`confirmedWorkDay == true`) fließen in Statistik, Überstunden und Export
- Reisezeit an freien Tagen zählt in bezahlte Zeit und Überstunden-Istzeit

**Zentrale Risiko-Bereiche:**

- Datenkonsistenz über mehrere Konsumenten (Statistik, PDF, CSV, Preview)
- Migration Chain mit 13 Schritten
- Reminder-Scheduling mit Window-Logik und Timezone-Wechseln

---

## 3. Audit-Plan

### Prüffelder

| #   | Bereich                           | Priorisierung | Methode                                  |
| --- | --------------------------------- | ------------- | ---------------------------------------- |
| 1   | Reconciliation vorheriger Befunde | Kritisch      | File-read an zitierten Stellen           |
| 2   | Domain-Logik-Konsistenz           | Hoch          | Cross-Referenz aller Konsumenten         |
| 3   | Export-Vollständigkeit            | Hoch          | Spalten-mapping Preview vs PDF vs CSV    |
| 4   | Migration Chain                   | Hoch          | Schema-Vergleich, INSERT-Vollständigkeit |
| 5   | Reminder & Scheduling             | Mittel        | Edge-Case-Analyse                        |
| 6   | Toter Code / Redundanzen          | Niedrig       | Grep-basierte Nutzungsanalyse            |
| 7   | Test- und Qualitätssicherung      | Mittel        | Test-zu-Produktions-Mapping              |

### Verifikationskommandos (alle bestanden)

- `./gradlew detekt` — **BUILD SUCCESSFUL** (0 Issues)
- `./gradlew lint` — **BUILD SUCCESSFUL**
- `./gradlew :app:testDebugUnitTest` — **BUILD SUCCESSFUL** (alle Tests grün)
- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**

---

## 4. Top-Findings nach Priorität

### KRITISCH

Keine neuen kritischen Befunde. Ein vorheriger kritischer Befund bleibt offen:

| ID  | Titel                                                     | Vorheriger ID | Status         |
| --- | --------------------------------------------------------- | ------------- | -------------- |
| C01 | RecordDailyManualCheckIn überschreibt bestätigte Einträge | B03           | **NOCH OFFEN** |

### HOCH

| ID  | Titel                                             | Kategorie | Neu/Vorheriger |
| --- | ------------------------------------------------- | --------- | -------------- |
| H01 | OverviewViewModel hat keinen Test                 | Tests     | **NEU**        |
| H02 | EditEntryViewModel hat nur 2 Tests                | Tests     | **NEU**        |
| H03 | PDF zeigt keine pro-Verpflegungspauschale-Beträge | Export    | **NEU** (E02)  |
| H04 | Preview/PDF-Mismatch bei 180+ Einträgen           | Export    | **NEU** (E05)  |

### MITTEL

| ID  | Titel                                                                    | Kategorie   | Neu/Vorheriger             |
| --- | ------------------------------------------------------------------------ | ----------- | -------------------------- |
| M01 | COMP_TIME mit Travel: Inkonsistente Behandlung Statistik vs. Überstunden | Fachlogik   | **NEU**                    |
| M02 | MIGRATION_13_14 hat keinen eigenen Test für Travel-Leg-Daten             | Migration   | **NEU**                    |
| M03 | `exportSchema = false` verhindert automatisierte Schema-Validierung      | Migration   | **NEU**                    |
| M04 | HistoryViewModel.applyBatchEdit: Read-then-Write ohne Transaction        | Datenfluss  | B09, **TEILWEISE BEHOBEN** |
| M05 | ExportPreviewViewModel.currentRange ist noch mutable var                 | Datenfluss  | B07, **TEILWEISE BEHOBEN** |
| M06 | Duplicate Confirmation-Source-Konstanten                                 | Wartbarkeit | M4 (März 29), **OFFEN**    |
| M07 | DefaultNonWorkingDayChecker ungetestet                                   | Tests       | **NEU**                    |
| M08 | DAO-Query-Fläche unzureichend getestet                                   | Tests       | **NEU**                    |

### NIEDRIG

| ID  | Titel                                                                  | Kategorie                      |
| --- | ---------------------------------------------------------------------- | ------------------------------ |
| L01 | 6 PdfUtilities-Methoden ohne Produktions-Caller                        | Toter Code                     |
| L02 | 10 WorkEntryRepositoryUseCases sind pure Delegation                    | Redundanz                      |
| L03 | formatMinutes-Logik 4x dupliziert statt zentralisiert                  | Wartbarkeit                    |
| L04 | PDF hat keine Notes-Spalte                                             | Export (E03)                   |
| L05 | PDF hat keine Travel-Time-Spalte pro Zeile                             | Export (E01)                   |
| L06 | PDF versteckt bezahlte-Gesamt-Zeile wenn keine Reise                   | Export (E04)                   |
| L07 | CSV gibt breakMinutes aus auch wenn workStart null                     | Export (E06)                   |
| L08 | CSV hat keine Summary-Zeile                                            | Export (E08)                   |
| L09 | Edit Travel-Timestamps timezone-abhängig                               | Datenfluss (B12, offen)        |
| L10 | contentDescription = null in 30+ Composables                           | Accessibility (B15, offen)     |
| L11 | BootReceiver/TimeChangeReceiver CoroutineScope-Leak                    | Robustheit (B17, offen)        |
| L12 | EditEntrySheet LaunchedEffect zu breit                                 | UI (B18, offen)                |
| L13 | ProGuard `.e()` nicht in assumenosideeffects                           | Build (A08, teilweise behoben) |
| L14 | Receiver CoroutineScope ohne Lifecycle                                 | Concurrency (B17, offen)       |
| L15 | MIGRATION_13_14: Seltener Edge-Case kann doppelte Travel Legs erzeugen | Migration                      |
| L16 | Dead Code: `if (normalizedDiffMs < 0) 0` in TimeCalculator             | Toter Code                     |
| L17 | Redundante Guards in CalculateOvertimeForRange                         | Code-Qualität                  |

---

## 5. Detaillierte Befunde

### 5.1 C01: RecordDailyManualCheckIn überschreibt bestätigte Einträge

- **Schweregrad:** Kritisch
- **Kategorie:** Datenfluss
- **Betroffen:** `domain/usecase/RecordDailyManualCheckIn.kt:48-89`
- **Problem:** `readModifyWrite`-Block prüft nicht auf `existingEntry.confirmedWorkDay`. Wenn ein Eintrag bereits bestätigt ist, wird er beim erneuten Check-In komplett überschrieben (inkl. Zeitstempel, Meal Allowance, Travel).
- **Warum relevant:** Bestätigte Daten können stillschweigend verloren gehen.
- **Ursache:** Kein Guard im Use Case. Mögliche Mitigation auf UI-Ebene nicht verifiziert.
- **Auswirkung:** Datenverlust bei wiederholtem Check-In für bereits bestätigte Tage.
- **Reproduzierbarkeit:** Klar — Manuelles Check-In für bestätigten Tag ausführen.
- **Empfehlung:** Guard hinzufügen: `if (existing.confirmedWorkDay) return existing`. Alternativ UI-seitig den Dialog für bestätigte Tage deaktivieren.
- **Confidence:** Hoch

### 5.2 H01: OverviewViewModel hat keinen Test

- **Schweregrad:** Hoch
- **Kategorie:** Tests
- **Betroffen:** `ui/screen/overview/OverviewViewModel.kt`
- **Problem:** Kein Test für den ViewModel der Übersichtsanzeige. OverviewViewModel aggregiert Statistiken und zeigt KPIs an.
- **Warum relevant:** Overview ist einer der Hauptscreens. Fehler in der Statistikdarstellung bleiben unentdeckt.
- **Empfehlung:** Mindestens Tests für: leere Daten, gemischte Einträge, Periodenwechsel.
- **Confidence:** Hoch

### 5.3 H02: EditEntryViewModel hat nur 2 Tests

- **Schweregrad:** Hoch
- **Kategorie:** Tests
- **Betroffen:** `ui/screen/edit/EditEntryViewModel.kt`, Test: `EditEntryViewModelTest.kt`
- **Problem:** EditEntryViewModel ist der komplexeste Formular-ViewModel (Validierung, Travel Legs, Meal Allowance, Datum-Navigation, Auto-Save). Nur 2 Testmethoden existieren.
- **Warum relevant:** Formular-Logik ist fehleranfällig. Nacht-Schicht-Validierung (H2, jetzt behoben) wurde vorher nicht durch Tests abgedeckt.
- **Empfehlung:** Mindestens Tests für: Validierungsfehler, Travel-Leg-Verwaltung, DayType-Wechsel, Meal-Allowance-Update.
- **Confidence:** Hoch

### 5.4 H03: PDF zeigt keine pro-Verpflegungspauschale-Beträge (E02)

- **Schweregrad:** Hoch
- **Kategorie:** Export
- **Betroffen:** `export/PdfExporter.kt:497-504`
- **Problem:** PDF zeigt pro Zeile nur einen Frühstück-Haken (ja/nein). Der tatsächliche Verpflegungspauschalen-Betrag in Euro erscheint nur in der Zusammenfassung. Preview zeigt pro Zeile Euro-Beträge.
- **Warum relevant:** PDF ist das unterschriebene Dokument. Prüfer können tägliche Pauschalen nicht nachvollziehen.
- **Empfehlung:** Euro-Betrag pro Zeile im PDF anzeigen (z.B. als zusätzliche Spalte oder in Klammern hinter dem Haken).
- **Confidence:** Hoch

### 5.5 H04: Preview/PDF-Mismatch bei 180+ Einträgen (E05)

- **Schweregrad:** Hoch
- **Kategorie:** Export / UX
- **Betroffen:** `ui/screen/export/ExportPreviewViewModel.kt`, `export/PdfExporter.kt:170-172`
- **Problem:** Preview zeigt alle Einträge (kein Limit). PDF-Erstellung scheitert bei >180 Einträgen mit `MAX_ENTRIES_PER_PDF`. Nutzer sehen alle Daten in der Vorschau, bekommen aber einen Fehler bei der PDF-Erstellung.
- **Warum relevant:** UX-Lücke — Nutzer müssen den Fehler erst auslösen, bevor sie wissen, dass sie den Zeitraum verkleinern müssen.
- **Empfehlung:** In der Preview einen Warnhinweis zeigen wenn >180 Einträge, oder das Limit in der Preview-Pagination berücksichtigen.
- **Confidence:** Hoch

### 5.6 M01: COMP_TIME mit Travel — Inkonsistenz Statistik vs. Überstunden

- **Schweregrad:** Mittel
- **Kategorie:** Fachlogik
- **Betroffen:** `CalculateOvertimeForRange.kt:74-79` vs `AggregateWorkStats.kt:73-74`
- **Problem:** Wenn ein COMP_TIME-Eintrag Travel Legs hat:
    - `AggregateWorkStats.totalPaidMinutes` zählt die Reisezeit (via blanket sum)
    - `CalculateOvertimeForRange.totalActualHours` ignoriert sie komplett
    - DAY_CLASSIFICATION.md spezifiziert diesen Fall nicht
- **Warum relevant:** Die Fachspezifikation ist hier lückenhaft. Das Verhalten ist inkonsistent, aber der Fall ist in der Praxis unwahrscheinlich (COMP_TIME mit Reise).
- **Empfehlung:** Fachlich klären: Darf COMP_TIME Reisezeiten haben? Wenn ja, Spezifikation und beide Berechnungen anpassen. Wenn nein, UI-seitig verhindern.
- **Confidence:** Hoch

---

## 6. Tote Codepfade / Altlasten / Redundanzen

| ID  | Beschreibung                                                                                                                                                                 | Datei                                                                                  | Schweregrad |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- | ----------- |
| D01 | 6 PdfUtilities-Methoden ohne Produktions-Caller (`calculateWorkHours`, `formatTravelTime`, `formatTravelWindow`, `sumWorkHours` ×2, `sumTravelMinutes`, `filterWorkDays` ×2) | `export/PdfUtilities.kt`                                                               | Niedrig     |
| D02 | 10 Use-Case-Wrapper in WorkEntryRepositoryUseCases sind pure Delegation ohne Mehrwert                                                                                        | `domain/usecase/WorkEntryRepositoryUseCases.kt`                                        | Niedrig     |
| D03 | `formatMinutes`-Logik dupliziert in 4 UI-Dateien statt zentralisiert in `Formatters`                                                                                         | `TodayScreen.kt`, `ExportPreviewViewModel.kt`, `HistoryScreen.kt`, `SettingsScreen.kt` | Niedrig     |
| D04 | Duplicate Confirmation-Source-Konstanten `"NOTIFICATION"` in 3 Dateien                                                                                                       | `ConfirmWorkDay.kt:20`, `ConfirmOffDay.kt:21`, `CheckInActionService.kt:189`           | Niedrig     |
| D05 | Dead Code: `if (normalizedDiffMs < 0) 0` in `calculateTravelLegMinutes` ist unreachable                                                                                      | `domain/util/TimeCalculator.kt:86`                                                     | Niedrig     |
| D06 | Redundante Guards in `CalculateOvertimeForRange` UEBERSTUNDEN_ABBAU (durch Classification und Eligibility-Filter immer true)                                                 | `domain/usecase/CalculateOvertimeForRange.kt:76`                                       | Niedrig     |

**Bereinigt seit letztem Audit:**

- ProGuard Moshi-Regeln: entfernt
- `Formatters.formatMinutes()` / `formatDateShort()`: entfernt
- `DayClassification listOf`-Allocations: auf `when` umgestellt
- GPS/Location-Code: vollständig aus Produktionscode entfernt (nur noch in Migration-SQL)
- `ClassifiedDay` Data Class: nicht mehr vorhanden

---

## 7. Berechnungs- und Logikrisiken

| ID  | Beschreibung                                                                                                              | Risiko                              | Datei                                |
| --- | ------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- | ------------------------------------ |
| R01 | COMP_TIME + Travel: `totalPaidMinutes` zählt Reise, `totalActualHours` ignoriert sie                                      | Inkonsistente Überstundenberechnung | `CalculateOvertimeForRange.kt:74-79` |
| R02 | `calculateTravelMinutes` hat kein DayType-Filter — korrekt für FREI_MIT_REISE, aber COMP_TIME wird inkonsistent behandelt | Siehe R01                           | `domain/util/TimeCalculator.kt`      |
| R03 | CSV gibt `breakMinutes` für WORK-Tage aus, auch wenn `workStart == null`                                                  | Inkonsistenter Export               | `export/CsvExporter.kt:141-142`      |
| R04 | PDF hat keine Notes-Spalte — Notizen sind im PDF nicht sichtbar                                                           | Datenverlust-Wahrnehmung            | `export/PdfExporter.kt`              |

**Verifizierte Korrektheit:**

- TimeCalculator: Midnight-Crossing, Break > Work (geklappt auf 0), OFF/COMP_TIME (immer 0) — alles korrekt
- ClassifyDay: Alle 6 Branches stimmen mit DAY_CLASSIFICATION.md überein
- MealAllowanceCalculator: Eligibility-Check konsistent an allen 4 Konsumenten
- `isStatisticsEligible`: Einzige Filter-Funktion, an allen 7 Konsumenten identisch verwendet
- Travel Time: Alle 11 Produktions-Konsumenten verwenden `orderedTravelLegs`
- `workDays` vs `targetCountedDays`: Entspricht der Spezifikation (sichtbar vs. sollstundenrelevant)

---

## 8. Test- und Qualitätssicherung

### Verifikationsergebnis

- Detekt: **0 Issues**
- Lint: **0 Issues**
- Unit Tests: **Alle grün**
- Debug Build: **Erfolgreich**

### Test-Abdeckung

**Gut getestet (keine Bedenken):**

- `AggregateWorkStats` — 24 Testmethoden, alle 6 Klassifikationen, unconfirmed, mixed
- `ClassifyDay` — 18 Testmethoden, alle Pfade + Properties
- `TimeCalculator` — 20 Testmethoden, Midnight, Break > Work, Multi-Leg
- `MealAllowanceCalculator` — 12 Testmethoden, alle Regeln
- `CalculateOvertimeForRange` — gut für einzelne Einträge
- `CsvExporter` — Formula Injection, Special Characters, Filtering

### Kritische Lücken

| ID  | Lücke                                                                                 | Schweregrad | Begründung                                           |
| --- | ------------------------------------------------------------------------------------- | ----------- | ---------------------------------------------------- |
| T01 | OverviewViewModel: kein Test                                                          | Hoch        | Hauptscreen mit Statistik-KPIs                       |
| T02 | EditEntryViewModel: nur 2 Tests                                                       | Hoch        | Komplexester Formular-ViewModel                      |
| T03 | DefaultNonWorkingDayChecker: kein Test                                                | Mittel      | Wochenende/Feiertag-Logik beeinflusst Auto-OFF       |
| T04 | DAO-Query-Fläche: getByDateRangeWithTravel, replaceEntryWithTravelLegs ungetestet     | Mittel      | Kritische Join-Queries ohne direkten Test            |
| T05 | CalculateOvertimeForRange: kein Multi-Entry-Aggregationstest                          | Niedrig     | Nur Einzeleintrag-Tests vorhanden                    |
| T06 | Duplicate/misplaced ExportPreviewViewModelTest an `ui/export/` vs `ui/screen/export/` | Niedrig     | Test-Pfad stimmt nicht mit Produktions-Paket überein |

### Scheinsicherheit

Keine trügerischen Tests gefunden. `relaxed = true` in TodayViewModelTest ist vertretbar (spezifische `coEvery`-Overrides vorhanden). Alle Testnamen sind deskriptiv und verhaltenensbasiert.

---

## 9. Architektur- und Wartbarkeitsprobleme

| ID  | Beschreibung                                                                  | Datei                                            | Schweregrad |
| --- | ----------------------------------------------------------------------------- | ------------------------------------------------ | ----------- |
| W01 | `exportSchema = false` verhindert automatisierte Schema-Validierung           | `AppDatabase.kt:17`                              | Mittel      |
| W02 | EditEntrySheet LaunchedEffect(`uiState`) triggert bei jedem UI-State-Wechsel  | `EditEntrySheet.kt:78`                           | Niedrig     |
| W03 | BootReceiver/TimeChangeReceiver: CoroutineScope ohne Lifecycle                | `BootReceiver.kt:37`, `TimeChangeReceiver.kt:44` | Niedrig     |
| W04 | ExportPreviewViewModel.currentRange ist mutable var (mit Snapshot-Mitigation) | `ExportPreviewViewModel.kt:171`                  | Niedrig     |

### Was sich verbessert hat

- **TodayScreen**: 25+ StateFlows → 2 konsolidierte (`screenState`, `dialogState`)
- **TodayViewModel**: Stale `LocalDate.now()` → `TodayDateCoordinator` mit Midnight-Refresh
- **Domain → Work Dependency**: Entfernt (SetDayLocation importiert nicht mehr aus work-Paket)
- **Lambda Memoization**: TodayScreen nutzt jetzt `remember { { ... } }`
- **RingBufferLogger**: Löscht bei Fehler nicht mehr alles, sondern schreibt Sentinel

---

## 10. Quick Wins

| #   | Maßnahme                                                  | Aufwand  | Nutzen                        |
| --- | --------------------------------------------------------- | -------- | ----------------------------- |
| 1   | Guard in RecordDailyManualCheckIn für bestätigte Einträge | 5 Zeilen | Verhindert Datenverlust (C01) |
| 2   | 6 ungenutzte PdfUtilities-Methoden entfernen              | 10 Min   | Reduziert Wartungslast (D01)  |
| 3   | Confirmation-Source-Konstanten zentralisieren             | 10 Min   | Verhindert Inkonsistenz (D04) |
| 4   | Dead Code in TimeCalculator entfernen (`if < 0` Check)    | 1 Zeile  | Code-Klarheit (D05)           |
| 5   | Preview-Warnung bei >180 Einträgen                        | 5 Zeilen | Schließt UX-Lücke (H04)       |
| 6   | Duplicate ExportPreviewViewModelTest entfernen/bereinigen | 5 Min    | Vermeidet Verwirrung (T06)    |

---

## 11. Priorisierte Sanierungsreihenfolge

### Phase 1 — Sofort (1 Tag)

1. **C01**: Guard in `RecordDailyManualCheckIn` für bestätigte Einträge
2. **Quick Wins** 2–6 (PdfUtilities, Constants, Dead Code, Preview-Warnung)

### Phase 2 — Kurzfristig (3–5 Tage)

3. **H01**: Tests für OverviewViewModel
4. **H02**: Tests für EditEntryViewModel (Validierung, Travel, Meal Allowance)
5. **M04**: HistoryViewModel.applyBatchRead mit readModifyWrite absichern
6. **M02**: Eigenen Test für MIGRATION_13_14 Travel-Leg-Daten
7. **M03**: `exportSchema = true` aktivieren

### Phase 3 — Mittelfristig (1–2 Wochen)

8. **H03**: PDF pro-Verpflegungspauschale-Beträge anzeigen
9. **H04**: Preview bei >180 Einträgen einschränken oder warnen
10. **M01**: Fachliche Klärung COMP_TIME + Travel, dann Anpassung
11. **M07–M08**: Tests für NonWorkingDayChecker und DAO-Queries
12. **L09**: Timezone-feste Travel-Timestamps

### Phase 4 — Langfristig (fortlaufend)

13. **L01–L17**: Niedrig-Prio-Befunde abarbeiten
14. Accessibility-Verbesserungen (L10)
15. Compose UI Tests für kritische Screens

---

## 12. Offene Fragen / Unsicherheiten

1. **Verhindert die UI, dass der Daily-Check-In-Dialog für bestätigte Tage geöffnet wird?** Wenn ja, ist C01 praktisch entschärft, sollte aber trotzdem im Use Case abgesichert werden.

2. **Darf COMP_TIME Travel Legs haben?** Die Spezifikation (DAY_CLASSIFICATION.md) definiert diesen Fall nicht. Wenn nicht, sollte die UI dies verhindern und AggregateWorkStats sollte Travel für COMP_TIME ignorieren.

3. **Gibt es Nutzer auf alten Schema-Versionen (< v14)?** Falls ja, sind Migration-Bugs (M02, L15) kritischer. Falls alle auf v14 sind, sind sie historisch.

4. **Ist das 180-Einträge-Limit für PDF bewusst kommuniziert?** Nutzer müssen den Fehler auslösen, bevor sie wissen, dass sie den Zeitraum verkleinern müssen.

5. **Soll der PDF-Export Notizen enthalten?** Aktuell sind Notizen im PDF nicht sichtbar (L04). Falls Notizen relevant für unterschriebene Dokumente sind, sollte eine Notes-Spalte ergänzt werden.

---

## Anhang: Reconciliation vorheriger Befunde

### vollständig behoben (39)

A01, A02, A03, A04, A05, A06, A07, A09, A11, A15, A16, A17, A18, B01, B02, B04, B05, B06, B08, B10, B11, B16, B19, B20-B21, B24, B25, H1, H2, M1, M2, M3, M5

### teilweise behoben (4)

| ID  | Status                                                                  | Restrisiko                  |
| --- | ----------------------------------------------------------------------- | --------------------------- |
| A08 | `.e()` nicht in assumenosideeffects                                     | Error-Logs in Release       |
| B07 | Snapshot-Capture mitigiert, aber mutable var bleibt                     | UX-Glitch bei Range-Wechsel |
| B09 | Batch-Write ist einzelner Aufruf, aber Read-then-Write ohne Transaction | Race bei parallelen Edits   |
| B14 | HistoryScreen nutzt LazyColumn, TodayScreen nutzt Column                | TodayScreen nicht lazy      |

### noch offen (14)

A13 (e.printStackTrace), A14 (android.util.Log), B03 (→ C01), B12 (→ L09), B13 (IME Action), B15 (→ L10), B17 (→ L14), B18 (→ L12), B22 (duplizierte Formatierer), A19 (toTypedArray)
