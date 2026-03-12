# Next Tasks - MontageZeit

**Letzte Aktualisierung:** 2026-03-09
**Status:** MVP produktionsreif (v1.0.2), offene Verbesserungen

---

## Offene Features

### 1. Reminder History Screen
- **Priorität:** P1
- Übersicht über gesendete Reminder (Datum, Typ, User-Reaktion)
- Quelle: `docs/REMINDERS.md` TODOs

### 2. Snooze Action (10 Minuten)
- **Priorität:** P1
- Notification Action „Erinnere in 10 Minuten" zusätzlich zu „Später"
- `ReminderLaterWorker` mit kurzem Delay erweitern
- Quelle: `docs/REMINDERS.md` TODOs

### 3. PDF Export vollständig dokumentieren
- **Priorität:** P2
- `PdfExporter` ist implementiert und funktionsfähig, fehlt aber in `docs/ARCHITECTURE.md`
  und `README.md`
- Dokumentation nachholen

### 4. Erweiterte benutzerdefinierte Reminder-Slots
- **Priorität:** P3
- Aktuell: 3 feste Fenster (Morning/Evening/Fallback)
- Optional: User-konfigurierbare zusätzliche Reminder-Zeiten
- Quelle: `docs/REMINDERS.md` TODOs

---

## Nach dem nächsten Release (Optional / P3)

- **Reverse Geocoding:** Standort-Label automatisch ermitteln (nur wenn online)
- **„Needs Review" Workflow:** Badge + Filter für Review-Einträge
- **Auswertungen:** Wochen/Monats-Übersicht
- **Import/Restore:** CSV Import für Backup-Wiederherstellung
