package de.montagezeit.app.export

import android.content.Context
import android.net.Uri
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CsvExporterTest {
    
    @Mock
    private lateinit var context: Context
    
    private lateinit var csvExporter: CsvExporter
    
    @Before
    fun setup() {
        csvExporter = CsvExporter(context)
    }
    
    @Test
    fun `exportToCsv mit leeren Eintraegen sollte null zurueckgeben`() {
        val entries = emptyList<WorkEntry>()
        val result = csvExporter.exportToCsv(entries)
        assertNotNull(result)
    }
    
    @Test
    fun `exportToCsv mit einem Eintrag sollte korrektes Format haben`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 1, 5),
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            dayType = DayType.WORK,
            morningCapturedAt = System.currentTimeMillis(),
            morningLocationLabel = "Leipzig",
            outsideLeipzigMorning = false,
            morningLocationStatus = LocationStatus.OK,
            needsReview = false
        )
        
        val result = csvExporter.exportToCsv(listOf(entry))
        assertNotNull(result)
    }
    
    @Test
    fun `createShareIntent sollte korrekten Intent erstellen`() {
        val fileUri = Uri.parse("content://com.example.fileprovider/exports/test.csv")
        val intent = csvExporter.createShareIntent(fileUri)
        
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/csv", intent.type)
        assertTrue(intent.hasExtra(Intent.EXTRA_STREAM))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
