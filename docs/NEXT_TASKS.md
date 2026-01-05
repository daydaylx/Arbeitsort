# Next Tasks - MontageZeit MVP

**Erstellt:** 2026-01-05  
**Status:** Projekt Setup abgeschlossen, Ready für Implementation

---

## Voraussetzungen für folgende Tasks

1. Android SDK muss installiert sein (API 34)
2. Gradle oder Android Studio muss verfügbar sein
3. `./gradlew assembleDebug` muss erfolgreich laufen

---

## Top 10 Next Tasks (in chronologischer Reihenfolge)

### 1. LocationProvider Implementieren
- **Datei:** `app/src/main/java/de/montagezeit/app/data/location/LocationProvider.kt`
- **Aufgaben:**
  - Interface definieren: `suspend fun getCurrentLocation(timeoutMs: Long): LocationResult`
  - Fused Location Provider implementieren
  - Fallback auf Android Location Manager
  - Timeout-Handling (15s)
  - Accuracy-Prüfung (≤3000m OK, >3000m LOW_ACCURACY)
  - Permission-Handling
- **Tests:** Unit Tests für Timeout, Accuracy, Unavailable
- **Dokumentation:** ARCHITECTURE.md Sektion 5

### 2. SettingsRepository Implementieren (DataStore)
- **Datei:** `app/src/main/java/de/montagezeit/app/data/preferences/SettingsRepository.kt`
- **Aufgaben:**
  - DataStore Preferences initialisieren
  - `AppSettings` data class definieren
  - getSettings(): Flow<AppSettings>
  - updateSettings(transform: (AppSettings) -> AppSettings)
  - Defaults aus ASSUMPTIONS.md (08:00-19:00, 30km Radius, etc.)
- **Tests:** Unit Tests für Persistenz, Defaults, Updates
- **Dokumentation:** ARCHITECTURE.md Sektion 6

### 3. UseCases Implementieren (Domain Layer)
- **Dateien:** `app/src/main/java/de/montagezeit/app/domain/usecase/*.kt`
- **Aufgaben:**
  - `RecordMorningCheckIn`: Location holen → Radius-Check → Upsert DB
  - `RecordEveningCheckIn`: Analog Morning
  - `UpdateEntry`: Manuelle Änderungen
  - `GetTodayEntry`: Hole Entry mit Defaults wenn nicht existent
  - `CalculateDistanceToLeipzig`: Haversine-Formel
  - `IsOutsideLeipzig`: Distance > Radius + Grenzzone
- **Tests:** Unit Tests für alle UseCases (Radius, Grenzzone, Defaults)
- **Dokumentation:** ARCHITECTURE.md Sektion 3

### 4. Reminder System Implementieren (WorkManager)
- **Dateien:** 
  - `app/src/main/java/de/montagezeit/app/work/ReminderScheduler.kt`
  - `app/src/main/java/de/montagezeit/app/work/ReminderWorker.kt`
- **Aufgaben:**
  - Morning Reminder Worker (OneTimeWorkRequest, 06:00-13:00 Fenster)
  - Evening Reminder Worker (16:00-22:30 Fenster)
  - Fallback Reminder (22:30, wenn Tag unvollständig)
  - Notification mit Actions (Check-in, Ohne Standort)
  - `BootReceiver` implementieren: Reminder nach Reboot neu planen
- **Tests:** Instrumented Tests für WorkManager, BootReceiver
- **Dokumentation:** ARCHITECTURE.md Sektion 4

### 5. TodayScreen Implementieren (Compose)
- **Dateien:**
  - `app/src/main/java/de/montagezeit/app/ui/screen/today/TodayScreen.kt`
  - `app/src/main/java/de/montagezeit/app/ui/screen/today/TodayViewModel.kt`
- **Aufgaben:**
  - Statuskarte (Datum, Morning/Evening Snapshots, Defaults)
  - Primäraktionen ("Morgens check-in", "Abends check-in", "Bearbeiten")
  - Travel-Events (Anreise Start/Ende) - optional einklappbar
  - Footer (Zuletzt gespeichert, Accuracy-Warnung)
  - StateFlow für UI Updates
  - Integration mit UseCases
- **Tests:** UI Tests (Compose UI Test)
- **Dokumentation:** ARCHITECTURE.md Sektion 7

### 6. HistoryScreen Implementieren (Compose)
- **Dateien:**
  - `app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryScreen.kt`
  - `app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryViewModel.kt`
- **Aufgaben:**
  - LazyColumn mit KW-Gruppierung
  - Badges (⚠️ wenn Snapshot fehlt / ungenau / needsReview)
  - Tap → Edit Modal (BottomSheet mit Formular)
  - Export CSV Button (Share Intent)
  - StateFlow für UI Updates
- **Tests:** UI Tests für Liste, Edit Flow, Export
- **Dokumentation:** ARCHITECTURE.md Sektion 7

### 7. SettingsScreen Implementieren (Compose)
- **Dateien:**
  - `app/src/main/java/de/montagezeit/app/ui/screen/settings/SettingsScreen.kt`
  - `app/src/main/java/de/montagezeit/app/ui/screen/settings/SettingsViewModel.kt`
- **Aufgaben:**
  - Arbeitszeit Defaults (Start, Ende, Pause)
  - Reminder Fenster (Start/Ende Morgen/Abend)
  - Leipzig-Radius (Slider/Input)
  - Standort-Modus (Check-in only / Background - Optional)
  - Samsung Sleep-Ausnahme Hinweis-Karte mit Button
  - Export/Backup
  - Integration mit SettingsRepository
- **Tests:** UI Tests für Settings-Persistenz
- **Dokumentation:** ARCHITECTURE.md Sektion 7

### 8. Bottom Navigation Implementieren
- **Datei:** `app/src/main/java/de/montagezeit/app/ui/navigation/MontageZeitNavGraph.kt` (erweitern)
- **Aufgaben:**
  - Navigation Compose integrieren
  - Bottom Navigation Bar (3 Tabs: Heute, Verlauf, Einstellungen)
  - NavController mit State management
  - Navigation Graph erweitern
- **Tests:** UI Tests für Navigation
- **Dokumentation:** ARCHITECTURE.md Sektion 7

### 9. NotificationReceiver Implementieren
- **Datei:** `app/src/main/java/de/montagezeit/app/receiver/NotificationReceiver.kt`
- **Aufgaben:**
  - ACTION_MORNING_CHECK_IN: Morning Check-in auslösen
  - ACTION_EVENING_CHECK_IN: Evening Check-in auslösen
  - ACTION_WITHOUT_LOCATION: Check-in ohne Standort
  - PendingIntent in Reminder Worker erstellen
  - Notification Actions konfigurieren
- **Tests:** Instrumented Tests für Receiver
- **Dokumentation:** ARCHITECTURE.md Sektion 4

### 10. CSV Export Implementieren
- **Datei:** `app/src/main/java/de/montagezeit/app/domain/usecase/ExportCsv.kt`
- **Aufgaben:**
  - CSV-Generierung aus List<WorkEntry>
  - Semikolon-separiert (; statt ,)
  - UTF-8 Encoding
  - Header Zeile (date;workStart;workEnd;...)
  - Share Intent (FileProvider)
  - Datei im Cache speichern
- **Tests:** Unit Tests für Format, Encoding, Edge Cases
- **Dokumentation:** ARCHITECTURE.md Sektion 3 + QA_CHECKLIST.md Test 11

---

## Nach MVP (Optional / P1)

1. **Reverse Geocoding:** Standort-Label automatisch ermitteln (nur wenn online)
2. **"Needs Review" Workflow:** Badge + Filter für Review-Entries
3. **Travel Actions in Notification:** Anreise Start/Ende direkt in Notification
4. **Auswertungen:** Wochen/Monats-Übersicht (Charts optional)
5. **Import/Restore:** CSV Import für Backup-Wiederherstellung

---

## Testing Strategy

### Während der Implementation:
- **TDD wo möglich:** Tests vor UseCase-Implementation schreiben
- **Manuelle Checks:** Nach jedem Feature die Top 10 Tests aus QA_CHECKLIST.md prüfen
- **Regression:** Vor jedem Commit `./gradlew test` laufen lassen

### Vor Release:
- Alle Top 10 Tests aus QA_CHECKLIST.md bestanden
- Code Coverage ≥ 80% (Unit), ≥ 60% (Instrumented)
- Samsung Sleep-Ausnahme getestet
- Offline-Workflow getestet
- Reboot-Szenario getestet

---

## Build & Run

```bash
# Projekt bauen
./gradlew assembleDebug

# Auf Emulator/Device installieren
./gradlew installDebug

# Unit Tests
./gradlew testDebugUnitTest

# Instrumented Tests
./gradlew connectedDebugAndroidTest
```

---

## Resources

- **Tech Spec:** `Konzept.md`
- **Architecture:** `docs/ARCHITECTURE.md`
- **Assumptions:** `docs/ASSUMPTIONS.md`
- **QA Checklist:** `docs/QA_CHECKLIST.md`

---

**Letzte Änderung:** 2026-01-05  
**Owner:** Senior Android Lead Engineer
