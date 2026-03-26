# Arbeitsort – Audit-Plan pro Datei

**Projekt:** `daydaylx/Arbeitsort`
**Stand der Analyse:** statische Quellcode-Prüfung, zuletzt gegen `main` am 2026-03-15 abgeglichen
**Ziel:** systematische Bereinigung von Logikfehlern, Inkonsistenzen und Architekturproblemen
**Wichtig:** Dieser Plan priorisiert **fachliche Korrektheit vor kosmetischem Refactoring**.

---

## Status-Snapshot (2026-03-15)

- Die P0/P1-Kernpunkte aus diesem Plan sind inzwischen weitgehend umgesetzt:
  - Travel-Timestamps gewinnen gegen alte `travelPaidMinutes`-Overrides.
  - Today-, History- und Overtime-Summen folgen derselben Finalitätsregel.
  - Edit-Save, `SetDayType` und `ConfirmOffDay` behandeln OFF/COMP_TIME konsistenter.
  - Export-Ortslogik sowie CSV/PDF-Darstellung für OFF/COMP_TIME wurden bereinigt.
  - Reminder-Entscheidungen respektieren bestätigte Tage als terminalen Zustand.
- Dieses Dokument bleibt als **ursprünglicher Audit-Plan** erhalten; die Befund-Abschnitte unten beschreiben den Ausgangszustand vor der Bereinigung.
- Noch offen außerhalb dieses Fix-Passes:
  - manueller QA-Durchlauf auf Gerät
  - optionale Produkt-Erweiterungen aus `docs/NEXT_TASKS.md`

# 1. Zusammenfassung der Hauptprobleme

Das Repo wirkt insgesamt lauffähig, hat aber mehrere **stille Inkonsistenzen** in der Geschäftslogik.
Die größten Problemklassen sind:

1. **Travel-/Fahrzeitlogik** ist nicht eindeutig und kann durch Altwerte verfälscht werden.
2. **Überstunden, Wochen-/Monatswerte und History** rechnen nach unterschiedlichen Regeln.
3. **OFF-, WORK- und COMP_TIME-Tage** werden in verschiedenen Pfaden unterschiedlich validiert und gespeichert.
4. **Mehrere Schreibpfade** erzeugen `WorkEntry`-Zustände mit leicht abweichenden Defaults.
5. **Export (PDF/CSV/Preview)** nutzt teils andere Datenquellen als die eigentliche Tageslogik.

---

# 2. Prioritätsstufen

## P0 – Kritisch
Fehler mit hohem Risiko für falsche Berechnungen oder inkonsistente Kernzustände.

## P1 – Hoch
Fachlich problematisch, führt zu widersprüchlichen Anzeigen oder schwer nachvollziehbarem Verhalten.

## P2 – Mittel
Kein Totalschaden, aber unsauber, verwirrend oder langfristig fehleranfällig.

## P3 – Niedrig
Verbesserung von Struktur, Robustheit und Wartbarkeit.

---

# 3. Zielarchitektur für die Bereinigung

## Zielprinzipien

- **eine zentrale Wahrheit** für gültige `WorkEntry`-Zustände
- **eine klare Prioritätsregel** für Travel-Zeit
- **eine gemeinsame Finalitätsregel** für bestätigte vs. unbestätigte Tage
- **gleiche Datenbasis** für UI, Überstunden, Export und History
- **weniger direkte DAO-Schreibpfade**
- **mehr fachliche Tests, weniger Vertrauen in Zufall und Hoffnung**

---

# 4. Audit pro Datei

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/util/TimeCalculator.kt`

### Priorität
**P0**

### Befunde
- `calculateTravelMinutes()` nutzt `travelPaidMinutes` mit absoluter Priorität.
- Wenn `travelPaidMinutes` gesetzt ist, werden `travelStartAt` und `travelArriveAt` ignoriert.
- Dadurch können alte Override-Werte neue manuelle Fahrzeiten aushebeln.
- Arbeitszeitlogik ist für Standardfälle okay, aber sie hängt davon ab, dass vorgelagerte Zustände konsistent sind.

### Risiken
- Fahrzeiten im UI stimmen nicht mit Berechnung und Export überein.
- OFF-Tage mit manuell gepflegter Fahrzeit können falsch als `0` laufen.
- Historie und Preview übernehmen falsche Totalzeiten.

### Maßnahmen
- Eindeutige Regel definieren:
  - `travelPaidMinutes` ist **nur expliziter Override**
  - manuell gepflegte `travelStartAt`/`travelArriveAt` löschen diesen Override
- Dokumentation direkt im Code schärfen.
- Optional: separate Funktion einführen wie `hasManualTravelOverride()` oder sauber benannten Resolver.

### Tests
- bestehender Eintrag mit `travelPaidMinutes = 0`, danach manuelle Timestamps gesetzt → Ergebnis darf **nicht** 0 bleiben
- `travelPaidMinutes = null`, gültige Timestamps → korrekte Berechnung
- negativer Override → weiterhin 0 clampen
- Overnight-Reise → korrekt

---

## Datei: `app/src/main/java/de/montagezeit/app/ui/screen/edit/EditEntryViewModel.kt`

### Priorität
**P0**

### Befunde
- speichert direkt via `workEntryDao.upsert(entryToSave)` statt über zentrale Domain-Logik
- `travelPaidMinutes` wird im Save-Pfad nicht sauber behandelt
- Validation behandelt `OFF` uneinheitlich im Vergleich zu `ConfirmOffDay`
- `save()` enthält viel Fachlogik, die eigentlich in einen UseCase gehört
- Wechsel auf `COMP_TIME` ist teilweise behandelt, aber Travel-/Work-Zustand wird nicht konsequent bereinigt

### Risiken
- doppelte Logik
- Drift zwischen UI-Validierung und Domain-Validierung
- Travel-Override-Bug bleibt bestehen
- OFF-/COMP_TIME-Zustände können fachlich halbgar gespeichert werden

### Maßnahmen
- `save()` auf zentralen UseCase umstellen
- beim Speichern manueller Fahrzeiten:
  - `travelPaidMinutes = null`
  - `travelUpdatedAt = now`
- OFF-Regeln mit Domain abstimmen:
  - kein Pflicht-Ort, falls OFF fachlich ohne Tagesort gültig sein soll
- beim Wechsel auf `COMP_TIME` definieren:
  - Travel nullen oder explizit ignorieren
  - Work-/Pause-Werte entweder behalten nur für UI oder fachlich neutralisieren

### Tests
- OFF-Eintrag aus `ConfirmOffDay` öffnen und speichern → darf nicht plötzlich ungültig werden
- manueller Travel-Edit löscht `travelPaidMinutes`
- Wechsel WORK → COMP_TIME → keine Restlogik aus Arbeitszeit/Fahrzeit
- Wechsel COMP_TIME → WORK → Confirmation korrekt zurücksetzen

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/usecase/UpdateEntry.kt`

### Priorität
**P1**

### Befunde
- gute Basis für zentrale Validierung
- wird aber im wichtigsten Edit-Speicherpfad offenbar nicht konsequent genutzt
- validiert `dayLocationLabel`, Arbeitszeit und Travel grob solide
- enthält Geschäftslogik, die aktuell faktisch teilweise umgangen wird

### Risiken
- ein zentraler UseCase existiert, ist aber nicht echte Source of Truth
- spätere Fixes an `UpdateEntry` helfen nur teilweise

### Maßnahmen
- `UpdateEntry` zur zentralen Persistenzlogik für manuelle Änderungen machen
- Travel-Override-Regel hier integrieren
- OFF-/COMP_TIME-Sonderlogik hier bündeln
- `updatedAt` und `travelUpdatedAt` sauber zentral setzen

### Tests
- Edit-Save-Pfad nutzt wirklich `UpdateEntry`
- OFF, WORK, COMP_TIME validieren nach denselben Regeln wie UI
- Travel-Override-Tests zentral hier abbilden

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/usecase/CalculateOvertimeForRange.kt`

### Priorität
**P0**

### Befunde
- zählt nur `confirmedWorkDay == true`
- `OFF`-Fahrzeit wird separat gesammelt, aber nicht in Überstundensaldo aufgenommen
- `COMP_TIME` zieht Zielstunden ab, ohne Ist-Stunden
- Logik ist intern konsistent, aber **nicht** konsistent mit anderen Ansichten

### Risiken
- Today-/History-Summen widersprechen dem Überstundenkonto
- Nutzer versteht nicht, warum Zahlen voneinander abweichen
- OFF-Day-Travel ist fachlich leicht missverständlich

### Maßnahmen
- definieren:
  - finale Salden nur aus bestätigten Tagen
  - OFF-Day-Travel separat oder integriert, aber eindeutig
- falls OFF-Day-Travel separat bleibt:
  - überall klar labeln
  - nie so darstellen, als sei es bereits im Saldo enthalten

### Tests
- bestätigter + unbestätigter WORK-Tag
- OFF mit Fahrzeit
- COMP_TIME mit verschiedenen Targets
- Kombination mehrerer Tagtypen in einem Zeitraum

---

## Datei: `app/src/main/java/de/montagezeit/app/ui/screen/today/TodayViewModel.kt`

### Priorität
**P0**

### Befunde
- `calculateWeekStats()` und `calculateMonthStats()` summieren `WORK`-Einträge unabhängig von `confirmedWorkDay`
- Überstundenanzeige nutzt andere Logik als Wochen-/Monatswerte
- erzeugt damit mehrere Wahrheiten
- `ensureTodayEntryThen()` erzeugt neue Einträge mit Defaults, die nicht zwingend identisch zu anderen Pfaden sind

### Risiken
- Wochen-/Monatswerte widersprechen dem Überstundenkonto
- UI wird unzuverlässig
- Nutzer kann nicht erkennen, was final und was nur vorläufig ist

### Maßnahmen
- Week-/Month-Stats auf dieselbe Finalitätsregel ziehen wie Überstunden
- optional zwei Ebenen anzeigen:
  - **final**
  - **vorläufig**
- `ensureTodayEntryThen()` prüfen, ob zentrale Entry-Erzeugung sinnvoller wäre

### Tests
- bestätigte vs. unbestätigte Tage
- OFF-/COMP_TIME-Tage in Wochenübersicht
- neuer Tages-Eintrag via Today-Pfad vs. Edit-/UseCase-Pfad

---

## Datei: `app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryViewModel.kt`

### Priorität
**P1**

### Befunde
- summiert `totalHours` und `totalPaidHours` ohne dieselbe Finalitätsregel wie Überstunden
- `workDaysCount` und `offDaysCount` sind eher kategorial als fachlich final
- Batch-Edit baut neue Einträge mit eigenen Defaults

### Risiken
- History zeigt andere Summen als Today/Überstunden
- Batch-Edit erzeugt Zustände, die nicht zu anderen Entry-Erzeugern passen

### Maßnahmen
- Gruppensummen klar unterscheiden:
  - finale Summen
  - optionale vorläufige Summen
- Batch-Edit prüfen:
  - zentrale UseCases nutzen
  - Bestätigungslogik nicht manuell nachbauen
- OFF/COMP_TIME/WORK fachlich einheitlich behandeln

### Tests
- History-Summen mit bestätigten/unbestätigten Tagen
- Batch-Edit auf COMP_TIME / Rückwechsel auf WORK
- Monats- und Wochenblöcke mit Mischzuständen

---

## Datei: `app/src/main/java/de/montagezeit/app/export/PdfUtilities.kt`

### Priorität
**P1**

### Befunde
- `getLocation()` nutzt nur `morningLocationLabel` oder `eveningLocationLabel`
- `dayLocationLabel` wird ignoriert
- aktueller manueller Tagesort kann dadurch in Preview/PDF fehlen

### Risiken
- Export zeigt leeren Ort trotz gespeicherter Tagesort-Angabe
- Nutzer vertraut Export weniger als Datenbankzustand

### Maßnahmen
- `getLocation()` auf sinnvolle Priorität umstellen:
  1. `dayLocationLabel`
  2. `morningLocationLabel`
  3. `eveningLocationLabel`
- dokumentieren, was "Ort" im Export fachlich genau bedeuten soll

### Tests
- nur `dayLocationLabel` gesetzt
- nur Morning/Evening gesetzt
- alle gesetzt → Priorität korrekt

---

## Datei: `app/src/main/java/de/montagezeit/app/export/PdfExporter.kt`

### Priorität
**P1**

### Befunde
- rendert Start/Ende/Pause unabhängig vom Tagtyp
- OFF- und COMP_TIME-Tage können dadurch optisch wie normale Arbeitstage wirken
- Ort hängt an `PdfUtilities.getLocation()` und erbt damit deren Schwäche

### Risiken
- fachlich irreführender Export
- freie oder Überstundenabbau-Tage sehen wie normale Schichten aus

### Maßnahmen
- Darstellung je nach `dayType` anpassen:
  - OFF: Arbeitszeitfelder leer oder neutral
  - COMP_TIME: klar markieren
- Travel-/Totaldarstellung je Tagtyp bewusst gestalten
- Ort auf `dayLocationLabel`-Logik umstellen

### Tests
- PDF-Vorschau/Erzeugung mit OFF, WORK, COMP_TIME
- OFF-Tag darf nicht wie normaler Arbeitstag wirken
- Ort korrekt sichtbar

---

## Datei: `app/src/main/java/de/montagezeit/app/export/CsvExporter.kt`

### Priorität
**P1**

### Befunde
- exportiert Zeiten und Pausen unabhängig vom Tagtyp
- nutzt zwar `TimeCalculator`, aber Rohfelder wie Start/Ende/Pause werden immer mit ausgegeben
- CSV kann fachlich missverständlich werden

### Risiken
- OFF-/COMP_TIME-Tage sehen im CSV wie normale Schichttage aus
- Folgefehler bei Weiterverarbeitung in Excel oder externen Systemen

### Maßnahmen
- CSV-Spalten je nach Tagtyp bewusst befüllen oder leeren
- optional zusätzliche Spalte `isConfirmed` / `confirmedWorkDay`
- klarere Export-Semantik definieren

### Tests
- CSV mit Mischdatensätzen prüfen
- Tagtypabhängige Spaltenbelegung
- Travel-Minuten konsistent mit Preview/PDF

---

## Datei: `app/src/main/java/de/montagezeit/app/export/ExportPreviewViewModel.kt`

### Priorität
**P1**

### Befunde
- Preview nutzt `PdfUtilities.getLocation()`
- erbt dadurch denselben Ortsfehler wie PDF
- Summen basieren auf `TimeCalculator`, also potenziell betroffen vom Travel-Override-Problem

### Risiken
- Preview stimmt nicht mit tatsächlichem Datensatz überein
- Preview und finaler Export zeigen denselben falschen Ort

### Maßnahmen
- `formatLocationNote()` über korrigierte Ortslogik ziehen
- Summen nach Travel-Fix erneut validieren
- optional Bestätigungsstatus in Preview berücksichtigen

### Tests
- Tagesort nur über `dayLocationLabel`
- Travel mit/ohne Override
- Mischdatensätze mit OFF/COMP_TIME

---

## Datei: `app/src/main/java/de/montagezeit/app/data/local/entity/WorkEntryExtensions.kt`

### Priorität
**P0**

### Befunde
- `withTravelCleared()` setzt `travelPaidMinutes = 0`
- das ist fachlich nicht neutral, sondern ein harter Override-Wert
- wird von `ConfirmOffDay` indirekt genutzt

### Risiken
- neutralisierte Fahrzeit verhindert spätere korrekte Neuberechnung
- Travel-Logik bleibt dauerhaft verfälscht

### Maßnahmen
- `travelPaidMinutes = null` statt `0`
- prüfen, ob `travelUpdatedAt` dann weiterhin sinnvoll gesetzt wird
- überall dokumentieren, was "gelöscht" heißt

### Tests
- OFF-Tag bestätigen, dann Fahrzeit manuell setzen
- nach ClearTravel keine Restwirkung alter Daten

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/usecase/ConfirmOffDay.kt`

### Priorität
**P1**

### Befunde
- erzeugt gültige OFF-Tage mit leerem Tagesort
- verlässt sich auf `withConfirmedOffDay()` / `createConfirmedOffDayEntry()`
- kann mit Edit-Validierung kollidieren

### Risiken
- OFF-Tag ist in Domain gültig, im Edit-Screen aber problematisch
- unterschiedliche Fachregeln für denselben Zustand

### Maßnahmen
- OFF-Definition festziehen:
  - braucht OFF einen Tagesort oder nicht?
- falls nein:
  - Edit-Validation anpassen
- falls ja:
  - `ConfirmOffDay` muss Fallback-Label sinnvoll setzen

### Tests
- OFF erzeugen → Edit öffnen → speichern
- OFF mit/ohne Tagesort
- OFF → WORK Wechsel

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/usecase/ConfirmWorkDay.kt`

### Priorität
**P1**

### Befunde
- überschreibt `workStart`, `workEnd`, `breakMinutes` mit aktuellen Settings
- setzt Tagesort aus Resolver
- bestätigt Tag direkt

### Risiken
- manuell angepasste Zeiten können verloren gehen
- Bestätigung ändert unbeabsichtigt Tagesdaten

### Maßnahmen
- Entscheidung treffen:
  - Bestätigung soll nur bestätigen
  - oder auch bewusst Defaults neu anwenden
- falls nur bestätigen:
  - bestehende manuelle Zeiten nicht überschreiben
- Resolver-Logik mit `dayLocationLabel` konsistent halten

### Tests
- bestehender manuell geänderter WORK-Tag, danach ConfirmWorkDay
- Zeiten dürfen nur geändert werden, wenn das fachlich explizit gewollt ist

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/usecase/RecordDailyManualCheckIn.kt`

### Priorität
**P1**

### Befunde
- setzt bei manueller Tagesbestätigung Morning- und Evening-Felder direkt
- trägt `resolvedLabel` auch in Snapshot-Labels ein
- überschreibt ebenfalls Start/Ende/Pause aus Settings

### Risiken
- Snapshot-Felder werden semantisch als "erfasst" markiert, obwohl faktisch keine echte Snapshot-Logik stattgefunden hat
- individuelle Zeiten können überschrieben werden
- Export/History können Snapshot-Daten missinterpretieren

### Maßnahmen
- klar definieren:
  - sind Morning/Evening-Felder hier nur UI-Statusmarker?
  - oder echte fachliche Check-ins?
- wenn nur Statusmarker:
  - Export/History dürfen sie nicht wie echte Snapshots behandeln
- Zeitüberschreibung nur wenn fachlich gewollt

### Tests
- existierender Tag mit manuellen Zeiten, danach DailyManualCheckIn
- Snapshot-Labels/Status sinnvoll gesetzt
- Preview/PDF mit solchen Einträgen

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/usecase/SetDayLocation.kt`

### Priorität
**P2**

### Befunde
- erzeugt bei fehlendem Eintrag neue `WorkEntry`-Objekte mit eigenen Defaults
- nutzt `ReminderWindowEvaluator.isNonWorkingDay()` für initialen `dayType`
- wieder ein weiterer Entry-Erzeuger mit eigener Logik

### Risiken
- neuer Eintrag über Ortsänderung unterscheidet sich von anderen Pfaden
- Zustände driften je nach Einstiegspunkt auseinander

### Maßnahmen
- Entry-Erzeugung zentralisieren
- gemeinsamen Builder oder Factory für neue Einträge einführen

### Tests
- neuer Tag über SetDayLocation
- neuer Tag über Today
- neuer Tag über Edit
- Feldzustände vergleichen

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/usecase/SetDayType.kt`

### Priorität
**P1**

### Befunde
- `COMP_TIME` wird direkt bestätigt
- Rückwechsel von `COMP_TIME` auf andere Typen setzt Confirmation zurück
- erzeugt ebenfalls neue Einträge mit eigenen Defaults

### Risiken
- Fachlogik okay-ish, aber wieder ein eigener Erzeugungspfad
- Travel/Arbeitszeit-Reste bleiben möglicherweise hängen

### Maßnahmen
- beim Wechsel auf `COMP_TIME` entscheiden:
  - Work-/Travel-Daten nullen oder ignorieren
- zentrale State-Transition-Regeln definieren:
  - WORK → OFF
  - OFF → WORK
  - WORK → COMP_TIME
  - COMP_TIME → WORK

### Tests
- alle Typwechsel
- Restdaten nach Wechseln
- Überstundenberechnung nach Typwechseln

---

## Datei: `app/src/main/java/de/montagezeit/app/domain/usecase/SetTravelEvent.kt`

### Priorität
**P2**

### Befunde
- `DEPARTURE` teilt sich fachlich Felder mit `ARRIVE`
- Hin- und Rückfahrt sind nicht sauber abbildbar
- MVP-Kommentar im Code bestätigt die Modellgrenze

### Risiken
- semantisch falsche Travel-Daten
- spätere Erweiterung schwierig

### Maßnahmen
- kurzfristig:
  - als Legacy/MVP markieren
  - nicht als präzises Modell verkaufen
- mittelfristig:
  - separates Datenmodell für Outbound/Return

### Tests
- falls weiter genutzt: dokumentieren, was überschrieben wird
- Regressionstest für erwartetes aktuelles Verhalten

---

## Datei: `app/src/main/java/de/montagezeit/app/work/WindowCheckWorker.kt`

### Priorität
**P2**

### Befunde
- Reminder-Flags werden unabhängig vom tatsächlichen DB-Zustand gesetzt
- Daily-Reminder markiert Flag auch dann, wenn nichts gezeigt wurde
- Reminder-Entscheidungen basieren teils auf `entry == null` und teils auf tatsächlichem `dayType`

### Risiken
- Nutzer kann Reminder nicht erneut bekommen, obwohl Datenlage sich später geändert hat
- Statusflag und reale Tagesdaten driften auseinander

### Maßnahmen
- prüfen, ob Daily-Flag wirklich immer gesetzt werden soll
- klar definieren:
  - "heute wurde Daily geprüft"
  - vs.
  - "heute wurde Daily tatsächlich gezeigt"
- Flag-Namenssemantik schärfen

### Tests
- Tag ändert Zustand nach erstem Daily-Check
- Reminder soll/nicht soll erneut möglich sein
- nicht-arbeitsfreie Tage vs. manuell überschrieben

---

## Datei: `app/src/main/java/de/montagezeit/app/work/ReminderWindowEvaluator.kt`

### Priorität
**P2**

### Befunde
- Logik für Nicht-Arbeitstag ist okay, aber stark vom aktuellen Eintrag abhängig
- `WORK` überschreibt Auto-Off indirekt nur dadurch, dass nur OFF/COMP_TIME als non-working gewertet werden

### Risiken
- okay, aber sollte klar dokumentiert sein
- bei späteren Änderungen leicht missverständlich

### Maßnahmen
- Semantik direkt im Code noch klarer machen
- Tests für manuelle WORK-Tage am Wochenende/Feiertag

### Tests
- Wochenende + manueller WORK-Eintrag
- Feiertag + manueller WORK-Eintrag
- OFF/COMP_TIME priorisiert korrekt

---

## Datei: `app/src/main/java/de/montagezeit/app/data/local/database/AppDatabase.kt`

### Priorität
**P2**

### Befunde
- Migrationen sehen insgesamt plausibel aus
- `travelPaidMinutes` bleibt natürlich als Altdatenfeld bestehen
- keine akute Schema-Katastrophe sichtbar

### Risiken
- nach fachlicher Korrektur könnten Altbestände problematisch bleiben
- besonders `travelPaidMinutes = 0` als historischer Override-Wert

### Maßnahmen
- falls Travel-Regel geändert wird:
  - gezielte Datenmigration prüfen
  - `travelPaidMinutes = 0` nicht blind als neutral interpretieren
- optional einmalige "Datenreparatur"-Migration für problematische Travel-Kombinationen

### Tests
- Migrationstest für Daten mit Travel-Feldern
- Datenbestand mit OFF + `travelPaidMinutes = 0` + späteren Travel-Timestamps

---

## Datei: `app/src/main/java/de/montagezeit/app/ui/screen/settings/SettingsViewModel.kt`

### Priorität
**P2**

### Befunde
- exportiert PDF aus Settings direkt
- nutzt denselben Exporter wie Preview
- Reminder-Settings werden an mehreren Stellen ohne zentrale fachliche Validierung geändert
- einige Textzeichen im Kommentar wirken encoding-verhunzt, fachlich aber zweitrangig

### Risiken
- keine akute Kernlogik-Katastrophe
- aber Settings beeinflussen direkt Tagesdefaults und damit viele Folgezustände

### Maßnahmen
- prüfen, ob Änderungen an WorkStart/WorkEnd/Break fachlich nur für neue Tage gelten sollen
- klar kommunizieren, ob bestehende Tage davon unberührt bleiben

### Tests
- Settings ändern, bestehende Tage prüfen
- neue Tage vs. alte Tage konsistent

---

# 5. Übergreifende Maßnahmen

## A. Zentrale `WorkEntryFactory` oder `WorkEntryStateService` einführen
### Ziel
Alle neuen Einträge und Zustandswechsel über eine zentrale Stelle aufbauen.

### Warum
Aktuell entstehen neue Entries in vielen Dateien mit leicht anderen Defaults.

### Nutzen
- weniger Drift
- weniger versteckte Sonderfälle
- einfachere Tests

---

## B. Klare Fachregeln dokumentieren

## Tagtypen
### WORK
- Arbeitszeit relevant
- Fahrzeit relevant
- Zielzeit relevant

### OFF
- Arbeitszeit 0
- Fahrzeit optional
- Zielzeit 0
- Tagesort optional oder Pflicht, aber dann überall gleich

### COMP_TIME
- Arbeitszeit 0
- Zielzeit wird vom Saldo abgezogen
- Travel entweder verboten oder eindeutig ignoriert

---

## C. Finalitätsregel definieren

### Final
- confirmedWorkDay == true

### Vorläufig
- nicht bestätigt, aber ggf. bereits teilweise gepflegt

### Konsequenz
Alle Screens müssen entscheiden:
- zeigen sie final?
- zeigen sie vorläufig?
- oder beides getrennt?

Nicht mehr dieses charmante Chaos "jede Ansicht nimmt einfach irgendwas".

---

## D. Export-Regel definieren
- Preview, PDF und CSV müssen dieselbe Datenquelle und dieselbe Priorität nutzen
- `dayLocationLabel` muss als primäre Ortsquelle sauber eingebunden sein
- OFF/COMP_TIME-Darstellung muss bewusst anders aussehen als WORK

---

# 6. Testplan

## Sofort ergänzen

### Travel
- Override vs. Timestamps
- ClearTravel
- OFF + spätere Fahrzeitpflege
- Overnight-Reise

### Finalität
- bestätigte vs. unbestätigte Tage in:
  - Overtime
  - Today week/month
  - History

### Tagtypen
- OFF validieren/bearbeiten
- COMP_TIME-Wechsel
- Typwechsel mit Restdaten

### Export
- Tagesort nur über `dayLocationLabel`
- OFF/COMP_TIME in PDF/CSV
- Preview = PDF = CSV Logik

### Erzeugungspfade
Vergleich von neu erzeugten Einträgen über:
- Today
- Edit
- SetDayLocation
- ConfirmOffDay
- ConfirmWorkDay
- SetDayType

---

# 7. Empfohlene Reihenfolge der Umsetzung

## Phase 1 – Kritische Korrektheit
1. `TimeCalculator.kt`
2. `WorkEntryExtensions.kt`
3. `EditEntryViewModel.kt`
4. `UpdateEntry.kt`

## Phase 2 – Konsistente Zahlen
5. `CalculateOvertimeForRange.kt`
6. `TodayViewModel.kt`
7. `HistoryViewModel.kt`

## Phase 3 – Export-Korrektheit
8. `PdfUtilities.kt`
9. `PdfExporter.kt`
10. `CsvExporter.kt`
11. `ExportPreviewViewModel.kt`

## Phase 4 – Zustandsmodell vereinheitlichen
12. `ConfirmOffDay.kt`
13. `ConfirmWorkDay.kt`
14. `RecordDailyManualCheckIn.kt`
15. `SetDayLocation.kt`
16. `SetDayType.kt`
17. optional: `SetTravelEvent.kt`

## Phase 5 – Reminder/Feinschliff
18. `WindowCheckWorker.kt`
19. `ReminderWindowEvaluator.kt`
20. `SettingsViewModel.kt`
21. `AppDatabase.kt` Migrationen falls nötig

---

# 8. Definition of Done

Der Audit gilt als erfolgreich abgearbeitet, wenn:

- Travel-Zeit deterministisch und korrekt berechnet wird
- Preview, PDF, CSV und UI denselben Ort und dieselben Zeitwerte zeigen
- Überstunden, Week/Month und History dieselbe Finalitätsregel verwenden
- OFF-/COMP_TIME-Tage überall nach denselben Fachregeln behandelt werden
- der Edit-Save-Pfad nicht mehr an zentraler Domain-Logik vorbeischreibt
- neue `WorkEntry`-Objekte nicht mehr über fünf halbe Wahrheiten entstehen
- Regressionstests für alle kritischen Fälle vorhanden sind

---

# 9. Schlussbewertung

Das Repo ist **reparierbar**, aber es braucht **gezielte Konsolidierung**, nicht blindes Großrefactoring.

Der wichtigste Punkt ist nicht "mehr Architektur", sondern:

**weniger widersprüchliche Geschäftslogik pro Datei.**

Solange dieselben Zustände an mehreren Stellen unterschiedlich interpretiert werden, produziert die App weiter stille Fehler. Genau die Sorte, die besonders unerquicklich ist, weil nichts spektakulär crasht, sondern einfach Zahlen und Zustände langsam verrotten.
