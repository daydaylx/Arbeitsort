# UI QA-Checklist - Smoke-Tests

## Build & Installation

- [ ] Projekt erfolgreich bauen (`./gradlew assembleDebug`)
- [ ] APK generiert (`app/build/outputs/apk/debug/app-debug.apk`)
- [ ] APK auf Gerät installieren

## App-Start

- [ ] App öffnet ohne Absturz
- [ ] Startbildschirm lädt korrekt
- [ ] Bottom Navigation wird angezeigt (3 Tabs: Heute, Verlauf, Einstellungen)

## Heute-Screen

### Loading States
- [ ] Lade-Indikator wird beim ersten Start angezeigt
- [ ] "Standort wird ermittelt..." wird während Standortabfrage angezeigt
- [ ] "Ohne Standort speichern" Button funktioniert

### Empty State
- [ ] Statuskarte zeigt "Noch kein Check-in" an
- [ ] Buttons sind aktiviert

### Check-In Fluss
- [ ] "Morgens Check-in" Button funktioniert
- [ ] Standort-Abfrage wird gestartet
- [ ] Bei Standort-Fehler: Fehlermeldung wird angezeigt
- [ ] Bei Standort-Fehler: "Erneut versuchen" funktioniert
- [ ] Bei Standort-Fehler: "Ohne Standort speichern" funktioniert
- [ ] Nach erfolgreichem Check-in: Button ist deaktiviert und zeigt Zeit an
- [ ] "Abends Check-in" Button funktioniert analog

### Statuskarte
- [ ] Datum wird korrekt angezeigt (deutsches Format)
- [ ] Tagtyp-Icon wird angezeigt (Arbeitstag/Frei)
- [ ] Standort-Status wird angezeigt
- [ ] needsReview-Flag wird mit roter Warnung angezeigt

### Manuelle Bearbeitung
- [ ] "Manuell bearbeiten" Button öffnet Edit-Sheet
- [ ] Sheet wird korrekt angezeigt
- [ ] Sheet kann geschlossen werden

## Verlauf-Screen

### Loading & Empty States
- [ ] Lade-Indikator wird beim ersten Start angezeigt
- [ ] "Keine Einträge vorhanden" wird angezeigt wenn keine Daten

### Wochen-Gruppierung
- [ ] Einträge werden nach Kalenderwochen gruppiert
- [ ] KW-Header zeigt korrekte Woche an
- [ ] Jahr wird angezeigt wenn nicht aktuelles Jahr

### Entry Cards
- [ ] Datum wird korrekt formatiert (z.B. "Mo, 05.01.")
- [ ] Tagtyp-Icon wird angezeigt
- [ ] Standort-Status-Icons werden angezeigt
- [ ] needsReview-Flag zeigt Warn-Icon
- [ ] Tap auf Eintrag öffnet Edit-Sheet

## Edit Entry Sheet

### Formular-Layout
- [ ] Sheet wird vollständig angezeigt
- [ ] Scroll funktioniert bei langem Inhalt
- [ ] Alle Felder sind sichtbar und zugänglich

### Tagtyp-Auswahl
- [ ] FilterChips für "Arbeitstag" und "Frei" funktionieren
- [ ] Auswahl wird visuell hervorgehoben

### Arbeitszeiten
- [ ] Zeit-Picker für Arbeitsbeginn funktioniert
- [ ] Zeit-Picker für Arbeitsende funktioniert
- [ ] Slider für Pause funktioniert (0-120 min)
- [ ] Zeiten werden korrekt angezeigt

### Standort-Labels
- [ ] Morgens-Label kann bearbeitet werden
- [ ] Abends-Label kann bearbeitet werden
- [ ] Optional (leer lassen möglich)

### Notiz
- [ ] Notiz-Feld ist verfügbar
- [ ] Mehrzeiliger Eingabe funktioniert
- [ ] Optional (leer lassen möglich)

### Speichern
- [ ] "Speichern" Button speichert Änderungen
- [ "Überprüfungsflag zurücksetzen" funktioniert
- [ ] Nach Speichern: Erfolgsmeldung wird angezeigt
- [ ] Sheet schließt sich nach Erfolg

### Borderzone-Confirm
- [ ] Bei Grenzzone: Bestätigungs-Dialog wird angezeigt
- [ ] "Ja, speichern" speichert trotz Warnung
- [ ] "Abbrechen" bricht Speichervorgang ab

## Einstellungen-Screen

### Arbeitszeiten
- [ ] Standardwerte werden angezeigt
- [ ] Werte sind lesbar (nicht bearbeitbar)

### Erinnerungs-Fenster
- [ ] Morgen-Erinnerung: Zeitraum kann angepasst werden
- [ ] Morgen-Erinnerung: Startzeit-Picker funktioniert
- [ ] Morgen-Erinnerung: Endzeit-Picker funktioniert
- [ ] Abend-Erinnerung: Zeitraum kann angepasst werden
- [ ] Änderungen werden gespeichert

### Standort-Einstellungen
- [ ] Radius-Slider funktioniert (1-50 km)
- [ ] Radius-Wert wird aktual angezeigt
- [ ] Standortmodus-Radio-Buttons funktionieren
- [ ] Nur "Nur beim Check-in" ist im MVP verfügbar

### Export
- [ ] "Exportieren" Button startet Export
- [ ] Lade-Indikator wird angezeigt
- [ ] Export erfolgreich: Dialog wird angezeigt
- [ ] Export erfolgreich: "Kopieren" funktioniert (TODO)
- [ ] Bei Fehler: Fehlermeldung wird angezeigt

## Navigation

### Bottom Navigation
- [ ] Alle 3 Tabs sind sichtbar
- [ ] Icons und Labels werden angezeigt
- [ ] Tab-Wechsel funktioniert
- [ ] Aktiver Tab ist hervorgehoben
- [ ] Tab-Wechsel speichert State

### Modal Bottom Sheet
- [ ] Sheet wird über Inhalt gelegt
- [ ] Sheet kann durch Swipe-Down geschlossen werden
- [ ] Sheet kann durch Back-Button geschlossen werden
- [ ] Sheet kann durch Tip außerhalb geschlossen werden

## Accessibility & Usability

### Touch Targets
- [ ] Alle Buttons haben min. 48dp Touch-Target
- [ ] Check-In Buttons sind groß (64dp Höhe)
- [ ] Navigation Items sind gut klickbar

### 1-Hand Bedienung
- [ ] Wichtige Buttons sind unten positioniert
- [ ] Check-In Buttons sind leicht erreichbar
- [ ] Bottom Navigation ist leicht erreichbar

### Textgrößen
- [ ] Text ist lesbar (nicht zu klein)
- [ ] Wichtige Informationen sind hervorgehoben
- [ ] Fehlermeldungen sind deutlich sichtbar

### Feedback
- [ ] Lade-Indikatoren geben Feedback
- [ ] Erfolgsmeldungen geben Feedback
- [ ] Fehlermeldungen geben Feedback
- [ ] Button-Feedback (Ripple, Farbwechsel)

## Performance

- [ ] App startet in < 3 Sekunden
- [ ] Tab-Wechsel ist flüssig
- [ ] Sheet öffnet ohne Verzögerung
- [ ] Listen scrollen flüssig
- [ ] Keine sichtbaren Lags oder Ruckler

## Stability

- [ ] App stürzt nicht bei normaler Nutzung ab
- [ ] App stürzt nicht bei Screen-Rotation ab
- [ ] App stürkt nicht bei schnellem Tab-Wechsel ab
- [ ] App stürzt nicht bei schnellem Sheet-Öffnen ab
- [ ] Memory Leaks nicht offensichtlich (längere Nutzung)

## Edge Cases

- [ ] App startet ohne Internet-Verbindung
- [ ] App startet ohne GPS
- [ ] App startet nach App-Kill
- [ ] App startet nach Reboot
- [ ] Lange Listen im Verlauf werden korrekt angezeigt
- [ ] Spezielle Zeichen in Notizen werden korrekt behandelt

## Known Issues

- [ ] Clipboard-Kopieren im Export-Dialog muss noch implementiert werden

## Hinweise

- Alle Screens sind mit Material3 Design implementiert
- State-Handling ist sauber mit StateFlow
- ViewModels nutzen Hilt für Dependency Injection
- Navigation ist mit Jetpack Navigation Compose implementiert
- Alle Touch-Targets entsprechen Accessibility-Guidelines (min. 48dp)
