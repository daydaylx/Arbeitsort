# Android Development Guide - MontageZeit

Schnellstart-Anleitung fuer Android-Entwicklung, lokale Checks und Testing auf echten Geraeten.

## Voraussetzungen

- JDK 17 installiert
- Android SDK mit Platform Tools (`adb`)
- Gradle Wrapper (`./gradlew`)
- USB-Debugging auf dem Geraet aktiviert
- Optional: `lefthook` fuer lokale Git-Hooks

## Projekt-Setup

```bash
./gradlew assembleDebug
./scripts/setup_hooks.sh
```

Wenn `lefthook` lokal noch fehlt, gibt das Setup-Skript den naechsten Schritt aus.

## Lokale Qualitaetschecks

Diese Kommandos entsprechen dem empfohlenen lokalen Standard:

```bash
./gradlew lint
./gradlew :app:testDebugUnitTest
./gradlew assembleDebug
```

Mit installierten Hooks laufen:

- bei `git commit`: schnelle Staged-Diff-Checks
- bei `git push`: `lint`, `:app:testDebugUnitTest` und `assembleDebug`

## 1-Befehl Deployment

```bash
./scripts/android_debug_run.sh [DEVICE_SERIAL]
```

Das Skript fuehrt automatisch aus:

- Build und Installation der Debug-APK
- App-Start
- Logcat-Stream fuer den laufenden App-Prozess

Beispiel:

```bash
./scripts/android_debug_run.sh emulator-5554
```

Wenn genau ein Geraet verbunden ist, kann die Seriennummer entfallen. Bei mehreren verbundenen Geraeten entweder eine Seriennummer uebergeben oder `ANDROID_DEVICE_SERIAL` setzen.

## Manuelle Schritte

### 1. Build & Install

```bash
./gradlew :app:installDebug
```

Alternative (nur Build ohne Install):

```bash
./gradlew :app:assembleDebug
```

### 2. App starten

```bash
DEVICE_SERIAL="$(./scripts/resolve_android_device.sh)"
adb -s "$DEVICE_SERIAL" shell am start -n "de.montagezeit.app/de.montagezeit.app.MainActivity"
```

### 3. Logs ueberwachen

Mit PID-Filter (empfohlen):

```bash
DEVICE_SERIAL="$(./scripts/resolve_android_device.sh)"
PID=$(adb -s "$DEVICE_SERIAL" shell pidof de.montagezeit.app)
adb -s "$DEVICE_SERIAL" logcat --pid="$PID" -v time
```

Oder per Package-Name:

```bash
DEVICE_SERIAL="$(./scripts/resolve_android_device.sh)"
adb -s "$DEVICE_SERIAL" logcat -v time | grep montagezeit
```

### 4. App-Daten loeschen (bei DB-Schema-Aenderungen)

```bash
DEVICE_SERIAL="$(./scripts/resolve_android_device.sh)"
adb -s "$DEVICE_SERIAL" shell pm clear de.montagezeit.app
```

## VS Code Integration

Tasks sind in `.vscode/tasks.json` definiert.

Verfuegbare Tasks:

- `Android: Build Debug APK`
- `Android: Install Debug APK`
- `Android: Launch App`
- `Android: View Logcat`
- `Android: Clear App Data`
- `Android: Full Debug Run`
- `Android: Device List`

Ausfuehren: `Ctrl+Shift+B` und Task auswaehlen.

Die repo-getrackten Tasks verwenden ebenfalls die automatische Geraete-Aufloesung. Bei mehreren verbundenen Geraeten `ANDROID_DEVICE_SERIAL` in der Shell oder im VS-Code-Environment setzen.

## Wichtige Dateipfade

| Was | Pfad |
|-----|------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Build Logs | `debug_artifacts/build_*.txt` |
| Logcat Dumps | `debug_artifacts/logcat_*.txt` |
| Gradle Wrapper | `./gradlew` |
| Hook Setup | `./scripts/setup_hooks.sh` |
| Device Resolver | `./scripts/resolve_android_device.sh` |

## Troubleshooting

### Geraet nicht gefunden

```bash
adb start-server
adb devices -l
```

Falls `unauthorized`:

1. Telefon entsperren.
2. USB-Debugging-Prompt akzeptieren.
3. `adb devices` erneut ausfuehren.

Falls mehrere Geraete verbunden sind:

```bash
export ANDROID_DEVICE_SERIAL=<serial>
```

### App crasht sofort

```bash
DEVICE_SERIAL="$(./scripts/resolve_android_device.sh)"
adb -s "$DEVICE_SERIAL" logcat -d -v time "*:E" | grep montagezeit | tail -n 50
adb -s "$DEVICE_SERIAL" logcat -d > debug_artifacts/full_logcat.txt
```

### Room Database Schema-Fehler

```
Room cannot verify the data integrity...
```

Loesung:

```bash
DEVICE_SERIAL="$(./scripts/resolve_android_device.sh)"
adb -s "$DEVICE_SERIAL" shell pm clear de.montagezeit.app
```

Produktions-Fix: Datenbank-Version in `AppDatabase.kt` erhoehen und passende Migration + Tests ergaenzen.

## Projekt-Struktur

```text
.
├── app/
│   ├── src/main/
│   │   ├── java/de/montagezeit/app/
│   │   │   ├── MontageZeitApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   ├── domain/
│   │   │   ├── ui/
│   │   │   └── work/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── scripts/
│   ├── android_debug_run.sh
│   ├── resolve_android_device.sh
│   ├── setup_hooks.sh
│   └── hooks/
├── .vscode/
│   └── tasks.json
└── debug_artifacts/
```

## Aktuelle Konfiguration

- Package: `de.montagezeit.app`
- Version: `1.1.1` (`versionCode 5`)
- Target SDK: `34`
- Min SDK: `24`
- Launch Activity: `de.montagezeit.app.MainActivity`

## Bekannte Hinweise

1. WorkManager-Reminder bleiben fensterbasiert und koennen durch Doze oder Hersteller-Energiesparmechanismen verzoegert werden.
2. Bei Datenbank-Schema-Aenderungen sind lokale Test- oder Debug-Daten eventuell nicht mehr kompatibel, bis App-Daten geloescht oder eine Migration eingebaut wurde.

## Weitere Dokumentation

- `README.md` - Produktstatus, Build- und Architektur-Einstieg
- `docs/ARCHITECTURE.md` - App-Architektur
- `CONTRIBUTING.md` - Setup, Hooks und Contributing-Workflow
- `docs/README.md` - Dokumentations-Uebersicht
