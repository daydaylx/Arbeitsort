# Annahmen für MontageZeit MVP

**Letztes Update:** 2026-01-05

## Standort & Geografie
1. **Leipzig Zentrum (fester Referenzpunkt):**
   - Latitude: 51.340
   - Longitude: 12.374
   - Begründung: Zentralster Punkt, ausreichend für 30km-Radius-Check

2. **Leipzig-Radius (Default):**
   - 30 km (konfigurierbar in Settings)
   - Begründung: Deckt Leipziger Umland ab, nicht zu groß für präzise Auswertung

3. **Accuracy Threshold:**
   - 3000m (3 km)
   - Bei schlechterer Genauigkeit → `locationStatus=LOW_ACCURACY` + `needsReview=true`
   - Begründung: WLAN/Cell-Tower kann ungenau sein; besser Warnung als falsche Zuordnung

4. **Grenzzone:**
   - ±2 km um Radius (28-32 km bei 30km Default)
   - Erzwingt manuelle Bestätigung
   - Begründung: GPS-Jitter kann zu Springen zwischen Leipzig/Außerhalb führen

## Reminder-Fenster
5. **Morning Window (Default):**
   - 06:00 - 13:00
   - Begründung: Deckt typische Arbeitsbeginn-Szenarien ab, fensterbasiert nicht exakt

6. **Evening Window (Default):**
   - 16:00 - 22:30
   - Begründung: Flexibel für verschiedene Arbeitsenden, vor 23:00 Abschluss

7. **Fallback-Reminder (einmalig):**
   - 22:30 Uhr
   - Nur wenn Tag unvollständig (fehlender Morning/Evening Snapshot)
   - Begründung: Letzte Chance für Vervollständigung

## Zeit-Defaults
8. **Arbeitszeit Defaults:**
   - workStart: 08:00
   - workEnd: 19:00
   - breakMinutes: 60
   - Begründung: Typische Vollzeit-Montage, einfach anpassbar

## Datenschutz & Backup
9. **allowBackup:**
   - `false` (AndroidManifest)
   - Begründung: Privacy-first, keine automatische Cloud-Sync. User muss bewusst exportieren.

10. **Standort-Speicherung:**
    - Koordinaten werden gespeichert (Lat/Lon/Accuracy)
    - Optional später: Setting "nur Label speichern"
    - Begründung: Auswertung & Debugging, aber lokal bleiben

## Technische Entscheidungen
11. **Single-Module Architektur:**
    - Keine Multi-Module-Aufteilung für MVP
    - Begründung: Overkill für eine App, KISS-Prinzip

12. **Kotlin Gradle DSL:**
    - build.gradle.kts statt .gradle (Groovy)
    - Begründung: Type-safe, moderne Kotlin-Stack Integration

13. **Dependency Injection:**
    - Manuelles DI im MVP (kein Hilt/Koin)
    - Begründung: Einfach, keine zusätzliche Lernkurve für Single-User App

14. **Location Provider:**
    - Fused Location Provider (Play Services)
    - Fallback auf Android Location Manager wenn Play Services nicht verfügbar
    - Timeout: 15 Sekunden
    - Begründung: Beste Accuracy, aber robust ohne

15. **Notification Actions:**
    - Direkt Action-Buttons in Notification ("Check-in", "Ohne Standort")
    - Begründung: Maximale Geschwindigkeit (≤10 Sekunden Ziel)

## Permissions
16. **POST_NOTIFICATIONS:**
    - Erforderlich für Android 13+
    - Nur für Reminder

17. **ACCESS_COARSE_LOCATION:**
    - Minimal-Permission für Location-Snapshots
    - Reicht für Radius-Check

18. **ACCESS_FINE_LOCATION:**
    - Optional (nicht im MVP, aber vorbereitet)
    - Nur wenn explizit aktiviert

## Single-User Annahmen
19. **Nutzer:**
    - 1 Primary User (privat)
    - 1 Device (kein Sync/Cloud)
    - Begründung: MVP-Scope, keine Komplexität durch Multi-Tenancy

20. **DayType:**
    - WORK vs OFF (nicht mehr für MVP)
    - SICK/HOLIDAY optional später
    - Begründung: Minimale Komplexität, Haupt-Fokus: Arbeitstage loggen

## Scope-Grenzen (NICHT im MVP)
- Keine Cloud-Sync
- Keine Kartendarstellung
- Keine Spesen-Verwaltung
- Keine Arbeitszeitrecht-Validierung
- Keine komplexen Reports (nur CSV)
- Kein Background-Location-Tracking
- Kein Import/Restore (nur Export)

## Änderungen
Alle Änderungen an diesen Annahmen müssen dokumentiert werden mit Datum und Begründung.
