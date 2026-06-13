# Context-Pack: Export-Flow (PDF & CSV)

Für Aufgaben rund um PDF-/CSV-Generierung, Export-Preview und Exportdaten-Berechnung.

## Zuerst lesen

1. `docs/DATENMODELL.md` Abschnitte 1–4 — WorkEntry, TravelLeg, DayClassification
2. `docs/DAY_CLASSIFICATION.md` — Welche Tage in den Export einfließen
3. `docs/adr/0006-pdf-ohne-externe-libs.md` — Warum kein iText/PDFBox

## Schlüsseldateien

```
app/src/main/java/de/montagezeit/app/
├── export/
│   ├── PdfExporter.kt          ← PDF-Generierung (Canvas-basiert, max. 180 Einträge)
│   ├── CsvExporter.kt          ← CSV-Export
│   ├── PdfUtilities.kt         ← Formatierungs-Helper (Datum, Zeit, Reise, Verpflegung)
│   └── ExportDayMetrics.kt     ← Berechnete Kennzahlen pro Tag
└── ui/screen/export/
    ├── ExportPreviewScreen.kt   ← Compose-Screen
    └── ExportPreviewViewModel.kt
```

## Invarianten

- **Max. 180 Einträge pro PDF**: PdfExporter bricht bei Überschreitung ab — keine
  stille Kürzung. Validierung vor Export sicherstellen.
- **5 MB Mindest-Freispeicher**: `PdfExporter` prüft Storage vor dem Schreiben.
  Bei Unterschreitung wird `StorageError` zurückgegeben, kein Crash.
- **Sealed Result-Klasse**: `PdfExporter` gibt
  `Success(Uri) | ValidationError | StorageError | FileWriteError | UnknownError`
  zurück — alle Fälle im ViewModel behandeln.
- **Keine externe PDF-Library**: Nur `android.graphics.pdf.PdfDocument` (ADR-0006).
  Keine neuen Dependencies ohne ADR.
- **Export-Metriken aus TravelLeg**: `ExportDayMetrics` aggregiert Reisezeit aus
  `travel_legs`, nicht aus Flat-Fields in `WorkEntry` (die sind seit v14 leer).

## Verifikation

```bash
# Unit-Tests Export
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.export.*"
./gradlew :app:testDebugUnitTest --tests "de.montagezeit.app.ui.screen.export.*"

# Instrumented E2E
./gradlew connectedDebugAndroidTest \
  --tests "de.montagezeit.app.ExportPreviewFlowTest"
```

Manuelle Prüfpfade: `docs/MANUELLE_TESTS.md` → MT-05
