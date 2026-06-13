# ADR-0006: PDF-Generierung ohne externe Bibliotheken

**Status:** Akzeptiert
**Datum:** 2026-06-13

## Kontext

Die App exportiert Arbeitszeit-Nachweise als PDF. Für die PDF-Erstellung stehen
externe Libraries (iText, Apache PDFBox, OpenPDF) oder die Android-eigene
`android.graphics.pdf.PdfDocument`-API zur Wahl.

## Entscheidung

Verwendung von `android.graphics.pdf.PdfDocument` (Android SDK, kein externes Dependency).

`PdfExporter` zeichnet Seiten direkt über `Canvas`-Operationen:

- A4-Format, max. 180 Einträge pro PDF
- Mindest-Freispeicher vor Export: 5 MB (Validierung in `PdfExporter`)
- Ergebnis-Sealed-Class: `Success(Uri)`, `ValidationError`, `StorageError`,
  `FileWriteError`, `UnknownError`

## Begründung

- **Kein Lizenz-Overhead**: iText 7 (AGPL) und Apache PDFBox haben Lizenzanforderungen,
  die für eine proprietäre App relevant sind. Die SDK-API ist lizenzfrei.
- **Kein APK-Größen-Overhead**: Externe PDF-Libraries fügen 1–5 MB zum APK hinzu.
- **Ausreichend für den Anwendungsfall**: Tabellarische Berichte (Datum, Zeiten,
  Reise, Verpflegung) sind mit Canvas-Primitives gut umsetzbar.

## Konsequenzen

- Komplexe Layouts (z. B. eingebettete Bilder, Hyperlinks, Barrierefreiheits-Tags)
  sind mit der SDK-API aufwändiger als mit externen Libraries.
- Schriftarten sind auf Android-System-Fonts beschränkt — kein Custom-Font-Embedding
  ohne zusätzlichen Aufwand.
- Tests (`PdfExporterLogicTest`) laufen auf der JVM mit Robolectric-Umgebung.
