package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

/**
 * Unit Tests für PdfUtilities
 */
class PdfUtilitiesTest {
    
    @Test
    fun `calculateWorkHours - WORK Tag mit Standardwerten`() {
        val entry = WorkEntry(
            date = java.time.LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            confirmedWorkDay = true,
            confirmationAt = System.currentTimeMillis()
        )
        
        val hours = PdfUtilities.calculateWorkHours(entry)
        
        // (19:00 - 08:00 - 01:00) / 60 = 10/60 = 0.1666... = 10.00 Stunden
        val expected = 10.0
        assertEquals(expected, hours, 0.01)
    }
    
    @Test
    fun `calculateWorkHours - OFF Tag`() {
        val entry = WorkEntry(
            date = java.time.LocalDate.of(2026, 1, 15),
            dayType = DayType.OFF,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            confirmedWorkDay = true,
            confirmationAt = System.currentTimeMillis()
        )
        
        val hours = PdfUtilities.calculateWorkHours(entry)
        
        assertEquals(0.0, hours, 0.01)
    }
    
    @Test
    fun `calculateWorkHours - WORK Tag mit Pause`() {
        val entry = WorkEntry(
            date = java.time.LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            confirmedWorkDay = true,
            confirmationAt = System.currentTimeMillis()
        )
        
        val hours = PdfUtilities.calculateWorkHours(entry)
        
        // (17:00 - 08:00 - 01:00) / 60 = 8/60 = 0.1333...
        val expected = 8.0
        assertEquals(expected, hours, 0.01)
    }
    
    @Test
    fun `formatWorkHours - formatiert korrekt`() {
        val hours = 10.0
        val formatted = PdfUtilities.formatWorkHours(hours)
        
        assertEquals("10,00", formatted)
    }
    
    @Test
    fun `formatWorkHours - formatiert mit Nachkommastellen`() {
        val hours = 8.5
        val formatted = PdfUtilities.formatWorkHours(hours)
        
        assertEquals("8,50", formatted)
    }
    
    @Test
    fun `formatTravelTime - ohne Reisezeit`() {
        val formatted = PdfUtilities.formatTravelTime(null)
        
        assertEquals("", formatted)
    }
    
    @Test
    fun `formatTravelTime - mit 0 Minuten`() {
        val formatted = PdfUtilities.formatTravelTime(0)
        
        assertEquals("", formatted)
    }
    
    @Test
    fun `formatTravelTime - 60 Minuten`() {
        val formatted = PdfUtilities.formatTravelTime(60)
        
        assertEquals("1,00", formatted)
    }
    
    @Test
    fun `formatTravelTime - 90 Minuten`() {
        val formatted = PdfUtilities.formatTravelTime(90)
        
        assertEquals("1,50", formatted)
    }
    
    @Test
    fun `formatTime - formatiert korrekt`() {
        val time = LocalTime.of(14, 30)
        val formatted = PdfUtilities.formatTime(time)
        
        assertEquals("14:30", formatted)
    }
    
    @Test
    fun `formatDate - formatiert korrekt`() {
        val date = java.time.LocalDate.of(2026, 1, 15)
        val formatted = PdfUtilities.formatDate(date)
        
        assertEquals("15.01.2026", formatted)
    }
    
    @Test
    fun `getLocation - morningLocationLabel`() {
        val entry = WorkEntry(
            date = java.time.LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            morningLocationLabel = "Leipzig",
            eveningLocationLabel = "Außen",
            confirmedWorkDay = true,
            confirmationAt = System.currentTimeMillis()
        )
        
        val location = PdfUtilities.getLocation(entry)
        
        assertEquals("Leipzig", location)
    }
    
    @Test
    fun `getLocation - eveningLocationLabel`() {
        val entry = WorkEntry(
            date = java.time.LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            morningLocationLabel = null,
            eveningLocationLabel = "Halle",
            confirmedWorkDay = true,
            confirmationAt = System.currentTimeMillis()
        )
        
        val location = PdfUtilities.getLocation(entry)
        
        assertEquals("Halle", location)
    }
    
    @Test
    fun `getLocation - ohne Location`() {
        val entry = WorkEntry(
            date = java.time.LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            confirmedWorkDay = true,
            confirmationAt = System.currentTimeMillis()
        )
        
        val location = PdfUtilities.getLocation(entry)
        
        assertEquals("", location)
    }
    
    @Test
    fun `getNote - mit Notiz`() {
        val entry = WorkEntry(
            date = java.time.LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            note = "Kunde besucht",
            confirmedWorkDay = true,
            confirmationAt = System.currentTimeMillis()
        )
        
        val note = PdfUtilities.getNote(entry)
        
        assertEquals("Kunde besucht", note)
    }
    
    @Test
    fun `getNote - ohne Notiz`() {
        val entry = WorkEntry(
            date = java.time.LocalDate.of(2026, 1, 15),
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0),
            breakMinutes = 60,
            confirmedWorkDay = true,
            confirmationAt = System.currentTimeMillis()
        )
        
        val note = PdfUtilities.getNote(entry)
        
        assertEquals("", note)
    }
    
    @Test
    fun `sumWorkHours - mehrere Einträge`() {
        val entries = listOf(
            WorkEntry(
                date = java.time.LocalDate.of(2026, 1, 15),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            ),
            WorkEntry(
                date = java.time.LocalDate.of(2026, 1, 16),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            )
        )
        
        val sum = PdfUtilities.sumWorkHours(entries)
        
        // 2 * 10 = 20 Stunden
        assertEquals(20.0, sum, 0.01)
    }
    
    @Test
    fun `sumTravelMinutes - mehrere Einträge`() {
        val entries = listOf(
            WorkEntry(
                date = java.time.LocalDate.of(2026, 1, 15),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                travelPaidMinutes = 60,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            ),
            WorkEntry(
                date = java.time.LocalDate.of(2026, 1, 16),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                travelPaidMinutes = 30,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            )
        )
        
        val sum = PdfUtilities.sumTravelMinutes(entries)
        
        assertEquals(90, sum)
    }
    
    @Test
    fun `sumTravelMinutes - ohne Reisezeit`() {
        val entries = listOf(
            WorkEntry(
                date = java.time.LocalDate.of(2026, 1, 15),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            )
        )
        
        val sum = PdfUtilities.sumTravelMinutes(entries)
        
        assertEquals(0, sum)
    }
    
    @Test
    fun `filterWorkDays - filtert nur WORK Tags`() {
        val entries = listOf(
            WorkEntry(
                date = java.time.LocalDate.of(2026, 1, 15),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            ),
            WorkEntry(
                date = java.time.LocalDate.of(2026, 1, 16),
                dayType = DayType.OFF,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            ),
            WorkEntry(
                date = java.time.LocalDate.of(2026, 1, 17),
                dayType = DayType.WORK,
                workStart = LocalTime.of(8, 0),
                workEnd = LocalTime.of(19, 0),
                breakMinutes = 60,
                confirmedWorkDay = true,
                confirmationAt = System.currentTimeMillis()
            )
        )
        
        val workDays = PdfUtilities.filterWorkDays(entries)
        
        assertEquals(2, workDays.size)
    }
}
