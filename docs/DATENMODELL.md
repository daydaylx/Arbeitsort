# Datenmodell – MontageZeit

**Status:** Aktiv
**Letzte Aktualisierung:** 2026-06-13

Layer-Kontext und Architektur-Überblick: [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)
Entscheidungshintergrund: [`docs/adr/0002`](adr/0002-day-type-taxonomy.md),
[`docs/adr/0003`](adr/0003-travel-normalization.md),
[`docs/adr/0004`](adr/0004-confirmation-state.md)

---

## 1. WorkEntry

Primäre Entität. Repräsentiert einen Arbeitstag (PK: `date`).

| Feld                       | Typ         | Nullable | Bedeutung                                         |
| -------------------------- | ----------- | -------- | ------------------------------------------------- |
| `date`                     | `LocalDate` | Nein     | Primärschlüssel — ein Eintrag pro Tag             |
| `dayType`                  | `DayType`   | Nein     | Tagestyp (s. Abschnitt 3)                         |
| `confirmedWorkDay`         | `Boolean`   | Nein     | `true` = Eintrag abgeschlossen                    |
| `confirmationAt`           | `Long`      | Ja       | Zeitstempel der Bestätigung (Millis seit Epoch)   |
| `confirmationSource`       | `String`    | Ja       | `UI`, `NOTIFICATION`, `MIGRATION`                 |
| `workStart`                | `LocalTime` | Ja       | Arbeitsbeginn                                     |
| `workEnd`                  | `LocalTime` | Ja       | Arbeitsende                                       |
| `breakMinutes`             | `Int`       | Nein     | Pausenminuten (Default 0)                         |
| `dayLocationLabel`         | `String`    | Nein     | Einsatzort-Bezeichnung (leer = kein Ort)          |
| `morningCapturedAt`        | `Long`      | Ja       | Zeitstempel der Morning-Check-in-Aktion           |
| `eveningCapturedAt`        | `Long`      | Ja       | Zeitstempel der Evening-Check-in-Aktion           |
| `mealIsArrivalDeparture`   | `Boolean`   | Nein     | Abwesenheit beginnt/endet am gleichen Tag         |
| `mealBreakfastIncluded`    | `Boolean`   | Nein     | Frühstück in Verpflegungspauschale eingeschlossen |
| `mealAllowanceBaseCents`   | `Int`       | Nein     | Basisbetrag Verpflegungspauschale (Cent)          |
| `mealAllowanceAmountCents` | `Int`       | Nein     | Tatsächlicher Auszahlungsbetrag (Cent)            |
| `note`                     | `String`    | Ja       | Freitext-Notiz                                    |
| `createdAt`                | `Long`      | Nein     | Erstellungszeitpunkt (Millis)                     |
| `updatedAt`                | `Long`      | Nein     | Letzter Änderungszeitpunkt (Millis)               |

**Bestätigungsregel:** WORK wird nur bestätigt (`confirmedWorkDay = true`), wenn
positive Arbeitszeit (`workEnd - workStart - break > 0`) **oder** positive
Reisezeit (Summe `travel_legs`) vorliegt. OFF / VACATION / COMP_TIME gelten
immer als terminal.

---

## 2. TravelLeg

Kind-Entität. Reiseabschnitte für einen Arbeitstag (FK → `WorkEntry.date`).

| Feld                  | Typ                 | Nullable | Bedeutung                                  |
| --------------------- | ------------------- | -------- | ------------------------------------------ |
| `id`                  | `Long`              | Nein     | PK (autoincrement)                         |
| `workEntryDate`       | `LocalDate`         | Nein     | FK → `work_entries.date` (CASCADE DELETE)  |
| `sortOrder`           | `Int`               | Nein     | Anzeigereihenfolge (aufsteigend)           |
| `category`            | `TravelLegCategory` | Nein     | `OUTBOUND`, `INTERSITE`, `RETURN`, `OTHER` |
| `startAt`             | `Long`              | Ja       | Abfahrtzeitpunkt (Millis)                  |
| `arriveAt`            | `Long`              | Ja       | Ankunftszeitpunkt (Millis)                 |
| `startLabel`          | `String`            | Ja       | Abfahrtsort-Bezeichnung                    |
| `endLabel`            | `String`            | Ja       | Ankunftsort-Bezeichnung                    |
| `paidMinutesOverride` | `Int`               | Ja       | Manuell überschriebene vergütete Minuten   |
| `source`              | `TravelSource`      | Ja       | `MANUAL`, `ROUTED`, `ESTIMATED`            |
| `createdAt`           | `Long`              | Nein     | Erstellungszeitpunkt (Millis)              |
| `updatedAt`           | `Long`              | Nein     | Letzter Änderungszeitpunkt (Millis)        |

Reisedauer = `arriveAt - startAt` (wenn beide gesetzt) oder `paidMinutesOverride`.
Cascade: Löschen eines `WorkEntry` löscht alle zugehörigen `TravelLeg`-Einträge.

---

## 3. DayType (DB-Enum)

Persistierter Tagesstatus. Vier aktive Werte:

| Wert        | Soll              | Ist                   | Saldo-Logik |
| ----------- | ----------------- | --------------------- | ----------- |
| `WORK`      | Tagesziel-Stunden | Effektive Arbeitszeit | Ist − Soll  |
| `OFF`       | 0                 | 0                     | 0           |
| `VACATION`  | Tagesziel-Stunden | Tagesziel-Stunden     | 0           |
| `COMP_TIME` | Tagesziel-Stunden | 0                     | −Tagesziel  |

Legacy-Werte `SCHULUNG` und `LEHRGANG` wurden in Migration v16→17 auf `WORK`
abgebildet und existieren nicht mehr in der aktiven Enum (ADR-0008).

---

## 4. DayClassification (Domain-Enum)

Berechnete Klassifikation für Statistik, PDF-Export und Verpflegungspauschale.
Wird nicht in der DB gespeichert — aus `DayType` + `TravelLeg`-Daten berechnet.

| Wert                    | Herleitung                             |
| ----------------------- | -------------------------------------- |
| `FREI`                  | `OFF`, keine Reise                     |
| `FREI_MIT_REISE`        | `OFF` + mind. ein `TravelLeg`          |
| `URLAUB`                | `VACATION`                             |
| `ARBEITSTAG_MIT_ARBEIT` | `WORK` + positive Netto-Arbeitszeit    |
| `ARBEITSTAG_NUR_REISE`  | `WORK` + Reise, aber keine Arbeitszeit |
| `ARBEITSTAG_LEER`       | `WORK`, weder Arbeit noch Reise        |
| `UEBERSTUNDEN_ABBAU`    | `COMP_TIME`                            |

Details und Auswirkungen auf Verpflegungspauschale: [`docs/DAY_CLASSIFICATION.md`](DAY_CLASSIFICATION.md)

---

## 5. Migrations-Highlights

Vollständige Migrations-Kette: `MIGRATION_1_2` bis `MIGRATION_16_17` in
`AppDatabase.kt`.

| Version    | Wesentliche Änderung                                                        |
| ---------- | --------------------------------------------------------------------------- |
| v9→10      | `COMP_TIME` als neuer `DayType` hinzugefügt                                 |
| v10→11     | Verpflegungspauschale-Felder ergänzt (`meal*`)                              |
| **v13→14** | Travel-Normalisierung: Flat-Fields → `travel_legs`-Tabelle (ADR-0003)       |
| v14→15     | Historische Einträge mit positivem Tätigkeitsnachweis rückwirkend bestätigt |
| **v15→16** | Bestätigungsstatus vereinheitlicht; valide WORK-Tage auto-bestätigt         |
| **v16→17** | `SCHULUNG` / `LEHRGANG` → `WORK` (ADR-0008)                                 |

---

## 6. Typ-Konverter

Room-Konverter für nicht-primitive Typen (in `Converters.kt`):

- `LocalDate` ↔ `String` (ISO-8601)
- `LocalTime` ↔ `String` (ISO-8601)
- `DayType` ↔ `String`
- `TravelLegCategory` ↔ `String`
- `TravelSource` ↔ `String`

---

## 7. Helper-Methoden (WorkEntry)

Kapseln häufige Copy-Operationen zur Vermeidung von Copy-Bomben:

| Methode                           | Zweck                                     |
| --------------------------------- | ----------------------------------------- |
| `withTravelCleared(...)`          | Neuen WorkEntry ohne Reisedaten erzeugen  |
| `withConfirmedOffDay(...)`        | Eintrag als bestätigten OFF-Tag markieren |
| `createConfirmedOffDayEntry(...)` | Neuen, bestätigten OFF-Eintrag erstellen  |
