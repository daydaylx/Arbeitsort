# Android Development Guide - MontageZeit

Schnellstart-Anleitung für Android-Entwicklung und Testing auf echten Geräten.

## Voraussetzungen

- Java 17+ installiert
- Android SDK mit Platform Tools (adb)
- Gradle Wrapper (./gradlew)
- USB-Debugging auf dem Gerät aktiviert

## 1-Befehl Deployment

```bash
./scripts/android_debug_run.sh [DEVICE_SERIAL]
```

**Führt automatisch aus:**
- Build der Debug-APK
- Installation auf dem Gerät
- App-Start
- Live-Logcat-Stream

**Beispiel:**
```bash
./scripts/android_debug_run.sh RFCY210JHMJ
```

## Manuelle Schritte

### 1. Build & Install

```bash
./gradlew :app:installDebug
```

Alternative (nur Build ohne Install):
```bash
./gradlew :app:assembleDebug
# APK liegt dann unter: app/build/outputs/apk/debug/app-debug.apk
```

### 2. App Starten

```bash
adb -s RFCY210JHMJ shell am start -n "de.montagezeit.app/de.montagezeit.app.MainActivity"
```

### 3. Logs Überwachen

**Mit PID-Filter (empfohlen):**
```bash
PID=$(adb -s RFCY210JHMJ shell pidof de.montagezeit.app)
adb -s RFCY210JHMJ logcat --pid=$PID -v time
```

**Oder per Package-Name:**
```bash
adb -s RFCY210JHMJ logcat -v time | grep montagezeit
```

### 4. App-Daten Löschen (bei DB-Schema-Änderungen)

```bash
adb -s RFCY210JHMJ shell pm clear de.montagezeit.app
```

## VS Code Integration

Tasks sind in `.vscode/tasks.json` definiert.

**Verfügbare Tasks:**
- `Android: Build Debug APK` - Nur kompilieren
- `Android: Install Debug APK` - Build + Installation (Standard)
- `Android: Launch App` - App starten
- `Android: View Logcat` - Logs anzeigen
- `Android: Clear App Data` - Daten löschen
- `Android: Full Debug Run` - Kompletter Workflow
- `Android: Device List` - Verbundene Geräte auflisten

**Ausführen:** `Ctrl+Shift+B` → Task auswählen

## Wichtige Dateipfade

| Was | Pfad |
|-----|------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Build Logs | `debug_artifacts/build_*.txt` |
| Logcat Dumps | `debug_artifacts/logcat_*.txt` |
| Gradle Wrapper | `./gradlew` |

## Troubleshooting

### Gerät nicht gefunden

```bash
adb start-server
adb devices -l
```

Falls "unauthorized":
1. Telefon entsperren
2. USB-Debugging-Prompt akzeptieren
3. `adb devices` erneut ausführen

### App crasht sofort

```bash
# Zeige letzte Crash-Logs
adb -s RFCY210JHMJ logcat -d -v time "*:E" | grep montagezeit | tail -n 50

# Oder speichern
adb -s RFCY210JHMJ logcat -d > debug_artifacts/full_logcat.txt
```

### Room Database Schema-Fehler

```
Room cannot verify the data integrity...
```

**Lösung:** App-Daten löschen
```bash
adb -s RFCY210JHMJ shell pm clear de.montagezeit.app
```

**Produktions-Fix:** Database-Version in `AppDatabase.kt` erhöhen.

### Dependency Injection Fehler

```
lateinit property has not been initialized
```

**Ursache:** Hilt-Injektion erfolgt nach `onCreate()`, aber Property wird vorher verwendet.

**Lösung:** Lazy-Initialisierung verwenden:
```kotlin
private val myDependency by lazy { /* ... */ }
```

## Projekt-Struktur

```
.
├── app/
│   ├── src/main/
│   │   ├── java/de/montagezeit/app/
│   │   │   ├── MontageZeitApp.kt      # Application Entry Point
│   │   │   ├── MainActivity.kt        # Launch Activity
│   │   │   ├── data/                  # Room DB, Repositories
│   │   │   ├── domain/                # Use Cases, Business Logic
│   │   │   ├── ui/                    # Compose Screens
│   │   │   └── work/                  # WorkManager, Reminders
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── scripts/
│   └── android_debug_run.sh           # Automatisiertes Deployment
├── .vscode/
│   └── tasks.json                     # IDE Tasks
└── debug_artifacts/                    # Build & Log Outputs
```

## Aktuelle Konfiguration

- **Package:** de.montagezeit.app
- **Version:** 1.0.0 (versionCode 1)
- **Target SDK:** 34 (Android 14)
- **Min SDK:** 26 (Android 8.0)
- **Launch Activity:** de.montagezeit.app.MainActivity
- **Test Device:** Samsung SM-S931B (Galaxy S24)
- **Device Serial:** RFCY210JHMJ

## Bekannte Issues

1. **libpenguin.so not found** - Harmlos, Samsung-spezifische Library (optional)
2. **Screen-off pausiert App** - Normal, Android Lifecycle
3. **Freeccess friert App ein** - Samsung Battery Optimizer, in Settings deaktivierbar

## Weitere Dokumentation

- `docs/ARCHITECTURE.md` - App-Architektur
- `docs/ASSUMPTIONS.md` - Design-Entscheidungen
- `docs/QA_CHECKLIST.md` - Test-Szenarien
- `CLAUDE.md` - Claude-Code Projekthinweise
