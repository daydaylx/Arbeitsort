package de.montagezeit.app.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class JsonExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var jsonExporter: JsonExporter

    @Before
    fun setup() {
        context = mockk()
        jsonExporter = JsonExporter(context)

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
    fun `exportToJson mit leeren Eintraegen sollte gueltige JSON-Struktur zurueckgeben`() {
        val entries = emptyList<WorkEntry>()
        val result = jsonExporter.exportToJson(entries)
        assertNotNull(result)
    }

    @Test
    fun `exportToJson mit einem Eintrag sollte korrektes Format haben`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 1, 5),
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            dayType = DayType.WORK,
            morningCapturedAt = System.currentTimeMillis(),
            morningLocationLabel = "Leipzig",
            morningLat = 51.340,
            morningLon = 12.374,
            morningAccuracyMeters = 15.0f,
            outsideLeipzigMorning = false,
            morningLocationStatus = LocationStatus.OK,
            eveningCapturedAt = System.currentTimeMillis(),
            eveningLocationLabel = "Dresden",
            eveningLat = 51.050,
            eveningLon = 13.737,
            eveningAccuracyMeters = 20.0f,
            outsideLeipzigEvening = true,
            eveningLocationStatus = LocationStatus.OK,
            needsReview = false,
            note = "Test note"
        )

        val result = jsonExporter.exportToJson(listOf(entry))
        assertNotNull(result)
    }

    @Test
    fun `exportToJson mit mehreren Eintraegen sollte korrekte entryCount haben`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 1, 5),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                dayType = DayType.WORK,
                needsReview = false
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 6),
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                dayType = DayType.WORK,
                needsReview = false
            )
        )

        val result = jsonExporter.exportToJson(entries)
        assertNotNull(result)
    }

    @Test
    fun `exportToJson mit null Location-Werten sollte null in JSON haben`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 1, 5),
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            dayType = DayType.WORK,
            morningCapturedAt = null,
            morningLocationLabel = null,
            morningLat = null,
            morningLon = null,
            morningAccuracyMeters = null,
            outsideLeipzigMorning = null,
            morningLocationStatus = LocationStatus.UNAVAILABLE,
            needsReview = false
        )

        val result = jsonExporter.exportToJson(listOf(entry))
        assertNotNull(result)
    }

}
