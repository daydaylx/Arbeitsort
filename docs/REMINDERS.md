# Reminder & Notification System

## Status

Diese Datei ist eine technische Ergänzungsdoku. Verbindlich bleiben `README.md` und `docs/ARCHITECTURE.md`.

MontageZeit nutzt ein WorkManager-basiertes Reminder-System fuer manuelle Tageserfassung. Es gibt keine GPS- oder Standortpflicht. Reminder fuehren entweder in Notification-Aktionen oder in die Bearbeitung des Tages.

## Überblick

Aktive Reminder-Typen:

- `MORNING`: erinnert an den Morgen-Check-in
- `EVENING`: erinnert an den Abend-Check-in
- `FALLBACK`: erinnert spaet am Tag an unvollstaendige Tage
- `DAILY`: fragt, ob der Tag als Arbeitstag oder freier Tag bestaetigt werden soll

Ziel des Systems:

- unbestaetigte oder unvollstaendige Tage rechtzeitig sichtbar machen
- automatische Nicht-Arbeitstage respektieren
- Doppel-Notifications pro Tag und Reminder-Typ vermeiden
- trotz Doze/OEM-Verzoegerungen robust bleiben

## Komponenten

### Settings und Scheduler

- `ReminderSettingsManager` liest und schreibt Reminder-, Feiertags- und Arbeitszeit-Defaults.
- `ReminderScheduler` plant vier dedizierte Unique-Works fuer `MORNING`, `EVENING`, `FALLBACK` und `DAILY`.
- Morning und Evening laufen periodisch innerhalb konfigurierbarer Zeitfenster.
- Fallback und Daily laufen jeweils einmal pro Tag zur konfigurierten Uhrzeit.
- Initial Delays werden millisekundengenau gesetzt, damit Sub-Minuten-Grenzen nicht versehentlich zu `0` abrunden.

### Worker

- `WindowCheckWorker` ist der zentrale Entscheidungs-Worker fuer die regulaeren Reminder.
- `ReminderLaterWorker` zeigt gesnoozte Reminder spaeter erneut an.
- Beide Worker pruefen vor der Anzeige erneut, ob der Reminder fachlich noch sinnvoll ist.

### Notification- und Action-Schicht

- `ReminderNotificationManager` baut die vier Notification-Typen und ihre Actions.
- `CheckInActionService` verarbeitet Notification-Aktionen fuer Check-in, `Heute frei`, `Später`, Daily-Confirmation und Edit-Pfade.
- `BootReceiver` und `TimeChangeReceiver` planen alle Reminder nach Reboot, App-Update, Zeit- oder Zeitzonenwechsel neu.

## Fachlogik pro Reminder-Typ

### MORNING

Ein Morning-Reminder darf nur erscheinen, wenn:

- Morning-Reminder aktiviert ist
- aktuelle Uhrzeit im Morning-Window liegt
- der Tag nicht bereits als abgeschlossen bestaetigt wurde
- der Tag kein automatischer Nicht-Arbeitstag ohne manuellen Override ist
- entweder noch kein Eintrag existiert oder ein `WORK`-Eintrag ohne `morningCapturedAt` vorliegt

Notification-Aktionen:

- `Arbeit` / Morgen-Check-in
- `Heute frei`
- `10 Min`
- `+1 h`
- `+2 h`

### EVENING

Ein Evening-Reminder darf nur erscheinen, wenn:

- Evening-Reminder aktiviert ist
- aktuelle Uhrzeit im Evening-Window liegt
- der Tag nicht bereits bestaetigt wurde
- der Tag kein automatischer Nicht-Arbeitstag ohne manuellen Override ist
- entweder noch kein Eintrag existiert oder ein `WORK`-Eintrag ohne `eveningCapturedAt` vorliegt

Notification-Aktionen:

- Abend-Check-in
- `10 Min`
- `+1 h`
- `+2 h`
- `Heute frei`

### FALLBACK

Ein Fallback-Reminder darf nur erscheinen, wenn:

- Fallback aktiviert ist
- aktuelle Uhrzeit die Fallback-Zeit erreicht oder ueberschritten hat
- der Tag nicht bereits bestaetigt wurde
- der Tag kein automatischer Nicht-Arbeitstag ohne manuellen Override ist
- der Tag leer ist oder bei `WORK` noch Morning- oder Evening-Erfassung fehlt

Notification-Aktionen:

- `Bearbeiten`
- `10 Min`
- `+1 h`
- `+2 h`
- `Heute frei`

### DAILY

Der Daily-Reminder ist die taegliche Bestaetigungslogik fuer noch nicht abgeschlossene Tage. Er darf nur erscheinen, wenn:

- Daily-Reminder aktiviert ist
- aktuelle Uhrzeit die konfigurierte Daily-Zeit erreicht oder ueberschritten hat
- der Tag kein automatischer Nicht-Arbeitstag ohne manuellen Override ist
- kein terminaler Zustand vorliegt

Als terminal gelten:

- `confirmedWorkDay = true`
- `OFF`
- `COMP_TIME`

Notification-Aktionen:

- `Arbeit`
- `Frei`
- `Später`

`Später` im Daily-Kontext erzeugt einen erneuten Daily-Reminder spaeter am selben Tag. Daily wird bewusst nicht fuer automatische Wochenend-/Feiertage angezeigt.

## Scheduling-Strategie

### WorkManager statt exakter Alarme

Die App nutzt bewusst WorkManager und keine exakten Alarme:

- Morning und Evening sind fensterbasiert und wiederholen sich periodisch
- Fallback und Daily werden als taegliche periodische WorkRequests geplant
- echte Alarmgenauigkeit ist nicht Ziel des Produkts
- Doze und Hersteller-Energiesparen koennen Ausfuehrungen verzoegern

### Boundary-Verhalten

Wichtige aktuelle Details:

- Initial Delays fuer Fallback und Daily werden nicht mehr auf volle Minuten abgeschnitten.
- Ein Worker-Lauf vor der Zielzeit zeigt keinen Reminder und erzeugt keinen stillen Tagesverlust.
- `DAILY` prueft die Zielzeit im Worker ebenfalls explizit, statt sich nur auf das Scheduling zu verlassen.
- Legacy-Worker ohne `reminder_type` enden absichtlich als no-op, um Doppeltrigger zu vermeiden.

## Zusammenwirken mit DayType und Tagesabschluss

- `RecordDailyManualCheckIn` speichert einen manuellen Arbeitstagsabschluss inklusive Tagesort.
- `ConfirmWorkDay` bestaetigt einen Arbeitstag mit Default-Arbeitszeiten, wenn keine Zeiten vorhanden sind.
- `ConfirmOffDay` bestaetigt den Tag als `OFF`.
- `COMP_TIME` gilt ebenfalls als abgeschlossener Tag und unterdrueckt Daily-Reminder.
- Morning-, Evening- und Fallback-Reminder laufen nur sinnvoll fuer `WORK`-Pfad bzw. unvollstaendige Tage.

## Nicht-Arbeitstage

Automatische Nicht-Arbeitstage werden ueber Settings gesteuert:

- `autoOffWeekends`
- `autoOffHolidays`
- `holidayDates`

Wenn fuer einen Tag kein manueller Eintrag existiert und der Tag automatisch nicht arbeitsrelevant ist, unterdruecken die Worker die Reminder. Manuelle Eintraege ueberschreiben diese Automatik.

## Snooze- und "Später"-Logik

- Morning, Evening und Fallback unterstuetzen `10 Min`, `+1 h` und `+2 h`.
- Daily nutzt einen eigenen `Später`-Pfad fuer eine erneute taegliche Bestaetigung.
- Beim Snooze wird nur der betroffene Reminder-Typ gecancelt und spaeter erneut geplant.
- Vor dem erneuten Anzeigen prueft `ReminderLaterWorker` erneut, ob der Tag inzwischen schon abgeschlossen oder fachlich nicht mehr reminderfaehig ist.

## Test- und Prüfbereiche

Die Reminder-Logik sollte mindestens in diesen Bereichen abgesichert bleiben:

- Window-Boundaries fuer Morning und Evening
- taegliche Zielzeiten fuer Fallback und Daily
- Nicht-Arbeitstage und manuelle Overrides
- Snooze-/Reminder-Later-Pfade
- terminale Daily-Zustaende (`OFF`, `COMP_TIME`, bestaetigte Tage)
- Re-Scheduling nach Reboot, App-Update, Zeit- und Zeitzonenwechsel

## Bekannte Systemgrenzen

- WorkManager ist robust, aber nicht sekundengenau.
- OEM-Energiesparmechanismen und Doze koennen Reminder nach hinten verschieben.
- Das Produkt modelliert Reminder bewusst als "im sinnvollen Fenster" statt als harte Alarmuhr.
- Die App arbeitet ohne Standortberechtigung; Tagesort ist ein Textwert und kein GPS-Snapshot.

**Letzte Aktualisierung:** 2026-04-05
