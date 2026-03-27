# Dokumentation – MontageZeit

Diese Datei bietet eine Übersicht über alle Dokumente im Projekt.

## Source of Truth (Verbindlich)

Diese Dokumente sind die maßgeblichen Referenzen für Architektur und Produkt:

| Dokument | Beschreibung |
|----------|--------------|
| [`README.md`](../README.md) | Hauptdokumentation – Build, Features, Tech-Stack, Berechtigungen |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Verbindliche Architektur-Referenz (Layer, Datenmodell, Reminder-Logik) |

Nur diese beiden Dokumente sind verbindlich. Wenn ergänzende Dokumente abweichen, gelten `README.md` und Code.

## Aktive Referenzdokumente

Diese Dateien sind Arbeits- oder Hintergrundmaterial, aber keine Source of Truth:

| Dokument | Beschreibung |
|----------|--------------|
| [`CONCEPT.md`](CONCEPT.md) | Produkt-Konzept / MVP-Tech-Spec |
| [`REMINDERS.md`](REMINDERS.md) | Reminder-System Dokumentation |
| [`UI_IMPLEMENTATION_SUMMARY.md`](UI_IMPLEMENTATION_SUMMARY.md) | UI-Implementierungs-Zusammenfassung |
| [`../CONTRIBUTING.md`](../CONTRIBUTING.md) | Lokaler Setup-, Hook- und Contributing-Workflow |
| [`../README_ANDROID_DEV.md`](../README_ANDROID_DEV.md) | Android-Geraete-Workflow, `adb`-Nutzung und VS-Code-Tasks |

---

## Dokumentenkategorien

### 📊 Audits & Reports (Referenz)

Dokumente aus Code-Audits und Qualitätsprüfungen. Sie sind hilfreich fuer Kontext und Nachverfolgung, aber nicht verbindlich:

| Dokument | Datum | Beschreibung |
|----------|-------|--------------|
| [`AUDITS/AUDIT_REPORT_2026-03-14.md`](AUDITS/AUDIT_REPORT_2026-03-14.md) | 2026-03-14 | Haupt-Audit (A01–A20, B01–B25) mit Funden zu Race Conditions, Data-Flow, Performance |

### 🧭 Richtlinien & Guides

| Dokument | Beschreibung |
|----------|--------------|
| [`AGENTS.md`](../AGENTS.md) | Repository Guidelines (Coding Style, Testing, Commit-Konventionen) |
| [`../CONTRIBUTING.md`](../CONTRIBUTING.md) | Entwickler-Workflow und lokale Qualitaetschecks |

### 📦 Archiv (Historisch)

Veraltete oder nicht mehr aktive Dokumente:

| Dokument | Beschreibung |
|----------|--------------|
| [`ARCHIVE/ASSUMPTIONS.md`](ARCHIVE/ASSUMPTIONS.md) | Annahmen (veraltet) |
| [`ARCHIVE/AUDIT_PLAN.md`](ARCHIVE/AUDIT_PLAN.md) | Audit-Planung |
| [`ARCHIVE/NEXT_TASKS.md`](ARCHIVE/NEXT_TASKS.md) | Historische Aufgabenliste |
| [`ARCHIVE/QA_CHECKLIST.md`](ARCHIVE/QA_CHECKLIST.md) | QA-Checkliste |
| [`ARCHIVE/UI_PERFORMANCE_BASELINE.md`](ARCHIVE/UI_PERFORMANCE_BASELINE.md) | UI-Performance-Baseline |
| [`ARCHIVE/UI_QA_CHECKLIST.md`](ARCHIVE/UI_QA_CHECKLIST.md) | UI-QA-Checkliste |
| [`ARCHIVE/REPORT_CODE_COMPARISON.md`](ARCHIVE/REPORT_CODE_COMPARISON.md) | Code-Vergleichs-Report |
| [`ARCHIVE/REPORT.md`](ARCHIVE/REPORT.md) | Zusätzlicher Audit-/Report-Bestand |
| [`ARCHIVE/NEXTSTEPS.md`](ARCHIVE/NEXTSTEPS.md) | Historische Nächste-Schritte-Notizen |
| [`ARCHIVE/UI_AUDIT_REPORT_2026-01-31.md`](ARCHIVE/UI_AUDIT_REPORT_2026-01-31.md) | Älteres UI-Audit |
| [`ARCHIVE/UI_TARGET_ALIGNMENT_PLAN.md`](ARCHIVE/UI_TARGET_ALIGNMENT_PLAN.md) | UI Target Alignment Plan |
| [`ARCHIVE/CLAUDE.md`](ARCHIVE/CLAUDE.md) | Claude-spezifische Notizen |

---

## Dokumenten-Pflege

### Wann verschieben?
- **Audit-Ergebnisse** → `AUDITS/` mit Datumsstempel im Namen
- **Veraltete Pläne/Checklisten** → `ARCHIVE/`
- **Aktive Guidelines** → `GUIDES/` (neu erstellen)

### Namenskonventionen
- Audit-Reports: `AUDIT_REPORT_YYYY-MM-DD.md`
- Guides: `GUIDE_<THEMA>.md`
- Architektur: `ARCHITECTURE.md` (bleibt im Root von `docs/`)

---

**Letzte Aktualisierung:** 2026-03-27
