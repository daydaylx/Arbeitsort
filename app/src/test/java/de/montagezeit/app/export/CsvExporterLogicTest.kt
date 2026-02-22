package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Tests CSV row generation logic from CsvExporter without Android Context.
 * Mirrors the buildString logic in CsvExporter.exportToCsv to verify escaping.
 */
class CsvExporterLogicTest {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Reproduces the CSV row-building logic from CsvExporter. */
    private fun buildCsvLine(entry: WorkEntry): String {
        val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
        val travelMinutes = TimeCalculator.calculateTravelMinutes(entry)
        val paidTotalMinutes = TimeCalculator.calculatePaidTotalMinutes(entry)
        return buildString {
            append(entry.date.format(dateFormatter))
            append(";")
            append(entry.dayType.name)
            append(";")
            append(entry.dayLocationLabel.replace(";", ","))
            append(";")
            append(entry.dayLocationSource.name)
            append(";")
            append(entry.workStart.format(timeFormatter))
            append(";")
            append(entry.workEnd.format(timeFormatter))
            append(";")
            append(entry.breakMinutes)
            append(";")
            append(workMinutes)
            append(";")
            append(travelMinutes)
            append(";")
            append(paidTotalMinutes)
            append(";")
            append(entry.note?.replace(";", ",")?.replace("\n", " ")?.replace("\r", "") ?: "")
            append("\n")
        }
    }

    private fun entry(
        location: String = "Dresden",
        note: String? = null,
        dayType: DayType = DayType.WORK
    ) = WorkEntry(
        date = LocalDate.of(2024, 6, 10),
        dayType = dayType,
        dayLocationLabel = location,
        dayLocationSource = DayLocationSource.MANUAL,
        workStart = LocalTime.of(8, 0),
        workEnd = LocalTime.of(16, 0),
        breakMinutes = 60,
        note = note
    )

    @Test
    fun `normal entry produces correct CSV line`() {
        val line = buildCsvLine(entry())
        val cols = line.trimEnd('\n').split(";")
        assertEquals(11, cols.size)
        assertEquals("2024-06-10", cols[0])
        assertEquals("WORK", cols[1])
        assertEquals("Dresden", cols[2])
        assertEquals("08:00", cols[4])
        assertEquals("16:00", cols[5])
        assertEquals("60", cols[6])
    }

    @Test
    fun `semicolon in location is replaced with comma`() {
        val line = buildCsvLine(entry(location = "Stuttgart; Zentrum"))
        assertFalse("Semicolon in location should be escaped", line.contains("Stuttgart; Zentrum"))
        assertTrue(line.contains("Stuttgart, Zentrum"))
    }

    @Test
    fun `newline in note is replaced with space`() {
        val line = buildCsvLine(entry(note = "First line\nSecond line"))
        assertFalse("Newline in note should be escaped", line.contains("First line\nSecond line"))
        assertTrue(line.contains("First line Second line"))
    }

    @Test
    fun `carriage return in note is removed`() {
        val line = buildCsvLine(entry(note = "Line\r\nEnd"))
        assertFalse(line.contains("\r"))
        assertTrue(line.contains("Line\nEnd") || line.contains("Line End"))
    }

    @Test
    fun `semicolon in note is replaced with comma`() {
        val line = buildCsvLine(entry(note = "Key; Value"))
        assertFalse(line.contains("Key; Value"))
        assertTrue(line.contains("Key, Value"))
    }

    @Test
    fun `null note produces empty note field`() {
        val line = buildCsvLine(entry(note = null))
        val cols = line.trimEnd('\n').split(";")
        assertEquals("", cols[10])
    }

    @Test
    fun `off day entry has zero work minutes`() {
        val line = buildCsvLine(entry(dayType = DayType.OFF))
        val cols = line.trimEnd('\n').split(";")
        assertEquals("OFF", cols[1])
        assertEquals("0", cols[7]) // workMinutes
    }
}
