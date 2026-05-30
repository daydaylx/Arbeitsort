package de.montagezeit.app.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.montagezeit.app.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CsvExporterRobolectricTest {

    private lateinit var context: Context
    private lateinit var exporter: CsvExporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        exporter = CsvExporter(context)
    }

    @Test
    fun `exportToCsv returns validation error when no entries are eligible`() {
        val result = exporter.exportToCsv(emptyList())

        assertEquals(
            CsvExporter.CsvExportResult.ValidationError(
                context.getString(R.string.export_preview_empty_range)
            ),
            result
        )
    }
}
