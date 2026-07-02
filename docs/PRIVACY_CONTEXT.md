# Privacy Context – MontageZeit

Verbindliche Datenschutz-/Permission-Grenzen für Agenten und Contributor.
Bei jedem Widerspruch zwischen dieser Datei und Code gilt der Code — dann
diese Datei sofort korrigieren.

---

## Grundprinzipien

- **Offline-first.** Keine Cloud, kein Backend, kein Login, kein Nutzerkonto.
- **Keine Standortberechtigung, kein GPS.** Der Tagesort (`dayLocationLabel`
  in `WorkEntry`) ist und bleibt **manueller Freitext**. Es gibt keine
  `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`-Permission und keine
  GPS-/Geokoordinaten-Erfassung im Code.
- **Alle Daten bleiben lokal** in Room (`AppDatabase`) und DataStore
  (`data/preferences/`), bis der Nutzer sie **explizit** exportiert oder teilt.
- **Export ist immer eine bewusste Nutzeraktion** (PDF/CSV über den
  Export-Screen) — kein automatischer, stiller oder periodischer Export.

## Deklarierte Berechtigungen (`AndroidManifest.xml`)

| Permission                       | Zweck                                        |
| -------------------------------- | -------------------------------------------- |
| `POST_NOTIFICATIONS`             | Reminder-Notifications                       |
| `RECEIVE_BOOT_COMPLETED`         | Reboot-Resilienz für Reminder-Scheduling     |
| `FOREGROUND_SERVICE`             | Für `CheckInActionService`                   |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Foreground-Service-Subtyp `check_in_actions` |

Zusätzlich `<queries>` für: PDF öffnen/teilen (`ACTION_VIEW`/`ACTION_SEND`,
MIME `application/pdf`), Akku-Optimierungseinstellungen öffnen
(`IGNORE_BATTERY_OPTIMIZATION_SETTINGS`).

**Es gibt keine weiteren Permissions.** Jede neue `uses-permission` oder neue
exportierte Component ist sicherheitsrelevant und muss im PR explizit
begründet werden (siehe [`docs/AGENT_CONTEXT_PACKS.md`](AGENT_CONTEXT_PACKS.md)
Pack „Privacy / Permissions / Manifest").

## Backup & Datenextraktion

- `android:allowBackup="false"` in `AndroidManifest.xml` — Daten werden
  **nicht** über Android-Auto-Backup gesichert.
- `android:dataExtractionRules="@xml/data_extraction_rules"` und
  `android:fullBackupContent="@xml/backup_rules"` schließen alle Domains
  (Database, SharedPreferences, Files) explizit von Cloud-Backup und
  Geräteübertragung aus.
- Konsequenz: Uninstall oder Neuinstallation mit anderer Signatur löscht
  lokale Daten unwiderruflich. Ein normales Update (gleiche `applicationId`,
  höherer `versionCode`, gleiche Signatur) behält die Daten.

## Export-Mechanismus

- Export läuft über `androidx.core.content.FileProvider`
  (Authority `${applicationId}.fileprovider`, `exported="false"`,
  `grantUriPermissions="true"`, Pfade in `@xml/file_paths`).
- Kein direkter Dateisystemzugriff außerhalb des App-Sandbox-Verzeichnisses.

## Lokales Logging

- `data/logging/RingBufferLogger.kt` schreibt ausschließlich lokal nach
  `context.filesDir/logs/debug.log`.
- Rotation bei ca. 2 MB (`maxFileSize = 2 * 1024 * 1024L`), behält die
  neuesten ~50 % der Zeilen.
- **Kein Cloud-Upload, kein Netzwerkversand.** Schreibfehler werden
  still geloggt (`printStackTrace()`), lassen die App nicht abstürzen.

## Was NICHT committet werden darf

Bereits über `.gitignore` abgedeckt — bei jeder Änderung respektieren, nicht
umgehen:

- Keystores/Signing-Secrets: `keystore.properties`, `*.jks`, `*.keystore`
- `.env`, `.env.*`
- Lokale Datenbanken/Exporte: `*.db`, `*.sqlite`, `app/exports/`
- Logs/Debug-Artefakte: `*.log`, `logs/`, `debug_artifacts/`
- Lokale Assistenten-/IDE-Zustände: `.claude/`, `.clinerules/`, `.kilo/`,
  `.vscode/settings.json` u. Ä.

## No-Gos (verbindlich)

- Keine Standortberechtigung, kein GPS, keine Geokoordinaten-Erfassung.
- Kein Cloud-Sync, kein Backend, kein Login, kein Nutzerkonto.
- `android:allowBackup` bleibt `false`.
- Keine neue Permission oder exportierte Component ohne explizite
  Sicherheitsbegründung im PR.
- Keine Keystores, Secrets, Logs oder lokalen Tooling-Dateien committen.
- Kein automatischer/stiller Datenversand — jeder Export ist eine bewusste
  Nutzeraktion.
