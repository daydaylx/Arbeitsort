# MontageZeit (de.montagezeit.app)

Offline-first Android App für tägliche Erfassung von Arbeitstagen mit manuellem Tagesort, optionalen Reminder-Aktionen, Travel-Zeiten und Export.

## Source of Truth

Verbindliche Dokumentation:
- `README.md`
- `docs/ARCHITECTURE.md`

Alle anderen Dateien unter `docs/` sind nur ergänzende, teilweise archivierte Referenzen.

## Tech Stack

- Kotlin, Coroutines, Flow
- Jetpack Compose (Material3)
- Room (Schema-Version 10, Migrationen 1→10)
- WorkManager
- Hilt (Dependency Injection)
- DataStore Preferences

## Voraussetzungen

- JDK 17
- Android SDK / Build Tools für `compileSdk 34`
- Optional: Android Studio Hedgehog+ oder aktuelles Gradle-CLI Setup

## Setup

```bash
git clone https://github.com/daydaylx/Arbeitsort.git
cd Arbeitsort
./gradlew tasks
```

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Today-Workflow (aktuell)

- Primäre Aktion auf dem Heute-Screen: `Einchecken (Arbeit)`.
- Der Orts-Dialog wird automatisch vorbefüllt:
  1. heutiger `dayLocationLabel`
  2. letzter gespeicherter `dayLocationLabel` aus Work-Tagen
  3. letzter gespeicherter `dayLocationLabel` aus beliebigen Tagen
  4. `defaultDayLocationLabel` aus Settings
- Der tägliche manuelle Check-in setzt den Tag direkt als abgeschlossen (`confirmedWorkDay = true`) und markiert Morning/Evening-Snapshots als erfasst, damit keine zusätzlichen Today-Schritte erforderlich sind.
- Optionale Nebenaktion: `Heute frei`.

## DayType

- `WORK` - Arbeitstag (Standard)
- `OFF` - Frei/Urlaub
- `COMP_TIME` - Überstundenabbau (ganzer Tag)

## Tests

```bash
# Alle Unit-Tests (Debug + Release)
./gradlew test

# Nur Debug Unit-Tests
./gradlew :app:testDebugUnitTest

# Optional: Instrumentation-Tests (Device/Emulator nötig)
./gradlew connectedDebugAndroidTest
```

## Kernpfade

- Reminder Scheduling: `app/src/main/java/de/montagezeit/app/work/ReminderScheduler.kt`
- Reminder Worker: `app/src/main/java/de/montagezeit/app/work/WindowCheckWorker.kt`
- Notification Actions: `app/src/main/java/de/montagezeit/app/handler/CheckInActionService.kt`
- Daily Check-in UseCases: `app/src/main/java/de/montagezeit/app/domain/usecase/RecordDailyManualCheckIn.kt`, `app/src/main/java/de/montagezeit/app/domain/usecase/ResolveDayLocationPrefill.kt`, `app/src/main/java/de/montagezeit/app/domain/usecase/ConfirmOffDay.kt`
- Reminder Action UseCases: `app/src/main/java/de/montagezeit/app/domain/usecase/RecordMorningCheckIn.kt`, `app/src/main/java/de/montagezeit/app/domain/usecase/RecordEveningCheckIn.kt`, `app/src/main/java/de/montagezeit/app/domain/usecase/ConfirmWorkDay.kt`
- DB + Migrationen: `app/src/main/java/de/montagezeit/app/data/local/database/AppDatabase.kt`

## Reminder-Verhalten (aktuell)

- Morning/Evening laufen als dedizierte `PeriodicWorkRequest`s.
- Intervall ist konfigurierbar über Settings (`morningCheckIntervalMinutes`, `eveningCheckIntervalMinutes`) mit WorkManager-Minimum von 15 Minuten.
- Fallback und Daily Confirmation laufen jeweils 1x täglich als separate Periodic-Worker.
- Legacy-Worker ohne Typ (`reminder_type`) führen keinen Reminder mehr aus (No-op), um Doppeltrigger zu verhindern.

## Berechtigungen

Deklariert in `app/src/main/AndroidManifest.xml`:

- `POST_NOTIFICATIONS` - Für Reminder
- `RECEIVE_BOOT_COMPLETED` - Für Reboot-Resilienz
- `FOREGROUND_SERVICE` - Für Check-In Actions
- `FOREGROUND_SERVICE_SPECIAL_USE` - Für Check-In Actions

Keine Standort-Berechtigungen mehr (manuelles Check-in System).

## Datenschutz & Security

### Standortdaten

- Tagesort wird manuell vom Benutzer eingegeben.
- Morning/Evening Snapshots sind optional.
- Daten bleiben lokal in Room, sofern nicht explizit exportiert/geteilt wird.

### Logging

- Lokaler Ringbuffer-Logger (`files/logs/debug.log`, max. 2 MB).
- Rotation behält bei Überlauf die neuesten Einträge.
- Keine Cloud-Log-Übertragung.

## Release-Hinweise

Vor einem Release mindestens ausführen:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
```

Zusätzlich empfehlenswert:

- Reminder-Manuelltest (Morning/Evening/Fallback/Daily)
- Boot/Zeitzonen-Change Test
- Export Smoke-Test (CSV)

## Version

1.0.1
Database Version: 10
