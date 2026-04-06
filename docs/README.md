# Dokumentation – MontageZeit

Diese Datei ordnet die Projekt-Dokumentation nach Verbindlichkeit und Zweck ein.

## Source of Truth (verbindlich)

Diese Dokumente sind die maßgeblichen Referenzen fuer Produktverhalten und Architektur:

| Dokument | Beschreibung |
|----------|--------------|
| [`README.md`](../README.md) | Hauptdokumentation fuer Features, Build, Berechtigungen und aktuelles App-Verhalten |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Verbindliche Architektur-Referenz fuer Layer, Persistenz, Reminder und Datenmodell |

Wenn sich ergänzende Dokumente und Code widersprechen, gilt zuerst der Code. Fuer Doku-Entscheidungen gelten `README.md` und `docs/ARCHITECTURE.md`.

## Aktive Ergänzungsdokumente

Diese Dateien sind keine eigene Source of Truth, beschreiben aber den aktuellen Stand ergänzend:

| Dokument | Status | Beschreibung |
|----------|--------|--------------|
| [`DAY_CLASSIFICATION.md`](DAY_CLASSIFICATION.md) | aktiv | Fachreferenz fuer sichtbare Arbeitstage, Solltage, Overtime-Zaehltage und Verpflegungspauschalen |
| [`REMINDERS.md`](REMINDERS.md) | aktiv | Technische Ergänzungsdoku zum Reminder-System, Notification-Aktionen und Scheduling |
| [`CONCEPT.md`](CONCEPT.md) | aktiv | Produktkonzept und Leitbild auf Basis des aktuellen Offline-/Reminder-/History-Produkts |
| [`UI_IMPLEMENTATION_SUMMARY.md`](UI_IMPLEMENTATION_SUMMARY.md) | aktiv | Kurzuebersicht ueber den aktuell implementierten UI-Umfang |
| [`../CONTRIBUTING.md`](../CONTRIBUTING.md) | aktiv | Lokaler Setup-, Hook- und Contributing-Workflow |
| [`../README_ANDROID_DEV.md`](../README_ANDROID_DEV.md) | aktiv | Android-Geraete-Workflow, `adb`-Nutzung und VS-Code-Tasks |

## Historische oder referenzielle Dokumente

Diese Dateien behalten Kontext, sind aber nicht fuer den aktuellen Produktstand maßgeblich:

### Audits & Reviews

| Dokument | Beschreibung |
|----------|--------------|
| [`AUDITS/AUDIT_REPORT_2026-03-14.md`](AUDITS/AUDIT_REPORT_2026-03-14.md) | Groesseres Code-Audit mit technischen Funden zum damaligen Stand |
| [`AUDITS/AUDIT_REPORT_2026-03-29.md`](AUDITS/AUDIT_REPORT_2026-03-29.md) | Spaeteres Audit mit weiteren Architektur-, Reminder- und Wartungsfunden |
| [`weekly-review/2026-03-30.md`](weekly-review/2026-03-30.md) | Punktuelle Wochen-Review zum damaligen Stand |

### Planung / Archiv

| Dokument | Beschreibung |
|----------|--------------|
| [`ARCHIVE/`](ARCHIVE/) | Historische Plaene, Checklisten und aeltere Berichte |
| [`jira/ui_modernization_backlog.csv`](jira/ui_modernization_backlog.csv) | Backlog-Export, kein verbindlicher Produktstand |

## Pflege-Regeln

- Aendere `README.md` und `docs/ARCHITECTURE.md` immer dann mit, wenn sich reales Verhalten, Datenmodell oder Entwickler-Workflow aendern.
- Aendere aktive Ergänzungsdoku nur dann, wenn sie den aktuellen Stand hilfreicher erklaert als README/Architektur, aber vermeide eigene konkurrierende Wahrheiten.
- Audit-, Weekly-Review- und Archivdokumente werden nicht rueckwirkend "korrigiert"; sie dokumentieren historische Befunde.

**Letzte Aktualisierung:** 2026-04-05
