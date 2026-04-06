# UI-Implementierung – Zusammenfassung

## Status

Diese Datei ist eine kompakte Uebersicht ueber den aktuell implementierten UI-Umfang. Verbindlich bleiben `README.md` und `docs/ARCHITECTURE.md`.

## Aktuell sichtbare Hauptbereiche

### 1. Today

Der Today-Screen bildet den Hauptpfad fuer den aktuellen Tag:

- Tageskarte fuer den gewaehlten Tag
- Wochenleiste mit Tagesstatus
- manueller Daily-Check-in mit Tagesort
- optionale Angaben fuer Verpflegungspauschale im Daily-Dialog
- Aktion `Heute frei`
- Bearbeiten des aktuell gewaehlten Tags
- Loeschen des Tags mit Undo
- Anzeige von Wochen-/Monatswerten und relevanten Tagesdetails

Today arbeitet mit manuellem Tagesort und nicht mit aktiver Standortlogik.

### 2. History

Der History-Bereich dient zum Nachtragen, Kontrollieren und Korrigieren:

- Verlauf fuer die letzten 365 Tage
- Wochen- und Monatsgruppen
- Kalender- und Wochenansicht
- Edit-Zugriff auf einzelne Tage
- Batch-Edit fuer Datumsbereiche mit vorhandenen Eintraegen

Batch-Edit aendert nur bestehende Eintraege. Leere Tage werden dabei nicht implizit angelegt.

### 3. Settings

Der Settings-Screen deckt den operativen App-Betrieb ab:

- Arbeitszeit-Defaults
- Reminder-Konfiguration fuer Morning, Evening, Fallback und Daily
- Einstellungen fuer automatische Wochenend-/Feiertagsbehandlung
- Tagesziel-, Wochenziel- und Monatszielwerte
- Test-Reminder
- PDF-Stammdaten
- CSV- und PDF-Export inklusive Preview fuer benutzerdefinierte Zeitraeume
- Hinweise und Shortcuts fuer Notification- und Batterie-Einstellungen

### 4. Edit-/Export-Nebenpfade

- Edit-Sheet fuer DayType, Zeiten, Tagesort, Travel-Daten und Notiz
- Export-Preview als Bottom Sheet mit Drilldown auf einzelne Tage

## Implementierungsbild

### ViewModels

Aktive UI-Haupt-ViewModels:

- `TodayViewModel`
- `HistoryViewModel`
- `SettingsViewModel`
- `EditEntryViewModel`
- `ExportPreviewViewModel`

### Navigation

- Bottom-Navigation fuer Today, History und Settings
- Sheet- und Dialog-Pfade fuer Bearbeitung, Check-in und Export-Preview
- Reminder koennen ueber Notification-Aktionen in App- oder Bearbeitungspfade fuehren

### UI-Grundmuster

- Compose + Material3
- Hero-/Card-basierte Seitenstruktur
- Dialoge und Bottom Sheets fuer kontextgebundene Aktionen
- Snackbar-Feedback fuer kurzlebige Rueckmeldungen

## Wichtige fachliche UI-Entscheidungen

- Der primäre Today-Pfad ist der manuelle Daily-Check-in mit Pflichtfeld fuer den Tagesort.
- `Heute frei` bestaetigt direkt einen freien Tag.
- `COMP_TIME` ist ein eigener fachlicher Zustand und nicht einfach ein normaler Arbeitstag.
- Verpflegungspauschalen werden in der UI nur als fachliche Ableitung eines `WORK`-Tags mit Aktivitaet behandelt.
- History-, Statistik- und Exportanzeigen sollen dieselbe Fachlogik fuer Arbeitstage und Pauschalen verwenden.

## Nicht mehr aktueller Umfang

Nicht Teil des aktuellen UI-Produkts sind:

- GPS-gestuetzte Standortaufnahme als Hauptpfad
- Radius-Slider oder Standortmodus als aktive Settings-Funktion
- Loading- oder Error-Flows fuer laufende Standortermittlung als heutiger Normalfall

## Offene Dokumentationsgrenze

Diese Datei ist eine Uebersicht, keine vollstaendige Screen-Spezifikation. Wenn Details zu Reminder, Statistik oder DayType-Auswertung relevant sind, sind diese Dokumente maßgeblicher:

- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/DAY_CLASSIFICATION.md`
- `docs/REMINDERS.md`

**Letzte Aktualisierung:** 2026-04-05
