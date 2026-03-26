# UI Performance Baseline (UI-305)

Stand: 2026-02-12

## Scope

- `TodayScreenV2`
- `HistoryScreen`
- `EditEntrySheet`
- `ExportPreviewBottomSheet`

## Umgesetzte Optimierungen

1. Lifecycle-aware State Collection
- `collectAsState()` auf `collectAsStateWithLifecycle()` umgestellt, damit keine unnötigen Updates im Hintergrund verarbeitet werden.
- Relevante Datei: `app/src/main/java/de/montagezeit/app/ui/screen/export/ExportPreviewScreen.kt`

2. Recomposition-Kosten in History reduziert
- Wochen-/Monats-Statistiken werden im `HistoryViewModel` vorab berechnet und nicht mehrfach in Compose-gettern neu summiert.
- Review-Filter erzeugt gefilterte Gruppen inkl. konsistenter, neu berechneter Stats.
- Relevante Dateien:
  - `app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryViewModel.kt`
  - `app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryScreen.kt`

3. Kalender-Zellen entschlackt
- `LocalDate.now()` nur einmal je Kalender-Render statt in jeder einzelnen Day-Cell.
- Relevante Datei: `app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryScreen.kt`

4. Formatierer-Konsolidierung
- Wiederverwendbare Date/Time-Formatter statt wiederholter Neuanlage in Core-Screens.
- Relevante Dateien:
  - `app/src/main/java/de/montagezeit/app/ui/screen/today/TodayScreenV2.kt`
  - `app/src/main/java/de/montagezeit/app/ui/screen/history/HistoryScreen.kt`
  - `app/src/main/java/de/montagezeit/app/ui/screen/edit/EditEntrySheet.kt`

## Erwartete Wirkung

- Weniger unnötige Recomposition bei Listen/Gruppenkarten im Verlauf.
- Stabileres Scroll-Verhalten bei größeren Datenmengen.
- Geringere CPU-Last bei Kalenderansichten und Export-Vorschau.

## Verifikation

- Build validiert mit:
  - `./gradlew :app:assembleDebug`
- Zusätzlich vorhanden:
  - stabile `LazyList`-Keys in History/Export
  - keine bekannten Compile-Regressionen nach dem Pass
