# Data Model – MontageZeit

Kanonische, agententaugliche Referenz für Room-Datenmodell und DataStore-Settings.
Dokumentiert **ausschließlich den aktuellen Codezustand** (Schema-Version 16,
Stand `main`) — keine neuen Datenmodell-Entscheidungen. Layer-Kontext:
[`docs/ARCHITECTURE.md`](ARCHITECTURE.md).

---

## 1. AppDatabase

Datei: `app/src/main/java/de/montagezeit/app/data/local/database/AppDatabase.kt`

- `@Database(entities = [WorkEntry::class, TravelLeg::class], version = 16, exportSchema = true)`
- `fallbackToDestructiveMigration()` ist **nicht** aktiviert — fehlende
  Migrationen crashen die App absichtlich statt Daten zu löschen.

### Migrationen

Vollständige Kette: `MIGRATION_1_2` bis `MIGRATION_15_16`. Fachlich relevant
sind die letzten vier:

| Migration         | Änderung                                                                                                                                                                                                                                                                                                   |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `MIGRATION_12_13` | Fügt `returnStartAt`/`returnArriveAt` (Rückfahrt-Zeitstempel) zu `work_entries` hinzu                                                                                                                                                                                                                      |
| `MIGRATION_13_14` | **Travel-Normalisierung**: neue Child-Tabelle `travel_legs` (FK → `work_entries.date`, `ON DELETE CASCADE`); migriert Legacy-Travel-Spalten in bis zu 3 `travel_legs`-Zeilen (`OUTBOUND`, `RETURN`, `OTHER`); `work_entries` wird ohne Travel-Spalten neu aufgebaut, `workStart`/`workEnd` werden nullable |
| `MIGRATION_14_15` | Bestätigt Restore-Einträge automatisch (`confirmationSource = 'RESTORED_FROM_EXPORT'`), wenn ausreichend Daten vorhanden sind                                                                                                                                                                              |
| `MIGRATION_15_16` | Vereinheitlicht Bestätigungsstatus per Cursor-Iteration: bestätigt `WORK`/`SCHULUNG`/`LEHRGANG` bei positiver Arbeits- oder Reisezeit, setzt inkonsistente Bestätigungen zurück; `OFF`/`COMP_TIME` sind immer terminal bestätigt                                                                           |

Jede künftige Migration braucht Test-Coverage in
`app/src/test/java/de/montagezeit/app/data/local/database/AppDatabaseMigrationTest.kt`
und eine passende Instrumented-Verifikation.

---

## 2. WorkEntry

Datei: `app/src/main/java/de/montagezeit/app/data/local/entity/WorkEntry.kt`
`@Entity(tableName = "work_entries")`, `@PrimaryKey val date: LocalDate`

| Feld                       | Typ         | Nullable  | Default        | Bedeutung                                          |
| -------------------------- | ----------- | --------- | -------------- | -------------------------------------------------- |
| `date`                     | `LocalDate` | nein (PK) | –              | Ein Eintrag pro Tag                                |
| `workStart`                | `LocalTime` | ja        | `null`         | Arbeitsbeginn                                      |
| `workEnd`                  | `LocalTime` | ja        | `null`         | Arbeitsende                                        |
| `breakMinutes`             | `Int`       | nein      | `0`            | Pausenminuten                                      |
| `dayType`                  | `DayType`   | nein      | `DayType.WORK` | Tagestyp, siehe Abschnitt 4                        |
| `dayLocationLabel`         | `String`    | nein      | `""`           | **Manueller** Tagesort-Text (kein GPS)             |
| `morningCapturedAt`        | `Long`      | ja        | `null`         | Zeitstempel Morning-Check-in                       |
| `eveningCapturedAt`        | `Long`      | ja        | `null`         | Zeitstempel Evening-Check-in                       |
| `confirmedWorkDay`         | `Boolean`   | nein      | `false`        | `true` = Tag fachlich abgeschlossen                |
| `confirmationAt`           | `Long`      | ja        | `null`         | Zeitstempel der Bestätigung                        |
| `confirmationSource`       | `String`    | ja        | `null`         | z. B. `UI`, `NOTIFICATION`, `RESTORED_FROM_EXPORT` |
| `mealIsArrivalDeparture`   | `Boolean`   | nein      | `false`        | An-/Abreisetag (reduzierte Pauschale)              |
| `mealBreakfastIncluded`    | `Boolean`   | nein      | `false`        | Frühstück in Pauschale enthalten                   |
| `mealAllowanceBaseCents`   | `Int`       | nein      | `0`            | Basisbetrag Verpflegungspauschale (Cent)           |
| `mealAllowanceAmountCents` | `Int`       | nein      | `0`            | Tatsächlicher Betrag (Cent)                        |
| `note`                     | `String`    | ja        | `null`         | Freitext-Notiz                                     |
| `createdAt`                | `Long`      | nein      | `now()`        | Erstellungszeitpunkt                               |
| `updatedAt`                | `Long`      | nein      | `now()`        | Letzte Änderung                                    |

Indices: `date` (unique), `createdAt`, `(dayType, date)` composite.

**Bestätigungsregel:** `WORK` wird nur bestätigt (`confirmedWorkDay = true`),
wenn positive Netto-Arbeitszeit **oder** positive Reisezeit (Summe
`travel_legs`) vorliegt. `OFF`/`COMP_TIME` sind immer terminal bestätigt.

---

## 3. TravelLeg

Datei: `app/src/main/java/de/montagezeit/app/data/local/entity/TravelLeg.kt`
`@Entity(tableName = "travel_legs")`, FK → `WorkEntry` mit `onDelete = CASCADE`

| Feld                      | Typ                 | Nullable                 | Bedeutung                                |
| ------------------------- | ------------------- | ------------------------ | ---------------------------------------- |
| `id`                      | `Long`              | nein (PK, autoIncrement) | –                                        |
| `workEntryDate`           | `LocalDate`         | nein                     | FK → `work_entries.date`                 |
| `sortOrder`               | `Int`               | nein                     | Anzeigereihenfolge                       |
| `category`                | `TravelLegCategory` | nein                     | `OUTBOUND, INTERSITE, RETURN, OTHER`     |
| `startAt` / `arriveAt`    | `Long?`             | ja                       | Abfahrt-/Ankunftszeitpunkt (Millis)      |
| `startLabel` / `endLabel` | `String?`           | ja                       | Orts-Bezeichnungen                       |
| `paidMinutesOverride`     | `Int?`              | ja                       | Manuell überschriebene vergütete Minuten |
| `source`                  | `TravelSource?`     | ja                       | `MANUAL, ROUTED, ESTIMATED`              |
| `createdAt` / `updatedAt` | `Long`              | nein                     | Zeitstempel                              |

Indices: `workEntryDate`, `(workEntryDate, sortOrder)` unique. Zusätzlich
`WorkEntryWithTravelLegs` als `@Embedded`+`@Relation`-Datenklasse mit
`orderedTravelLegs`. Löschen eines `WorkEntry` löscht kaskadierend alle
zugehörigen `TravelLeg`-Zeilen.

---

## 4. DayType (DB-Enum)

Datei: `app/src/main/java/de/montagezeit/app/data/local/entity/WorkEntry.kt`

```kotlin
enum class DayType {
    WORK, OFF, COMP_TIME, SCHULUNG, LEHRGANG;
    val isWorkLike: Boolean get() = this == WORK || this == SCHULUNG || this == LEHRGANG
}
```

**5 aktive Werte.** `SCHULUNG` und `LEHRGANG` sind auf diesem Stand **aktiv
genutzt** (u. a. in `MealAllowanceCalculator.kt` via `isWorkLike`,
`PdfExporter.kt`, `CsvExporter.kt`, `DayTypeTransitions.kt`,
`EditEntryFormContent.kt`, `HistoryScreen.kt`, `TodayScreen.kt`) — **keine
Legacy-/tote Werte**. Es gibt auf diesem Stand **keine** `VACATION`-DayType.

Verpflegungspauschale ist trotz `isWorkLike` nur an `dayType == DayType.WORK`
gekoppelt (siehe `MealAllowanceCalculator.kt`), nicht an `SCHULUNG`/`LEHRGANG`.

---

## 5. DayClassification (Domain-Enum)

Datei: `app/src/main/java/de/montagezeit/app/domain/model/DayClassification.kt`

Berechnete Klassifikation für Statistik/Export, nicht in der DB gespeichert.
**8 Werte:** `FREI`, `FREI_MIT_REISE`, `ARBEITSTAG_MIT_ARBEIT`,
`ARBEITSTAG_NUR_REISE`, `ARBEITSTAG_LEER`, `UEBERSTUNDEN_ABBAU`, `SCHULUNG`,
`LEHRGANG`. Fachliche Details, Eligibility-Regeln und Beispielszenarien:
[`docs/DAY_CLASSIFICATION.md`](DAY_CLASSIFICATION.md).

---

## 6. MealAllowance / Verpflegungspauschale

Datei: `app/src/main/java/de/montagezeit/app/domain/util/MealAllowanceCalculator.kt`

- Konstanten: `BASE_NORMAL_CENTS = 2800`, `BASE_ARRIVAL_DEPARTURE_CENTS = 1400`,
  `BREAKFAST_DEDUCTION_CENTS = 560`.
- Ausschluss: `EXCLUDED_LOCATION = "leipzig"`, Prüfung über
  `locationLabel.trim().lowercase() == "leipzig"` — **exakter String-Vergleich**,
  keine Fuzzy-/Substring-Erkennung. Bekannte Lücke bei Ortsvarianten:
  [Issue #50](https://github.com/daydaylx/Arbeitsort/issues/50) (nicht Teil
  dieser Doku-Änderung, nicht fixen ohne separaten Auftrag).
- Gilt nur für `dayType == DayType.WORK`.

---

## 7. DataStore Settings (`data/preferences/`)

| Datei                        | DataStore-Name         | Inhalt                                                                                                                                                                      |
| ---------------------------- | ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ReminderSettings.kt`        | – (Datenklasse + Keys) | Arbeitszeit-Defaults, Morning-/Evening-Fenster + Intervalle, Fallback-/Daily-Zeit, `autoOffWeekends`, `autoOffHolidays`, `holidayDates`, PDF-Stammdaten, `dailyTargetHours` |
| `ReminderSettingsManager.kt` | `reminder_settings`    | Lese-/Schreib-Manager für `ReminderSettings`                                                                                                                                |
| `ReminderFlagsStore.kt`      | `reminder_flags_v2`    | Tages-Reminder-Flags (verhindert Mehrfach-Notifies); migriert alte SharedPreferences-Keys einmalig                                                                          |
| `PdfSettings.kt`             | – (reine Datenklasse)  | `employeeName`, `company`, `project`, `personnelNumber`                                                                                                                     |

---

## 8. Kritische Persistenz-No-Gos

- **Keine Migration ohne separaten, expliziten Auftrag.** Auch keine
  „vorsorgliche" Schema-Versionserhöhung.
- Kein `fallbackToDestructiveMigration()` aktivieren — Datenverlust bei
  fehlenden Migrationen ist nicht akzeptabel.
- Jede Migration braucht Test-Coverage (`AppDatabaseMigrationTest` +
  Instrumented-Schema-Test).
- Feldänderungen an `WorkEntry`/`TravelLeg` sofort in diesem Dokument
  nachziehen, damit es nicht selbst zur Drift-Quelle wird.
- Keine neuen `DayType`-/`DayClassification`-Werte ohne fachliche Abstimmung
  einführen — das betrifft Statistik, Export und Reminder-Terminal-Logik
  gleichzeitig.
- `dayLocationLabel` bleibt Freitext — keine Umstellung auf
  Koordinaten/Placeholder-IDs ohne expliziten Auftrag.
