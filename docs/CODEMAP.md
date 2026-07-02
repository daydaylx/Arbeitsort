# CODEMAP – MontageZeit

Feature-zu-Datei-Karte für schnellen Einstieg. Kein Architektur-Text —
dafür [`docs/ARCHITECTURE.md`](ARCHITECTURE.md). Keine neuen Entscheidungen,
nur Orientierung im aktuellen Codezustand (Schema-Version 16).

Basis-Pfad: `app/src/main/java/de/montagezeit/app/`

---

## Projektüberblick

Single-Module Android-App (`:app`), Kotlin, Jetpack Compose (Material3), Room,
WorkManager, Hilt, DataStore Preferences. PDF/CSV-Export. Offline-first, kein
Cloud-Sync, kein Backend, kein Login.

## Einstiegspunkte

| Datei                                  | Zweck                                                                                                                                        |
| -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `MontageZeitApp.kt`                    | `@HiltAndroidApp`, `Configuration.Provider` für WorkManager (`HiltWorkerFactory`), startet `ReminderScheduler.scheduleAll()` in `onCreate()` |
| `MainActivity.kt`                      | `@AndroidEntryPoint`, verarbeitet Edit-Intents (`ReminderActions.ACTION_EDIT_ENTRY`/`EXTRA_DATE`), hostet den Nav-Graph                      |
| `ui/navigation/MontageZeitNavGraph.kt` | Compose-Navigation-Graph                                                                                                                     |

## Datenbank & Persistenz

| Datei                                         | Zweck                                                                            |
| --------------------------------------------- | -------------------------------------------------------------------------------- |
| `data/local/database/AppDatabase.kt`          | Room-DB, Schema `version = 16`, alle `MIGRATION_1_2` … `MIGRATION_15_16`-Objekte |
| `data/local/dao/WorkEntryDao.kt`              | CRUD für `WorkEntry` + `TravelLeg`                                               |
| `data/local/entity/WorkEntry.kt`              | Entity + `DayType`-Enum (siehe [`docs/DATA_MODEL.md`](DATA_MODEL.md))            |
| `data/local/entity/TravelLeg.kt`              | Entity + `TravelLegCategory`/`TravelSource`, `WorkEntryWithTravelLegs`           |
| `data/local/converters/LocalDateConverter.kt` | Room-Typkonverter `LocalDate`                                                    |
| `data/local/converters/LocalTimeConverter.kt` | Room-Typkonverter `LocalTime`                                                    |
| `data/preferences/`                           | DataStore-basierte Settings (siehe unten)                                        |

Es gibt auf diesem Stand **keinen** `DatabaseBackupManager` und **keine**
`VACATION`-DayType — beides existiert nur auf einem nicht gemergten
Feature-Branch. Nicht aus Trainingsdaten/älteren Notizen annehmen, dass es
bereits da ist.

## Repository

| Datei                                        | Zweck                |
| -------------------------------------------- | -------------------- |
| `data/repository/WorkEntryRepository.kt`     | Repository-Interface |
| `data/repository/RoomWorkEntryRepository.kt` | Room-Implementierung |

## Domänenschicht (`domain/usecase/`)

| Datei                            | Zweck                                                                                            |
| -------------------------------- | ------------------------------------------------------------------------------------------------ |
| `RecordDailyManualCheckIn.kt`    | Primärpfad Today-Screen: Tag in einem Schritt abschließen (`WORK`, Tagesort, Bestätigung)        |
| `RecordCheckIn.kt`               | Morning-/Evening-Snapshot (Notification-Actions), transaktional via `readModifyWrite`            |
| `CheckInEntryBuilder.kt`         | Baut/aktualisiert `WorkEntry` aus Check-in-`Snapshot`; behält bestehenden `dayType`              |
| `ConfirmWorkDay.kt`              | Bestätigt Arbeitstag mit Default-Zeiten (Daily Confirmation „Ja")                                |
| `ConfirmOffDay.kt`               | Bestätigt Tag als `OFF` (Daily Confirmation „Nein")                                              |
| `SetDayType.kt`                  | Setzt Tagtyp; `COMP_TIME` wird sofort `confirmedWorkDay = true`                                  |
| `SetDayLocation.kt`              | Setzt Tagesort manuell (`dayLocationLabel`)                                                      |
| `SetTravelEvent.kt`              | Setzt einzelnes manuelles Travel-Fenster                                                         |
| `SaveEditedEntryWithTravel.kt`   | Speichert bearbeiteten Eintrag inkl. Travel-Legs                                                 |
| `UpdateEntry.kt`                 | Manuelle Korrektur eines bestehenden Eintrags                                                    |
| `DeleteDayEntry.kt`              | Löscht Eintrag, liefert `DeletedDaySnapshot` für Undo                                            |
| `DayLocationResolver.kt`         | Tagesort-Prefill-Fallback (aktueller Eintrag → letzter `WORK`-Ort → leer)                        |
| `ClassifyDay.kt`                 | Klassifiziert Tag (`DayType` + Zeiten) zu `DayClassification`                                    |
| `EntryStatusResolver.kt`         | Klassifikation + Statistik-Eligibility + Reminder-Terminal-Status aus `WorkEntry`+`TravelLeg`    |
| `StatisticsEligibility.kt`       | `isStatisticsEligible(entry)`                                                                    |
| `AggregateWorkStats.kt`          | Aggregiert Statistik (workDays, targetCountedDays, Stunden, Verpflegungspauschale) über Zeitraum |
| `CalculateOvertimeForRange.kt`   | Ist/Soll/Überstunden für Datumsbereich                                                           |
| `WorkEntryFactory.kt`            | `resolveAutoDayType()` — Wochenende/Feiertag → `OFF`                                             |
| `WorkEntryPersistenceSupport.kt` | Normalisierung vor Persistierung                                                                 |

## Domain-Modelle & Berechnung

| Datei                                                                                                  | Zweck                                                                                                                    |
| ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------ |
| `domain/model/DayClassification.kt`                                                                    | Domain-Enum (8 Werte), siehe [`docs/DAY_CLASSIFICATION.md`](DAY_CLASSIFICATION.md)                                       |
| `domain/util/TimeCalculator.kt`                                                                        | Netto-Arbeitszeit, Reisezeit, Überstunden                                                                                |
| `domain/util/MealAllowanceCalculator.kt`                                                               | Verpflegungspauschale, Leipzig-Ausschluss (siehe [`docs/AGENT_CONTEXT_PACKS.md`](AGENT_CONTEXT_PACKS.md) Pack „Auslöse") |
| `domain/util/NonWorkingDayChecker.kt`                                                                  | Interface, Impl in `work/DefaultNonWorkingDayChecker.kt`                                                                 |
| `domain/util/DayTypeTransitions.kt`                                                                    | Übergänge zwischen `DayType`-Werten inkl. `SCHULUNG`/`LEHRGANG`                                                          |
| `domain/util/WeekCalculator.kt`, `AppDefaults.kt`, `ConfirmationSources.kt`, `WorkScheduleDefaults.kt` | Helper/Default-Werte                                                                                                     |

## Export (`export/`)

| Datei             | Zweck                                                                                       |
| ----------------- | ------------------------------------------------------------------------------------------- |
| `PdfExporter.kt`  | PDF via `android.graphics.pdf.PdfDocument`, `MAX_ENTRIES_PER_PDF = 180`, 5-MB-Storage-Check |
| `CsvExporter.kt`  | CSV mit UTF-8-BOM, `CsvCellEncoder` (Quoting + Formel-Präfix-Hardening)                     |
| `PdfUtilities.kt` | Formatierungs-Helper (Datum, Zeit, Reise-Route, Verpflegung)                                |

## Erinnerungssystem

| Datei                                         | Zweck                                                                   |
| --------------------------------------------- | ----------------------------------------------------------------------- |
| `work/ReminderScheduler.kt`                   | Plant 4 dedizierte WorkManager-Jobs                                     |
| `work/ReminderType.kt`                        | Enum `MORNING, EVENING, FALLBACK, DAILY`                                |
| `work/WindowCheckWorker.kt`                   | Zentraler Entscheidungs-Worker, prüft Zeitfenster + sendet Notification |
| `work/ReminderLaterWorker.kt`                 | One-time Worker für „Später erinnern"                                   |
| `work/ReminderWindowEvaluator.kt`             | Fenster-/Zeitprüfung                                                    |
| `work/ReminderScheduleCalculator.kt`          | `MIN_PERIODIC_INTERVAL_MINUTES = 15`, Delay-Berechnung                  |
| `work/ReminderWorkEnqueuer.kt`                | Baut `PeriodicWorkRequest`s                                             |
| `work/DefaultNonWorkingDayChecker.kt`         | Wochenend-/Feiertagsprüfung                                             |
| `notification/ReminderNotificationManager.kt` | Baut die 4 Notification-Typen + Actions                                 |
| `notification/ConfirmationReminderLimiter.kt` | SharedPreferences-Limit (`maxRepeatsPerDay = 2`)                        |
| `service/CheckInActionService.kt`             | Foreground-Service (`specialUse`) für Notification-Actions              |
| `receiver/BootReceiver.kt`                    | `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` → reschedule                     |
| `receiver/TimeChangeReceiver.kt`              | `TIME_SET`/`TIME_CHANGED`/`TIMEZONE_CHANGED` → reschedule               |

## UI – Screens (`ui/screen/`)

| Screen   | Package     | Zweck                                                                                                     |
| -------- | ----------- | --------------------------------------------------------------------------------------------------------- |
| Today    | `today/`    | Primärer Check-in-Screen (`TodayScreen`, `TodayViewModel`, `TodayActionsHandler`, `TodayDateCoordinator`) |
| History  | `history/`  | Verlauf, Kalender, Batch-Edit (`HistoryScreen`, `HistoryViewModel`)                                       |
| Overview | `overview/` | Wochen-/Monatsübersicht, Statistik (`OverviewScreen`, `OverviewCalculations`)                             |
| Edit     | `edit/`     | Editor-Sheet inkl. Travel (`EditEntrySheet`, `EditEntrySaveBuilder`, `EditEntryTravelSections`)           |
| Export   | `export/`   | Export-Vorschau (`ExportPreviewScreen`, `ExportPreviewViewModel`)                                         |
| Settings | `settings/` | Reminder-/PDF-/Feiertags-Einstellungen (`SettingsScreen`, `SettingsSections`)                             |

## DataStore / Settings (`data/preferences/`)

| Datei                        | Zweck                                                                                                              |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `ReminderSettings.kt`        | Data class + `ReminderSettingsKeys` (Arbeitszeit-Defaults, Fenster/Intervalle, PDF-Stammdaten, `dailyTargetHours`) |
| `ReminderSettingsManager.kt` | DataStore `reminder_settings`                                                                                      |
| `ReminderFlagsStore.kt`      | DataStore `reminder_flags_v2` (Tages-Reminder-Flags)                                                               |
| `PdfSettings.kt`             | Data class für PDF-Stammdaten                                                                                      |

## Dependency Injection (`di/`)

| Datei                  | Bereitgestellte Objekte                                                 |
| ---------------------- | ----------------------------------------------------------------------- |
| `DatabaseModule.kt`    | `AppDatabase`, `WorkEntryDao`, `WorkEntryRepository`                    |
| `ApplicationModule.kt` | `Clock`, alle zentralen UseCases, `WorkManager`, `NonWorkingDayChecker` |

## Querschnitt

| Datei                              | Zweck                                                              |
| ---------------------------------- | ------------------------------------------------------------------ |
| `data/logging/RingBufferLogger.kt` | Lokales Debug-Log (`files/logs/debug.log`, max. 2 MB, kein Upload) |

## Tests

| Bereich                    | Pfad                                                                                            | Relevanz                                         |
| -------------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| Domain (UseCases, Util)    | `app/src/test/java/de/montagezeit/app/domain/`                                                  | Domain-/Auslöse-/Statistik-Änderungen            |
| UI/ViewModel               | `app/src/test/java/de/montagezeit/app/ui/screen/{edit,export,history,overview,settings,today}/` | Screen-/ViewModel-Änderungen                     |
| Reminder/WorkManager       | `app/src/test/java/de/montagezeit/app/work/`                                                    | Reminder-/Scheduling-Änderungen                  |
| Export                     | `app/src/test/java/de/montagezeit/app/export/`                                                  | PDF/CSV-Änderungen                               |
| Room/Migration             | `app/src/test/java/de/montagezeit/app/data/local/database/`                                     | Schema-/Migrationsänderungen                     |
| Receiver/Notification      | `app/src/test/java/de/montagezeit/app/receiver/`, `.../notification/`                           | Boot/Zeitänderung/Notification                   |
| Logging                    | `app/src/test/java/de/montagezeit/app/logging/`                                                 | `RingBufferLogger`                               |
| DAO/Cascade (instrumented) | `app/src/androidTest/java/de/montagezeit/app/data/local/dao/`                                   | Travel-Cascade, Upsert                           |
| Schema (instrumented)      | `app/src/androidTest/java/de/montagezeit/app/data/local/database/`                              | `AppDatabaseSchemaMigrationTest`                 |
| E2E (instrumented)         | `app/src/androidTest/java/de/montagezeit/app/`                                                  | `ExportPreviewFlowTest`, `SmokeInstrumentedTest` |

Vollständige Prüfmatrix je Änderungstyp: [`docs/VALIDATION_MATRIX.md`](VALIDATION_MATRIX.md)

## Quality Gates

```bash
./gradlew detekt
./gradlew lint
./gradlew :app:testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
bash scripts/hooks/run_local_quality_gate.sh
```

---

## Schnell-Suche nach Konzept

| Ich suche…                      | Hier nachschauen                                                                             |
| ------------------------------- | -------------------------------------------------------------------------------------------- |
| DB-Schema / Tabellen            | `AppDatabase.kt` + [`docs/DATA_MODEL.md`](DATA_MODEL.md)                                     |
| DayType/DayClassification-Werte | [`docs/DATA_MODEL.md`](DATA_MODEL.md), [`docs/DAY_CLASSIFICATION.md`](DAY_CLASSIFICATION.md) |
| Bestätigungslogik               | `EntryStatusResolver.kt`, `ConfirmWorkDay.kt`, `EditEntrySaveBuilder.kt`                     |
| Reisedaten                      | `TravelLeg.kt`, `SetTravelEvent.kt`                                                          |
| Erinnerungs-Timing              | `WindowCheckWorker.kt`, `ReminderWindowEvaluator.kt`, [`docs/REMINDERS.md`](REMINDERS.md)    |
| PDF-Aufbau                      | `PdfExporter.kt`, `PdfUtilities.kt`                                                          |
| Verpflegungspauschale / Leipzig | `MealAllowanceCalculator.kt`, [`docs/AGENT_CONTEXT_PACKS.md`](AGENT_CONTEXT_PACKS.md)        |
| Privacy/Permissions             | [`docs/PRIVACY_CONTEXT.md`](PRIVACY_CONTEXT.md)                                              |
| Aufgabenbezogener Einstieg      | [`docs/AGENT_CONTEXT_PACKS.md`](AGENT_CONTEXT_PACKS.md)                                      |
| Checks je Änderungstyp          | [`docs/VALIDATION_MATRIX.md`](VALIDATION_MATRIX.md)                                          |
