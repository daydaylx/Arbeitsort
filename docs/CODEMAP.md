# CODEMAP – MontageZeit

Feature-zu-Datei-Karte für schnellen Einstieg. Kein Architektur-Text —
dafür [`docs/ARCHITECTURE.md`](ARCHITECTURE.md).

Basis-Pfad: `app/src/main/java/de/montagezeit/app/`

---

## Datenbank & Persistenz

| Datei                                          | Zweck                                                         |
| ---------------------------------------------- | ------------------------------------------------------------- |
| `data/local/database/AppDatabase.kt`           | Room-DB, Schema v17, alle Migrations-Objekte                  |
| `data/local/database/WorkEntryDao.kt`          | Alle CRUD-Operationen für WorkEntry + TravelLeg               |
| `data/local/database/DatabaseBackupManager.kt` | Automatisches DB-Backup vor Migrationen                       |
| `data/local/database/Converters.kt`            | Room-Typ-Konverter (LocalDate, LocalTime, Enums)              |
| `data/local/datastore/`                        | DataStore-Präferenzen (Reminder-Einstellungen)                |
| `data/preferences/`                            | SharedPreferences-Manager (Reminder-Flags, App-Einstellungen) |

## Repository

| Datei                                        | Zweck                               |
| -------------------------------------------- | ----------------------------------- |
| `data/repository/WorkEntryRepository.kt`     | Repository-Interface                |
| `data/repository/RoomWorkEntryRepository.kt` | Room-Implementierung des Repository |

## Domänenschicht

| Datei / Paket                                | Zweck                                                         |
| -------------------------------------------- | ------------------------------------------------------------- |
| `domain/usecase/RecordDailyManualCheckIn.kt` | Primärpfad Today-Screen: Eintrag in einem Schritt abschließen |
| `domain/usecase/RecordMorningCheckIn.kt`     | Morning-Snapshot via Notification-Action                      |
| `domain/usecase/RecordEveningCheckIn.kt`     | Evening-Snapshot via Notification-Action                      |
| `domain/usecase/ConfirmWorkDay.kt`           | Arbeitstag manuell bestätigen                                 |
| `domain/usecase/ConfirmOffDay.kt`            | Freien Tag bestätigen                                         |
| `domain/usecase/SetDayLocation.kt`           | Einsatzort setzen                                             |
| `domain/usecase/SetTravelEvent.kt`           | Reiseabschnitt erfassen                                       |
| `domain/usecase/DeleteDayEntry.kt`           | Eintrag löschen                                               |
| `domain/model/DayClassification.kt`          | Domain-Enum (7 Werte) für Statistik/Export                    |
| `domain/time/TimeCalculator.kt`              | Netto-Arbeitszeit, Überstunden-Berechnung                     |
| `domain/util/MealAllowanceCalculator.kt`     | Verpflegungspauschale-Berechnung                              |
| `domain/util/NonWorkingDayChecker.kt`        | Wochenend-/Feiertags-Erkennung                                |

## Export

| Datei                        | Zweck                                                  |
| ---------------------------- | ------------------------------------------------------ |
| `export/PdfExporter.kt`      | PDF-Generierung (Canvas-basiert, max. 180 Einträge)    |
| `export/CsvExporter.kt`      | CSV-Export                                             |
| `export/PdfUtilities.kt`     | Formatierungs-Helper (Datum, Zeit, Reise, Verpflegung) |
| `export/ExportDayMetrics.kt` | Berechnete Kennzahlen pro Tag für Export               |

## Erinnerungssystem

| Datei                                         | Zweck                                                        |
| --------------------------------------------- | ------------------------------------------------------------ |
| `work/ReminderScheduler.kt`                   | Enqueued WorkManager-Workers                                 |
| `work/WindowCheckWorker.kt`                   | Periodischer Worker, prüft Zeitfenster + sendet Notification |
| `work/ReminderLaterWorker.kt`                 | One-time Worker für "Später erinnern"-Aktion                 |
| `work/ReminderWindowEvaluator.kt`             | Prüft ob aktuelles Zeitfenster aktiv ist                     |
| `work/ReminderScheduleCalculator.kt`          | Intervall-Berechnung                                         |
| `notification/ReminderNotificationManager.kt` | Notification-Erstellung und -Anzeige                         |
| `service/CheckInActionService.kt`             | Foreground-Service für Notification-Actions                  |
| `receiver/BootReceiver.kt`                    | Startet Workers nach Geräte-Reboot                           |
| `receiver/TimeChangeReceiver.kt`              | Reagiert auf Zeitzone-/Uhrzeitänderungen                     |

## UI – Screens

| Screen   | Package               | Zweck                                                   |
| -------- | --------------------- | ------------------------------------------------------- |
| Today    | `ui/screen/today/`    | Tageserfassung, Morning/Evening-Snapshots               |
| Overview | `ui/screen/overview/` | Monatsübersicht, Statistiken, Überstunden               |
| History  | `ui/screen/history/`  | Historische Einträge, Datumsbereich                     |
| Edit     | `ui/screen/edit/`     | Vollformular für einen Arbeitstag inkl. Reiseabschnitte |
| Export   | `ui/screen/export/`   | PDF/CSV-Export, Datumsbereich, Profildaten              |
| Settings | `ui/screen/settings/` | Reminder-Fenster, App-Einstellungen                     |

## Dependency Injection

| Datei                     | Bereitgestellte Objekte                                        |
| ------------------------- | -------------------------------------------------------------- |
| `di/DatabaseModule.kt`    | `AppDatabase`, `WorkEntryDao`, `WorkEntryRepository`           |
| `di/ApplicationModule.kt` | `Clock`, alle Use Cases, `WorkManager`, `NonWorkingDayChecker` |

## Querschnitt

| Datei                                  | Zweck                                            |
| -------------------------------------- | ------------------------------------------------ |
| `data/logging/RingBufferLogger.kt`     | Lokales Debug-Log (max. 2 MB, kein Cloud-Upload) |
| `diagnostics/DiagnosticTrace.kt`       | Structured-Logging für Reminder-Entscheidungen   |
| `ui/theme/`                            | Material Design 3 Theming (Farben, Typografie)   |
| `ui/navigation/MontageZeitNavGraph.kt` | Compose Navigation Graph                         |

---

## Schnell-Suche nach Konzept

| Ich suche…            | Hier nachschauen                                               |
| --------------------- | -------------------------------------------------------------- |
| DB-Schema / Tabellen  | `AppDatabase.kt` + `docs/DATENMODELL.md`                       |
| Bestätigungslogik     | `EditEntrySaveBuilder.kt`, `ConfirmWorkDay.kt`, ADR-0004       |
| Reisedaten            | `TravelLeg`, `SetTravelEvent.kt`, ADR-0003                     |
| Erinnerungs-Timing    | `WindowCheckWorker.kt`, `ReminderWindowEvaluator.kt`, ADR-0005 |
| PDF-Aufbau            | `PdfExporter.kt`, `PdfUtilities.kt`, ADR-0006                  |
| Verpflegungspauschale | `MealAllowanceCalculator.kt`, `DAY_CLASSIFICATION.md`          |
| Migration schreiben   | `AppDatabase.kt` (Migrations-Objekte), `docs/adr/0007`         |
