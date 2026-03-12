# MontageZeit (`de.montagezeit.app`)

Offline-first Android-App zur lokalen Erfassung von Arbeitstagen. Der aktuelle Stand ist auf manuelle Tageserfassung, Reminder, Verlauf/Bearbeitung und PDF-Export ausgelegt. Es gibt keinen Cloud-Sync und keine Standort-Berechtigung.

## Source of Truth

Verbindliche Projektdokumentation:
- `README.md`
- `docs/ARCHITECTURE.md`

Andere Dateien unter `docs/` sind nur ergänzende oder historische Referenzen. Wenn sich Doku und Code widersprechen, gilt der Code.

## Projektstatus

- Single-Module-Android-Projekt (`:app`)
- App-Version: `1.0.1` (`versionCode 2`)
- Room-Datenbank: Schema `11`, Migrationen `1 -> 11`

## Tech Stack

- Kotlin 1.9.20, Coroutines, Flow
- Jetpack Compose (Material3), Navigation Compose
- Room 2.6.1
- WorkManager 2.9.0
- Hilt 2.48 (`hilt-work`, `hilt-navigation-compose`)
- DataStore Preferences

Build-Umgebung:
- JDK 17
- `minSdk 24`
- `compileSdk 34`
- `targetSdk 34`

## Kernfunktionen

### Today

- Manueller Daily-Check-in mit Pflichtfeld für Tagesort
- Optionale Angaben für Verpflegungspauschale:
  - An-/Abreisetag
  - Frühstück enthalten
- Aktion `Heute frei`
- Bearbeiten des Tagesorts für bestehende Einträge
- Wochenleiste, Wochen-/Monatswerte und Jahresüberstundenanzeige
- Löschen eines Tages mit Undo

### History / Bearbeitung

- Verlauf für die letzten 365 Tage
- Wochen-, Monats- und Kalenderansichten
- Filter für `needsReview`
- Batch-Edit für Datumsbereiche
- Edit-Sheet für DayType, Zeiten, Travel-Daten, Notiz und weitere Tagesdetails

### Reminder

- Vier Reminder-Typen:
  - `MORNING`
  - `EVENING`
  - `FALLBACK`
  - `DAILY`
- Scheduling über `PeriodicWorkRequest`
- Re-Scheduling beim App-Start, nach Reboot/App-Update und nach Zeit-/Zeitzonenänderungen
- Aktionen in Notifications für Check-in, Tag als frei markieren, später erinnern und Daily Confirmation

### Export

- PDF-Export mit Preview-Flow in den Settings
- PDF-Stammdaten in den Settings:
  - Mitarbeitername
  - Firma
  - Projekt
  - Personalnummer
- Ein `CsvExporter` ist im Code vorhanden, ist aktuell aber nicht an den sichtbaren Settings-/Export-Flow angebunden

## Aktuelles Verhalten von Today, DayType und Remindern

### Daily-Check-in

- Primärer UI-Pfad im Today-Screen ist `Einchecken (Arbeit)`.
- Der Dialog für den Tagesort wird vorbefüllt in dieser Reihenfolge:
  1. `dayLocationLabel` des aktuellen Eintrags
  2. letzter gespeicherter `dayLocationLabel` eines `WORK`-Eintrags
  3. leer
- `RecordDailyManualCheckIn` setzt den Tag direkt auf abgeschlossen:
  - `dayType = WORK`
  - `confirmedWorkDay = true`
  - Arbeitszeit-Defaults aus den Settings
  - Morning-/Evening-Snapshots werden gesetzt, falls sie noch fehlen
  - Verpflegungspauschale wird aus den Dialogoptionen berechnet

### DayType

- `WORK`: normaler Arbeitstag
- `OFF`: freier Tag / Urlaub
- `COMP_TIME`: Überstundenabbau; wird im Code beim Setzen direkt bestätigt

`Heute frei` im Today-Screen verwendet `ConfirmOffDay` und bestätigt den Tag sofort. `COMP_TIME` kann über Bearbeitungsfunktionen gesetzt werden und unterdrückt Daily-Reminder.

### Reminder-Logik

- `MORNING` und `EVENING` laufen fensterbasiert in konfigurierbaren Intervallen. Wegen WorkManager gilt ein Mindestintervall von 15 Minuten.
- `FALLBACK` und `DAILY` laufen jeweils einmal pro Tag.
- `DAILY` erinnert an noch nicht bestätigte Tage und bietet in der Notification `Arbeit`, `Frei` und `Später`.
- Für Morning/Evening/Fallback gibt es zusätzlich `10 Min`, `+1 h` und `+2 h` als Später-Aktionen.
- Reminder werden für automatisch arbeitsfreie Tage (Wochenenden/Feiertage) unterdrückt, sofern kein manueller Eintrag den Tag überschreibt.
- Legacy-Worker ohne `reminder_type` führen bewusst keinen Reminder mehr aus.

### Standortbezug

- Die aktuelle Check-in-Logik arbeitet ohne Standort-Berechtigung und ohne GPS-Pflicht.
- Tagesort wird manuell als Text gepflegt.
- Das Datenmodell enthält weiterhin Standort- und Statusfelder für Tages- und Snapshot-Daten, die aktuelle Reminder-/Today-Logik nutzt aber keinen aktiven GPS-Flow.

## Build und Tests

Aus dem Repo-Root:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lint
./gradlew :app:testDebugUnitTest
./gradlew test
```

Optional auf Gerät/Emulator:

```bash
./gradlew connectedDebugAndroidTest
```

## Wichtige Pfade

- Einstieg:
  - `app/src/main/java/de/montagezeit/app/MontageZeitApp.kt`
  - `app/src/main/java/de/montagezeit/app/MainActivity.kt`
- Navigation:
  - `app/src/main/java/de/montagezeit/app/ui/navigation/MontageZeitNavGraph.kt`
- Today:
  - `app/src/main/java/de/montagezeit/app/ui/screen/today/`
- History:
  - `app/src/main/java/de/montagezeit/app/ui/screen/history/`
- Settings / Export-Preview:
  - `app/src/main/java/de/montagezeit/app/ui/screen/settings/`
  - `app/src/main/java/de/montagezeit/app/ui/screen/export/`
- Domain-UseCases:
  - `app/src/main/java/de/montagezeit/app/domain/usecase/`
- Datenhaltung:
  - `app/src/main/java/de/montagezeit/app/data/local/`
  - `app/src/main/java/de/montagezeit/app/data/preferences/`
- Reminder / Notifications:
  - `app/src/main/java/de/montagezeit/app/work/`
  - `app/src/main/java/de/montagezeit/app/notification/`
  - `app/src/main/java/de/montagezeit/app/handler/`
  - `app/src/main/java/de/montagezeit/app/receiver/`
- Exporter:
  - `app/src/main/java/de/montagezeit/app/export/`

## Berechtigungen, Datenschutz, lokale Daten

Deklariert im Manifest:
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_SPECIAL_USE`

Zusätzlich relevant:
- `android:allowBackup="false"`
- Keine Standort-Berechtigungen
- Daten bleiben lokal in Room/DataStore, bis der Benutzer sie explizit exportiert oder teilt
- Export-Freigabe läuft über `FileProvider`
- Lokaler Ringbuffer-Logger schreibt nach `files/logs/debug.log` und rotiert bei ca. 2 MB

## Relevante Einschränkungen

- Reminder sind WorkManager-basiert und damit bewusst fensterbasiert, nicht exakt alarmgenau.
- Hersteller-Energiesparmechanismen und Doze können die tatsächliche Ausführung verzögern.
- Die ergänzende Doku unter `docs/` enthält stellenweise ältere Aussagen zu Reminder-Aktionen, GPS-Logik und UI-Umfang; für den Ist-Zustand sind README und Code maßgeblich.
