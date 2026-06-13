# Context-Pack: Erinnerungssystem

Für Aufgaben rund um Reminder-Scheduling, Notification-Aktionen und Worker-Logik.

## Zuerst lesen

1. `docs/REMINDERS.md` — Technische Referenz (Reminder-Typen, Eligibility, Snooze-Logik)
2. `docs/ARCHITECTURE.md` Section 2.2 — Scheduling-Strategie
3. `docs/adr/0005-reminder-scheduling.md` — Entscheidungshintergrund WorkManager vs. AlarmManager

## Schlüsseldateien

```
app/src/main/java/de/montagezeit/app/
├── work/
│   ├── WindowCheckWorker.kt        ← Haupt-Worker, prüft Zeitfenster + sendet Notification
│   ├── ReminderLaterWorker.kt      ← One-time Worker für "Später erinnern"
│   ├── ReminderScheduler.kt        ← Enqueued WorkManager-Workers
│   ├── ReminderWindowEvaluator.kt  ← Fenster-Prüfung (Uhrzeit, Wochentag, Settings)
│   └── ReminderScheduleCalculator.kt
├── notification/
│   └── ReminderNotificationManager.kt  ← Notification-Erstellung und -Anzeige
├── service/
│   └── CheckInActionService.kt     ← Foreground-Service für Notification-Action-Buttons
├── receiver/
│   ├── BootReceiver.kt             ← Startet Workers nach Reboot
│   └── TimeChangeReceiver.kt       ← Reagiert auf Zeitzone-/Uhrzeitänderungen
└── data/preferences/
    └── ReminderSettingsManager.kt  ← Liest/schreibt Reminder-Konfiguration
```

## Invarianten

- **Mutex in WindowCheckWorker**: Die kritische Sektion (DB-Lesen + Flag setzen +
  Notification senden) ist durch einen Mutex atomar — nie außerhalb dieses Locks
  auf Reminder-Flags schreiben.
- **Keine exakten Alarme**: Kein `AlarmManager.setExact()`. WorkManager-Fenster
  bedeuten ~15 min Toleranz — das ist by design (ADR-0005).
- **Tagesflags verhindern Doppel-Notify**: `reminder_flags` in SharedPreferences
  werden im Worker gesetzt. Reset erfolgt täglich. Nie manuell löschen ohne den
  Worker-Lifecycle zu bedenken.
- **Legacy-Worker**: Alte Workers ohne `reminder_type` im Input-Data laufen als
  no-op. Dieses Verhalten beibehalten.

## Verifikation

```bash
# Unit-Tests Reminder-Bereich
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.work.*"
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.notification.*"
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.receiver.*"
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.service.*"

# Vollständige Unit-Test-Suite
./gradlew :app:testDebugUnitTest
```

Manuelle Prüfpfade: `docs/MANUELLE_TESTS.md` → MT-04
