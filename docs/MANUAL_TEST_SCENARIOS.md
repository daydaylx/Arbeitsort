# Manual Test Scenarios – MontageZeit

Kurze manuelle Prüfszenarien für Bereiche ohne vollständige automatisierte
Abdeckung. Format: Ziel / Schritte / Erwartetes Ergebnis / Relevante Risiken.
Voraussetzung: Debug-APK installiert
(`./scripts/android_debug_run.sh` oder `adb install -r app/build/outputs/apk/debug/app-debug.apk`).

---

## Daily Check-in Arbeit

**Ziel:** Ein Arbeitstag wird über den primären Today-Pfad korrekt abgeschlossen.

**Schritte:**

1. App öffnen, Today-Screen zeigt den heutigen Tag.
2. „Einchecken (Arbeit)" antippen, Tagesort eingeben.
3. Optional: An-/Abreisetag, Frühstück enthalten setzen.
4. Bestätigen.

**Erwartetes Ergebnis:** `dayType = WORK`, `confirmedWorkDay = true`,
Tagesort gespeichert, Verpflegungspauschale (falls Aktivität vorhanden)
berechnet.

**Risiken:** Ohne positive Arbeits-/Reisezeit bleibt der Tag unbestätigt und
fehlt in Statistik/Export — das ist beabsichtigtes Verhalten, kein Bug.

---

## Today frei

**Ziel:** „Heute frei" markiert den Tag korrekt als abgeschlossen.

**Schritte:**

1. Today-Screen öffnen.
2. „Heute frei" antippen.

**Erwartetes Ergebnis:** `dayType = OFF`, sofort terminal bestätigt, keine
Verpflegungspauschale, keine weiteren Daily-Reminder für diesen Tag.

**Risiken:** —

---

## Leipzig-Auslöse = 0 €

**Ziel:** Verifizieren, welche Leipzig-Eingaben aktuell tatsächlich 0 €
Auslöse ergeben (realer Codezustand, nicht Wunschverhalten).

**Schritte:**

1. Daily Check-in mit Tagesort exakt `Leipzig` durchführen.
2. Wiederholen mit `leipzig` (Leerzeichen) und `LEIPZIG` (Großschreibung).
3. Wiederholen mit `Leipzig Zentrum`, `Leipzig, Kunde XY`, `Leipzig-Lindenau`.

**Erwartetes Ergebnis:** Fall 1 und 2 (exaktes „Leipzig", trim/lowercase-
toleriert) ergeben **0 € Auslöse**. Fall 3 (Varianten mit Zusatztext) ergeben
**aktuell nicht** 0 € — das ist der bekannte, offene
[Issue #50](https://github.com/daydaylx/Arbeitsort/issues/50). Dieses Szenario
dient der Verifikation des Ist-Zustands, nicht dem Nachweis eines Fixes.

**Risiken:** Nicht mit Issue #50 verwechseln, dass hier ein Fix erwartet wird
— in diesem Auftrag/Kontext wird der Bug nur dokumentiert, nicht behoben.

---

## Nicht-Leipzig-Auslöse = normale Berechnung

**Ziel:** Verpflegungspauschale außerhalb Leipzig funktioniert normal.

**Schritte:**

1. Daily Check-in mit Tagesort z. B. `Dresden` durchführen, ohne
   An-/Abreisetag, ohne Frühstück.
2. Wiederholen mit An-/Abreisetag aktiv.
3. Wiederholen mit Frühstück enthalten aktiv.

**Erwartetes Ergebnis:** Normalbetrag (`BASE_NORMAL_CENTS`), reduzierter
Betrag bei An-/Abreisetag (`BASE_ARRIVAL_DEPARTURE_CENTS`), Abzug bei
Frühstück (`BREAKFAST_DEDUCTION_CENTS`).

**Risiken:** —

---

## History bearbeiten

**Ziel:** Bestehender Eintrag lässt sich über das Edit-Sheet korrekt ändern.

**Schritte:**

1. History öffnen, bestehenden Tag antippen.
2. Arbeitszeit ändern, Notiz hinzufügen, speichern.

**Erwartetes Ergebnis:** Änderungen persistiert, Nettozeit korrekt neu
berechnet, Bestätigungsstatus gemäß Bestätigungsregel (positive Arbeits-/
Reisezeit) aktualisiert.

**Risiken:** `WORK` mit `0` Netto-Arbeitszeit und ohne Reise bleibt bewusst
unbestätigt.

---

## Batch Edit

**Ziel:** Datumsbereich-Bearbeitung wirkt korrekt auf mehrere Tage.

**Schritte:**

1. History öffnen, Batch-Edit für einen Datumsbereich (z. B. 5 Tage) starten.
2. Gemeinsames Feld ändern (z. B. Notiz oder DayType), anwenden.

**Erwartetes Ergebnis:** Alle betroffenen Tage im Bereich aktualisiert, keine
Tage außerhalb des Bereichs verändert.

**Risiken:** Batch-Edit läuft nicht als atomarer DB-Range-UseCase, sondern
seriell über das Repository (dokumentierte Einschränkung) — bei sehr großen
Bereichen auf Teilfehler achten.

---

## Reminder Morning/Evening/Fallback/Daily

**Ziel:** Alle vier Reminder-Typen lösen im jeweiligen Fenster korrekt aus.

**Schritte:**

1. In Settings Morning-/Evening-Fenster auf einen nahen Zeitraum stellen.
2. Warten bzw. Systemuhr in das Fenster stellen.
3. Für Fallback/Daily die konfigurierte Uhrzeit erreichen lassen.

**Erwartetes Ergebnis:** Morning/Evening erscheinen fensterbasiert (~15 min
Toleranz), Fallback/Daily je einmal täglich; bereits bestätigte Tage und
automatische Nicht-Arbeitstage (Wochenende/Feiertag ohne manuellen Override)
erhalten keine Notification.

**Risiken:** Doze/Hersteller-Energiesparen kann Ausführung verzögern — das
ist erwartetes, dokumentiertes Verhalten, kein Bug.

---

## Später-Aktionen

**Ziel:** Snooze-Aktionen (`10 Min`, `+1 h`, `+2 h`) funktionieren pro
Reminder-Typ.

**Schritte:**

1. Auf eine Morning-/Evening-/Fallback-Notification eine Später-Aktion
   antippen.
2. Prüfen, dass die Notification verschwindet.
3. Nach Ablauf der Snooze-Zeit prüfen, ob die Notification erneut erscheint
   (sofern der Tag zwischenzeitlich nicht abgeschlossen wurde).

**Erwartetes Ergebnis:** Nur der betroffene Reminder-Typ wird neu geplant;
bereits abgeschlossene Tage zeigen nach Snooze-Ablauf keine Notification mehr.

**Risiken:** —

---

## Reboot/App-Update-Rescheduling

**Ziel:** Reminder funktionieren nach Geräte-Neustart und App-Update weiter.

**Schritte:**

1. Gerät neu starten, App nicht manuell öffnen.
2. Warten, bis ein Reminder-Fenster erreicht wird.
3. Alternativ: Debug-APK über bestehende Installation aktualisieren
   (höherer `versionCode`).

**Erwartetes Ergebnis:** `BootReceiver` reagiert auf `BOOT_COMPLETED` bzw.
`MY_PACKAGE_REPLACED` und plant alle Reminder neu; Notifications erscheinen
wie vor dem Neustart/Update.

**Risiken:** Bekannte Doku-Lücke (Audit O04) — der reale `scheduleAll()`-Pfad
ist nur teilweise automatisiert verifiziert, hier lohnt manuelle Prüfung
besonders.

---

## Zeit-/Zeitzonenänderung

**Ziel:** `TimeChangeReceiver` reagiert korrekt auf manuelle Zeit-/
Zeitzonenänderung.

**Schritte:**

1. Systemzeit oder Zeitzone manuell ändern.
2. Prüfen, dass Reminder-Fenster relativ zur neuen Zeit korrekt neu geplant
   werden.

**Erwartetes Ergebnis:** Keine doppelten oder fehlenden Notifications durch
die Zeitänderung.

**Risiken:** —

---

## PDF Export

**Ziel:** PDF-Export liefert korrekten, vollständigen Inhalt.

**Schritte:**

1. Export-Screen öffnen, Zeitraum mit mehreren gemischten Tagen wählen
   (WORK/OFF/COMP_TIME/SCHULUNG/LEHRGANG, mit und ohne Reise).
2. PDF erstellen und öffnen.

**Erwartetes Ergebnis:** Kopfzeile mit PDF-Stammdaten, Tabellenzeilen mit
Datum/Zeiten/Reise, korrekte Zusammenfassung, Signaturfeld vorhanden. Bei

> 180 Einträgen (`MAX_ENTRIES_PER_PDF`) erscheint eine kontrollierte
> Fehlermeldung statt stiller Kürzung.

**Risiken:** Storage-Check (< 5 MB frei) muss sauber fehlschlagen, nicht
crashen.

---

## CSV Export

**Ziel:** CSV-Export ist korrekt kodiert und sicher gegen Formel-Injection.

**Schritte:**

1. Export-Screen öffnen, CSV erstellen.
2. In Tabellenkalkulation öffnen; Tagesort mit `;`, Anführungszeichen oder
   führendem `=` testen.

**Erwartetes Ergebnis:** Spalten korrekt getrennt, Sonderzeichen korrekt
gequotet, Formel-Präfixe (`=+-@`) neutralisiert.

**Risiken:** —

---

## APK Update ohne Datenverlust

**Ziel:** Ein normales Update über die bestehende Installation behält alle
lokalen Room-/DataStore-Daten (Schema-Version 16, kein Backup-Manager
vorhanden auf diesem Stand).

**Schritte:**

1. Ältere Debug-APK installieren, mehrere Testeinträge anlegen.
2. Neue Debug-APK mit höherem `versionCode` über `adb install -r` installieren.

**Erwartetes Ergebnis:** App öffnet ohne Crash, alle vorherigen Einträge und
Statistiken sind vorhanden.

**Risiken:** Bei Schema-Änderungen ohne passende Migration crasht die App
kontrolliert (kein `fallbackToDestructiveMigration`) — das ist gewolltes
Verhalten, kein stiller Datenverlust.

---

## Notification Permission

**Ziel:** `POST_NOTIFICATIONS`-Anfrage (Android 13+) funktioniert korrekt.

**Schritte:**

1. App auf Android 13+ frisch installieren/App-Daten löschen.
2. App öffnen.

**Erwartetes Ergebnis:** Notification-Permission-Dialog erscheint; bei
Ablehnung funktioniert die App weiter, nur ohne sichtbare Reminder-
Notifications (kein Crash, kein Blocker für Check-in).

**Risiken:** —

---

## Release-APK Update mit gleicher Signatur

**Ziel:** Release-Build-Update funktioniert wie ein normales App-Update.

**Schritte:**

1. `keystore.properties` lokal anlegen (nicht committen), `./gradlew assembleRelease`.
2. Signierte Release-APK installieren, Testdaten anlegen.
3. Neue Version bauen (höherer `versionCode`), mit gleicher Signatur über
   `adb install -r` aktualisieren.

**Erwartetes Ergebnis:** Update erfolgreich, Daten bleiben erhalten,
Reminder werden nach dem Update neu geplant (`MY_PACKAGE_REPLACED`).

**Risiken:** Ohne `keystore.properties` entsteht nur eine unsignierte,
nicht update-fähige APK. Ein Install mit anderer Signatur löscht wegen
`allowBackup=false` alle lokalen Daten.
