package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimeCalculatorTest {

    private val date = LocalDate.now()

    @Test
    fun `calculate - Standard Work Day with Travel`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0), // 11 hours = 660 min
            breakMinutes = 60, // work = 600 min
            travelPaidMinutes = 120
        )

        assertEquals(600, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(120, TimeCalculator.calculateTravelMinutes(entry))
        assertEquals(720, TimeCalculator.calculatePaidTotalMinutes(entry))
        assertEquals(10.0, TimeCalculator.calculateWorkHours(entry), 0.01)
        assertEquals(12.0, TimeCalculator.calculatePaidTotalHours(entry), 0.01)
    }

    @Test
    fun `calculate - No Travel`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(16, 30), // 8.5h = 510 min
            breakMinutes = 30, // work = 480 min
            travelPaidMinutes = null
        )

        assertEquals(480, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(0, TimeCalculator.calculateTravelMinutes(entry))
        assertEquals(480, TimeCalculator.calculatePaidTotalMinutes(entry))
    }

    @Test
    fun `calculate - Break larger than Work`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(8, 30), // 30 min
            breakMinutes = 45, // work < 0 -> 0
            travelPaidMinutes = 60
        )

        assertEquals(0, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(60, TimeCalculator.calculatePaidTotalMinutes(entry))
    }
    
    @Test
    fun `calculate - End before Start`() {
         val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(18, 0),
            workEnd = LocalTime.of(8, 0), // Negative duration -> 0
            breakMinutes = 0,
            travelPaidMinutes = 60
        )
        
        assertEquals(0, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(60, TimeCalculator.calculatePaidTotalMinutes(entry))
    }

    @Test
    fun `calculate - Off Day`() {
         val entry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60,
            travelPaidMinutes = 120
        )
        
        assertEquals(0, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(120, TimeCalculator.calculateTravelMinutes(entry))
        assertEquals(120, TimeCalculator.calculatePaidTotalMinutes(entry))
    }
}
