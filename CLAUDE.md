# CLAUDE.md

## Projektübersicht

**MontageZeit** ist eine Offline-First-Android-App für Montageure zur täglichen Erfassung von Arbeitsorten mit manuellem Check-in und Arbeitszeiterfassung.
Kotlin + Jetpack Compose. Einzelnutzer, reist zwischen verschiedenen Arbeitsorten.

## Befehle

```bash
./gradlew assembleDebug          # Debug APK bauen
./gradlew installDebug           # Auf Device/Emulator installieren
./gradlew testDebugUnitTest      # Unit Tests
./gradlew lint                   # Lint-Prüfung
./gradlew assembleRelease        # Release APK (ProGuard/R8)
bash install_on_device.sh        # Auf physisches Gerät installieren (RFCY210JHMJ)
bash scripts/hooks/run_local_quality_gate.sh  # Vollständiger Quality Gate (lint + tests + debug build)
./gradlew connectedDebugAndroidTest           # Instrumentierte Tests (Device erforderlich)
```

## Architektur

Clean Architecture: UI → UseCase → Repository → Database

- **UI** (`ui/`): Compose Screens + ViewModels, Material3, Bottom-Nav (Heute/Verlauf/Einstellungen)
- **Domain** (`domain/`): UseCases mit Business-Logik
- **Data** (`data/`): Room DB (`work_entries`), DataStore Preferences

DI: Hilt, `@HiltAndroidApp` auf `MontageZeitApp`, Module in `di/`

## Wichtige Komponenten

- **DayType:** `WORK`, `OFF`, `COMP_TIME` (Überstundenabbau = ganzer Tag vom Konto)
- **Reminder:** WorkManager-Worker (MORNING 06–13h, EVENING 16–22:30h, FALLBACK, DAILY)
- **Samsung Sleep Mode:** App muss explizit vom Schlafmodus ausgenommen werden
- **`ACTION_REMIND_LATER`:** Cancelt nur den jeweiligen Reminder-Typ, nicht alle
- **Export:** CSV (Semikolon, UTF-8) + PDF — kein Netzwerk erforderlich
- **Logging:** RingBufferLogger, 2MB rotierend unter `files/logs/debug.log`, kein Cloud-Upload

## Package-Struktur

```
de.montagezeit.app/
├── data/local/        # Room: entity, dao, database, converters
├── data/preferences/  # DataStore Settings
├── domain/usecase/    # Alle Business-Logic UseCases
├── domain/model/      # Domain-Models
├── export/            # CsvExporter, PdfExporter
├── ui/screen/         # Today, History, Settings, Edit, ExportPreview
├── work/              # WindowCheckWorker, ReminderScheduler
├── notification/      # ReminderNotificationManager
├── receiver/          # BootReceiver, TimeChangeReceiver
├── di/                # Hilt-Module
└── MontageZeitApp.kt  # @HiltAndroidApp
```

## UseCase-Verhalten

- **Idempotent:** Mehrere Check-ins für dasselbe Datum überschreiben nur die jeweiligen Felder
- **Upsert:** `RecordDailyManualCheckIn` erstellt neuen Eintrag, wenn keiner existiert
- **KW-Gruppierung:** Verlauf-Screen gruppiert nach Kalenderwochen

## Tests

Unit Tests: MockK für Dependencies, TestCoroutineDispatcher
Muster: `app/src/test/java/de/montagezeit/app/domain/usecase/`
Manuelle Tests: `docs/QA_CHECKLIST.md`

## Versionen

App 1.1.1 · DB v14 · Kotlin 2.1.10 · Room 2.7.1 · Hilt 2.56.2
