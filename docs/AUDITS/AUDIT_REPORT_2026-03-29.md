# Audit-Report: MontageZeit Codebase

**Datum:** 2026-03-29
**Scope:** Gesamter Produktionscode (`app/src/main/java/`)
**Methode:** Statische Analyse, manuelle Code-Review, Cross-Referenzierung

---

## Befunde

### HOCH -- Logikfehler mit Auswirkung auf Berechnungen

#### H1: `WorkStatsResult.offDayTravelMinutes` ergibt immer 0

**Ort:** `domain/usecase/AggregateWorkStats.kt:51-52`

```kotlin
val offDayTravelMinutes: Int
    get() = totalTravelMinutes - (totalPaidMinutes - totalWorkMinutes)
```

**Problem:** Da `totalPaidMinutes = totalWorkMinutes + totalTravelMinutes` (Zeile 94), kuerzt sich die Formel zu `T - (W+T - W) = T - T = 0`. Die Property gibt immer 0 zurueck, unabhaengig von den tatsaechlichen Daten.

**Auswirkung:** Aktuell wird die Property nirgends konsumiert, daher kein Laufzeitfehler. Sollte sie aber fuer Anzeige oder Export genutzt werden, waere der Wert falsch. `CalculateOvertimeForRange.offDayTravelHours` implementiert die korrekte Logik ueber `DayClassification.FREI_MIT_REISE`.

**Empfohlene Loesung:** Property entfernen, da `OvertimeResult.offDayTravelHours` die korrekte Quelle ist. Alternativ: Berechnung ueber Klassifikation analog zu `CalculateOvertimeForRange` implementieren.

---

#### H2: `UpdateEntry.validateEntry()` lehnt Nachtschichten ab

**Ort:** `domain/usecase/UpdateEntry.kt:65`

```kotlin
if (workEnd <= workStart) {
    throw IllegalArgumentException("workEnd ($workEnd) muss nach workStart ($workStart) liegen")
}
```

**Problem:** Bei `workStart=22:00, workEnd=06:00` gilt `06:00 <= 22:00`, und die Validierung wirft eine Exception. `TimeCalculator.calculateWorkMinutes` (Zeile 26-30) unterstuetzt Mitternacht-Crossing korrekt, aber die Validierung verhindert das Speichern. Auch der `breakMinutes > workDurationMinutes`-Check (Zeile 71-74) berechnet die Dauer ohne Mitternacht-Korrektur.

**Auswirkung:** Nutzer koennen keine Nachtschichten ueber die manuelle Bearbeitung eintragen.

**Empfohlene Loesung:** Validierung an `TimeCalculator`-Logik anpassen. Wenn `workEnd < workStart`, Mitternacht-Berechnung verwenden. Nur ablehnen bei unplausiblen Werten (z.B. > 18h Arbeitszeit).

---

### MITTEL -- Toter Code & Duplikate

#### M1: `ClassifiedDay` Data-Class ist toter Produktionscode

**Ort:** `domain/usecase/ClassifyDay.kt:87-117`

**Problem:** Die `ClassifiedDay` Data-Class und ihre `from()`-Factory werden in keiner Produktionsdatei verwendet. `AggregateWorkStats` nutzt eine eigene interne `ClassifiedDayWithEntry`. Die Klasse taucht ausschliesslich in Testdateien auf.

**Auswirkung:** Erhoehter Wartungsaufwand, Verwirrung bei Code-Reviews.

**Empfohlene Loesung:** In Test-Fixture verschieben oder entfernen.

---

#### M2: `Formatters.formatMinutes()` ist ungenutzt -- drei parallele Implementierungen

**Ort:** `ui/util/Formatters.kt:91-106`

**Problem:** Die zentrale `Formatters.formatMinutes()`-Methode wird nirgends aufgerufen. Stattdessen existieren:
- `TodayScreen.formatMinutes()` (Zeile 679) -- lokale Composable-Funktion
- `ExportPreviewViewModel.formatMinutes()` (Zeile 71) -- lokale Methode

Drei verschiedene Implementierungen mit leicht unterschiedlicher Formatierung.

**Auswirkung:** Inkonsistente Darstellung moeglich, toter Code in `Formatters`.

**Empfohlene Loesung:** Ungenutzte Methode entfernen. Optional: Screens auf gemeinsame Implementierung migrieren.

---

#### M3: `Formatters.formatDateShort()` ist ungenutzt

**Ort:** `ui/util/Formatters.kt:49-51`

**Problem:** Wird nirgends im Produktionscode aufgerufen (0 Grep-Treffer).

**Auswirkung:** Toter Code.

**Empfohlene Loesung:** Entfernen.

---

#### M4: Duplizierte und ungenutzte `CONFIRMATION_SOURCE`-Konstanten

**Ort:**
- `ConfirmWorkDay.kt:19` -- `CONFIRMATION_SOURCE_NOTIFICATION`
- `ConfirmOffDay.kt:21-22` -- `CONFIRMATION_SOURCE_NOTIFICATION` + `CONFIRMATION_SOURCE_UI`
- `RecordDailyManualCheckIn.kt:27` -- `CONFIRMATION_SOURCE_UI`

**Problem:** Gleiche String-Konstanten (`"NOTIFICATION"`, `"UI"`) als `private const` in drei Companion-Objekten dupliziert. `CONFIRMATION_SOURCE_UI` in `ConfirmOffDay` ist komplett ungenutzt (deklariert, nie referenziert). Caller wie `CheckInActionService` und `TodayViewModel` verwenden ohnehin String-Literale.

**Auswirkung:** Inkonsistenz-Risiko bei Aenderungen, toter Code.

**Empfohlene Loesung:** Ungenutzte Konstante in `ConfirmOffDay` entfernen. Optional: Zentrale Konstanten definieren.

---

#### M5: `DayClassification`-Properties erzeugen bei jedem Zugriff neue Listen

**Ort:** `domain/model/DayClassification.kt:76, 91, 92, 98, 104`

```kotlin
val isCountedWorkDay: Boolean
    get() = this in listOf(ARBEITSTAG_MIT_ARBEIT, ARBEITSTAG_NUR_REISE, ARBEITSTAG_LEER, UEBERSTUNDEN_ABBAU)
```

**Problem:** `isCountedWorkDay`, `canHaveTravelTime`, `hasTravelTime`, `isMealAllowanceEligible` erzeugen bei jedem Zugriff eine neue `List`. Diese Properties werden in Schleifen ueber alle Eintraege aufgerufen (z.B. `AggregateWorkStats`, `CalculateOvertimeForRange`).

**Auswirkung:** Unnoetige GC-Last bei groesseren Datensaetzen (~1460 kurzlebige Listen pro Statistik-Aufruf bei 365 Tagen). Kein Crash, aber ineffizient.

**Empfohlene Loesung:** `when`-Ausdruck verwenden statt `this in listOf(...)`.

---

### NIEDRIG -- Inkonsistenzen & Stilprobleme

#### N1: `result!!` statt `requireNotNull` in `ConfirmWorkDay`

**Ort:** `domain/usecase/ConfirmWorkDay.kt:64`

**Problem:** `return result!!` statt `requireNotNull(result) { "..." }`. Andere UseCases verwenden konsistent `requireNotNull` mit beschreibender Nachricht.

**Empfohlene Loesung:** Auf `requireNotNull(result) { "readModifyWrite hat kein Ergebnis geliefert" }` umstellen.

---

#### N2: Doppelter `System.currentTimeMillis()`-Aufruf in `CheckInEntryBuilder`

**Ort:** `domain/usecase/CheckInEntryBuilder.kt:31 + 67`

**Problem:** `now` in Zeile 31 erfasst, aber `createDefaultEntry` ruft intern erneut `System.currentTimeMillis()` auf. Timestamps koennen um Millisekunden abweichen.

**Empfohlene Loesung:** `now` als Parameter an `createDefaultEntry` durchreichen.

---

#### N3: Alias-Variable ohne Transformation in `CheckInEntryBuilder`

**Ort:** `domain/usecase/CheckInEntryBuilder.kt:35`

```kotlin
val normalizedEntry = existingEntry
```

**Problem:** Reine Zuweisung ohne Transformation. Stammt vermutlich aus einem Refactoring.

**Empfohlene Loesung:** Direkt `existingEntry` verwenden.

---

#### N4: Duplizierter PendingIntent-Aufbau in `ReminderNotificationManager`

**Ort:** `notification/ReminderNotificationManager.kt:467-523`

**Problem:** `createRemindLaterPendingIntent`, `createSnooze10MinPendingIntent`, `createMarkDayOffPendingIntent` haben fast identischen Aufbau (~60 Zeilen Duplikation).

**Empfohlene Loesung:** Optional: Generische Hilfsfunktion. Kein dringender Handlungsbedarf.

---

#### N5: `targetHoursForPeriod(YEAR)` ist ungenutzte Naeherung

**Ort:** `ui/screen/overview/OverviewCalculations.kt:48`

**Problem:** `settings.monthlyTargetHours * 12` ist eine Naeherung. Die Funktion wird nur in Tests verwendet, nicht in `buildOverviewMetrics`.

**Empfohlene Loesung:** Kommentar ergaenzen oder entfernen falls nicht benoetigt.

---

## Zusammenfassung

| Schwere | Anzahl | Beschreibung |
|---------|--------|--------------|
| **HOCH** | 2 | 1x Berechnungsfehler (immer 0), 1x Validierungsfehler (Nachtschicht) |
| **MITTEL** | 5 | 2x toter Code, 1x duplizierte Konstanten, 1x ungenutzter Formatierer, 1x ineffiziente Properties |
| **NIEDRIG** | 5 | Inkonsistenzen, Alias-Variable, Code-Duplikation |
| **Gesamt** | **12** | |

## Positiv

- Saubere Clean-Architecture-Trennung (UI/Domain/Data)
- 50 Unit-Tests mit guter Domain-Abdeckung
- Konsistentes `readModifyWrite`-Pattern fuer DB-Operationen
- Korrektes Mitternacht-Handling in `TimeCalculator`
- Privacy-by-Default (kein Cloud-Sync, kein GPS, kein Analytics)
- Moderne Dependencies (Kotlin 2.1, Compose, Room 2.7, Hilt 2.56)
