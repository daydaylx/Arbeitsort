# MontageZeit (`de.montagezeit.app`)

Offline-first Android-App zur lokalen Erfassung von Arbeitstagen. Der aktuelle Stand ist auf manuelle Tageserfassung, Reminder, Verlauf/Bearbeitung und PDF-Export ausgelegt. Es gibt keinen Cloud-Sync und keine Standort-Berechtigung.

## Source of Truth

Verbindliche Projektdokumentation:
- `README.md`
- `docs/ARCHITECTURE.md`

Andere Dateien unter `docs/` sind nur ergänzende oder historische Referenzen. Wenn sich Doku und Code widersprechen, gilt der Code.

## Projektstatus

- Single-Module-Android-Projekt (`:app`)
- App-Version: `1.1.1` (`versionCode 5`)
- Room-Datenbank: Schema `16`, Migrationen `1 -> 16`

## Tech Stack

- Kotlin 2.1.10, Coroutines, Flow
- Jetpack Compose (Material3), Navigation Compose
- Room 2.6.1
- WorkManager 2.9.0
- Hilt 2.56.2 (`hilt-work`, `hilt-navigation-compose`)
- DataStore Preferences

Build-Umgebung:
- JDK 17
- `minSdk 24`
- `compileSdk 35`
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
- Batch-Edit für Datumsbereiche auf bestehenden Einträgen
- Edit-Sheet für DayType, Zeiten, Travel-Daten, Tagesort und Notiz
- Gültig gespeicherte `WORK`-Tage werden beim Bearbeiten automatisch bestätigt, wenn positive Arbeits- oder Reisezeit vorliegt
- `WORK`-Tage ohne positive Netto-Arbeitszeit und ohne Reise gelten nicht als abgeschlossen und zählen weder in Statistik noch Export

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
- CSV-Export als sichtbarer Schnell-Export in den Settings
- PDF-Stammdaten in den Settings:
  - Mitarbeitername
  - Firma
  - Projekt
  - Personalnummer
- CSV-Zellwerte werden mit Quoting fuer `;`, Anfuehrungszeichen und Zeilenumbrueche exportiert
- Fuehrende Formel-Praefixe (`=`, `+`, `-`, `@`) werden fuer CSV zusaetzlich neutralisiert
- PDF und CSV teilen sich dieselben Export-Zeitraeume:
  - aktueller Monat
  - letzte 30 Tage
  - benutzerdefinierter Zeitraum

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
  - Bestehende Arbeitszeiten bleiben erhalten; sonst werden Settings-Defaults gesetzt
  - Verpflegungspauschale wird aus den Dialogoptionen berechnet, aber nur für Tage mit Arbeit oder Reise geführt

### DayType

- `WORK`: normaler Arbeitstag
- `OFF`: freier Tag / Urlaub
- `COMP_TIME`: Überstundenabbau; wird im Code beim Setzen direkt bestätigt

`Heute frei` im Today-Screen verwendet `ConfirmOffDay` und bestätigt den Tag sofort. `COMP_TIME` kann über Bearbeitungsfunktionen gesetzt werden und unterdrückt Daily-Reminder.

### Reminder-Logik

- `MORNING` und `EVENING` laufen fensterbasiert in konfigurierbaren Intervallen. Wegen WorkManager gilt ein Mindestintervall von 15 Minuten.
- `FALLBACK` und `DAILY` laufen jeweils einmal pro Tag.
- `DAILY` erinnert an offene `WORK`-Tage ohne terminalen Abschlussstatus und bietet in der Notification `Arbeit`, `Frei` und `Später`.
- Für Morning/Evening/Fallback gibt es zusätzlich `10 Min`, `+1 h` und `+2 h` als Später-Aktionen.
- Reminder werden für automatisch arbeitsfreie Tage (Wochenenden/Feiertage) unterdrückt, sofern kein manueller Eintrag den Tag überschreibt.
- Legacy-Worker ohne `reminder_type` führen bewusst keinen Reminder mehr aus.

### Standortbezug

- Die aktuelle Check-in-Logik arbeitet ohne Standort-Berechtigung und ohne GPS-Pflicht.
- Tagesort wird nur noch manuell als Text (`dayLocationLabel`) gepflegt.
- Morning/Evening speichern nur noch ihre Erfassungszeitpunkte (`morningCapturedAt`, `eveningCapturedAt`).

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

## Entwicklungs-Workflow

- `CONTRIBUTING.md` beschreibt den lokalen Setup- und Review-Workflow.
- `README_ANDROID_DEV.md` beschreibt Geräte-Deployment, `adb`-Flows und VS-Code-Tasks.
- `AGENTS.md` bündelt die repo-spezifischen Arbeitsregeln für Coding-Agents.
- Repo-lokale Codex-Plugin- und Skill-Metadaten liegen unter `plugins/montagezeit-android/` sowie `.agents/plugins/marketplace.json`; fuer PR- und CI-Arbeit ist GitHub der bevorzugte externe Connector, die Android-Entwicklung bleibt lokal shell-zentriert.

## APK-Update ohne Datenverlust

Ein normales Update über die bestehende Installation behält die lokalen Room-/DataStore-Daten.
Voraussetzungen dafür:

- gleiche `applicationId` (`de.montagezeit.app`)
- höherer `versionCode` als in der installierten APK
- dieselbe Signatur wie bei der bereits installierten App

Wichtig:

- `android.intent.action.MY_PACKAGE_REPLACED` wird empfangen; Reminder werden nach dem Update neu geplant.
- `android:allowBackup="false"` bleibt aktiv. Ein Uninstall oder ein Install über eine andere Signatur löscht die lokalen Daten.

Lokale Release-Signatur vorbereiten:

```properties
# keystore.properties (Repo-Root, nicht committen)
storeFile=release-keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Release bauen und als Update installieren:

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Ohne `keystore.properties` wird weiterhin nur eine unsignierte Release-APK gebaut; die ist nicht update-fähig.

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
- Historische Doku unter `docs/AUDITS/` und `docs/ARCHIVE/` kann bewusst ältere Aussagen enthalten; für den Ist-Zustand sind README, `docs/ARCHITECTURE.md` und der Code maßgeblich.
