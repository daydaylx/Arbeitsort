package de.montagezeit.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfExporterRobolectricTest {

    private lateinit var context: Context
    private lateinit var exporter: PdfExporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        exporter = PdfExporter(context)
        File(context.cacheDir, "exports").deleteRecursively()
    }

    @Test
    fun `exportToPdf returns validation error when employee name is blank`() = runTest {
        val result = exporter.exportToPdf(
            entries = listOf(workRecord(LocalDate.of(2026, 3, 3))),
            employeeName = "",
            startDate = LocalDate.of(2026, 3, 3),
            endDate = LocalDate.of(2026, 3, 3)
        )

        assertEquals(
            PdfExporter.PdfExportResult.ValidationError(
                context.getString(R.string.pdf_export_error_name_missing)
            ),
            result
        )
    }

    @Test
    fun `exportToPdf returns validation error when entry limit is exceeded`() = runTest {
        val startDate = LocalDate.of(2026, 3, 1)
        val entries = List(PdfExporter.MAX_ENTRIES_PER_PDF + 1) { index ->
            workRecord(startDate.plusDays(index.toLong()))
        }

        val result = exporter.exportToPdf(
            entries = entries,
            employeeName = "Max Mustermann",
            startDate = startDate,
            endDate = startDate.plusDays(entries.lastIndex.toLong())
        )

        assertEquals(
            PdfExporter.PdfExportResult.ValidationError(
                context.getString(R.string.pdf_export_error_too_many_entries)
            ),
            result
        )
    }

    @Test
    fun `createShareIntent configures chooser with pdf stream and read permission`() {
        val fileUri = Uri.parse("content://${context.packageName}.fileprovider/exports/test.pdf")

        val chooserIntent = exporter.createShareIntent(fileUri)

        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)
        @Suppress("DEPRECATION")
        val shareIntent = requireNotNull(chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent)
        assertEquals(Intent.ACTION_SEND, shareIntent.action)
        assertEquals("application/pdf", shareIntent.type)
        @Suppress("DEPRECATION")
        assertEquals(fileUri, shareIntent.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue((shareIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }

    // -------------------------------------------------------------------------
    // Test A: Ein normaler Monat (auch ein voller 31-Tage-Monat) passt auf genau eine Seite.
    //
    // countPagesNeeded() teilt sich die Seitenumbruch-Arithmetik mit dem echten Zeichenpfad
    // (renderPdfDocument), benötigt für die Berechnung selbst aber kein android.graphics.pdf.
    // PdfDocument – das lässt sich unter Robolectric hier nicht zuverlässig konstruieren
    // (bereits ein frisches PdfDocument() meldet "document is closed!" bei startPage()).
    // Reale Paint-Textmetriken (für Schriftgrößen-Auswahl und Zeilenhöhe) werden dagegen von
    // Robolectric zuverlässig unterstützt.
    // -------------------------------------------------------------------------

    @Test
    fun `countPagesNeeded - normaler Monat mit 14 Eintraegen passt auf genau eine Seite`() {
        assertEquals(1, exporter.countPagesNeeded(entryCount = 14, legendPresent = false))
    }

    @Test
    fun `countPagesNeeded - Monat mit nur Werktagen passt auf genau eine Seite`() {
        assertEquals(1, exporter.countPagesNeeded(entryCount = 22, legendPresent = true))
    }

    @Test
    fun `countPagesNeeded - voller 31-Tage-Monat passt auf genau eine Seite`() {
        assertEquals(1, exporter.countPagesNeeded(entryCount = 31, legendPresent = true))
    }

    // -------------------------------------------------------------------------
    // Test B (Seitenaspekt): Lange Ortsnamen/Routen ändern nichts an der Zeilenhöhe – jede Zeile
    // bleibt einzeilig (Ellipsis-Kürzung statt Umbruch, siehe PdfExporter.fitTextToWidth), die
    // Seitenzahl hängt also nur von der Eintragsanzahl ab, nicht von der Textlänge.
    // -------------------------------------------------------------------------

    @Test
    fun `countPagesNeeded - zehn Eintraege mit Reise passen auf genau eine Seite`() {
        assertEquals(1, exporter.countPagesNeeded(entryCount = 10, legendPresent = true))
    }

    // -------------------------------------------------------------------------
    // Ausnahmefall: sehr viele Einträge (deutlich über einen Monat hinaus) lösen kontrolliert
    // mehrere Seiten aus, statt abgeschnittene Inhalte zu erzeugen.
    // -------------------------------------------------------------------------

    @Test
    fun `countPagesNeeded - sehr viele Eintraege loesen kontrollierten Mehrseiten-Fallback aus`() {
        val pages = exporter.countPagesNeeded(entryCount = PdfExporter.MAX_ENTRIES_PER_PDF, legendPresent = true)
        assertTrue("expected more than one page for ${PdfExporter.MAX_ENTRIES_PER_PDF} entries", pages > 1)
    }

    private fun workRecord(date: LocalDate): WorkEntryWithTravelLegs {
        return WorkEntryWithTravelLegs(
            workEntry = WorkEntry(
                date = date,
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(17, 0),
                breakMinutes = 60,
                confirmedWorkDay = true
            ),
            travelLegs = emptyList()
        )
    }
}
