# CLAUDE.md

Diese Datei enthält Hinweise für Claude Code (claude.ai/code) bei der Arbeit in diesem Repository.

## Projektübersicht

**MontageZeit** ist eine Offline-First-Android-App für Montageure zur täglichen Erfassung von Arbeitsorten mit manuellem Check-in und Arbeitszeiterfassung. Produktionsreifes MVP mit Kotlin + Jetpack Compose.

**Zielgruppe:** Einzelnutzer (Privatnutzung), der zwischen verschiedenen Arbeitsorten reist.

## Build-Befehle

```bash
# Debug APK bauen
./gradlew assembleDebug

# Auf verbundenem Device/Emulator installieren
./gradlew installDebug

# Unit Tests ausführen
./gradlew testDebugUnitTest

# Instrumentierte Tests (benötigt Device/Emulator)
./gradlew connectedDebugAndroidTest

# Clean Build
./gradlew clean

# Release Build (mit ProGuard/R8-Minifizierung)
./gradlew assembleRelease

# Gradle Wrapper erstellen, falls fehlend
gradle wrapper --gradle-version 8.2
```

## Architektur

Clean Architecture mit drei Layern:

1. **UI Layer** (`ui/`): Jetpack Compose Screens mit ViewModels, Material3-Theme, Bottom-Navigation (Heute/Verlauf/Einstellungen)
2. **Domain Layer** (`domain/`): UseCases mit Business-Logik
3. **Data Layer** (`data/`): Room-Datenbank (`work_entries`-Tabelle), DataStore für Preferences

**Datenfluss:** UI → UseCase → Repository → Database

**Dependency Injection:** Hilt mit `@HiltAndroidApp` auf `MontageZeitApp` und Modulen in `di/`

## Wichtige Komponenten

### Manuelles Check-in System
- **Daily Check-in:** `RecordDailyManualCheckIn` setzt Tagesort und markiert Tag als abgeschlossen
- **Tagesort Prefill:** `ResolveDayLocationPrefill` liefert Vorschläge basierend auf letzten Arbeitstagen
- **DayType:** `WORK`, `OFF`, `COMP_TIME` (Überstundenabbau)

### Datenmodell (`work_entries`-Tabelle)
- Primary Key: `date` (LocalDate) - ein Eintrag pro Tag
- Tagesstatus: `dayType`, `confirmedWorkDay`, `confirmation*`
- Tagesort: `dayLocationLabel`, `dayLocationSource`, Koordinaten
- Morgens-/Abends-Snapshots: Timestamp, Ortslabel, Koordinaten, Genauigkeit, Status
- Arbeitszeiten: `workStart`, `workEnd`, `breakMinutes` (Defaults: 08:00-19:00, 60min)
- Reise-Events (optional): `travelStartAt`, `travelArriveAt`, Labels
- Meta: `needsReview`, `note`, `createdAt`, `updatedAt`

### Reminder-System
- **WorkManager** für zeitbasierte Erinnerungen (Fenster: 06:00-13:00 morgens, 16:00-22:30 abends)
- **Dedizierte Worker:** `MORNING`, `EVENING`, `FALLBACK`, `DAILY`
- **Notification Actions:** Check-in, später erinnern
- **Edge Cases:** `BOOT_COMPLETED`- und `TIMEZONE_CHANGED`-Receiver planen Reminder neu
- **Samsung Sleep Mode:** Erfordert besondere Behandlung (App muss vom Schlafmodus ausgenommen werden)

### Export
- **CSV-Format:** Semikolon-separiert, UTF-8
- **Offline:** Kein Netzwerk erforderlich
- **Felder:** date, dayType, workStart, workEnd, breakMinutes, dayLocation, morning/evening Snapshots, travel, note, needsReview, Timestamps

### Logging
- **RingBufferLogger:** 2MB rotierendes lokales Debug-Log unter `files/logs/debug.log`
- Kein Cloud-Upload (Privacy-First)

## Package-Struktur

```
de.montagezeit.app/
├── data/
│   ├── local/          # Room: entity, dao, database, converters
│   └── preferences/    # DataStore Settings
├── domain/
│   ├── model/          # Domain models
│   └── usecase/        # Alle Business-Logic UseCases
├── export/            # CsvExporter
├── logging/           # RingBufferLogger
├── ui/
│   ├── theme/          # Material3 Theme
│   ├── navigation/     # Nav Graph
│   └── screen/         # Today, History, Settings, Edit Screens
├── receiver/          # BootReceiver, TimeChangeReceiver
├── handler/           # CheckInActionService
├── work/              # WindowCheckWorker, ReminderScheduler
├── notification/      # ReminderNotificationManager
├── di/                # Hilt Module
├── MainActivity.kt
└── MontageZeitApp.kt   # @HiltAndroidApp
```

## Wichtige Implementierungshinweise

### DayType Handling
- **WORK:** Standard-Arbeitstag mit Tagesort
- **OFF:** Frei/Urlaub - keine Snapshots erforderlich
- **COMP_TIME:** Überstundenabbau - ganzer Tag wird vom Überstundenkonto abgezogen

### UseCase-Verhalten
- **Idempotent:** Mehrere Check-ins für dasselbe Datum überschreiben nur die jeweiligen Felder
- **Upsert:** `RecordDailyManualCheckIn` erstellt neuen Eintrag, wenn keiner existiert
- **Tages-Handling:** Kalenderwochen-(KW)-Gruppierung für Verlauf-Screen

### Testing
- **Unit Tests:** UseCases, Helpers (MockK für Dependencies)
- **Instrumentiert:** Room DAO, Migrationen
- **Manuelle Tests:** Siehe `docs/QA_CHECKLIST.md` für Top-10-Szenarien

## Dependencies

- Kotlin 1.9.20, JVM 17, Compose Compiler 1.5.4
- Room 2.6.1, DataStore 1.0.0, WorkManager 2.9.0
- Hilt 2.48, Navigation Compose 2.7.5
- MockK 1.13.8 für Tests

## Dokumentation

- `docs/ARCHITECTURE.md` - Vollständige Architektur-Spezifikation
- `docs/ASSUMPTIONS.md` - Alle technischen Annahmen und Entscheidungen
- `docs/QA_CHECKLIST.md` - Top-10-Test-Szenarien

## Version

Aktuell: 1.0.1
Database Version: 10
