# ADR-0004: Bestätigungsstatus-Semantik

**Status:** Akzeptiert
**Datum:** 2026-06-13

## Kontext

Ein Arbeitstag gilt erst dann als abgeschlossen, wenn der Nutzer ihn explizit
bestätigt. Ohne Bestätigung ist der Eintrag offen und für Reminder, Statistik
und Export als unvollständig zu behandeln.

## Entscheidung

`WorkEntry` trägt drei Bestätigungsfelder:

| Feld                 | Typ       | Semantik                                   |
| -------------------- | --------- | ------------------------------------------ |
| `confirmedWorkDay`   | `Boolean` | Hauptflag — `true` = Eintrag abgeschlossen |
| `confirmationAt`     | `Long?`   | Unix-Timestamp der Bestätigung (Millis)    |
| `confirmationSource` | `String?` | Quelle: `UI`, `NOTIFICATION`, `MIGRATION`  |

**Regeln:**

- `WORK`-Einträge werden nur bestätigt, wenn positive Arbeitszeit **oder** positive
  Reisezeit vorliegt.
- `OFF`, `VACATION`, `COMP_TIME` sind terminale Zustände — sie gelten immer als
  bestätigt (auch ohne `confirmedWorkDay = true`).
- Migration v15→16 hat historische Einträge mit positivem Tätigkeitsnachweis
  rückwirkend bestätigt (`confirmationSource = "MIGRATION"`).

## Begründung

- Ein binäres Flag ist einfach abfragbar und braucht keinen eigenen Status-Enum.
- Timestamp und Quelle ermöglichen Audit-Nachvollziehbarkeit ohne separaten
  Audit-Log.
- Die Quelle `MIGRATION` macht maschinell gesetzte Bestätigungen von
  Nutzer-Aktionen unterscheidbar.

## Konsequenzen

- Reminder-Logik prüft `confirmedWorkDay` — nicht bestätigte WORK-Einträge
  werden im Abend-/Fallback-Fenster angemahnt.
- Statistik und PDF-Export schließen unbestätigte WORK-Tage aus (leere
  Zero-Net-WORK-Einträge zählen nicht).
- `EditEntrySaveBuilder` setzt Bestätigung nur, wenn die Validierung positiv ist.
