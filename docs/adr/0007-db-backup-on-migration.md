# ADR-0007: Automatisches DB-Backup vor Migrationen

**Status:** Akzeptiert
**Datum:** 2026-06-13

## Kontext

Room-Migrationen sind destruktiv: Falsch implementierte Migrationen können
Nutzerdaten unwiederbringlich löschen. Da die App offline-first ist (ADR-0001),
gibt es kein Cloud-Recovery. Nutzer haben keine einfache Möglichkeit, den
DB-Zustand vor einer Migration zu sichern.

## Entscheidung

`DatabaseBackupManager.backupIfVersionMismatch()` wird beim App-Start via
`DatabaseModule` aufgerufen, bevor Room die Datenbank öffnet:

- Liest die aktuelle DB-Version direkt von der Datei (ohne Room zu öffnen)
- Liegt die Version unter `AppDatabase.DATABASE_VERSION`, wird die DB
  nach `files/db_backups/v{version}_{timestamp}.db` kopiert
  (inkl. WAL- und SHM-Companion-Dateien)
- Maximal 3 Backups werden vorgehalten (`MAX_BACKUPS = 3`), ältere werden gelöscht
- Schlägt das Backup fehl, wird stillschweigend weitergemacht — Room öffnet
  trotzdem (kein hard-fail)

## Begründung

- **Minimales Risiko**: Backups passieren automatisch, ohne Nutzerinteraktion.
- **Bounded Storage**: Maximal 3 Backups verhindern unkontrollierten Speicherwachstum.
- **Fail-safe**: Stilles Versagen ist sicherer als ein App-Crash, der den Nutzer
  aus der App aussperrt, wenn Backup-Storage knapp ist.

## Konsequenzen

- Backups liegen in `files/db_backups/` — nicht in `external storage` und nicht
  Cloud-synchronisiert (konsistent mit ADR-0001).
- Recovery erfordert manuelle Aktion (z. B. `adb pull`) — kein In-App-Restore.
- `DatabaseBackupManagerTest` verifiziert Backup-Erstellung und Pruning-Logik.
- Jede neue DB-Version löst automatisch ein Backup aus — auch Minor-Migrationen.
