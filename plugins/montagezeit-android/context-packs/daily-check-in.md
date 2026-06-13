# Context-Pack: Tageserfassung (Today-Screen & Check-in-Flow)

Für Aufgaben rund um den Today-Screen, Check-in-Use-Cases und den Edit-Save-Pfad.

## Zuerst lesen

1. `docs/ARCHITECTURE.md` Sections 2.3 und 2.4 — Daily Flow und Reminder-Action-UseCases
2. `docs/DATENMODELL.md` Abschnitt 1 — WorkEntry-Felder und Bestätigungsregel
3. `docs/adr/0004-confirmation-state.md` — Bestätigungsstatus-Semantik

## Schlüsseldateien

```
app/src/main/java/de/montagezeit/app/
├── domain/usecase/
│   ├── RecordDailyManualCheckIn.kt   ← Primärpfad Today-Screen (alles in einem Schritt)
│   ├── RecordCheckIn.kt              ← Morning- und Evening-Snapshot (Notification-Action via CheckInActionService)
│   ├── CheckInEntryBuilder.kt        ← Baut WorkEntry-Snapshot ohne dayType zu überschreiben
│   ├── ConfirmWorkDay.kt             ← Arbeitstag manuell bestätigen
│   └── ConfirmOffDay.kt              ← OFF/VACATION/COMP_TIME bestätigen
├── ui/screen/today/
│   ├── TodayScreen.kt
│   └── TodayViewModel.kt
└── ui/screen/edit/
    ├── EditEntrySheet.kt
    ├── EditEntrySaveBuilder.kt       ← Validierung + Bestätigungs-Logik beim Speichern
    └── EditEntryViewModel.kt
```

## Invarianten

- **WORK wird nur bestätigt mit positivem Tätigkeitsnachweis**: `EditEntrySaveBuilder`
  setzt `confirmedWorkDay = true` nur, wenn `nettoArbeitszeit > 0` **oder**
  `summeReisezeit > 0`. Ein leerer WORK-Eintrag bleibt offen.
- **OFF / VACATION / COMP_TIME sind terminal**: Diese Typen werden immer als
  abgeschlossen behandelt — unabhängig von `confirmedWorkDay`.
- **`RecordDailyManualCheckIn` ist der Haupt-Pfad**: Setzt `dayType = WORK`,
  `confirmedWorkDay = true`, `dayLocationLabel` und Bestätigungs-Metadaten in
  einem atomaren UseCase-Aufruf.
- **`CheckInEntryBuilder` behält bestehenden `dayType`**: Für existierende Einträge
  wird der `dayType` nicht überschrieben. `WORK` ist nur Default für neue Einträge.
- **`confirmationSource` mitführen**: Bei Nutzer-Aktionen im UI `"UI"` setzen,
  bei Notification-Aktionen `"NOTIFICATION"`. Nie leer lassen wenn bestätigt.

## Verifikation

```bash
# Use-Case-Tests
./gradlew :app:testDebugUnitTest \
  --tests "de.montagezeit.app.domain.usecase.*"

# Edit-Screen-Tests
./gradlew :app:testDebugUnitTest \
  --tests "de.montagezeit.app.ui.screen.edit.*"

# Today-ViewModel-Test
./gradlew :app:testDebugUnitTest \
  --tests "de.montagezeit.app.ui.screen.today.*"
```

Manuelle Prüfpfade: `docs/MANUELLE_TESTS.md` → MT-01, MT-02
