# Architektur - MontageZeit

**Status:** Aktiv / verbindlich  
**Letzte Aktualisierung:** 2026-02-07

## Dokumentstatus

Diese Datei ist zusammen mit `README.md` die verbindliche Architektur-Referenz.

## 1. Systemüberblick

MontageZeit ist eine single-module Android App mit offline-first Datenhaltung.

Layer:
- UI: Compose Screens + ViewModels
- Domain: UseCases + reine Geschäftslogik
- Data: Room/DAO, DataStore, LocationProvider, Netzwerkadapter

## 2. Kernkomponenten

### 2.1 Persistence

- Datenbank: `AppDatabase` (`version = 6`)
- Haupttabelle: `work_entries`
- Cache-Tabelle: `route_cache`
- Migrationen: `MIGRATION_1_2` bis `MIGRATION_5_6`

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

### 2.3 Confirmation

- `ConfirmWorkDay`: markiert Arbeitstag + Location-Entscheidung + `needsReview` bei Unsicherheit
- `ConfirmOffDay`: markiert freien Tag + setzt Confirmation-Felder + löscht Travel-Details
- Quelle (`confirmationSource`) wird mitgeführt (z. B. Notification/UI)

### 2.4 Check-in UseCases

- `RecordMorningCheckIn`
- `RecordEveningCheckIn`

Gemeinsame Logik liegt in:
- `domain/usecase/CheckInEntryBuilder.kt`
- `domain/usecase/DayLocationResolver.kt`

Ziel:
- keine divergierende Morning/Evening-Implementierung
- konsistente `needsReview`/DayLocation-Regeln

## 3. Datenmodell (WorkEntry)

`WorkEntry` beinhaltet:
- Tagesstatus (`dayType`, `confirmedWorkDay`, `confirmation*`)
- Arbeitszeit-Defaults (`workStart`, `workEnd`, `breakMinutes`)
- Morning/Evening Snapshots (Koordinaten, Label, Accuracy, Status)
- Tagesstandort (`dayLocation*`)
- Travel-Daten (`travel*`)
- Qualitätsflags (`needsReview`, `note`, Timestamps)

Ergänzende Helper zur Entkopplung von Copy-Bomben:
- `WorkEntry.withTravelCleared(...)`
- `WorkEntry.withConfirmedOffDay(...)`
- `createConfirmedOffDayEntry(...)`

## 4. Privacy / Security

### 4.1 API-Key Handling

- Routing-Key wird über `RoutingSettingsManager` in DataStore gespeichert.
- Key wird nicht in Logs geschrieben.
- Ohne Key wird Routing mit `ApiError` abgebrochen.

### 4.2 Externe Datenflüsse

Outbound zu `api.openrouteservice.org` nur bei aktiver Distanzberechnung:
- Geocoding: Standortlabel -> Koordinaten
- Routing: Koordinaten Start/Ziel -> Distanz

### 4.3 Lokale Logs

- `RingBufferLogger` schreibt lokal in `files/logs/debug.log`
- max. 2 MB, Rotation behält neueste Zeilen

## 5. Teststrategie

- Unit-Tests für Domain-UseCases und Helper
- Reminder-Window/Boundary-Tests
- Robolectric Migrationstests für Room-Migrationen

Mindest-Pipelines:
- `./gradlew assembleDebug`
- `./gradlew assembleRelease`
- `./gradlew test`
