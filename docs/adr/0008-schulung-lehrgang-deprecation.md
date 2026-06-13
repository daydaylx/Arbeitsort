# ADR-0008: Deprecation von SCHULUNG/LEHRGANG (v16→17)

**Status:** Akzeptiert
**Datum:** 2026-06-13

## Kontext

Frühe Versionen der App enthielten die `DayType`-Werte `SCHULUNG` (Schulungstag)
und `LEHRGANG` (Lehrgang/Kurs). Diese Werte wurden in der Datenbank persistiert,
aber nie vollständig in der aktiven UI-Auswahl implementiert. Sie erzeugten
Sonderfälle in Statistik-Berechnungen und Export-Logik ohne klar definierten
Nutzwert gegenüber `WORK`.

## Entscheidung

Migration v16→17 bildet alle persistierten `SCHULUNG`- und `LEHRGANG`-Werte
auf `WORK` ab:

```sql
UPDATE work_entries SET day_type = 'WORK'
WHERE day_type IN ('SCHULUNG', 'LEHRGANG')
```

Die Werte sind danach nicht mehr Teil des aktiven `DayType`-Enums. Nur die
vier Werte `WORK`, `OFF`, `VACATION`, `COMP_TIME` bleiben.

## Begründung

- **Semantische Klarheit**: Schulungen und Lehrgänge sind aus Zeiterfassungssicht
  Arbeitstage — die Unterscheidung hat keinen Mehrwert für Soll/Ist-Berechnung
  oder Verpflegungspauschale.
- **Reduzierte Komplexität**: Weniger Enum-Werte bedeuten weniger
  `when`-Branches in Statistik, Export und UI — alle Branches waren ohnehin
  identisch mit `WORK`.
- **Keine aktive UI-Nutzung**: Die Werte waren nie in der Auswahl-UI sichtbar —
  ein Entfernen hat keinen sichtbaren Nutzungsimpact.

## Konsequenzen

- Bereits erfasste `SCHULUNG`/`LEHRGANG`-Einträge erscheinen in History und
  Export als `WORK` — die inhaltliche Arbeitszeit-Information bleibt erhalten.
- `AppDatabaseMigrationTest` deckt v16→17 ab und prüft die Typ-Konvertierung.
- Keine UI-Änderung erforderlich — die Werte waren bereits nicht auswählbar.
