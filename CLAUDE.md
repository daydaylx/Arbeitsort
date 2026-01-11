# CLAUDE.md

Diese Datei enthält Hinweise für Claude Code (claude.ai/code) bei der Arbeit in diesem Repository.

## Projektübersicht

**MontageZeit** ist eine Offline-First-Android-App für Montageure zur täglichen Erfassung von Arbeitsorten mit Morgens-/Abends-Snapshots und Arbeitszeiterfassung. Produktionsreifes MVP mit Kotlin + Jetpack Compose.

**Zielgruppe:** Einzelnutzer (Privatnutzung), der zwischen verschiedenen Arbeitsorten reist und Standorte relativ zu Leipzig erfassen muss.

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
2. **Domain Layer** (`domain/`): UseCases mit Business-Logik, LocationCalculator für Radius-Checks um Leipzig
3. **Data Layer** (`data/`): Room-Datenbank (`work_entries`-Tabelle), LocationProvider (FusedLocationProvider), DataStore für Preferences

**Datenfluss:** UI → UseCase → Repository/Provider → Database/System

**Dependency Injection:** Hilt mit `@HiltAndroidApp` auf `MontageZeitApp` und Modulen in `di/`

## Wichtige Komponenten

### Standort & Radius-Logik
- **Leipzig-Zentrum:** 51.340, 12.374
- **Standard-Radius:** 30 km (konfigurierbar)
- **Grenzzone:** ±2 km (28-32 km) erfordert manuelle Bestätigung
- **Genauigkeits-Schwellenwert:** ≤3000m ist akzeptabel, >3000m setzt `needsReview=true`
- **LocationCalculator:** Verwendet Haversine-Formel für Distanzberechnung
- **LocationProvider:** 15s Timeout, COARSE Location Priorität, Graceful Fallback

### Datenmodell (`work_entries`-Tabelle)
- Primary Key: `date` (LocalDate) - ein Eintrag pro Tag
- Morgens-/Abends-Snapshots: Timestamp, Ortslabel, Koordinaten, Genauigkeit, `outsideLeipzig`-Boolean
- Arbeitszeiten: `workStart`, `workEnd`, `breakMinutes` (Defaults: 08:00-19:00, 60min)
- Reise-Events (optional): `travelStartAt`, `travelArriveAt`, Labels
- Meta: `needsReview`, `note`, `createdAt`, `updatedAt`

### Reminder-System
- **WorkManager** für zeitbasierte Erinnerungen (Fenster: 06:00-13:00 morgens, 16:00-22:30 abends)
- **Notification Actions:** Check-in, ohne Standort, später
- **Edge Cases:** `BOOT_COMPLETED`- und `TIMEZONE_CHANGED`-Receiver planen Reminder neu
- **Samsung Sleep Mode:** Erfordert besondere Behandlung (App muss vom Schlafmodus ausgenommen werden)

### Export
- **CSV-Format:** Semikolon-separiert, UTF-8
- **Offline:** Kein Netzwerk erforderlich
- **Felder:** date, dayType, workStart, workEnd, breakMinutes, morning/evening Snapshots, travel, note, needsReview, Timestamps

### Logging
- **RingBufferLogger:** 2MB rotierendes lokales Debug-Log unter `files/logs/debug.log`
- Kein Cloud-Upload (Privacy-First)

## Package-Struktur

```
de.montagezeit.app/
├── data/
│   ├── local/          # Room: entity, dao, database, converters
│   ├── location/       # LocationProvider interface + impl
│   └── preferences/    # DataStore Settings
├── domain/
│   ├── model/          # LocationResult sealed class
│   ├── location/       # LocationCalculator
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
├── di/                # Hilt Module
├── MainActivity.kt
└── MontageZeitApp.kt   # @HiltAndroidApp
```

## Wichtige Implementierungshinweise

### Graceful Degradation
- App funktioniert auch ohne Standort-Berechtigung (Einträge werden ohne Location-Daten gespeichert)
- `needsReview`-Flag wird gesetzt bei: Grenzzone, niedrige Genauigkeit, Timeout, nicht verfügbar
- `LocationStatus`-Enum: `OK`, `UNAVAILABLE`, `LOW_ACCURACY`

### UseCase-Verhalten
- **Idempotent:** Mehrere Check-ins für dasselbe Datum überschreiben nur die jeweiligen Snapshot-Felder
- **Upsert:** `RecordMorningCheckIn` erstellt neuen Eintrag, wenn keiner existiert
- **Tages-Handling:** Kalenderwochen-(KW)-Gruppierung für Verlauf-Screen

### Testing
- **Unit Tests:** LocationCalculator, UseCases (MockK für Dependencies)
- **Instrumentiert:** Room DAO, LocationProvider Integration
- **Manuelle Tests:** Siehe `docs/QA_CHECKLIST.md` für Top-10-Szenarien

## Dependencies

- Kotlin 1.9.20, JVM 17, Compose Compiler 1.5.4
- Room 2.6.1, DataStore 1.0.0, WorkManager 2.9.0
- Play Services Location 21.0.1
- Hilt 2.48, Navigation Compose 2.7.5
- MockK 1.13.8 für Tests

## Dokumentation

- `docs/ARCHITECTURE.md` - Vollständige Architektur-Spezifikation
- `docs/ASSUMPTIONS.md` - Alle technischen Annahmen und Entscheidungen
- `docs/QA_CHECKLIST.md` - Top-10-Test-Szenarien
- `docs/KNOWN_LIMITATIONS.md` - Ehrliche Übersicht der Limitationen
- `docs/IMPLEMENTATION_NOTES.md` - Implementierungsdetails

## Version

Aktuell: 1.0.0-MVP-Production-Ready
