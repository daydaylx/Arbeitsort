#!/usr/bin/env bash

set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "adb is required but was not found in PATH." >&2
    exit 1
fi

if [[ $# -gt 0 && -n "${1}" ]]; then
    printf '%s\n' "$1"
    exit 0
fi

if [[ -n "${ANDROID_DEVICE_SERIAL:-}" ]]; then
    printf '%s\n' "${ANDROID_DEVICE_SERIAL}"
    exit 0
fi

devices=()
while IFS= read -r device; do
    if [[ -n "$device" ]]; then
        devices+=("$device")
    fi
done < <(adb devices | awk '$2 == "device" { print $1 }')

if [[ ${#devices[@]} -eq 0 ]]; then
    echo "No connected Android device found." >&2
    exit 1
fi

if [[ ${#devices[@]} -gt 1 ]]; then
    echo "Multiple Android devices found. Pass a serial or set ANDROID_DEVICE_SERIAL." >&2
    printf 'Devices:\n' >&2
    printf '  %s\n' "${devices[@]}" >&2
    exit 1
fi

printf '%s\n' "${devices[0]}"
