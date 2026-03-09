# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- **History**: KW-Header in Listenansicht zeigt jetzt Datumsbereich (`KW 10 · 03.03.2026 – 09.03.2026`),
  konsistent mit der Kalenderansicht.
- **Notifications**: Snooze-Action „10 Min." in Morning-, Evening- und Fallback-Reminder ergänzt.
  Tritt neben die bestehenden 1h/2h-Optionen. Die `scheduleReminderLater`-Funktion verwendet
  intern jetzt Minuten statt Stunden als Zeiteinheit.

## [1.0.2]

### Fixed
- **Coroutines**: `CancellationException` in `LocationProviderImpl.requestLocation()` wird jetzt
  korrekt re-thrown statt zu `LocationResult.Timeout` konvertiert zu werden. Zuvor war die
  strukturierte Nebenläufigkeit gebrochen – der Parent-Scope konnte hängen bleiben, wenn er
  gecancelt wurde.
- **Service**: Race Condition in `CheckInActionService` bei `ACTION_REMIND_LATER` behoben.
  `stopSelf()` wurde außerhalb des Coroutine-Blocks aufgerufen und beendete den Service (und
  damit den `serviceScope`) bevor WorkManager den Reminder einreihen konnte. Der Reminder ging
  auf langsamen Geräten oder unter Last verloren.
- **Service**: `CheckInActionService` cancelt bei `ACTION_REMIND_LATER` jetzt nur noch den
  spezifischen Reminder-Typ (MORNING/EVENING/FALLBACK/DAILY), der gesnoozed wird. Nur wenn
  kein Typ übergeben wird (`null`-Fallback), werden alle Reminder gecancelt.
- **Worker**: `WindowCheckWorker` prüft in `checkAndShowDailyReminder()` jetzt ebenfalls auf
  Nicht-Werktage (`isNonWorkingDay`), analog zu Morning/Evening/Fallback. An Nicht-Werktagen
  wird das Flag gesetzt und kein Daily-Reminder angezeigt.
- **History**: COMP_TIME Confirmation-State wird bei Batch-Edit korrekt synchronisiert.
- **Logic**: Boolean-Präzedenz in `ConfirmWorkDay`, `RecordMorningCheckIn` und
  `RecordEveningCheckIn` korrigiert, um `needsReview`-Status korrekt zu bestimmen.
- **CheckInEntryBuilder**: Behält den bestehenden `dayType` eines existierenden Eintrags.
  Nur für neue Einträge (kein DB-Eintrag vorhanden) wird `WORK` als Default gesetzt.
- **Worker**: `ReminderLaterWorker` behandelt `null`-Fallback korrekt (kein Absturz mehr
  wenn kein Reminder-Typ übergeben wird).

### Added
- **DayType**: `COMP_TIME` als eigener Tagestyp für Überstundenabbau.

### Changed
- **Repo-Hygiene**: `.gitignore` erweitert um `debug_artifacts/`, `*.db`, `*.sqlite`,
  `app/exports/`. Bereits getrackte Dateien (`debug_artifacts/`, `app/exports/`,
  `logs/*.log`, 4 macOS-Ghost-Pfade `>/Users/...`) aus dem Git-Index entfernt.

### Added
- **PDF Export**:
  - Implemented correct travel time calculation using `travelPaidMinutes` field.
  - Added "Reisezeit" column to PDF table.
  - Added summary totals for travel time and paid time.
  - Added explicit storage space check (min 5MB) before generating PDF to prevent partial file corruption.
  - Improved error handling during file writing.
- **Today Daily Check-in**:
  - Added automatic location prefill from the most recent stored `dayLocationLabel` (prefer `WORK`, fallback any day, then settings default).
  - Added explicit test coverage for "today entry missing -> prefill from last location".
  - Added `ResolveDayLocationPrefill` as a shared resolver to avoid duplicated prefill logic between UI and use case.

### Fixed
- **PDF Export**:
  - Fixed "Travel Time" appearing as empty when paid minutes are missing; now falls back to calculation.
- **Logic**: Clarified boolean precedence in `ConfirmWorkDay`, `RecordEveningCheckIn`, and `RecordMorningCheckIn` to correctly determine `needsReview` status.
- **CSV Export**: Sanitized note fields by removing newlines and returns to prevent CSV format corruption.

### Changed
- **UI**:
  - Improved `ExportPreviewViewModel` error messaging.
  - Updated `TodayScreen` status card layout to include an edit icon for better discoverability.
  - Simplified Today flow to one primary daily action path and removed legacy morning/evening confirm branches from `TodayViewModel`.
  - Removed unused legacy action card and location-error full-screen branch from `TodayScreenV2`.
  - Removed unused advanced history options sheet and stale string resources.
  - Added worker decision tests to verify completed manual daily entries do not re-trigger reminder logic.
