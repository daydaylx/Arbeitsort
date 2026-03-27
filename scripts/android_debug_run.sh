#!/usr/bin/env bash
# MontageZeit Android Debug Deployment Script
# Automates: Build -> Install -> Launch -> Logcat

set -euo pipefail

MODULE="app"
PACKAGE="de.montagezeit.app"
ACTIVITY="de.montagezeit.app.MainActivity"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEVICE_SERIAL="$("$SCRIPT_DIR/resolve_android_device.sh" "${1:-}")"

cd "$ROOT_DIR"

echo "=== MontageZeit Debug Deployment ==="
echo "Device: $DEVICE_SERIAL"
echo "Module: $MODULE"
echo ""

echo "[1/4] Building and installing APK..."
./gradlew ":$MODULE:installDebug"

echo ""
echo "[2/4] Launching $ACTIVITY..."
adb -s "$DEVICE_SERIAL" shell am start -n "$PACKAGE/$ACTIVITY"

echo ""
echo "[3/4] Waiting for app process..."
sleep 3
PID="$(adb -s "$DEVICE_SERIAL" shell pidof "$PACKAGE" || echo "")"

if [[ -z "$PID" ]]; then
    echo "WARNING: Process not found. App may have crashed or is not running."
    echo "Showing recent error logs:"
    adb -s "$DEVICE_SERIAL" logcat -d -v time "*:E" | grep "$PACKAGE" | tail -n 20 || true
    exit 1
fi

echo "App running with PID: $PID"

echo ""
echo "[4/4] Starting logcat (Ctrl+C to stop)..."
echo "Command: adb -s $DEVICE_SERIAL logcat --pid=$PID -v time"
echo ""
adb -s "$DEVICE_SERIAL" logcat --pid="$PID" -v time
