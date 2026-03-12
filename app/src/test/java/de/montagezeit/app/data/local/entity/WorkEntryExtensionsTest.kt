package de.montagezeit.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class WorkEntryExtensionsTest {

    private val now = System.currentTimeMillis()
    private val date = LocalDate.now()

    private fun baseEntry(
        travelPaidMinutes: Int? = null,
        travelStartAt: Long? = null,
        travelArriveAt: Long? = null
    ) = WorkEntry(
        date = date,
        dayType = DayType.WORK,
        dayLocationLabel = "Baustelle",
        workStart = LocalTime.of(8, 0),
        workEnd = LocalTime.of(17, 0),
        breakMinutes = 60,
        travelPaidMinutes = travelPaidMinutes,
        travelStartAt = travelStartAt,
        travelArriveAt = travelArriveAt
    )

    @Test
    fun `withTravelCleared setzt travelPaidMinutes auf null`() {
        val entry = baseEntry(travelPaidMinutes = 120)
        val cleared = entry.withTravelCleared(now)
        assertNull(
            "withTravelCleared muss travelPaidMinutes auf null setzen, nicht 0",
            cleared.travelPaidMinutes
        )
    }

    @Test
    fun `withTravelCleared setzt travelPaidMinutes nicht auf 0`() {
        val entry = baseEntry(travelPaidMinutes = 60)
        val cleared = entry.withTravelCleared(now)
        assertNull(cleared.travelPaidMinutes)
        assertEquals(0 != cleared.travelPaidMinutes, true)
    }

    @Test
    fun `withTravelCleared loescht Timestamps`() {
        val start = epochOf(LocalTime.of(7, 0))
        val arrive = epochOf(LocalTime.of(9, 0))
        val entry = baseEntry(travelStartAt = start, travelArriveAt = arrive)
        val cleared = entry.withTravelCleared(now)
        assertNull(cleared.travelStartAt)
        assertNull(cleared.travelArriveAt)
    }

    @Test
    fun `nach withTravelCleared koennen neue Timestamps wirken`() {
        val entry = baseEntry(travelPaidMinutes = 60)
        val cleared = entry.withTravelCleared(now)
        val start = epochOf(LocalTime.of(7, 0))
        val arrive = epochOf(LocalTime.of(9, 0))
        val withTimestamps = cleared.copy(travelStartAt = start, travelArriveAt = arrive)
        val travelMin = de.montagezeit.app.domain.util.TimeCalculator.calculateTravelMinutes(withTimestamps)
        assertEquals("Nach withTravelCleared müssen Timestamps ausgewertet werden, nicht blockiert", 120, travelMin)
    }

    @Test
    fun `createConfirmedOffDayEntry hat travelPaidMinutes null`() {
        val entry = createConfirmedOffDayEntry(
            date = date,
            source = "TEST",
            now = now,
            fallbackDayLocationLabel = ""
        )
        assertNull(
            "createConfirmedOffDayEntry darf travelPaidMinutes nicht auf 0 setzen",
            entry.travelPaidMinutes
        )
    }

    @Test
    fun `withConfirmedOffDay clearTravel nutzt null`() {
        val entry = baseEntry(travelPaidMinutes = 90)
        val offDay = entry.withConfirmedOffDay(source = "TEST", now = now, fallbackDayLocationLabel = "")
        assertNull(offDay.travelPaidMinutes)
    }

    @Test
    fun `confirmationStateForDayType sets fresh comp time confirmation`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.WORK,
            confirmedWorkDay = true,
            confirmationAt = 1_000L,
            confirmationSource = "UI"
        )

        val result = entry.confirmationStateForDayType(
            dayType = DayType.COMP_TIME,
            now = 5_000L
        )

        assertTrue(result.confirmedWorkDay)
        assertEquals(5_000L, result.confirmationAt)
        assertEquals(DayType.COMP_TIME.name, result.confirmationSource)
    }

    @Test
    fun `confirmationStateForDayType clears comp time confirmation when leaving comp time`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true,
            confirmationAt = 2_000L,
            confirmationSource = DayType.COMP_TIME.name
        )

        val result = entry.confirmationStateForDayType(
            dayType = DayType.WORK,
            now = 5_000L
        )

        assertFalse(result.confirmedWorkDay)
        assertNull(result.confirmationAt)
        assertNull(result.confirmationSource)
    }

    @Test
    fun `confirmationStateForDayType preserves existing confirmation for non comp time transition`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.WORK,
            confirmedWorkDay = true,
            confirmationAt = 3_000L,
            confirmationSource = "UI"
        )

        val result = entry.confirmationStateForDayType(
            dayType = DayType.OFF,
            now = 5_000L
        )

        assertTrue(result.confirmedWorkDay)
        assertEquals(3_000L, result.confirmationAt)
        assertEquals("UI", result.confirmationSource)
    }

    private fun epochOf(time: LocalTime): Long =
        date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
