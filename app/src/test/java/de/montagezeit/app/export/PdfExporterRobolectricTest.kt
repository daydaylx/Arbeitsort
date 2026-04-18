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
