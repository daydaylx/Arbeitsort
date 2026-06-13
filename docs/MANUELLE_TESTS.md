# Manuelle Tests – MontageZeit

**Status:** Aktiv
**Letzte Aktualisierung:** 2026-06-13

Checklisten für Prüfpfade, die nicht automatisiert abgedeckt sind.
Automatisierte Abdeckung: [`docs/PRUEFMATRIX.md`](PRUEFMATRIX.md)

Voraussetzung: Debug-APK installiert (`./scripts/android_debug_run.sh` oder
`adb install -r app/build/outputs/apk/debug/app-debug.apk`)

---

## MT-01: Tageserfassung – Today-Screen

- [ ] App öffnen — heutiger Tag wird im Today-Screen angezeigt
- [ ] Morning-Snapshot: "Arbeitsstart erfassen" antippen → Zeitstempel wird
      gespeichert, Feld nicht mehr editierbar
- [ ] Einsatzort eingeben → wird gespeichert
- [ ] Evening-Snapshot: "Arbeitsende erfassen" antippen → Zeitstempel gespeichert
- [ ] "Abschließen" → `confirmedWorkDay = true`, Today-Screen zeigt
      abgeschlossenen Status
- [ ] Zweites Öffnen am gleichen Tag → bestätigter Status bleibt erhalten

---

## MT-02: Eintrag bearbeiten (Edit-Screen)

- [ ] Bestehenden Eintrag per History öffnen → Edit-Sheet erscheint mit
      vorausgefüllten Werten
- [ ] Arbeitszeit ändern (Start/Ende) → Nettozeit in Echtzeit aktualisiert
- [ ] Daytype auf OFF setzen → Bestätigung direkt, keine Arbeitszeit nötig
- [ ] Daytype auf WORK setzen, Arbeitszeit = 0 → Speichern möglich, aber
      Eintrag bleibt unbestätigt
- [ ] Notiz eingeben → wird gespeichert und wieder angezeigt
- [ ] Eintrag löschen → nicht mehr in History sichtbar

---

## MT-03: Reiseabschnitte

- [ ] Edit-Screen öffnen → "Reise hinzufügen" antippen
- [ ] Abfahrt- und Ankunftszeit eingeben → Reisedauer wird berechnet und angezeigt
- [ ] Zweiten Abschnitt (INTERSITE) hinzufügen → beide Abschnitte in korrekter
      Reihenfolge angezeigt
- [ ] Abschnitt löschen → Liste aktualisiert sich korrekt
- [ ] Eintrag speichern → Reisedaten in PDF-Export sichtbar (MT-05 prüfen)

---

## MT-04: Erinnerungssystem

- [ ] Einstellungen öffnen → Morning/Evening-Fenster konfigurieren
- [ ] Gerät auf Morning-Fenster-Zeit stellen (Systemuhr) → Notification erscheint
      innerhalb der WorkManager-Toleranz (~15 min)
- [ ] Notification antippen → Today-Screen öffnet sich mit Morning-Snapshot
- [ ] "Später erinnern" → nach konfigurierter Zeit erscheint erneute Notification
- [ ] Bereits bestätigten Tag: keine Reminder-Notification
- [ ] Gerät neu starten → Erinnerungen funktionieren nach Reboot weiter

**Doze-Simulation** (optional, erfordert ADB):

```bash
adb shell dumpsys deviceidle force-idle
# Abwarten, ob Worker trotzdem ausgeführt wird (kann länger dauern)
adb shell dumpsys deviceidle unforce
```

---

## MT-05: Export

### PDF

- [ ] Export-Screen öffnen → Datumsbereich wählen (min. 3 Tage mit Einträgen)
- [ ] "PDF erstellen" → Export-Dialog erscheint, PDF wird erzeugt
- [ ] PDF öffnen → Kopfzeile mit Profildaten, Tabellenzeilen mit Datum/Zeiten/Reise,
      Zusammenfassung am Ende, Signaturfeld vorhanden
- [ ] PDF mit leerem Datumsbereich (keine Einträge) → Fehlermeldung oder leere
      Tabelle, kein Crash
- [ ] Export bei wenig freiem Speicher simulieren: PDF-Erstellung schlägt sauber fehl

### CSV

- [ ] "CSV erstellen" → Datei wird via Share-Intent angeboten
- [ ] CSV in Tabellenkalkulationsprogramm öffnen → alle Spalten korrekt getrennt,
      Datumsformat lesbar

---

## MT-06: Datenbankmigrationen

Für Release-Upgrades (alte APK → neue APK):

- [ ] Alte APK mit DB-Version < aktuell installieren und Einträge anlegen
      (Testdaten erzeugen)
- [ ] Neue APK per `adb install -r` darüber installieren (kein Datenverlust)
- [ ] App öffnen → Einträge aus alter Version sind noch vorhanden
- [ ] History und Statistik zeigen korrekte Werte
- [ ] Kein Crash beim ersten Start nach Migration

---

## MT-07: Datenbank-Backup

- [ ] App mit niedrigerer DB-Version installieren, dann Update einspielen
- [ ] Nach erstem Start Backup-Verzeichnis prüfen:

```bash
adb shell run-as de.montagezeit.app ls files/db_backups/
# Erwartung: Datei mit Namen vXX_TIMESTAMP.db vorhanden
```

- [ ] Nach drei Migrations-Updates: nur die drei neuesten Backups vorhanden (Pruning)
- [ ] Backup-Datei ist lesbar (gültige SQLite-Datei):

```bash
adb pull $(adb shell run-as de.montagezeit.app pwd)/files/db_backups/<datei>.db /tmp/
sqlite3 /tmp/<datei>.db ".tables"
```

---

## MT-08: Statistik & Übersicht

- [ ] Übersichts-Screen öffnen → aktueller Monat mit Soll/Ist/Saldo angezeigt
- [ ] Woche mit Mix aus WORK, OFF, VACATION, COMP_TIME → Saldo-Berechnung korrekt
- [ ] Überstunden-Anzeige nach mehreren Wochen mit Überstunden → Summe korrekt
- [ ] Verpflegungspauschale: Tag mit Reise und `mealIsArrivalDeparture = true` →
      reduzierter Betrag angezeigt

---

## MT-09: Einstellungen

- [ ] Reminder-Fenster (Morning/Evening Start/Ende) anpassen → Werte nach App-Neustart
      erhalten
- [ ] Reminder deaktivieren → keine Notifications mehr
- [ ] Reminder reaktivieren → Notifications erscheinen wieder

---

## MT-10: Erstinstallation & Smoke-Test

- [ ] App-Daten löschen (`Einstellungen → App → Speicher löschen`) oder
      Neuinstallation
- [ ] App öffnen → kein Crash, Today-Screen lädt
- [ ] Notification-Berechtigung wird angefragt (Android 13+)
- [ ] Erster Eintrag anlegen → wird gespeichert und in History angezeigt
