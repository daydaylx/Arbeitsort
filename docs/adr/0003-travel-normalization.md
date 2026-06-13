# ADR-0003: Travel-Normalisierung in eigene Tabelle (v13→14)

**Status:** Akzeptiert
**Datum:** 2026-06-13

## Kontext

Bis Schema v13 waren Reisedaten als Flat-Fields direkt in `WorkEntry` gespeichert
(z. B. `outboundStartAt`, `outboundArriveAt`, `returnStartAt`, `returnArriveAt`).
Das erlaubte maximal eine Hin- und eine Rückreise pro Tag und machte
Mehrfach-Abschnitte (z. B. Monteur fährt mit Zwischenstopp) unmöglich.

## Entscheidung

Migration v13→14 extrahiert alle Reisedaten in eine separate Tabelle `travel_legs`
mit FK auf `work_entries.date`:

```sql
CREATE TABLE travel_legs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    work_entry_date TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    category TEXT NOT NULL,   -- OUTBOUND | INTERSITE | RETURN | OTHER
    ...
    FOREIGN KEY (work_entry_date) REFERENCES work_entries(date) ON DELETE CASCADE
)
```

Bestehende Flat-Fields in `WorkEntry` werden auf `null` gesetzt und sind seitdem
nicht mehr aktiv (bleiben zur Schema-Kompatibilität erhalten).

## Begründung

- **n:1-Beziehung**: Beliebig viele Reiseabschnitte pro Tag möglich.
- **Sortierbar**: `sort_order` erlaubt explizite Reihenfolge unabhängig von
  Erstellungsreihenfolge.
- **Kategorisierbar**: `TravelLegCategory` unterscheidet Hin-/Rückfahrt von
  Fahrten zwischen Baustellen (`INTERSITE`).
- **Kaskadierend**: `ON DELETE CASCADE` stellt referenzielle Integrität sicher —
  Reisedaten werden automatisch mit dem Eintrag gelöscht.

## Konsequenzen

- Alle Abfragen, die Reisedaten benötigen, müssen `travel_legs` joinen.
- `ExportDayMetrics` aggregiert Reiseminuten aus `travel_legs`, nicht aus Flat-Fields.
- Instrumented-Tests prüfen Cascade-Delete explizit (`WorkEntryDaoCascadeDeleteTest`).
- Migration muss im `AppDatabaseMigrationTest` abgedeckt sein.
