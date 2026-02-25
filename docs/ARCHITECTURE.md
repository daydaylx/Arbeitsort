# Architektur - MontageZeit

**Status:** Aktiv / verbindlich
**Letzte Aktualisierung:** 2026-02-25

## Dokumentstatus

Diese Datei ist zusammen mit `README.md` die verbindliche Architektur-Referenz.

## 1. Systemüberblick

MontageZeit ist eine single-module Android App mit offline-first Datenhaltung.

Layer:
- UI: Compose Screens + ViewModels
- Domain: UseCases + reine Geschäftslogik
- Data: Room/DAO, DataStore

## 2. Kernkomponenten

### 2.1 Persistence

- Datenbank: `AppDatabase` (`version = 10`)
- Haupttabelle: `work_entries`
- Migrationen: `MIGRATION_1_2` bis `MIGRATION_9_10`

### 2.2 Reminder & Scheduling

Dateien:
- `work/ReminderScheduler.kt`
- `work/WindowCheckWorker.kt`
- `work/ReminderLaterWorker.kt`
- `notification/ReminderNotificationManager.kt`
- `handler/CheckInActionService.kt`

Strategie:
- Dedizierte WorkManager-Jobs für `MORNING`, `EVENING`, `FALLBACK`, `DAILY`
- Morning/Evening nutzen konfigurierbare Intervalle (min. 15 Minuten wegen WorkManager-Limit)
- Fensterprüfung im Worker über `ReminderWindowEvaluator`
- Tagesflags in SharedPreferences (`reminder_flags`) verhindern Mehrfach-Notifies
- Legacy-Worker ohne `reminder_type` laufen als no-op zur Spam-Vermeidung

### 2.3 Daily Flow (Today UI)

- Primärpfad im Today-Screen: `RecordDailyManualCheckIn`
- Setzt einen Arbeitstag in einem Schritt auf abgeschlossen:
  - `dayType = WORK`
  - `dayLocationLabel` aus manueller Eingabe (mit Prefill/Fallback über `ResolveDayLocationPrefill`)
  - `confirmedWorkDay = true` + `confirmation*`
  - Morning/Evening-Snapshots werden als erfasst markiert, damit keine weiteren Today-Schritte offen bleiben
- Optionale Nebenaktion: `ConfirmOffDay`
- Quelle (`confirmationSource`) wird mitgeführt (z. B. `UI`, `NOTIFICATION`)

### 2.4 Reminder Action UseCases

- `RecordMorningCheckIn`
- `RecordEveningCheckIn`
- `ConfirmWorkDay`

Diese Pfade bleiben für Notification-Actions/Worker relevant.

## 3. Datenmodell (WorkEntry)

`WorkEntry` beinhaltet:
- Tagesstatus (`dayType`, `confirmedWorkDay`, `confirmation*`)
- Arbeitszeit-Defaults (`workStart`, `workEnd`, `breakMinutes`)
- Daily Location (`dayLocationLabel`, `dayLocationSource`, Koordinaten)
- Morning/Evening Snapshots (Timestamp, Label, Koordinaten, Accuracy, Status)
- Travel-Daten (`travel*`)
- Qualitätsflags (`needsReview`, `note`, Timestamps)

### DayType Enum
- `WORK` - Arbeitstag
- `OFF` - Frei/Urlaub
- `COMP_TIME` - Überstundenabbau (ganzer Tag)

Ergänzende Helper zur Entkopplung von Copy-Bomben:
- `WorkEntry.withTravelCleared(...)`
- `WorkEntry.withConfirmedOffDay(...)`
- `createConfirmedOffDayEntry(...)`

## 4. Privacy / Security

### 4.1 Lokale Logs

- `RingBufferLogger` schreibt lokal in `files/logs/debug.log`
- max. 2 MB, Rotation behält neueste Zeilen
- Kein Cloud-Upload

### 4.2 Berechtigungen

- `POST_NOTIFICATIONS` - Für Reminder
- `RECEIVE_BOOT_COMPLETED` - Für Reboot-Resilienz
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` - Für Check-In Actions

Keine Standort-Berechtigungen mehr (manuelles Check-in System).

## 5. Teststrategie

- Unit-Tests für Domain-UseCases und Helper
- Reminder-Window/Boundary-Tests
- Robolectric Migrationstests für Room-Migrationen

Mindest-Pipelines:
- `./gradlew assembleDebug`
- `./gradlew assembleRelease`
- `./gradlew test`
