# MontageZeit - Android MVP

Offline-first Stunden- & Ortslog für Montage mit Fokus auf Standort-Snapshots (Morgens + Abends).

## Projektstatus

✅ **MVP abgeschlossen** - Produktionsreif  
📦 **CSV Export** implementiert  
📝 **Ringbuffer Logging** für Debug  
🛡️ **Edge Cases** abgedeckt (GPS, Permissions, Time Change, Reboot)  
🧪 **Tests** vorhanden

## Features

- ✅ **Room Database:** Lokale Speicherung von Arbeitstagen
- ✅ **DataStore:** Persistente Settings
- ✅ **Jetpack Compose:** Modernes UI mit Material3
- ✅ **Navigation:** Bottom Nav (Heute, Verlauf, Einstellungen)
- ✅ **LocationProvider:** Standort-Abfragen mit Timeout & Accuracy-Prüfung
- ✅ **Reminder System:** Fenster-basierte Notifications (Morning/Evening)
- ✅ **CSV Export:** Semikolon-separiert, UTF-8, Offline-fähig
- ✅ **Ringbuffer Logger:** 2MB lokales Debug-Log
- ✅ **Edge Case Handling:** Permission Denied, GPS Off, Time Change, Reboot

## Voraussetzungen

- **Android Studio** (oder Android SDK + Gradle)
- **Java JDK 17** oder höher
- **Android SDK API 34**
- **Kotlin 1.9.20**

## Projektstruktur

```
app/src/main/java/de/montagezeit/app/
├── data/
│   ├── local/
│   │   ├── entity/          # WorkEntry (Room Entity)
│   │   ├── dao/             # WorkEntryDao
│   │   ├── database/         # AppDatabase
│   │   └── converters/      # LocalDate/LocalTime Converters
│   ├── location/             # LocationProvider (GPS Fallback)
│   └── preferences/          # ReminderSettings (DataStore)
├── domain/
│   ├── model/               # LocationResult
│   ├── location/             # LocationCalculator (Radius-Check)
│   └── usecase/             # UseCases (Export, Check-in, etc.)
├── export/
│   └── CsvExporter          # CSV Export (Semikolon, UTF-8)
├── logging/
│   └── RingBufferLogger     # Lokales Debug-Log (2MB)
├── ui/
│   ├── theme/               # Material3 Theme
│   ├── navigation/           # Navigation Graph
│   └── screen/             # Today, History, Settings, Edit
├── receiver/               # BootReceiver, TimeChangeReceiver
├── handler/               # CheckInActionService
├── work/                   # WindowCheckWorker, ReminderScheduler
├── MainActivity.kt          # Entry Point
└── MontageZeitApp.kt       # Application Class

docs/
├── ARCHITECTURE.md          # Detaillierte Architektur-Dokumentation
├── ASSUMPTIONS.md          # Alle Annahmen & Entscheidungen
├── QA_CHECKLIST.md          # Top 10 Tests
├── KNOWN_LIMITATIONS.md    # Bekannte Limitationen
└── IMPLEMENTATION_NOTES.md # Implementierungsdetails
```

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

# Clean
./gradlew clean

# Release Build
./gradlew assembleRelease
```

**Hinweis:** Wenn `./gradlew` nicht existiert, muss zuerst der Gradle Wrapper erstellt werden:
```bash
gradle wrapper --gradle-version 8.2
```

## Permissions

Die App benötigt folgende Berechtigungen:

| Permission | Zweck | Begründung |
|------------|--------|-------------|
| `POST_NOTIFICATIONS` | Reminder Notifications | Benachrichtigungen für Check-in Erinnerungen |
| `ACCESS_COARSE_LOCATION` | Standort-Abfrage | Radius-Check um Leipzig (ca. 30km) |
| `ACCESS_FINE_LOCATION` | Optionale Genauigkeit | Bessere Genauigkeit auf Android ≤32 |
| `RECEIVE_BOOT_COMPLETED` | Boot Receiver | Reminder nach Reboot neu planen |
| `SCHEDULE_EXACT_ALARM` | Exakte Reminders | Präzise Notification-Zeiten |
| `FOREGROUND_SERVICE` | Check-In Service | Foreground Service für Notification Actions |

**Wichtig:** Die App funktioniert auch ohne Standort-Berechtigung (Graceful Degradation). Einträge werden dann ohne Location-Informationen gespeichert.

## Samsung Setup Hinweis

Auf Samsung-Geräten kann der "Sleep-Modus" die Reminder-Notifications verhindern.

**Lösung:**
1. Öffne "Einstellungen" → "Akku & Wartung" → "Akku"
2. Suche "MontageZeit" in der Liste der Apps
3. Deaktiviere "In den Ruhemodus versetzen" (Put apps to sleep)
4. Optional: Deaktiviere "Einschränken" (Limit background usage)

Dadurch wird sichergestellt, dass Reminder auch im Hintergrund korrekt ausgelöst werden.

## Export & Backup

### CSV Export

Der Export ist über den "Verlauf"-Screen verfügbar:
1. Öffne den "Verlauf"-Tab
2. Klicke auf das Share-Icon (📤) oben rechts
3. Wähle eine App zum Teilen (Email, Google Drive, etc.)

**Export-Format:**
- Semikolon-separiert (;)
- UTF-8 Kodierung
- Datum: `yyyy-MM-dd`
- Timestamp: `yyyy-MM-dd HH:mm:ss`
- Felder: `date;dayType;workStart;workEnd;breakMinutes;morningCapturedAt;morningLocationLabel;morningOutside;eveningCapturedAt;eveningLocationLabel;eveningOutside;travelStartAt;travelArriveAt;travelLabelStart;travelLabelEnd;note;needsReview;createdAt;updatedAt`

**Offline-fähig:** Der Export funktioniert ohne Internet-Verbindung.

### Debug Logs

Die App speichert automatisch Debug-Informationen in einer lokalen Log-Datei:
- Speicherort: `/data/data/de.montagezeit.app/files/logs/debug.log`
- Max. Größe: 2MB (rotierend)
- Inhalt: ERROR, WARN, INFO, DEBUG Nachrichten mit Timestamp

**Hinweis:** Logs sind lokal und werden nicht in die Cloud hochgeladen.

## Tech Stack

- **Language:** Kotlin 1.9.20
- **UI:** Jetpack Compose + Material3
- **Database:** Room 2.6.1
- **Preferences:** DataStore 1.0.0
- **Async:** Coroutines + Flow
- **Location:** Play Services Location 21.0.1
- **Scheduling:** WorkManager 2.9.0
- **Navigation:** Navigation Compose 2.7.5
- **Dependency Injection:** Hilt 2.48
- **Build:** Gradle 8.2 + Kotlin DSL

## Dokumentation

### Architektur
`docs/ARCHITECTURE.md` - Vollständige Architektur-Dokumentation mit:
- Layer-Architektur (UI → Domain → Data)
- Datenmodell (work_entries Entity)
- UseCases (Export, Check-in, etc.)
- Scheduling/Reminder Ansatz
- LocationProvider Ansatz
- Settings (DataStore)
- UI/Navigation
- Error Handling

### Annahmen
`docs/ASSUMPTIONS.md` - Alle technischen Annahmen:
- Leipzig Zentrum (51.340, 12.374)
- Radius Default: 30 km
- Accuracy Threshold: 3000m
- Reminder-Fenster (Morning 06:00-13:00, Evening 16:00-22:30)
- Arbeitszeit Defaults (08:00-19:00, 60 min Pause)
- Privacy-Einstellungen (allowBackup=false)

### QA Checkliste
`docs/QA_CHECKLIST.md` - Top 10 Tests:
1. Radius-Check: Leipzig vs >30 km
2. Grenzzone: 28-32 km → Confirm Required
3. Low Accuracy → Outside = Unknown, needsReview=true
4. Morning Check-in upsert setzt capturedAt + Defaults korrekt
5. Evening Check-in überschreibt nicht kaputt (idempotent)
6. Notification Action speichert Morning/Evening
7. Permission Denied → "Ohne Standort speichern" funktioniert
8. Reboot → Reminder neu geplant
9. Offline → App startet, Verlauf ok, Export ok
10. DayType OFF verhindert Warnspam

### Bekannte Limitationen
`docs/KNOWN_LIMITATIONS.md` - Ehrliche Übersicht über Limitationen der App.

## Troubleshooting

### GPS ist ausgeschaltet
Wenn der Standort deaktiviert ist, zeigt die App einen Hinweis an. Check-ins sind weiterhin möglich, aber ohne Location-Informationen.

**Lösung:** GPS in den Android-Einstellungen aktivieren.

### Permission Denied
Wenn die Standort-Berechtigung verweigert wurde, funktioniert die App weiterhin, aber ohne Location-Informationen.

**Lösung:** Berechtigung in den App-Einstellungen erteilen.

### Reboot nach Reminder-Setup
Nach einem Reboot werden alle Reminder automatisch neu geplant. Dies dauert einige Sekunden nach dem Start.

### Zeitzone-Wechsel
Wenn die Zeitzone geändert wird, werden alle Reminder neu geplant.

### Export fehlgeschlagen
Wenn der CSV-Export fehlschlägt:
- Prüfe ob der Cache-Speicher verfügbar ist
- Prüfe ob die Datei-Berechtigung vorhanden ist
- Versuche es erneut

## Development Workflow

1. **Feature aus docs/ARCHITECTURE.md wählen**
2. **Tests schreiben** (TDD wo möglich)
3. **Feature implementieren**
4. **Tests laufen lassen:** `./gradlew test`
5. **Manuelle Checks** aus QA_CHECKLIST.md
6. **Commit mit sprechender Nachricht**

## Release Kriterien

- ✅ Alle 10 Top-Tests bestanden (Unit + Instrumented + Manual)
- ✅ Code Coverage ≥ 80% (Unit), ≥ 60% (Instrumented)
- ✅ Keine P0/P1 Bugs offen
- ✅ Samsung Sleep-Ausnahme getestet
- ✅ Offline-Workflow getestet
- ✅ Export CSV validiert
- ✅ Reboot-Szenario getestet
- ✅ CSV Export funktioniert
- ✅ Ringbuffer Logging getestet
- ✅ Edge Cases abgedeckt

## License

Siehe LICENSE Datei im Root-Verzeichnis.

## Kontakt / Issues

Für Issues oder Fragen bitte:
1. ISSUE anlegen mit:
   - Beschreibung des Problems
   - Schritte zum Reproduzieren
   - Erwartetes vs. Tatsächliches Ergebnis
   - Logcat / Screenshots
   - Device + Android Version
2. Fix implementieren
3. Tests wiederholen
4. Regression Check

---

**Version:** 1.0.0-MVP-Production-Ready  
**Letzte Änderung:** 2026-01-05  
**Owner:** Senior Android Lead Engineer
