# Dokumentation – MontageZeit

Diese Datei bietet eine Übersicht über alle Dokumente im Projekt.

## Source of Truth (Verbindlich)

Diese Dokumente sind die maßgeblichen Referenzen für Architektur und Produkt:

| Dokument | Beschreibung |
|----------|--------------|
| [`README.md`](../README.md) | Hauptdokumentation – Build, Features, Tech-Stack, Berechtigungen |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Verbindliche Architektur-Referenz (Layer, Datenmodell, Reminder-Logik) |
| [`CONCEPT.md`](CONCEPT.md) | Produkt-Konzept (APK Tech Spec MVP) |

---

## Dokumentenkategorien

### 📊 Audits & Reports

Dokumente aus Code-Audits und Qualitätsprüfungen:

| Dokument | Datum | Beschreibung |
|----------|-------|--------------|
| [`AUDITS/AUDIT_REPORT_2026-03-14.md`](AUDITS/AUDIT_REPORT_2026-03-14.md) | 2026-03-14 | Haupt-Audit (A01–A20, B01–B25) mit Funden zu Race Conditions, Data-Flow, Performance |
| [`AUDITS/UI_AUDIT_REPORT_2026-01-31.md`](AUDITS/UI_AUDIT_REPORT_2026-01-31.md) | 2026-01-31 | UI-Audit (Reader Noir, Mobile Pacing, Accessibility, Gaps) |
| [`AUDITS/REPORT.md`](AUDITS/REPORT.md) | – | Zusätzlicher Report |

### 🧭 Richtlinien & Guides

| Dokument | Beschreibung |
|----------|--------------|
| [`AGENTS.md`](../AGENTS.md) | Repository Guidelines (Coding Style, Testing, Commit-Konventionen) |

### 📦 Archiv (Historisch)

Veraltete oder nicht mehr aktive Dokumente:

| Dokument | Beschreibung |
|----------|--------------|
| [`ARCHIVE/ASSUMPTIONS.md`](ARCHIVE/ASSUMPTIONS.md) | Annahmen (veraltet) |
| [`ARCHIVE/AUDIT_PLAN.md`](ARCHIVE/AUDIT_PLAN.md) | Audit-Planung |
| [`ARCHIVE/NEXT_TASKS.md`](ARCHIVE/NEXT_TASKS.md) | Nächste Aufgaben (veraltet durch NEXTSTEPS.md) |
| [`ARCHIVE/QA_CHECKLIST.md`](ARCHIVE/QA_CHECKLIST.md) | QA-Checkliste |
| [`ARCHIVE/UI_PERFORMANCE_BASELINE.md`](ARCHIVE/UI_PERFORMANCE_BASELINE.md) | UI-Performance-Baseline |
| [`ARCHIVE/UI_QA_CHECKLIST.md`](ARCHIVE/UI_QA_CHECKLIST.md) | UI-QA-Checkliste |
| [`ARCHIVE/REPORT_CODE_COMPARISON.md`](ARCHIVE/REPORT_CODE_COMPARISON.md) | Code-Vergleichs-Report |
| [`ARCHIVE/NEXTSTEPS.md`](ARCHIVE/NEXTSTEPS.md) | Nächste Schritte (veraltet) |
| [`ARCHIVE/UI_TARGET_ALIGNMENT_PLAN.md`](ARCHIVE/UI_TARGET_ALIGNMENT_PLAN.md) | UI Target Alignment Plan |
| [`ARCHIVE/CLAUDE.md`](ARCHIVE/CLAUDE.md) | Claude-spezifische Notizen |

### 🔧 Weitere Dokumente (Referenz)

| Dokument | Beschreibung |
|----------|--------------|
| [`REMINDERS.md`](REMINDERS.md) | Reminder-System Dokumentation |
| [`UI_IMPLEMENTATION_SUMMARY.md`](UI_IMPLEMENTATION_SUMMARY.md) | UI-Implementierungs-Zusammenfassung |

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

**Letzte Aktualisierung:** 2026-03-26
