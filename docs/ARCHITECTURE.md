# Architektur - MontageZeit MVP

**Letztes Update:** 2026-01-05  
**Status:** Initial Design (Setup-Phase)

## Übersicht

MontageZeit ist eine **single-module Android App** (Kotlin + Jetpack Compose) mit **offline-first** Fokus. Die Architektur ist clean-architectural angelehnt, aber pragmatisch gehalten - kein akademischer Overkill.

**Kernziele:**
- Einfachheit: Single-Module, manuelles DI
- Offline: Room + DataStore lokal
- Robustheit: Fehlertolerante Location & Reminder
- Testbarkeit: UseCases separiert von UI

---

## 1. Layer-Architektur

```
┌─────────────────────────────────────┐
│           UI Layer                   │
│  (Compose Screens + ViewModels)      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Domain Layer                 │
│    (UseCases + Models)               │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Data Layer                 │
│  (Room + Location + DataStore)      │
└─────────────────────────────────────┘
```

**Richtung:** UI → UseCase → Repository/Provider → DB/System

---

## 2. Datenmodell (Room Entity)

### Tabelle: `work_entries`

**Primary Key:** `date: LocalDate` (einzigartig pro Tag)

#### Core Fields (Arbeitszeit-Defaults)
```kotlin
val date: LocalDate                    // Primary Key
val workStart: LocalTime               // Default: 08:00
val workEnd: LocalTime                 // Default: 19:00
val breakMinutes: Int                  // Default: 60
val dayType: DayType                   // WORK | OFF (nicht mehr für MVP)
```

#### Morning Snapshot
```kotlin
val morningCapturedAt: Long?           // Epoch millis
val morningLocationLabel: String?     // "Leipzig" oder Ortsname
val morningLat: Double?                // Latitude
val morningLon: Double?                // Longitude
val morningAccuracyMeters: Float?      // Accuracy in Metern
val outsideLeipzigMorning: Boolean?    // null = unknown
val morningLocationStatus: LocationStatus  // OK | UNAVAILABLE | LOW_ACCURACY
```

#### Evening Snapshot (analog)
```kotlin
val eveningCapturedAt: Long?
val eveningLocationLabel: String?
val eveningLat: Double?
val eveningLon: Double?
val eveningAccuracyMeters: Float?
val outsideLeipzigEvening: Boolean?
val eveningLocationStatus: LocationStatus
```

#### Travel (optional, MVP-minimal)
```kotlin
val travelStartAt: Long?              // Timestamp
val travelArriveAt: Long?             // Timestamp
val travelLabelStart: String?         // Optional
val travelLabelEnd: String?           // Optional
```

#### Meta & Flags
```kotlin
val needsReview: Boolean              // Marker für manuelle Prüfung
val createdAt: Long                   // Erstellt
val updatedAt: Long                   // Letztes Update
val note: String?                     // Kurze Notiz (200-300 Zeichen)
```

### Enums
```kotlin
enum class DayType {
    WORK,
    OFF
    // SICK, HOLIDAY optional später
}

enum class LocationStatus {
    OK,              // Standort verfügbar, akzeptable Accuracy
    UNAVAILABLE,     // GPS aus / Permission denied / Timeout
    LOW_ACCURACY     // Accuracy > 3000m
}
```

---

## 3. UseCases (Domain Layer)

**Prinzip:** Ein UseCase = eine klare Business-Logik-Einheit.

### Core UseCases
1. **RecordMorningCheckIn**
   - Input: `(date: LocalDate, locationResult: LocationResult?, forceWithoutLocation: Boolean)`
   - Output: `WorkEntry`
   - Logic: Location prüfen → Radius-Check → Leipzig/Außerhalb → Upsert DB → `needsReview` setzen wenn nötig

2. **RecordEveningCheckIn**
   - Analog zu Morning, aber optionalen Morning-Check korrigieren

3. **UpdateEntry**
   - Input: `(entry: WorkEntry)`
   - Output: `WorkEntry`
   - Logic: Manuelle Änderungen aus Verlauf/Settings

4. **GetTodayEntry**
   - Input: `(date: LocalDate)`
   - Output: `WorkEntry?`
   - Logic: Hole Entry für heute, erstelle Defaults wenn nicht existent

5. **GetEntriesForWeek**
   - Input: `(weekNumber: Int, year: Int)`
   - Output: `List<WorkEntry>`
   - Logic: KW-Gruppierung für Verlauf-Screen

6. **ExportCsv**
   - Input: `(entries: List<WorkEntry>)`
   - Output: `File`
   - Logic: CSV-Generierung (semikolon-separiert, UTF-8)

7. **SetTravelEvent**
   - Input: `(date: LocalDate, type: TravelType, timestamp: Long, label: String?)`
   - Logic: Travel-Start/Ende speichern

### Helper UseCases
8. **CalculateDistanceToLeipzig**
   - Input: `(lat: Double, lon: Double)`
   - Output: `DistanceMeters: Double`
   - Logic: Haversine-Formel

9. **IsOutsideLeipzig**
   - Input: `(lat: Double, lon: Double, radiusKm: Double)`
   - Output: `Boolean`
   - Logic: Distance > Radius + Grenzzone-Check

---

## 4. Scheduling / Reminder Ansatz

**Philosophie:** Fenster-basiert, nicht exakt. Android darf "ungefähr" sein.

### Mechanismen
- **Primary:** WorkManager (OneTimeWorkRequest mit Constraints)
- **Fallback:** AlarmManager (inexact) für kritische Reminders

### Morning Reminder
- **Fenster:** 06:00 - 13:00 (konfigurierbar)
- **Trigger:** Jeden Morgen innerhalb des Fensters
- **Notification:** 
  - Title: "Guten Morgen! Check-in?"
  - Actions: "Check-in", "Ohne Standort", "Später"
  - Timeout: 15s Location-Abruf

### Evening Reminder
- **Fenster:** 16:00 - 22:30 (konfigurierbar)
- **Trigger:** Jeden Abend innerhalb des Fensters
- **Notification:** Analog Morning

### Fallback Reminder (22:30)
- **Bedingung:** Nur wenn Tag unvollständig (fehlender Morning/Evening)
- **Zweck:** Letzte Chance vor Tagesabschluss
- **Notification:** "Tag unvollständig - jetzt check-in?"

### Reboot Handling
- `BOOT_COMPLETED` BroadcastReceiver
- Neue Reminder planen nach Reboot
- Failsafe: Wenn Reminder fehlen → Verlauf zeigt Badge

---

## 5. LocationProvider Ansatz

### Verantwortlichkeiten
- Standort-Request (einmalig beim Check-in)
- Timeout-Handling (15s)
- Accuracy-Prüfung (≤3000m)
- Fallback (Play Services → Android Location Manager)

### Interface
```kotlin
interface LocationProvider {
    suspend fun getCurrentLocation(timeoutMs: Long): LocationResult
}

sealed class LocationResult {
    data class Success(
        val lat: Double,
        val lon: Double,
        val accuracyMeters: Float
    ) : LocationResult()
    
    object Unavailable : LocationResult()      // GPS aus / Permission denied
    object Timeout : LocationResult()           // 15s ohne Ergebnis
    data class LowAccuracy(val accuracyMeters: Float) : LocationResult()
}
```

### Accuracy Rules
1. **OK:** Accuracy ≤ 3000m → `LocationStatus.OK`
2. **LowAccuracy:** Accuracy > 3000m → `LocationStatus.LOW_ACCURACY` + `needsReview=true`
3. **Unavailable:** GPS aus / Permission denied → `LocationStatus.UNAVAILABLE` + `needsReview=true`

### Radius-Check (Leipzig)
1. **Referenzpunkt:** Leipzig Zentrum (51.340, 12.374)
2. **Radius:** 30 km (konfigurierbar)
3. **Distanz-Berechnung:** Haversine-Formel
4. **Grenzzone:** ±2 km um Radius (28-32 km) → manuelle Bestätigung

### Flow
```
User tappt "Check-in"
    ↓
LocationProvider.getCurrentLocation(15000ms)
    ↓
├─ Success → Distance zu Leipzig berechnen
│            ├─ ≤28km → insideLeipzig=true, Label="Leipzig"
│            ├─ >32km → outsideLeipzig=true, UI fragt nach Ortsname
│            └─ 28-32km → Grenzzone → User bestätigen
│
├─ LowAccuracy → locationStatus=LOW_ACCURACY, needsReview=true
│
├─ Timeout → User: "Ohne Standort speichern?" → needsReview=true
│
└─ Unavailable → User: "Standort aktivieren?" → needsReview=true
```

---

## 6. Settings (DataStore Preferences)

### Struktur
```kotlin
data class AppSettings(
    // Arbeitszeit Defaults
    val defaultWorkStart: LocalTime = LocalTime.of(8, 0),
    val defaultWorkEnd: LocalTime = LocalTime.of(19, 0),
    val defaultBreakMinutes: Int = 60,
    
    // Leipzig-Radius
    val leipzigRadiusKm: Double = 30.0,
    
    // Reminder-Fenster
    val reminderEnabled: Boolean = true,
    val morningWindowStart: LocalTime = LocalTime.of(6, 0),
    val morningWindowEnd: LocalTime = LocalTime.of(13, 0),
    val eveningWindowStart: LocalTime = LocalTime.of(16, 0),
    val eveningWindowEnd: LocalTime = LocalTime.of(22, 30),
    
    // Standort-Modus
    val locationMode: LocationMode = LocationMode.CHECK_IN_ONLY,
    
    // Privacy
    val saveCoordinates: Boolean = true,  // false = nur Label
)
```

### Storage
- **Technology:** DataStore Preferences (protobuf-based)
- **Persistence:** Robust, thread-safe
- **Migration:** Nicht nötig (nur Settings)

---

## 7. UI / Navigation

### Navigation Structure
**Bottom Nav (3 Tabs):**
1. **Heute** (`TodayScreen`)
2. **Verlauf** (`HistoryScreen`)
3. **Einstellungen** (`SettingsScreen`)

### Screen-Übersicht

#### TodayScreen
- **Oben:** Statuskarte (Datum, Morning/Evening Snapshots, Defaults)
- **Mitte:** Primäraktionen ("Morgens check-in", "Abends check-in", "Bearbeiten")
- **Optional (einklappbar):** Travel-Events (Anreise Start/Ende)
- **Unten:** Footer (Zuletzt gespeichert, Accuracy-Warnung)

#### HistoryScreen
- **Liste:** KW-gruppiert, pro Tag Eintrag
- **Badges:** ⚠️ wenn Snapshot fehlt / ungenau / needsReview
- **Tap → Edit Modal:** BottomSheet mit Formular
- **Export:** Button oben ("Export CSV")

#### SettingsScreen
- **Abschnitte:**
  - Arbeitszeit Defaults
  - Reminder Fenster
  - Leipzig-Radius
  - Standort-Modus
  - Samsung Sleep-Ausnahme Hinweis
  - Export/Backup

### State Management
- **ViewModels:** `StateFlow` für UI State
- **Single Source of Truth:** Room DB + DataStore
- **UI Refresh:** `collectAsState()` in Composables

---

## 8. Dependency Injection (Manuell)

**Prinzip:** Kein Hilt/Koin für MVP - einfache Singleton-Factory im DI-Package.

### Struktur
```kotlin
// di/AppContainer.kt
object AppContainer {
    val database: AppDatabase by lazy { AppDatabase.getInstance(context) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }
    val locationProvider: LocationProvider by lazy { LocationProviderImpl(context) }
    
    // UseCases
    val recordMorningCheckIn: RecordMorningCheckIn by lazy {
        RecordMorningCheckIn(database, locationProvider, settingsRepository)
    }
    
    // ... weitere UseCases
}
```

### Benefits
- Minimal Setup
- Easy to Debug
- Keine Annotation-Overhead
- Für Single-User App ausreichend

---

## 9. Error Handling

### UI-Errors
- **Standort nicht verfügbar:** "GPS aktivieren?" + "Ohne Standort speichern"
- **DB-Fehler:** "Fehler beim Speichern" + "Log exportieren"
- **Export-Fehler:** "Export fehlgeschlagen" + "Neuer Versuch"

### Silent-Errors (Logging)
- Location Timeout → Log + needsReview Flag
- Accuracy Warning → Log + needsReview Flag
- Reminder failed → Log + Verlauf zeigt Badge

### Crash-Handling
- Lokales Crash-Log (Ringbuffer, 1-2 MB)
- Export-Log über Settings
- Keine Crashlytics/Sentry (Privacy)

---

## 10. Performance & Optimierungen

### Room
- **Indizes:** `date` (Primary Key), `updatedAt`
- **Queries:** Optimierte KW-Filter, keine N+1 Queries
- **Transaction:** Bulk-Updates für Travel-Events

### Location
- **Timeout:** 15s (User wartet nicht ewig)
- **Accuracy Tradeoff:** Coarse reicht für Radius-Check
- **Caching:** Kein Caching (einmalig pro Check-in)

### UI
- **LazyColumn:** Für Verlauf-Liste (keine Performance-Probleme bei 100+ Einträgen)
- **StateFlow:** Minimaler Re-Render
- **Compose Stable Classes:** `@Immutable` für Models

---

## 11. Testing Strategy

### Unit Tests
- UseCases (Business-Logik)
- Radius-Check (Haversine)
- Location-Accuracy Rules
- Settings-Serialization

### Instrumented Tests
- Room DAO (CRUD)
- LocationProvider (Integration)
- Reminder Scheduling

### UI Tests (optional)
- TodayScreen Check-in Flow
- HistoryScreen Edit Flow

### Manual Tests
- Samsung Sleep-Ausnahme
- Reboot → Reminder neu planen
- Offline → Export CSV
- Permission Denied → Graceful Degradation

---

## 12. Migration & Future-Proofing

### Single-Module → Multi-Module (optional später)
- **Feature Modules:** `:feature-today`, `:feature-history`, `:feature-settings`
- **Core Module:** `:core-data`, `:core-domain`
- **Begründung:** Nur wenn Team > 2 Entwickler

### Hilt Integration (optional)
- Ersetzt manuelles DI
- Mehr Boilerplate, aber Type-safer
- Nur wenn Projekt wächst

### Background Location (experimentell)
- Nicht im MVP
- Erfordert:
  - Runtime Permissions
  - Foreground Service
  - Battery Optimization Ausnahme
  - User-Klarheit

---

## 13. Security & Privacy

### Permissions
- **Minimal:** POST_NOTIFICATIONS + ACCESS_COARSE_LOCATION
- **Runtime:** Request beim ersten Check-in
- **Durable:** User kann jederzeit widerrufen

### Data Storage
- **Lokal:** Room DB (SQLite) + DataStore
- **Encryption:** Nicht nötig (Single-Device, kein Root-Access-Szenario)
- **Backup:** `allowBackup=false` (Privacy-first)

### Location Data
- **Koordinaten:** Optional gespeichert (Setting)
- **Label:** Immer gespeichert ("Leipzig", "Dresden", etc.)
- **Kein Upload:** 100% lokal

---

## 14. Build & Deployment

### Build Configuration
- **Debug:** Keine Obfuscation, Logging aktiv
- **Release:** R8/ProGuard, Logging minimal
- **Signing:** Keine Signierung für lokale APK (optional für Play Store)

### Versioning
- **Major.Minor.Patch:** 1.0.0 → MVP
- **Semantic Versioning:** Kleine Fixes = Patch, Features = Minor, Breaking = Major

### Distribution
- **Initial:** APK lokal installieren (`adb install`)
- **Später:** Play Store (optional, nicht MVP)

---

## 15. Dokumentation

### Code Documentation
- **KDoc:** Für öffentliche APIs (UseCases, Repository, Provider)
- **Inline:** Nur für komplexe Logic (Haversine, Grenzzone)

### External Docs
- **ARCHITECTURE.md:** Dieses Dokument
- **ASSUMPTIONS.md:** Alle Annahmen & Entscheidungen
- **QA_CHECKLIST.md:** Test-Szenarien
- **NEXT_TASKS.md:** Aufgaben-Liste für Entwicklung

---

## 16. Scope Check (MVP)

**Included:**
- ✅ Room DB + DAO + Entities
- ✅ LocationProvider + Radius-Check
- ✅ Morning/Evening Check-in UseCases
- ✅ Reminder (WorkManager)
- ✅ Today/History/Settings Screens
- ✅ DataStore Settings
- ✅ CSV Export
- ✅ Offline-first

**Excluded (Nicht MVP):**
- ❌ Cloud-Sync
- ❌ Kartenansicht
- ❌ Spesen-Verwaltung
- ❌ Background-Location-Tracking
- ❌ Import/Restore
- ❌ Reverse Geocoding
- ❌ Multi-User
- ❌ Hilt DI

---

## Zusammenfassung

MontageZeit ist eine **fokussierte, offline-first App** mit:
- **Einfacher Architektur** (Single-Module, manuelles DI)
- **Robuster Location-Logik** (Timeout, Accuracy, Radius, Grenzzone)
- **Fenster-basierten Remindern** (nicht exakt, praktisch)
- **Klarer Datenhaltung** (Room + DataStore, lokal)
- **Minimaler Permissions-Set** (Privacy-first)

Der Fokus liegt auf **Zuverlässigkeit** und **Einfachheit**, nicht auf akademischer Architektur oder Over-Engineering.
