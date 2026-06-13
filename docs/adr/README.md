# Architecture Decision Records (ADR)

Dieses Verzeichnis dokumentiert bedeutende Architekturentscheidungen des MontageZeit-Projekts.

## Format

Jede ADR folgt diesem Schema:

```
# ADR-XXXX: Titel

**Status:** Akzeptiert | Abgelöst | Vorgeschlagen
**Datum:** YYYY-MM-DD

## Kontext
## Entscheidung
## Begründung
## Konsequenzen
```

Status `Abgelöst` enthält einen Verweis auf die nachfolgende ADR.

## Index

| Nr.                                           | Titel                                            | Status     | Datum      |
| --------------------------------------------- | ------------------------------------------------ | ---------- | ---------- |
| [0001](0001-offline-first-model.md)           | Offline-First, kein Cloud-Sync                   | Akzeptiert | 2026-06-13 |
| [0002](0002-day-type-taxonomy.md)             | DayType-Taxonomie und DayClassification          | Akzeptiert | 2026-06-13 |
| [0003](0003-travel-normalization.md)          | Travel-Normalisierung in eigene Tabelle (v13→14) | Akzeptiert | 2026-06-13 |
| [0004](0004-confirmation-state.md)            | Bestätigungsstatus-Semantik                      | Akzeptiert | 2026-06-13 |
| [0005](0005-reminder-scheduling.md)           | WorkManager für Erinnerungsplanung               | Akzeptiert | 2026-06-13 |
| [0006](0006-pdf-ohne-externe-libs.md)         | PDF-Generierung ohne externe Bibliotheken        | Akzeptiert | 2026-06-13 |
| [0007](0007-db-backup-on-migration.md)        | Automatisches DB-Backup vor Migrationen          | Akzeptiert | 2026-06-13 |
| [0008](0008-schulung-lehrgang-deprecation.md) | Deprecation von SCHULUNG/LEHRGANG (v16→17)       | Akzeptiert | 2026-06-13 |

## Pflege

Neue ADRs werden sequenziell nummeriert. Abgelöste ADRs behalten Status `Abgelöst`
und verweisen auf den Nachfolger — sie werden nicht gelöscht.
