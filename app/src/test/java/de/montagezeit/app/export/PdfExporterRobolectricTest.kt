package de.montagezeit.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
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
    // Test A: Ein normaler Monat mit ca. 14 Einträgen passt auf genau eine Seite.
    // -------------------------------------------------------------------------

    @Test
    fun `exportToPdf - normaler Monat mit 14 Eintraegen passt auf genau eine Seite`() {
        val startDate = LocalDate.of(2026, 8, 3)
        val entries = (0 until 14).map { offset -> workRecord(startDate.plusDays(offset.toLong())) }

        val document = exporter.renderPdfDocument(
            eligibleEntries = entries,
            employeeName = "David Grunert",
            company = "TBM Maifarth",
            project = null,
            personnelNumber = "25",
            startDate = startDate,
            endDate = startDate.plusDays(entries.lastIndex.toLong())
        )

        assertEquals(1, document.pages.size)
        document.close()
    }

    // -------------------------------------------------------------------------
    // Test B: Lange Ortsnamen und Routen dürfen nicht zu abgeschnittenen Seiten führen.
    // -------------------------------------------------------------------------

    @Test
    fun `exportToPdf - lange Ortsnamen und Routen erzeugen weiterhin ein gueltiges einseitiges PDF`() {
        val startDate = LocalDate.of(2026, 8, 3)
        val longLocation = "Bad Wiesenfeld-Oberhausen an der Longstraße"
        val entries = (0 until 10).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            WorkEntryWithTravelLegs(
                workEntry = WorkEntry(
                    date = date,
                    dayType = DayType.WORK,
                    workStart = LocalTime.of(8, 0),
                    workEnd = LocalTime.of(17, 0),
                    breakMinutes = 60,
                    dayLocationLabel = longLocation,
                    confirmedWorkDay = true
                ),
                travelLegs = if (offset == 0) {
                    listOf(
                        TravelLeg(
                            workEntryDate = date,
                            sortOrder = 0,
                            category = TravelLegCategory.OUTBOUND,
                            startLabel = "Leipzig-Zentrum Hauptbahnhof",
                            endLabel = longLocation,
                            paidMinutesOverride = 180
                        )
                    )
                } else {
                    emptyList()
                }
            )
        }

        val document = exporter.renderPdfDocument(
            eligibleEntries = entries,
            employeeName = "David Grunert",
            company = null,
            project = null,
            personnelNumber = null,
            startDate = startDate,
            endDate = startDate.plusDays(entries.lastIndex.toLong())
        )

        assertEquals(1, document.pages.size)
        document.close()
    }

    @Test
    fun `exportToPdf - reiner Reisetag ohne Arbeitszeit wird erfolgreich exportiert`() = runTest {
        val date = LocalDate.of(2026, 8, 10)
        val entries = listOf(
            WorkEntryWithTravelLegs(
                workEntry = WorkEntry(
                    date = date,
                    dayType = DayType.WORK,
                    workStart = null,
                    workEnd = null,
                    breakMinutes = 0,
                    confirmedWorkDay = true,
                    mealAllowanceAmountCents = 1400
                ),
                travelLegs = listOf(
                    TravelLeg(
                        workEntryDate = date,
                        sortOrder = 0,
                        category = TravelLegCategory.OUTBOUND,
                        startLabel = "Leipzig",
                        endLabel = "Nürnberg",
                        paidMinutesOverride = 240
                    )
                )
            )
        )

        val result = exporter.exportToPdf(
            entries = entries,
            employeeName = "David Grunert",
            startDate = date,
            endDate = date
        )

        assertTrue(result is PdfExporter.PdfExportResult.Success)
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
