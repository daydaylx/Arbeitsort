# ADR-0002: DayType-Taxonomie und DayClassification

**Status:** Akzeptiert
**Datum:** 2026-06-13

## Kontext

Arbeitstage in der Zeiterfassung haben unterschiedliche Bedeutungen: für die Sichtbarkeit
in Berichten, für die Soll/Ist-Berechnung und für Verpflegungspauschalen-Anspruch.
Eine einzige Enum reicht nicht aus, um alle drei Aspekte sauber zu trennen.

## Entscheidung

Zwei getrennte Konzepte:

**`DayType` (DB-Enum, 4 Werte)** — persistierter Tagesstatus:

- `WORK` — Arbeitstag (mit oder ohne Tätigkeitsnachweis)
- `OFF` — freier Tag ohne Soll- und Istzeit
- `VACATION` — bezahlter Urlaub; Soll = Ist = Tagesziel, Saldo = 0
- `COMP_TIME` — Überstundenabbau; Soll = Tagesziel, Ist = 0

**`DayClassification` (Domain-Enum, 7 Werte)** — berechnete Klassifikation für
Statistik, Export und Verpflegungspauschale:

- `FREI` — OFF-Tag ohne Reise
- `FREI_MIT_REISE` — OFF-Tag mit Reisedaten
- `URLAUB` — VACATION
- `ARBEITSTAG_MIT_ARBEIT` — WORK + positive Arbeitszeit
- `ARBEITSTAG_NUR_REISE` — WORK + Reise, aber keine Arbeitszeit
- `ARBEITSTAG_LEER` — WORK, aber weder Arbeit noch Reise
- `UEBERSTUNDEN_ABBAU` — COMP_TIME

Details: `docs/DAY_CLASSIFICATION.md`

## Begründung

- `DayType` ist klein und stabil — geeignet für DB-Spalten und Migrationen.
- `DayClassification` kann sich an Geschäftslogik-Anforderungen anpassen, ohne
  DB-Migrationen zu erfordern.
- Die Trennung verhindert, dass die DB-Enum mit Präsentations- oder
  Abrechnungslogik aufgebläht wird.

## Konsequenzen

- Neue Tagestypen in der UI erfordern ggf. nur `DayClassification`-Erweiterung,
  nicht zwingend eine DB-Migration.
- Code, der direkt auf `DayType` matcht, darf keine Domänenlogik über
  Abrechnungsregeln enthalten — das gehört in `DayClassification`.
