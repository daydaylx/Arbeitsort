#!/bin/bash
# MontageZeit Android Debug Deployment Script
# Automatisiert: Build → Install → Launch → Logcat

set -e  # Exit on error

DEVICE_SERIAL="${1:-RFCY210JHMJ}"
MODULE="app"
PACKAGE="de.montagezeit.app"
ACTIVITY="de.montagezeit.app.MainActivity"

echo "=== MontageZeit Debug Deployment ==="
echo "Device: $DEVICE_SERIAL"
echo "Module: $MODULE"
echo ""

# 1. Build und Install
echo "[1/4] Building and installing APK..."
./gradlew :$MODULE:installDebug

# 2. Launch
echo ""
echo "[2/4] Launching $ACTIVITY..."
adb -s $DEVICE_SERIAL shell am start -n "$PACKAGE/$ACTIVITY"

# 3. Get PID
echo ""
echo "[3/4] Waiting for app process..."
sleep 3
PID=$(adb -s $DEVICE_SERIAL shell pidof $PACKAGE || echo "")

if [ -z "$PID" ]; then
    echo "⚠️  Process not found. App may have crashed or is not running."
    echo "    Showing recent error logs:"
    adb -s $DEVICE_SERIAL logcat -d -v time "*:E" | grep "$PACKAGE" | tail -n 20
    exit 1
fi

echo "✓ App running with PID: $PID"

# 4. Start logcat
echo ""
echo "[4/4] Starting logcat (Ctrl+C to stop)..."
echo "    Command: adb -s $DEVICE_SERIAL logcat --pid=$PID -v time"
echo ""
adb -s $DEVICE_SERIAL logcat --pid=$PID -v time
