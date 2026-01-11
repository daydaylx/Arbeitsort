# Nächste Schritte - MontageZeit

**Erstellt:** 2026-01-11  
**Status:** Nach vollständiger Repo-Analyse und Qualitäts-Upgrade

---

## Priorisierte nächste technische Schritte

### P0 - Kritisch (Sofort)

**Keine P0-Tasks mehr offen** ✅

Alle kritischen Probleme wurden behoben:
- ✅ Lint-Fehler behoben (0 Errors, 0 Warnings)
- ✅ Tests für alle kritischen UseCases vorhanden
- ✅ Build funktioniert ohne Fehler
- ✅ Android 7.0+ Kompatibilität sichergestellt

---

### P1 - Hoch (Nächste Iteration)

#### 1. Instrumented Tests für Room DAO
- **Aufwand:** M (3-4h)
- **Risiko:** Low
- **Begründung:** Aktuell nur Unit Tests vorhanden. Instrumented Tests für Room DAO würden Datenbank-Interaktionen besser abdecken.
- **Schritte:**
  1. `WorkEntryDaoTest` erstellen (Instrumented Test)
  2. CRUD-Operationen testen
  3. Migration-Tests (falls zukünftige Schema-Änderungen)

#### 2. Code Coverage Report aktivieren
- **Aufwand:** S (1h)
- **Risiko:** Low
- **Begründung:** Aktuell keine automatischen Coverage-Reports. Wichtig für Qualitätssicherung.
- **Schritte:**
  1. JaCoCo Plugin in `build.gradle.kts` aktivieren
  2. Coverage-Report in CI/CD integrieren
  3. Coverage-Target definieren (z.B. ≥70% Unit, ≥50% Instrumented)

#### 3. ViewModel Tests
- **Aufwand:** M (4-5h)
- **Risiko:** Low
- **Begründung:** ViewModels enthalten wichtige Business-Logik, sind aber aktuell nicht getestet.
- **Schritte:**
  1. `TodayViewModelTest` erstellen
  2. `HistoryViewModelTest` erstellen
  3. `EditEntryViewModelTest` erstellen
  4. `SettingsViewModelTest` erstellen

---

### P2 - Mittel (Wartbarkeit)

#### 4. Dependency Updates
- **Aufwand:** S (1-2h)
- **Risiko:** Medium
- **Begründung:** Mehrere Dependencies haben neuere Versionen verfügbar (siehe Lint-Warnings).
- **Schritte:**
  1. Dependencies auf neueste stabile Versionen aktualisieren
  2. Tests ausführen, um Regressionen zu prüfen
  3. Manuelle Tests durchführen

#### 5. ProGuard Rules verfeinern
- **Aufwand:** S (1h)
- **Risiko:** Low
- **Begründung:** Release-Build verwendet ProGuard, aber Rules könnten optimiert werden.
- **Schritte:**
  1. ProGuard-Rules für Room, Hilt, Compose prüfen
  2. Crash-Logs analysieren (falls vorhanden)
  3. Rules bei Bedarf anpassen

#### 6. Logging verbessern
- **Aufwand:** S (1-2h)
- **Risiko:** Low
- **Begründung:** `RingBufferLogger` existiert, aber könnte strukturierter sein.
- **Schritte:**
  1. Log-Level konsistent verwenden
  2. Strukturierte Logs (z.B. JSON-Format für bessere Analyse)
  3. Log-Rotation optimieren

---

### P3 - Niedrig (Nice-to-Have)

#### 7. Filter/Suche im Verlauf
- **Aufwand:** M (2-3h)
- **Risiko:** Low
- **Begründung:** Bei vielen Einträgen wird Navigation schwierig.
- **Schritte:**
  1. Search-TextField im History-Screen hinzufügen
  2. Filter nach DayType, needsReview, Datum implementieren
  3. UI für Filter-Optionen

#### 8. Dark Mode Support
- **Aufwand:** S (1-2h)
- **Risiko:** Low
- **Begründung:** Material3 unterstützt Dark Mode, nur Theme anpassen.
- **Schritte:**
  1. Dark Theme in `Theme.kt` hinzufügen
  2. System-Theme-Erkennung implementieren
  3. Manuelle Tests in Dark Mode

#### 9. Import-Funktion (CSV/JSON)
- **Aufwand:** M (3-4h)
- **Risiko:** Medium (Datenkonsistenz)
- **Begründung:** Export ohne Import ist nur halbe Miete.
- **Schritte:**
  1. CSV-Parser implementieren
  2. JSON-Parser implementieren
  3. Validation + Conflict-Resolution
  4. UI für Import-Dialog

#### 10. Backup-Reminder
- **Aufwand:** S (1-2h)
- **Risiko:** Low
- **Begründung:** `allowBackup=false` bedeutet Datenverlust bei Deinstallation.
- **Schritte:**
  1. Wöchentlicher Export-Reminder mit WorkManager
  2. Auto-Share-Funktionalität
  3. Settings-Option zum Aktivieren/Deaktivieren

---

## Was wurde bewusst NICHT gemacht

### Architektur-Änderungen
- **Begründung:** Clean Architecture ist bereits sauber umgesetzt. Keine Änderung nötig.

### UI-Overhaul
- **Begründung:** UI ist funktional und Material3-konform. Nur gezielte Verbesserungen (Error-States, Stats) wurden implementiert.

### Neue Dependencies
- **Begründung:** Keine neuen Dependencies hinzugefügt, da nicht nötig. Bestehende Dependencies wurden nur konfiguriert.

### Cloud-Sync/Import
- **Begründung:** Scope-Creep, nicht im MVP. Offline-First ist Kern-Feature.

### Dark Mode
- **Begründung:** Nice-to-Have, nicht kritisch. Kann später hinzugefügt werden.

---

## Empfohlene Reihenfolge

1. **P1-2: Code Coverage Report** (1h) - Schnell, hoher Wert für Qualitätssicherung
2. **P1-3: ViewModel Tests** (4-5h) - Wichtig für Stabilität
3. **P1-1: Instrumented Tests** (3-4h) - Wichtig für Datenbank-Interaktionen
4. **P2-4: Dependency Updates** (1-2h) - Wartbarkeit
5. **P3-7: Filter/Suche** (2-3h) - User-Value

---

## Metriken & Ziele

### Aktuelle Metriken
- **Lint Errors:** 0 ✅
- **Lint Warnings:** 0 ✅
- **Test-Klassen:** 11 (7 UseCase-Tests, 4 andere)
- **Code Coverage:** Nicht gemessen (Ziel: ≥70% Unit, ≥50% Instrumented)

### Ziele für nächste Iteration
- **Code Coverage:** ≥70% Unit, ≥50% Instrumented
- **ViewModel Tests:** Alle 4 ViewModels getestet
- **Instrumented Tests:** Room DAO vollständig getestet
- **Dependencies:** Alle auf neueste stabile Versionen

---

**Version:** 1.0.1-Production-Ready  
**Letzte Aktualisierung:** 2026-01-11

