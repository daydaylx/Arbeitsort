# Prüfmatrix – MontageZeit

**Status:** Aktiv
**Letzte Aktualisierung:** 2026-06-13

Übersicht der Testabdeckung je Feature-Bereich. Manuelle Prüfpfade:
[`docs/MANUELLE_TESTS.md`](MANUELLE_TESTS.md)

---

## Legende

- **Unit-Tests** — `app/src/test/` (JVM, Robolectric)
- **Instrumented** — `app/src/androidTest/` (Gerät/Emulator)
- **Manuell** — Checkliste in `MANUELLE_TESTS.md`

---

## Matrix

### Tageserfassung (Today-Screen)

| Aspekt                              | Unit-Tests                                               | Instrumented | Manuell |
| ----------------------------------- | -------------------------------------------------------- | ------------ | ------- |
| Morning-Snapshot anlegen            | `RecordMorningCheckInTest`                               | —            | MT-01   |
| Evening-Snapshot anlegen            | `RecordEveningCheckInTest`                               | —            | MT-01   |
| Manueller Check-in (Today-Screen)   | `RecordDailyManualCheckInTest`                           | —            | MT-01   |
| Eintrag bearbeiten & speichern      | `EditEntrySaveBuilderTest`, `EditFormDataValidationTest` | —            | MT-02   |
| Eintrag löschen                     | `DeleteDayEntryTest`                                     | —            | MT-02   |
| Einsatzort setzen                   | `SetDayLocationTest`                                     | —            | MT-02   |
| Bestätigung WORK (pos. Arbeitszeit) | `ConfirmWorkDayTest`                                     | —            | MT-02   |
| Bestätigung OFF-Tag                 | `ConfirmOffDayTest`                                      | —            | MT-02   |
| ViewModel Today-Screen              | `TodayViewModelTest`                                     | —            | —       |

### Reiseerfassung

| Aspekt                             | Unit-Tests           | Instrumented                    | Manuell |
| ---------------------------------- | -------------------- | ------------------------------- | ------- |
| Reiseabschnitt anlegen             | `SetTravelEventTest` | —                               | MT-03   |
| Mehrere Abschnitte, Sortierung     | `SetTravelEventTest` | —                               | MT-03   |
| Cascade-Delete bei Eintrag-Löschen | —                    | `WorkEntryDaoCascadeDeleteTest` | —       |
| Travel-Leg-Daten im DAO            | —                    | `WorkEntryDaoTravelLegTest`     | —       |

### Erinnerungssystem

| Aspekt                                       | Unit-Tests                       | Instrumented | Manuell |
| -------------------------------------------- | -------------------------------- | ------------ | ------- |
| Zeitfenster-Evaluierung                      | `ReminderWindowEvaluatorTest`    | —            | MT-04   |
| Morning/Evening-Fenster-Boundaries           | `ReminderWindowBoundaryTest`     | —            | MT-04   |
| WindowCheckWorker-Logik                      | `WindowCheckWorkerTest`          | —            | —       |
| ReminderLaterWorker                          | `ReminderLaterWorkerTest`        | —            | MT-04   |
| Notification-Aktionen (CheckInActionService) | `CheckInActionServiceTest`       | —            | MT-04   |
| Reboot-Resilienz (BootReceiver)              | `BootReceiverTest`               | —            | MT-04   |
| Zeitzone-/Uhrzeitänderung                    | `TimeChangeReceiverTest`         | —            | —       |
| Intervall-Berechnung                         | `ReminderScheduleCalculatorTest` | —            | —       |

### PDF-Export

| Aspekt                            | Unit-Tests                   | Instrumented            | Manuell |
| --------------------------------- | ---------------------------- | ----------------------- | ------- |
| Export-Logik (Inhalt, Felder)     | `PdfExporterLogicTest`       | —                       | MT-05   |
| Export-Preview Flow E2E           | —                            | `ExportPreviewFlowTest` | MT-05   |
| Datumsgrenzen (leerer Bereich)    | `PdfExporterLogicTest`       | —                       | MT-05   |
| Storage-Validierung (< 5 MB frei) | `PdfExporterLogicTest`       | —                       | —       |
| ViewModel Export-Screen           | `ExportPreviewViewModelTest` | —                       | —       |

### CSV-Export

| Aspekt                           | Unit-Tests        | Instrumented | Manuell |
| -------------------------------- | ----------------- | ------------ | ------- |
| CSV-Inhalt und Trennzeichen      | `CsvExporterTest` | —            | MT-05   |
| Fehlerbehandlung (Schreibfehler) | `CsvExporterTest` | —            | —       |

### Datenbankmigrationen

| Aspekt                         | Unit-Tests                               | Instrumented                     | Manuell |
| ------------------------------ | ---------------------------------------- | -------------------------------- | ------- |
| Migrations-Kette v1→17         | `AppDatabaseMigrationTest` (Robolectric) | `AppDatabaseSchemaMigrationTest` | MT-06   |
| Datenintegrität nach Migration | `AppDatabaseMigrationTest`               | —                                | MT-06   |
| v13→14 Travel-Normalisierung   | `AppDatabaseMigrationTest`               | —                                | —       |
| v16→17 SCHULUNG→WORK           | `AppDatabaseMigrationTest`               | —                                | —       |
| DAO CRUD nach Migration        | —                                        | `WorkEntryDaoTest`               | —       |

### Backup-Manager

| Aspekt                           | Unit-Tests                                | Instrumented | Manuell |
| -------------------------------- | ----------------------------------------- | ------------ | ------- |
| Backup bei Versionsdifferenz     | `DatabaseBackupManagerTest` (Robolectric) | —            | MT-07   |
| Kein Backup bei gleicher Version | `DatabaseBackupManagerTest`               | —            | —       |
| Pruning (max. 3 Backups)         | `DatabaseBackupManagerTest`               | —            | MT-07   |

### Statistik & Übersicht

| Aspekt                  | Unit-Tests                                 | Instrumented | Manuell |
| ----------------------- | ------------------------------------------ | ------------ | ------- |
| Netto-Arbeitszeit       | `TimeCalculatorTest`                       | —            | —       |
| Überstunden-Berechnung  | `TimeCalculatorTest`, `WeekCalculatorTest` | —            | MT-08   |
| Verpflegungspauschale   | `MealAllowanceCalculatorTest`              | —            | MT-08   |
| DayClassification-Logik | `DayClassificationTest`                    | —            | —       |
| ViewModel Overview      | `OverviewViewModelTest`                    | —            | —       |
| ViewModel History       | `HistoryViewModelTest`                     | —            | —       |

### Einstellungen

| Aspekt                              | Unit-Tests              | Instrumented | Manuell |
| ----------------------------------- | ----------------------- | ------------ | ------- |
| Reminder-Einstellungen persistieren | `SettingsViewModelTest` | —            | MT-09   |
| Reminder-Fenster konfigurieren      | —                       | —            | MT-09   |

### Querschnitt

| Aspekt                                | Unit-Tests                    | Instrumented            | Manuell |
| ------------------------------------- | ----------------------------- | ----------------------- | ------- |
| RingBufferLogger (Rotation, max 2 MB) | `RingBufferLoggerTest`        | —                       | —       |
| Repository-Schicht                    | `RoomWorkEntryRepositoryTest` | —                       | —       |
| Wochenend-/Feiertagserkennung         | `NonWorkingDayCheckerTest`    | —                       | —       |
| TimePickerDialog                      | —                             | `TimePickerDialogTest`  | —       |
| App startet (Smoke-Test)              | —                             | `SmokeInstrumentedTest` | MT-10   |

---

## Lücken (Stand 2026-06-13)

- Kein UI-Test für Today-Screen, Edit-Screen, History-Screen, Settings-Screen
- Keine automatisierte Prüfung für Doze-Verhalten des Erinnerungssystems
- Backup-Recovery (manuelles Wiederherstellen) nur manuell prüfbar (MT-07)
