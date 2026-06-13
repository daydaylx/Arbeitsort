# ADR-0005: WorkManager für Erinnerungsplanung

**Status:** Akzeptiert
**Datum:** 2026-06-13

## Kontext

Die App muss Nutzer zu festen Tageszeiten an die Zeiterfassung erinnern (morgens,
abends, Fallback). Auf Android stehen `AlarmManager` (exakt) und `WorkManager`
(fensterbasiert, Doze-kompatibel) zur Wahl.

## Entscheidung

WorkManager mit periodischen Workers:

- `WindowCheckWorker` — periodisch, prüft im Worker selbst, ob das konfigurierte
  Zeitfenster aktiv ist (`ReminderWindowEvaluator`)
- `ReminderLaterWorker` — One-time, wird durch "Später erinnern"-Aktion ausgelöst
- Minimales Intervall: 15 Minuten (WorkManager-Systemlimit)
- Vier `ReminderType`-Werte: `MORNING`, `EVENING`, `FALLBACK`, `DAILY`
- SharedPreferences-Flags (`reminder_flags`) verhindern Mehrfach-Notifications
  am selben Tag

## Begründung

- **Doze-Kompatibilität**: `AlarmManager.setExact()` erfordert
  `USE_EXACT_ALARM`-Permission (restriktiv ab Android 12) oder
  `SCHEDULE_EXACT_ALARM` (widerrufbar durch Nutzer). WorkManager ist
  batterieschonender und benötigt keine Sonder-Permission.
- **Kein `POST_NOTIFICATIONS`-Risiko bei exakten Alarmen**: WorkManager delegiert
  die Ausführung an das System — die App wird nicht im Hintergrund geweckt.
- **Einfacheres State-Management**: Worker-Lifecycle ist durch WorkManager
  kontrolliert; kein manuelles Alarm-Reschedule nach Reboot nötig (WorkManager
  macht das automatisch, ergänzt durch `BootReceiver`).

## Konsequenzen

- Erinnerungen können bis zu ~15 Minuten verspätet sein (WorkManager-Fenster,
  Doze-Verzögerung durch OEM möglich).
- Das Timing ist nicht präzise genug für medizinische oder sicherheitskritische
  Anwendungen — für Zeiterfassung akzeptabel.
- `DiagnosticTrace` im `WindowCheckWorker` loggt Entscheidungen lokal zur
  Fehlersuche.
- Tests: `WindowCheckWorkerTest`, `ReminderLaterWorkerTest`,
  `ReminderWindowEvaluatorTest` und Boundary-Tests decken die Scheduling-Logik ab.
