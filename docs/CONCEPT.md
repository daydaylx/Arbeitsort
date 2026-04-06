# Produktkonzept – MontageZeit

## Status

Diese Datei beschreibt das Produktleitbild und den fachlichen Zuschnitt der App. Sie ist nicht verbindlich; verbindlich sind `README.md` und `docs/ARCHITECTURE.md`.

Der aktuelle Produktkern ist eine lokale Android-App fuer Tageserfassung, Reminder, Historie, Bearbeitung und Export. Fruehere GPS-/Radius-Ideen sind kein aktiver Produktbestandteil mehr.

## Zielbild

MontageZeit soll Arbeitstage so erfassen, dass spaetere Nachvollziehbarkeit ohne Cloud-Sync moeglich bleibt:

- Tagesstatus eindeutig festhalten
- Tagesort als einfachen Text pflegen
- Morning-/Evening-Erfassung und Tagesabschluss niedrigschwellig machen
- freie Tage und Ueberstundenabbau sauber vom Arbeitstag unterscheiden
- Historie, Korrektur und Export ohne Medienbruch bereitstellen

Die App ist auf einen einzelnen Nutzer und ein einzelnes Geraet ausgelegt. Offline-first und lokale Datenhaltung sind Kern des Produkts.

## Produktbausteine

### 1. Today

Today ist der schnellste Pfad fuer den aktuellen Tag:

- manueller Daily-Check-in mit Pflichtfeld fuer den Tagesort
- optionale Angaben fuer Verpflegungspauschale:
  - An-/Abreisetag
  - Fruehstueck enthalten
- `Heute frei`
- Bearbeiten des gewaehlten Tags
- Wochenleiste sowie Wochen-/Monatswerte
- Loeschen eines Tags mit Undo

Today ist nicht auf GPS-Standortaufnahme ausgerichtet. Der Tagesort ist ein manueller Textwert.

### 2. History

History ist der Korrektur- und Auswertungspfad:

- Verlauf fuer die letzten 365 Tage
- Gruppierung nach Wochen, Monaten und Kalenderansichten
- Edit-Sheet fuer DayType, Zeiten, Travel-Daten, Tagesort und Notiz
- Batch-Edit fuer Datumsbereiche auf bestehenden Eintraegen

Batch-Edit ist bewusst eine Aenderungsfunktion. Fehlende Tage werden dabei nicht stillschweigend angelegt.

### 3. Reminder

Reminder unterstuetzen die Tageserfassung, ersetzen sie aber nicht:

- Morning-Reminder
- Evening-Reminder
- Fallback-Reminder
- Daily-Bestaetigung fuer noch unbestaetigte Tage

Reminder sind fenster- bzw. zeitbasiert und WorkManager-gestuetzt. Das Produkt optimiert fuer praktische Robustheit, nicht fuer exakte Alarm-Sekunden.

### 4. Export und Nachweise

Die App soll aus lokalen Tagesdaten einfache Nachweise erzeugen:

- PDF-Export mit Preview
- CSV-Export
- Export-Zeitraeume fuer aktueller Monat, letzte 30 Tage und benutzerdefinierte Ranges
- PDF-Stammdaten in den Settings

## Fachliche Leitlinien

### DayType und Tagesabschluss

Es gibt drei aktive Tagtypen:

- `WORK`
- `OFF`
- `COMP_TIME`

Wichtige fachliche Unterschiede:

- `WORK` kann Arbeit, Reise oder auch einen bestaetigten leeren Arbeitstag darstellen
- `OFF` ist ein freier Tag
- `COMP_TIME` ist Ueberstundenabbau und kein sichtbarer Arbeitstag

Nur bestaetigte Tage sollen in Statistik, Export-Summen und Ueberstundenlogik eingehen.

### Verpflegungspauschale

Verpflegungspauschale ist kein frei stehender Zahlenwert, sondern eine fachliche Ableitung:

- nur fuer bestaetigte `WORK`-Tage
- nur bei tatsaechlicher Aktivitaet
- Aktivitaet bedeutet positive Arbeitszeit oder positive Reisezeit

Diese Regel soll in Bearbeitung, Statistik und Export identisch gelten.

### Arbeitszeit und Defaults

Die App arbeitet mit konfigurierbaren Default-Arbeitszeiten:

- Arbeitsbeginn
- Arbeitsende
- Pause
- Tageszielstunden

Diese Defaults muessen fachlich sinnvoll bleiben. Unsinnige Kombinationen sollen nicht spaeter als bestaetigte 0-Netto-Tage im System landen.

## UX-Prinzipien

- moeglichst wenige Schritte fuer den Tagesabschluss
- keine Pflicht zu externer Infrastruktur, Cloud oder Standortberechtigung
- Bearbeiten und Nachtragen muessen alltagstauglich bleiben
- Auswertungen muessen fachlich nachvollziehbar sein und keine unterschiedlichen Wahrheiten zwischen UI, Statistik und Export erzeugen

## Technische Leitidee

MontageZeit bleibt bewusst einfach:

- Single-Module Android App
- Jetpack Compose fuer UI
- Room fuer lokale Persistenz
- DataStore fuer Settings
- WorkManager fuer Reminder und Rescheduling

Das Produkt ist kein Tracking-System. Es ist eine lokale Tagesdokumentation mit Reminder-Unterstuetzung.

## Nicht-Ziele des aktuellen Produkts

Diese Punkte gehoeren derzeit nicht zum aktiven Produktumfang:

- Cloud-Sync
- Mehrbenutzerbetrieb
- GPS- oder Radius-basierte Ortsklassifikation
- Standortmodus-Auswahl
- automatische Hintergrund-Ortserkennung
- exakte Alarmplanung mit `SCHEDULE_EXACT_ALARM`

## Konsequenz fuer Dokumentation und Weiterentwicklung

Neue Features sollten sich an diesen Fragen messen:

- verbessert es die Zuverlaessigkeit der Tageserfassung?
- bleibt die Fachlogik fuer Arbeitstag, freien Tag und Ueberstundenabbau konsistent?
- bleibt die App offline-first und lokal nachvollziehbar?
- fuehrt es nicht wieder eine zweite, widerspruechliche Fachlogik neben der bestehenden ein?

**Letzte Aktualisierung:** 2026-04-05
