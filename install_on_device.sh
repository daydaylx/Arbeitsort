#!/usr/bin/env bash
# install_on_device.sh
# Installiert Android SDK (falls nötig), baut die APK und spielt sie per adb auf.
set -e

ANDROID_SDK="$HOME/Android/Sdk"
CMDLINE_TOOLS="$ANDROID_SDK/cmdline-tools/latest"
SDKMANAGER="$CMDLINE_TOOLS/bin/sdkmanager"

echo "=== Schritt 1: Android SDK prüfen ==="

if [ ! -f "$SDKMANAGER" ]; then
    echo "  → sdkmanager nicht gefunden, lade cmdline-tools herunter..."
    mkdir -p "$ANDROID_SDK/cmdline-tools"
    wget -q --show-progress \
        https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
        -O /tmp/cmdline-tools.zip
    unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_SDK/cmdline-tools"
    mv "$ANDROID_SDK/cmdline-tools/cmdline-tools" "$CMDLINE_TOOLS"
    rm /tmp/cmdline-tools.zip
    echo "  ✓ cmdline-tools installiert"
else
    echo "  ✓ sdkmanager bereits vorhanden"
fi

echo ""
echo "=== Schritt 2: SDK-Komponenten installieren ==="

if [ ! -d "$ANDROID_SDK/platforms/android-34" ]; then
    echo "  → platforms;android-34 wird installiert..."
    yes | "$SDKMANAGER" --sdk_root="$ANDROID_SDK" "platforms;android-34" "build-tools;34.0.0" 2>&1 | grep -v "^\[="
    echo "  ✓ Fertig"
else
    echo "  ✓ SDK-Komponenten bereits vorhanden"
fi

echo ""
echo "=== Schritt 3: APK bauen ==="

cd "$(dirname "$0")"

# local.properties mit SDK-Pfad sicherstellen
echo "sdk.dir=$ANDROID_SDK" > local.properties

export ANDROID_HOME="$ANDROID_SDK"
./gradlew assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    echo "FEHLER: APK nicht gefunden nach Build!"
    exit 1
fi
echo "  ✓ APK erstellt: $APK"

echo ""
echo "=== Schritt 4: Gerät prüfen und APK installieren ==="

if ! command -v adb &>/dev/null; then
    echo "FEHLER: adb nicht gefunden. Installiere android-sdk-platform-tools."
    exit 1
fi

DEVICE=$(adb devices | grep -v "List of" | grep "device$" | head -1 | cut -f1)
if [ -z "$DEVICE" ]; then
    echo "FEHLER: Kein Android-Gerät verbunden."
    echo "  → USB-Debugging aktivieren und Gerät anschließen, dann erneut ausführen."
    exit 1
fi

echo "  → Gerät gefunden: $DEVICE"
echo "  → Installiere APK (ohne Datenverlust)..."
adb -s "$DEVICE" install -r "$APK"
echo ""
echo "✓ Fertig! App wurde aktualisiert. Daten bleiben erhalten."
