package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.MealAllowanceCalculator
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

    /**
     * Reproduces the CSV row-building logic from CsvExporter.
     * Neue Spalten: confirmedWorkDay (index 2); Zeitfelder leer bei OFF/COMP_TIME.
     * Neue Spaltenreihenfolge:
     *   0:date 1:dayType 2:confirmedWorkDay 3:dayLocation 4:dayLocationSource
     *   5:workStart 6:workEnd 7:breakMinutes 8:workMinutes 9:travelMinutes
     *   10:paidTotalMinutes 11:mealIsArrivalDeparture 12:mealBreakfastIncluded
     *   13:mealAllowanceBaseCents 14:mealAllowanceAmountCents 15:mealAllowanceEuro 16:note
     */
    private fun buildCsvLine(entry: WorkEntry): String {
        val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
        val travelMinutes = TimeCalculator.calculateTravelMinutes(entry)
        val paidTotalMinutes = TimeCalculator.calculatePaidTotalMinutes(entry)
        val isWorkDay = entry.dayType == DayType.WORK
        val dayTypeLabel = when (entry.dayType) {
            DayType.COMP_TIME -> "Ü-Abbau"
            else -> entry.dayType.name
        }
        return buildString {
            append(entry.date.format(dateFormatter))
            append(";")
            append(dayTypeLabel)
            append(";")
            append(if (entry.confirmedWorkDay) 1 else 0)
            append(";")
            append(entry.dayLocationLabel.replace(";", ","))
            append(";")
            append(entry.dayLocationSource.name)
            append(";")
            append(if (isWorkDay) entry.workStart.format(timeFormatter) else "")
            append(";")
            append(if (isWorkDay) entry.workEnd.format(timeFormatter) else "")
            append(";")
            append(if (isWorkDay) entry.breakMinutes.toString() else "")
            append(";")
            append(workMinutes)
            append(";")
            append(travelMinutes)
            append(";")
            append(paidTotalMinutes)
            append(";")
            append(if (entry.mealIsArrivalDeparture) 1 else 0)
            append(";")
            append(if (entry.mealBreakfastIncluded) 1 else 0)
            append(";")
            append(entry.mealAllowanceBaseCents)
            append(";")
            append(entry.mealAllowanceAmountCents)
            append(";")
            append(MealAllowanceCalculator.formatEuro(entry.mealAllowanceAmountCents))
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
        assertEquals(17, cols.size)
        assertEquals("2024-06-10", cols[0])
        assertEquals("WORK", cols[1])
        // cols[2] = confirmedWorkDay
        assertEquals("Dresden", cols[3])
        assertEquals("08:00", cols[5])
        assertEquals("16:00", cols[6])
        assertEquals("60", cols[7])
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
        assertEquals("", cols[16])
    }

    @Test
    fun `off day entry has zero work minutes`() {
        val line = buildCsvLine(entry(dayType = DayType.OFF))
        val cols = line.trimEnd('\n').split(";")
        assertEquals("OFF", cols[1])
        assertEquals("0", cols[8]) // workMinutes (index shifted +1)
    }

    @Test
    fun `meal allowance fields exported correctly`() {
        val entryWithMeal = WorkEntry(
            date = java.time.LocalDate.of(2024, 6, 10),
            dayType = DayType.WORK,
            dayLocationLabel = "Dresden",
            dayLocationSource = DayLocationSource.MANUAL,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(16, 0),
            breakMinutes = 60,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820
        )
        val line = buildCsvLine(entryWithMeal)
        val cols = line.trimEnd('\n').split(";")
        assertEquals(17, cols.size)
        assertEquals("1", cols[11])    // mealIsArrivalDeparture (shifted +1)
        assertEquals("1", cols[12])    // mealBreakfastIncluded
        assertEquals("1400", cols[13]) // mealAllowanceBaseCents
        assertEquals("820", cols[14])  // mealAllowanceAmountCents
        assertEquals("8,20 €", cols[15]) // mealAllowanceEuro
    }

    @Test
    fun `OFF Tag hat leere workStart workEnd breakMinutes Felder`() {
        val line = buildCsvLine(entry(dayType = DayType.OFF))
        val cols = line.trimEnd('\n').split(";")
        assertEquals("OFF", cols[1])
        assertEquals("", cols[5])  // workStart leer
        assertEquals("", cols[6])  // workEnd leer
        assertEquals("", cols[7])  // breakMinutes leer
        assertEquals("0", cols[8]) // workMinutes = 0 (berechnet)
    }

    @Test
    fun `COMP_TIME Tag hat leere Zeitfelder`() {
        val line = buildCsvLine(entry(dayType = DayType.COMP_TIME))
        val cols = line.trimEnd('\n').split(";")
        assertEquals("Ü-Abbau", cols[1])
        assertEquals("", cols[5])  // workStart leer
        assertEquals("", cols[6])  // workEnd leer
        assertEquals("", cols[7])  // breakMinutes leer
    }

    @Test
    fun `WORK Tag hat Zeitfelder befuellt`() {
        val line = buildCsvLine(entry(dayType = DayType.WORK))
        val cols = line.trimEnd('\n').split(";")
        assertEquals("WORK", cols[1])
        assertEquals("08:00", cols[5])  // workStart befüllt
        assertEquals("16:00", cols[6])  // workEnd befüllt
        assertEquals("60", cols[7])     // breakMinutes befüllt
    }

    @Test
    fun `confirmedWorkDay Spalte korrekt gesetzt`() {
        val confirmedLine = buildCsvLine(entry().copy(confirmedWorkDay = true))
        val confirmedCols = confirmedLine.trimEnd('\n').split(";")
        assertEquals("1", confirmedCols[2])

        val unconfirmedLine = buildCsvLine(entry().copy(confirmedWorkDay = false))
        val unconfirmedCols = unconfirmedLine.trimEnd('\n').split(";")
        assertEquals("0", unconfirmedCols[2])
    }
}
