package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.TravelLeg
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TimeCalculatorMidnightTest {

    private val date = LocalDate.of(2024, 1, 5)

    private fun epochOf(time: LocalTime): Long =
        date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun legWithTimestamps(start: LocalTime, arrive: LocalTime) = TravelLeg(
        workEntryDate = date,
        sortOrder = 0,
        startAt = epochOf(start),
        arriveAt = epochOf(arrive)
    )

    @Test
    fun `normal travel within day returns correct minutes`() {
        val leg = legWithTimestamps(LocalTime.of(8, 0), LocalTime.of(17, 0))
        assertEquals(540, TimeCalculator.calculateTravelMinutes(listOf(leg)))
    }

    @Test
    fun `overnight travel crossing midnight returns correct minutes`() {
        // 23:00 → 01:00 = 120 min
        val leg = legWithTimestamps(LocalTime.of(23, 0), LocalTime.of(1, 0))
        assertEquals(120, TimeCalculator.calculateTravelMinutes(listOf(leg)))
    }

    @Test
    fun `short overnight travel crossing midnight returns correct minutes`() {
        // 23:50 → 00:10 = 20 min
        val leg = legWithTimestamps(LocalTime.of(23, 50), LocalTime.of(0, 10))
        assertEquals(20, TimeCalculator.calculateTravelMinutes(listOf(leg)))
    }

    @Test
    fun `timestamp leg takes priority over paidMinutesOverride`() {
        val leg = TravelLeg(
            workEntryDate = date,
            sortOrder = 0,
            startAt = epochOf(LocalTime.of(8, 0)),
            arriveAt = epochOf(LocalTime.of(9, 0)),
            paidMinutesOverride = 999
        )
        // When startAt and arriveAt are set, timestamps are used (paidMinutesOverride ignored)
        assertEquals(60, TimeCalculator.calculateTravelMinutes(listOf(leg)))
    }

    @Test
    fun `negative paidMinutesOverride is clamped to zero`() {
        val leg = TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = -15)
        assertEquals(0, TimeCalculator.calculateTravelMinutes(listOf(leg)))
    }

    @Test
    fun `paidMinutesOverride used when no timestamps set`() {
        val leg = TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = 45)
        assertEquals(45, TimeCalculator.calculateTravelMinutes(listOf(leg)))
    }

    @Test
    fun `no travel legs returns zero`() {
        assertEquals(0, TimeCalculator.calculateTravelMinutes(emptyList()))
    }
}
