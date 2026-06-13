# ADR-0001: Offline-First, kein Cloud-Sync

**Status:** Akzeptiert
**Datum:** 2026-06-13

## Kontext

MontageZeit erfasst sensible Arbeitszeitdaten von Monteuren. Früh stand die Frage, ob
Daten in einer Cloud-Datenbank oder lokal gespeichert werden sollen.

## Entscheidung

Alle Nutzerdaten verbleiben ausschließlich auf dem Gerät. Kein Cloud-Sync, kein
Backend, kein Nutzeraccount.

- `android:allowBackup="false"` im Manifest
- `android:dataExtractionRules="@xml/data_extraction_rules"` schließt alle Domains
  (Database, SharedPreferences, Files) von Cloud-Backup und Geräteübertragung aus
- Kein Netzwerk-Permission im Manifest
- Export (PDF, CSV) über Android Share-Intent — der Nutzer entscheidet selbst über Ablageort

## Begründung

- **Datenschutz by Design**: Arbeitszeitdaten sind personenbezogen und gehören nicht
  automatisch in externe Systeme.
- **Kein Server-Overhead**: Keine Infrastruktur, keine Authentifizierung, keine
  Datenschutzgrundverordnung-Compliance für Cloud-Speicher zu implementieren.
- **Offline-Zuverlässigkeit**: Monteure arbeiten oft ohne stabile Internetverbindung.
  Die App muss zu 100 % offline funktionieren.

## Konsequenzen

- Geräteverlust bedeutet Datenverlust — kein Recovery über Cloud möglich. Nutzer
  sind verantwortlich für eigene Backups via Export.
- Kein Geräte-übergreifendes Arbeiten möglich (Bewusste Nicht-Anforderung, vgl. `CONCEPT.md`).
- Kein automatisches Backup über Google-Backup-Service.
