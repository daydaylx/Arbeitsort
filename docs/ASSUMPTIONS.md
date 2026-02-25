# Annahmen für MontageZeit MVP

**Letztes Update:** 2026-02-25

## Reminder-Fenster
1. **Morning Window (Default):**
   - 06:00 - 13:00
   - Begründung: Deckt typische Arbeitsbeginn-Szenarien ab, fensterbasiert nicht exakt

2. **Evening Window (Default):**
   - 16:00 - 22:30
   - Begründung: Flexibel für verschiedene Arbeitsenden, vor 23:00 Abschluss

3. **Fallback-Reminder (einmalig):**
   - 22:30 Uhr
   - Nur wenn Tag unvollständig (fehlender Morning/Evening Snapshot)
   - Begründung: Letzte Chance für Vervollständigung

## Zeit-Defaults
4. **Arbeitszeit Defaults:**
   - workStart: 08:00
   - workEnd: 19:00
   - breakMinutes: 60
   - Begründung: Typische Vollzeit-Montage, einfach anpassbar

## Tageserfassung
5. **Tagesort (Pflicht):**
   - `dayLocationLabel` muss für jeden Arbeitstag ausgefüllt werden
   - Begründung: Kern-Feature der App - erfassen wo gearbeitet wurde

6. **DayType:**
   - `WORK` - Arbeitstag (Standard)
   - `OFF` - Frei/Urlaub
   - `COMP_TIME` - Überstundenabbau (ganzer Tag)
   - Begründung: Drei Typen decken alle Anwendungsfälle ab

## Datenschutz & Backup
7. **allowBackup:**
   - `false` (AndroidManifest)
   - Begründung: Privacy-first, keine automatische Cloud-Sync. User muss bewusst exportieren.

8. **Daten-Speicherung:**
   - Alle Daten bleiben lokal in Room-Datenbank
   - Optional: Export als CSV
   - Begründung: Privacy-First, volle Kontrolle über eigene Daten

## Technische Entscheidungen
9. **Single-Module Architektur:**
   - Keine Multi-Module-Aufteilung für MVP
   - Begründung: Overkill für eine App, KISS-Prinzip

10. **Kotlin Gradle DSL:**
    - build.gradle.kts statt .gradle (Groovy)
    - Begründung: Type-safe, moderne Kotlin-Stack Integration

11. **Dependency Injection:**
    - Hilt mit `@HiltAndroidApp`
    - Begründung: Standard-Android-DI, gut getestet, Type-safe

12. **Notification Actions:**
    - Direkt Action-Buttons in Notification ("Check-in", "Ohne Standort", "Später")
    - Begründung: Maximale Geschwindigkeit (≤10 Sekunden Ziel)

## Permissions
13. **POST_NOTIFICATIONS:**
    - Erforderlich für Android 13+
    - Nur für Reminder

14. **RECEIVE_BOOT_COMPLETED:**
    - Erforderlich für Reboot-Resilienz der Reminder

15. **FOREGROUND_SERVICE:**
    - Erforderlich für Check-In Actions aus Notifications

## Single-User Annahmen
16. **Nutzer:**
    - 1 Primary User (privat)
    - 1 Device (kein Sync/Cloud)
    - Begründung: MVP-Scope, keine Komplexität durch Multi-Tenancy

## Scope-Grenzen (NICHT im MVP)
- Keine Cloud-Sync
- Keine Kartendarstellung
- Keine Spesen-Verwaltung
- Keine Arbeitszeitrecht-Validierung
- Keine komplexen Reports (nur CSV)
- Kein Background-Location-Tracking (manuelles Check-in System)
- Kein Import/Restore (nur Export)
- Keine GPS-basierte Standort-Erfassung

## Änderungen
Alle Änderungen an diesen Annahmen müssen dokumentiert werden mit Datum und Begründung.
