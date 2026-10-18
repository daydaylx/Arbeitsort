# Agent Context Packs – MontageZeit

Aufgabenbezogene Kontextpakete. Vor Beginn einer Aufgabe das passende Pack
lesen, statt das ganze Repo zu durchsuchen. Jedes Pack: Zweck, Pflichtlektüre,
zusätzliche Prüfpunkte, Risiken, Mindestchecks, No-Gos.

Übergreifend gilt immer zuerst: [`AGENTS.md`](../AGENTS.md), [`README.md`](../README.md),
[`docs/ARCHITECTURE.md`](ARCHITECTURE.md). Vor Handoff: [`docs/VALIDATION_MATRIX.md`](VALIDATION_MATRIX.md).

---

## 1. Today / Daily Check-in

**Zweck:** Änderungen am primären Check-in-Flow (`TodayScreen`, manueller
Tagesabschluss).

**Immer lesen:**

- `docs/ARCHITECTURE.md` Abschnitt „Daily Flow"
- `docs/DATA_MODEL.md` Abschnitt WorkEntry / Bestätigungsregel

**Zusätzlich prüfen:**

- `domain/usecase/RecordDailyManualCheckIn.kt`, `CheckInEntryBuilder.kt`, `DayLocationResolver.kt`
- `ui/screen/today/TodayScreen.kt`, `TodayViewModel.kt`, `TodayActionsHandler.kt`, `TodayDateCoordinator.kt`

**Risiken:** `confirmedWorkDay` wird nur bei positiver Arbeits- oder Reisezeit
gesetzt — ein leerer `WORK`-Tag muss offen bleiben. Tagesort-Prefill-Reihenfolge
(aktueller Eintrag → letzter `WORK`-Ort → leer) nicht verändern, ohne das
explizit zu wollen.

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.domain.usecase.*"
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.ui.screen.today.*"
```

**No-Gos:** keine Standortabfrage/GPS einbauen, `dayLocationLabel` bleibt Text.

---

## 2. Auslöse / Verpflegungspauschale

**Zweck:** Änderungen an der Verpflegungspauschale-Berechnung.

**Immer lesen:**

- `domain/util/MealAllowanceCalculator.kt`
- `docs/DAY_CLASSIFICATION.md` (Eligibility-Regel)

**Zusätzlich prüfen:**

- `WorkEntry`-Felder `mealIsArrivalDeparture`, `mealBreakfastIncluded`,
  `mealAllowanceBaseCents`, `mealAllowanceAmountCents` (`data/local/entity/WorkEntry.kt`)
- Verwendung in `RecordDailyManualCheckIn.kt`, `CsvExporter.kt`, `PdfExporter.kt`

**Fachregeln (verbindlich):**

- **Leipzig-Arbeitstage dürfen keine Auslöse berechnen** — aktuell gilt in
  `MealAllowanceCalculator.isExcludedLocation(...)` ein **exakter** Vergleich
  (`locationLabel.trim().lowercase() == "leipzig"`). Varianten wie
  „Leipzig Zentrum", „Leipzig, Kunde XY" oder „Leipzig-Lindenau" werden
  **aktuell nicht** erkannt — bekannter, offener Bug: **[Issue #50](https://github.com/daydaylx/Arbeitsort/issues/50)**.
- **Tagesort bleibt manueller Text** (`dayLocationLabel`) — keine Normalisierung
  über Geokoordinaten oder externe Ortsdatenbanken einbauen.
- **Keine Standortberechtigung/GPS** einbauen, auch nicht zur „Verbesserung"
  der Leipzig-Erkennung.

**Risiken:** Eligibility ist an `DayType == WORK` gekoppelt (nicht `isWorkLike`,
d. h. `SCHULUNG`/`LEHRGANG` sind laut Code-Kommentar bewusst ausgeschlossen) —
vor Änderungen mit Test decken, nicht aus dem Bauch heraus „korrigieren".

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.domain.util.MealAllowanceCalculatorTest"
```

**No-Gos:** **Issue #50 nicht in einer unrelated Doku-/Workflow-Aufgabe
mitfixen** (nur wenn der Nutzer das separat beauftragt). Keine
Standortberechtigung. Keine UI-Komplexität für Ortserkennung.

---

## 3. Day Classification / Statistik / Export Eligibility

**Zweck:** Änderungen an der Klassifikation, an denen sichtbare Arbeitstage,
Solltage oder Export-/Statistik-Summen hängen.

**Immer lesen:**

- `docs/DAY_CLASSIFICATION.md` (vollständige Fachreferenz)
- `docs/DATA_MODEL.md` Abschnitt DayClassification

**Zusätzlich prüfen:**

- `domain/model/DayClassification.kt` (8 Werte, inkl. `SCHULUNG`/`LEHRGANG`)
- `domain/usecase/ClassifyDay.kt`, `StatisticsEligibility.kt`, `EntryStatusResolver.kt`
- `domain/usecase/AggregateWorkStats.kt`, `CalculateOvertimeForRange.kt`

**Risiken:** `workDays` (sichtbare Arbeitstage) und `targetCountedDays`
(Sollstunden-relevant, inkl. `COMP_TIME`) sind fachlich **nicht austauschbar**
— siehe `docs/DAY_CLASSIFICATION.md`. Nur bestätigte Tage dürfen in Summen
auftauchen.

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.domain.*"
```

**No-Gos:** Keine neue `DayClassification`-Kategorie ohne fachliche Klärung
mit dem Nutzer einführen.

---

## 4. History / Edit / Batch Edit

**Zweck:** Verlaufsansicht, Editor-Sheet, Datumsbereich-Batch-Bearbeitung.

**Immer lesen:**

- `docs/ARCHITECTURE.md` Abschnitt „Daily Flow" (Edit-Save-Pfad)

**Zusätzlich prüfen:**

- `ui/screen/history/HistoryScreen.kt`, `HistoryViewModel.kt`, `HistoryOpenRequest.kt`
- `ui/screen/edit/EditEntrySheet.kt`, `EditEntrySaveBuilder.kt`, `EditEntryTravelSections.kt`, `EditEntryDraftRules.kt`

**Risiken:** Batch-Edit läuft nicht als atomarer DB-Range-UseCase, sondern
über bestehende Repository-Serialisierung (dokumentierte Einschränkung, siehe
Kommentar in `HistoryViewModel.applyBatchEdit`) — bei großen Bereichen
entsprechend vorsichtig testen.

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.ui.screen.history.*"
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.ui.screen.edit.*"
```

**No-Gos:** Keine stille Änderung der Bestätigungsregel beim Speichern.

---

## 5. Reminder / WorkManager / Notifications

**Zweck:** Reminder-Scheduling, Zeitfenster, Notification-Actions.

**Immer lesen:**

- `docs/REMINDERS.md` (vollständige Fachreferenz)
- `docs/ARCHITECTURE.md` Abschnitt „Reminder & Scheduling"

**Zusätzlich prüfen:**

- `work/WindowCheckWorker.kt`, `ReminderLaterWorker.kt`, `ReminderScheduler.kt`, `ReminderWindowEvaluator.kt`, `ReminderScheduleCalculator.kt`, `ReminderWorkEnqueuer.kt`
- `notification/ReminderNotificationManager.kt`, `ConfirmationReminderLimiter.kt`
- `service/CheckInActionService.kt`

**Risiken:** Kein `AlarmManager.setExact()` — WorkManager-Fenster sind bewusst
~15 Minuten toleranzbehaftet. `reminder_type` fehlt bei Legacy-Workern
bewusst → no-op, nicht „reparieren".

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.work.*"
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.notification.*"
```

**No-Gos:** Keine exakten Alarme (`SCHEDULE_EXACT_ALARM`) einbauen.

---

## 6. Receiver / Reboot / App Update / Time Change

**Zweck:** Reboot-Resilienz, Re-Scheduling nach App-Update, Zeit-/Zeitzonenwechsel.

**Immer lesen:**

- `docs/REMINDERS.md` Abschnitt Boundary-Verhalten

**Zusätzlich prüfen:**

- `receiver/BootReceiver.kt` (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`)
- `receiver/TimeChangeReceiver.kt` (`TIME_SET`, `TIME_CHANGED`, `TIMEZONE_CHANGED`)
- `AndroidManifest.xml` Receiver-Deklarationen (`exported="false"`)

**Risiken:** Bekannte Doku-Lücke (Audit O04): der echte `scheduleAll()`-Pfad
wird in bestehenden Tests nur oberflächlich über Intent-Filter gespiegelt,
nicht end-to-end verifiziert — bei Änderungen entsprechend vorsichtig sein.

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.receiver.*"
```

**No-Gos:** Keine neuen exportierten Components ohne Sicherheitsprüfung.

---

## 7. Room / Migration / Datenmodell

**Zweck:** Schema-Änderungen, neue Migrationen, Entity-Felder.

**Immer lesen:**

- `docs/DATA_MODEL.md` (vollständig)
- `docs/ARCHITECTURE.md` Abschnitt Persistence

**Zusätzlich prüfen:**

- `data/local/database/AppDatabase.kt` (Version, `MIGRATIONS`-Array)
- `data/local/entity/WorkEntry.kt`, `TravelLeg.kt`
- `data/local/converters/`

**Risiken:** Kein `fallbackToDestructiveMigration()` — fehlende Migrationen
crashen die App absichtlich. Jede neue Migration braucht Test-Coverage.

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.data.local.database.*"
./gradlew connectedDebugAndroidTest --tests "de.montagezeit.app.data.local.database.AppDatabaseSchemaMigrationTest"
```

**No-Gos:** **Keine Migration ohne separaten, expliziten Auftrag.** Kein
Schema-Versionssprung „just in case".

---

## 8. DataStore / Settings

**Zweck:** Reminder-Fenster, PDF-Stammdaten, Feiertagslisten in DataStore.

**Immer lesen:**

- `docs/DATA_MODEL.md` Abschnitt DataStore Settings

**Zusätzlich prüfen:**

- `data/preferences/ReminderSettings.kt`, `ReminderSettingsManager.kt` (`reminder_settings`)
- `data/preferences/ReminderFlagsStore.kt` (`reminder_flags_v2`)
- `data/preferences/PdfSettings.kt`

**Risiken:** `ReminderFlagsStore` migriert alte SharedPreferences-Keys einmalig
— Key-Namen nicht ohne Migrationslogik umbenennen.

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.ui.screen.settings.*"
```

**No-Gos:** Keine Klartext-Secrets in DataStore.

---

## 9. Export PDF / CSV

**Zweck:** PDF-/CSV-Generierung, Export-Preview.

**Immer lesen:**

- `docs/DATA_MODEL.md` Abschnitte WorkEntry/TravelLeg/DayClassification
- `docs/DAY_CLASSIFICATION.md` (welche Tage einfließen)

**Zusätzlich prüfen:**

- `export/PdfExporter.kt` (`MAX_ENTRIES_PER_PDF = 180`, 5-MB-Storage-Check)
- `export/CsvExporter.kt` (`CsvCellEncoder`, Formel-Präfix-Hardening)
- `ui/screen/export/ExportPreviewScreen.kt`, `ExportPreviewViewModel.kt`

**Risiken:** Keine externe PDF-Library — nur `android.graphics.pdf.PdfDocument`.
Storage-/Entry-Limit-Validierung nicht stillschweigend entfernen.

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.export.*"
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.ui.screen.export.*"
```

**No-Gos:** Keine neue externe Dependency für PDF/CSV ohne Rücksprache.

---

## 10. Compose UI / Navigation

**Zweck:** Screen-/Komponenten-Änderungen, Navigationsfluss.

**Immer lesen:**

- `ui/navigation/MontageZeitNavGraph.kt`

**Zusätzlich prüfen:**

- Betroffenes Screen-Package unter `ui/screen/`
- `app/src/main/res/values/strings.xml` (Ressourcen statt Hardcoded-Text)

**Risiken:** Dieser Auftrag ist **kein** UI-Redesign-Kontext — Änderungen hier
nur im Rahmen des konkret beauftragten Bugfixes/Features, nicht als
Gelegenheit für Restyling.

**Mindestchecks:**

```bash
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.ui.screen.<betroffenes-package>.*"
./gradlew :app:testDebugUnitTest
```

**No-Gos:** Kein Hardcoded-UI-Text. Kein Strukturumbau „nebenbei".

---

## 11. Privacy / Permissions / Manifest

**Zweck:** Änderungen an `AndroidManifest.xml`, Berechtigungen, Datenumgang.

**Immer lesen:**

- `docs/PRIVACY_CONTEXT.md` (vollständig, verbindlich)

**Zusätzlich prüfen:**

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`, `backup_rules.xml`

**Risiken:** Jede neue `uses-permission` oder exportierte Component ist
sicherheitsrelevant und muss im PR explizit begründet werden.

**Mindestchecks:** manuelle Prüfung, keine automatisierte Coverage —
Diff der Manifest-Änderung im PR-Body explizit benennen.

**No-Gos:** **Keine Standortberechtigung. Kein GPS. Kein Cloud-Sync. Kein
Backend. Kein Login.** `android:allowBackup="false"` bleibt `false`.

---

## 12. Release / APK Update / Signing

**Zweck:** Release-Build, Signatur, Update-Verhalten.

**Immer lesen:**

- `README.md` Abschnitt „APK-Update ohne Datenverlust"
- `README_ANDROID_DEV.md`

**Zusätzlich prüfen:**

- `app/build.gradle.kts` (`versionCode`, `versionName`, Signing-Config)
- `keystore.properties` (lokal, **nicht committen**)

**Risiken:** Ohne `keystore.properties` entsteht nur eine unsignierte, nicht
update-fähige Release-APK. Ein Uninstall oder Install mit anderer Signatur
löscht lokale Daten (`allowBackup=false`).

**Mindestchecks:**

```bash
./gradlew assembleRelease
```

Manuell: [`docs/MANUAL_TEST_SCENARIOS.md`](MANUAL_TEST_SCENARIOS.md) Szenario „Release-APK-Update mit gleicher Signatur".

**No-Gos:** Keine Keystores/Signing-Secrets committen. Kein `versionCode`-
Rückschritt.

---

## 13. Agenten-Doku / Workflow / CI

**Zweck:** Änderungen an `AGENTS.md`, `docs/*`, CI-Workflows, Hooks, Templates.

**Immer lesen:**

- `AGENTS.md`
- `docs/README.md` (Doku-Index und Pflegeregeln)
- `docs/VALIDATION_MATRIX.md`

**Zusätzlich prüfen:**

- `.github/workflows/ci.yml`, `instrumentation.yml`, `release.yml`
- `scripts/hooks/run_local_quality_gate.sh`, `lefthook.yml`
- `.github/pull_request_template.md`, `docs/AGENT_HANDOFF_TEMPLATE.md`

**Risiken:** Neue oder konkurrierende Source-of-Truth-Dokumente vermeiden —
`README.md`/`docs/ARCHITECTURE.md` bleiben verbindlich, alles andere unter
`docs/` ist Ergänzung.

**Mindestchecks:** Für reine Doku-Änderungen genügt eine Link-/Konsistenzprüfung;
bei CI-/Skript-Änderungen zusätzlich lokal ausführen:

```bash
bash scripts/hooks/run_local_quality_gate.sh
```

**No-Gos:** Keine Toolchain-/Dependency-Upgrades im Rahmen einer Doku-/
Workflow-Aufgabe. Alte Audit-/Review-Vollberichte nicht wieder ausbreiten
(zusammengefasst in `docs/AUDITS/CURRENT_STATUS.md`).
