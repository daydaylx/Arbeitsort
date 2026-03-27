package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimeCalculatorTest {

    private val date = LocalDate.now()

    private fun travelLeg(paidMinutes: Int) =
        TravelLeg(workEntryDate = date, sortOrder = 0, paidMinutesOverride = paidMinutes)

    private fun travelLegWithTimestamps(start: LocalTime, arrive: LocalTime): TravelLeg {
        val startMs = date.atTime(start).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val arriveMs = date.atTime(arrive).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return TravelLeg(workEntryDate = date, sortOrder = 0, startAt = startMs, arriveAt = arriveMs)
    }

    @Test
    fun `calculate - Standard Work Day with Travel`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(19, 0), // 11 hours = 660 min
            breakMinutes = 60 // work = 600 min
        )
        val legs = listOf(travelLeg(120))

        assertEquals(600, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(120, TimeCalculator.calculateTravelMinutes(legs))
        assertEquals(720, TimeCalculator.calculatePaidTotalMinutes(entry, legs))
        assertEquals(10.0, TimeCalculator.calculateWorkHours(entry), 0.01)
        assertEquals(12.0, TimeCalculator.calculatePaidTotalHours(entry, legs), 0.01)
    }

    @Test
    fun `calculate - No Travel`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(16, 30), // 8.5h = 510 min
            breakMinutes = 30 // work = 480 min
        )

        assertEquals(480, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(0, TimeCalculator.calculateTravelMinutes(emptyList()))
        assertEquals(480, TimeCalculator.calculatePaidTotalMinutes(entry, emptyList()))
    }

    @Test
    fun `calculate - Break larger than Work`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(8, 30), // 30 min
            breakMinutes = 45 // work < 0 -> 0
        )
        val legs = listOf(travelLeg(60))

        assertEquals(0, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(60, TimeCalculator.calculatePaidTotalMinutes(entry, legs))
    }

    @Test
    fun `calculate - End before Start`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(18, 0),
            workEnd = LocalTime.of(8, 0), // Negative duration -> 0
            breakMinutes = 0
        )
        val legs = listOf(travelLeg(60))

        assertEquals(0, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(60, TimeCalculator.calculatePaidTotalMinutes(entry, legs))
    }

    @Test
    fun `calculate - Off Day with Travel`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.OFF,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        val legs = listOf(travelLeg(120))

        assertEquals(0, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(120, TimeCalculator.calculateTravelMinutes(legs))
        assertEquals(120, TimeCalculator.calculatePaidTotalMinutes(entry, legs))
    }

    @Test
    fun `calculate - Paid Total uses Travel Time`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(17, 0), // 9h = 540 min
            breakMinutes = 40 // Work = 500 min
        )
        val legs = listOf(travelLeg(45)) // Travel = 45 min

        val work = TimeCalculator.calculateWorkMinutes(entry)
        val travel = TimeCalculator.calculateTravelMinutes(legs)
        val total = TimeCalculator.calculatePaidTotalMinutes(entry, legs)

        assertEquals(500, work)
        assertEquals(45, travel)
        assertEquals(545, total) // 500 + 45
    }

    @Test
    fun `calculate - Travel from Timestamps via TravelLeg`() {
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(9, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60
        )
        val legs = listOf(travelLegWithTimestamps(LocalTime.of(7, 0), LocalTime.of(9, 0)))

        assertEquals(120, TimeCalculator.calculateTravelMinutes(legs))
        assertEquals(420 + 120, TimeCalculator.calculatePaidTotalMinutes(entry, legs))
    }

    @Test
    fun `calculate - Anreise plus Arbeit plus Rueckreise (3 Legs) summiert korrekt`() {
        // Außendienst-Tag: Anreise 60min, Arbeit 8h, Rückreise 90min
        val entry = WorkEntry(
            date = date,
            dayType = DayType.WORK,
            workStart = LocalTime.of(9, 0),
            workEnd = LocalTime.of(17, 0),
            breakMinutes = 60 // net work = 420 min
        )
        val outbound = TravelLeg(workEntryDate = date, sortOrder = 0,
            category = TravelLegCategory.OUTBOUND,
            paidMinutesOverride = 60)
        val ret = TravelLeg(workEntryDate = date, sortOrder = 1,
            category = TravelLegCategory.RETURN,
            paidMinutesOverride = 90)
        val legs = listOf(outbound, ret)

        assertEquals(420, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(150, TimeCalculator.calculateTravelMinutes(legs)) // 60+90
        assertEquals(570, TimeCalculator.calculatePaidTotalMinutes(entry, legs)) // 420+150
    }

    @Test
    fun `calculate - INTERSITE Legs summieren korrekt`() {
        val legs = listOf(
            TravelLeg(workEntryDate = date, sortOrder = 0,
                category = TravelLegCategory.OUTBOUND,
                paidMinutesOverride = 30),
            TravelLeg(workEntryDate = date, sortOrder = 1,
                category = TravelLegCategory.INTERSITE,
                paidMinutesOverride = 45),
            TravelLeg(workEntryDate = date, sortOrder = 2,
                category = TravelLegCategory.RETURN,
                paidMinutesOverride = 30)
        )

        assertEquals(30 + 45 + 30, TimeCalculator.calculateTravelMinutes(legs))
    }

    @Test
    fun `calculate - Pause exakt gleich Arbeitszeit ergibt 0 nicht negativ`() {
        val entry = WorkEntry(date = date, dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(9, 0), // 60 min
            breakMinutes = 60) // Pause = Arbeitszeit

        assertEquals(0, TimeCalculator.calculateWorkMinutes(entry))
    }

    @Test
    fun `calculate - OFF-Tag mit TravelLegs ergibt Arbeitszeit 0 und Reisezeit korrekt`() {
        val entry = WorkEntry(date = date, dayType = DayType.OFF,
            workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(17, 0), breakMinutes = 60)
        val legs = listOf(travelLeg(180))

        assertEquals(0, TimeCalculator.calculateWorkMinutes(entry))
        assertEquals(180, TimeCalculator.calculateTravelMinutes(legs))
        assertEquals(180, TimeCalculator.calculatePaidTotalMinutes(entry, legs))
    }
}
