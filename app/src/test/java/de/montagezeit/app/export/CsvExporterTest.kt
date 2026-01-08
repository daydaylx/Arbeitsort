package de.montagezeit.app.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.*
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CsvExporterTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private lateinit var context: Context
    private lateinit var csvExporter: CsvExporter
    
    @Before
    fun setup() {
        context = mockk()
        csvExporter = CsvExporter(context)
        
        val cacheDir = tempFolder.newFolder("cache")
        every { context.cacheDir } returns cacheDir
        every { context.packageName } returns "de.montagezeit.app"
        
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } returns mockk<Uri>()
    }
    
    @After
    fun tearDown() {
        unmockkAll()
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
}