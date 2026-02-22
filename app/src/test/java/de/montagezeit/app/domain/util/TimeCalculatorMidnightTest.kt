package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TimeCalculatorMidnightTest {

    private val date = LocalDate.of(2024, 1, 5)

    private fun epochOf(time: LocalTime): Long =
        date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun entryWithTravel(start: LocalTime, arrive: LocalTime) = WorkEntry(
        date = date,
        travelStartAt = epochOf(start),
        travelArriveAt = epochOf(arrive)
    )

    @Test
    fun `normal travel within day returns correct minutes`() {
        val entry = entryWithTravel(LocalTime.of(8, 0), LocalTime.of(17, 0))
        assertEquals(540, TimeCalculator.calculateTravelMinutes(entry))
    }

    @Test
    fun `overnight travel crossing midnight returns correct minutes`() {
        // 23:00 → 01:00 = 120 min
        val entry = entryWithTravel(LocalTime.of(23, 0), LocalTime.of(1, 0))
        assertEquals(120, TimeCalculator.calculateTravelMinutes(entry))
    }

    @Test
    fun `short overnight travel crossing midnight returns correct minutes`() {
        // 23:50 → 00:10 = 20 min
        val entry = entryWithTravel(LocalTime.of(23, 50), LocalTime.of(0, 10))
        assertEquals(20, TimeCalculator.calculateTravelMinutes(entry))
    }

    @Test
    fun `travelPaidMinutes takes priority over calculated value`() {
        val entry = WorkEntry(
            date = date,
            travelStartAt = epochOf(LocalTime.of(8, 0)),
            travelArriveAt = epochOf(LocalTime.of(9, 0)),
            travelPaidMinutes = 999
        )
        assertEquals(999, TimeCalculator.calculateTravelMinutes(entry))
    }

    @Test
    fun `negative travelPaidMinutes are clamped to zero`() {
        val entry = WorkEntry(
            date = date,
            travelPaidMinutes = -15
        )
        assertEquals(0, TimeCalculator.calculateTravelMinutes(entry))
    }

    @Test
    fun `no travel fields returns zero`() {
        val entry = WorkEntry(date = date)
        assertEquals(0, TimeCalculator.calculateTravelMinutes(entry))
    }
}
