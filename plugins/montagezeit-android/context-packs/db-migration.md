# Context-Pack: Datenbankmigrationen

Für Aufgaben rund um Room-Schema-Änderungen, Migrationen und DB-Backup.

## Zuerst lesen

1. `docs/DATENMODELL.md` Abschnitt 5 — Migrations-Highlights
2. `docs/adr/0003-travel-normalization.md` — v13→14 als Referenz für komplexe Migration
3. `docs/adr/0007-db-backup-on-migration.md` — Backup-Mechanismus
4. `docs/adr/0008-schulung-lehrgang-deprecation.md` — Enum-Deprecation als Muster

## Schlüsseldateien

```
app/src/main/java/de/montagezeit/app/
├── data/local/database/
│   ├── AppDatabase.kt                    ← Schema-Version, alle MIGRATION_X_Y-Objekte
│   └── DatabaseBackupManager.kt          ← Backup vor Migration (via DatabaseModule)
├── data/local/converters/
│   ├── LocalDateConverter.kt             ← Typ-Konverter für LocalDate (bei neuen Feldern anpassen)
│   └── LocalTimeConverter.kt             ← Typ-Konverter für LocalTime
└── di/
    └── DatabaseModule.kt                 ← Ruft backupIfVersionMismatch() vor DB-Open auf

app/src/test/java/de/montagezeit/app/
└── data/local/database/
    ├── AppDatabaseMigrationTest.kt       ← Robolectric-Tests für alle Migrationen
    └── DatabaseBackupManagerTest.kt      ← Tests für Backup-Logik

app/src/androidTest/java/de/montagezeit/app/
└── data/local/database/
    └── AppDatabaseSchemaMigrationTest.kt ← Instrumented Schema-Verifikation
```

## Checkliste: Neue Migration

1. `AppDatabase.kt`: `DATABASE_VERSION` um 1 erhöhen
2. `MIGRATION_X_Y`-Objekt anlegen (nach bestehendem Muster)
3. In `AppDatabase.Builder` per `.addMigrations(MIGRATION_X_Y)` registrieren
4. `AppDatabaseMigrationTest`: Test für v(X)→v(Y) ergänzen
5. `docs/DATENMODELL.md` Abschnitt 5 (Migrations-Highlights) aktualisieren,
   wenn die Migration strukturell bedeutsam ist
6. `docs/ARCHITECTURE.md` Section 2.1 Migrationsreferenz aktualisieren

## Invarianten

- **Jede Migration braucht einen Test**: Kein Merge ohne Coverage in
  `AppDatabaseMigrationTest`. Die Migration muss Datenvollständigkeit und
  korrekte Typ-Konvertierung prüfen.
- **Backup-Manager wird automatisch ausgelöst**: `DatabaseModule` ruft
  `backupIfVersionMismatch()` auf — kein manuelles Triggern nötig.
- **Kein destructive fallback**: `fallbackToDestructiveMigration()` ist nicht
  aktiviert. Fehlende Migrationen crashen die App — das ist beabsichtigt.
- **WAL-Modus**: Die DB läuft im WAL-Modus. Backup-Manager kopiert auch
  `.db-wal` und `.db-shm` Companion-Dateien.

## Verifikation

```bash
# Migrations-Unit-Tests (Robolectric)
./gradlew :app:testDebugUnitTest \
  --tests "de.montagezeit.app.data.local.database.AppDatabaseMigrationTest"

# Backup-Manager-Tests
./gradlew :app:testDebugUnitTest \
  --tests "de.montagezeit.app.data.local.database.DatabaseBackupManagerTest"

# Instrumented Schema-Test
./gradlew connectedDebugAndroidTest \
  --tests "de.montagezeit.app.data.local.database.AppDatabaseSchemaMigrationTest"
```

Manuelle Prüfpfade: `docs/MANUELLE_TESTS.md` → MT-06, MT-07
