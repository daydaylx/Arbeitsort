# Dokumentation – MontageZeit

Diese Datei ordnet die Projekt-Dokumentation nach Verbindlichkeit und Zweck ein.

## Source of Truth (verbindlich)

Diese Dokumente sind die maßgeblichen Referenzen fuer Produktverhalten und Architektur:

| Dokument                             | Beschreibung                                                                        |
| ------------------------------------ | ----------------------------------------------------------------------------------- |
| [`README.md`](../README.md)          | Hauptdokumentation fuer Features, Build, Berechtigungen und aktuelles App-Verhalten |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Verbindliche Architektur-Referenz fuer Layer, Persistenz, Reminder und Datenmodell  |

Wenn sich ergänzende Dokumente und Code widersprechen, gilt zuerst der Code. Fuer Doku-Entscheidungen gelten `README.md` und `docs/ARCHITECTURE.md`.

## Aktive Ergänzungsdokumente

Diese Dateien sind keine eigene Source of Truth, beschreiben aber den aktuellen Stand ergänzend:

| Dokument                                                                     | Status | Beschreibung                                                                                     |
| ---------------------------------------------------------------------------- | ------ | ------------------------------------------------------------------------------------------------ |
| [`DAY_CLASSIFICATION.md`](DAY_CLASSIFICATION.md)                             | aktiv  | Fachreferenz fuer sichtbare Arbeitstage, Solltage, Overtime-Zaehltage und Verpflegungspauschalen |
| [`REMINDERS.md`](REMINDERS.md)                                               | aktiv  | Technische Ergänzungsdoku zum Reminder-System, Notification-Aktionen und Scheduling              |
| [`CONCEPT.md`](CONCEPT.md)                                                   | aktiv  | Produktkonzept und Leitbild auf Basis des aktuellen Offline-/Reminder-/History-Produkts          |
| [`AUDITS/CURRENT_STATUS.md`](AUDITS/CURRENT_STATUS.md)                       | aktiv  | Zusammengefasste offene Audit-Punkte, UI-Status und letzte Verifikation                          |
| [`CODEMAP.md`](CODEMAP.md)                                                   | aktiv  | Feature-zu-Datei-Karte fuer schnelle Orientierung (Agenten und Entwickler)                       |
| [`AGENT_CONTEXT_PACKS.md`](AGENT_CONTEXT_PACKS.md)                           | aktiv  | Aufgabenbezogene Kontextpakete (Pflichtlektuere, Risiken, Mindestchecks, No-Gos je Bereich)      |
| [`VALIDATION_MATRIX.md`](VALIDATION_MATRIX.md)                               | aktiv  | Pruefmatrix: welche Checks pro Aenderungstyp minimal/erweitert/manuell noetig sind               |
| [`DATA_MODEL.md`](DATA_MODEL.md)                                             | aktiv  | Kanonische Referenz fuer WorkEntry, TravelLeg, DayType, DataStore-Settings und Persistenz-No-Gos |
| [`PRIVACY_CONTEXT.md`](PRIVACY_CONTEXT.md)                                   | aktiv  | Verbindliche Datenschutz-/Permission-Grenzen (offline-first, keine Standortberechtigung)         |
| [`MANUAL_TEST_SCENARIOS.md`](MANUAL_TEST_SCENARIOS.md)                       | aktiv  | Manuelle Prüfszenarien im Format Ziel/Schritte/Erwartetes Ergebnis/Risiken                       |
| [`AGENT_HANDOFF_TEMPLATE.md`](AGENT_HANDOFF_TEMPLATE.md)                     | aktiv  | Kopierbare Handoff-Vorlage fuer Uebergabe an Nutzer oder naechsten Agenten                       |
| [`../.github/pull_request_template.md`](../.github/pull_request_template.md) | aktiv  | PR-Vorlage mit Validation-, Doku-Freshness- und Risk-Checkboxen                                  |
| [`../CONTRIBUTING.md`](../CONTRIBUTING.md)                                   | aktiv  | Lokaler Setup-, Hook- und Contributing-Workflow                                                  |
| [`../README_ANDROID_DEV.md`](../README_ANDROID_DEV.md)                       | aktiv  | Android-Geraete-Workflow, `adb`-Nutzung und VS-Code-Tasks                                        |

## Entfernte historische Dokumente

Alte Audit-Vollberichte, Weekly-Reviews, UI-Planungsberichte, Archivmaterial und Backlog-Exporte wurden in `AUDITS/CURRENT_STATUS.md` zusammengefasst und entfernt. Die Dokumentation soll bewusst schlank bleiben; neue historische Zwischenberichte sollen nur angelegt werden, wenn sie einen klaren aktuellen Nutzen haben.

## Pflege-Regeln

- Aendere `README.md` und `docs/ARCHITECTURE.md` immer dann mit, wenn sich reales Verhalten, Datenmodell oder Entwickler-Workflow aendern.
- Aendere aktive Ergänzungsdoku nur dann, wenn sie den aktuellen Stand hilfreicher erklaert als README/Architektur, aber vermeide eigene konkurrierende Wahrheiten.
- Audit-, Review- und Berichtsinhalte werden nach Abschluss in `AUDITS/CURRENT_STATUS.md` zusammengefasst; veraltete Zwischenberichte werden entfernt.

**Letzte Aktualisierung:** 2026-05-05
