package de.montagezeit.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkEntryExtensionsTest {

    private val now = System.currentTimeMillis()
    private val date = LocalDate.now()

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
    fun `confirmationStateForDayType sets fresh off confirmation for explicit off transition`() {
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
        assertEquals(5_000L, result.confirmationAt)
        assertEquals(DayType.OFF.name, result.confirmationSource)
    }

    @Test
    fun `confirmationStateForDayType preserves existing off confirmation when already confirmed off`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.OFF,
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

    @Test
    fun `transitionToDayType clears meal data for comp time`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.WORK,
            dayLocationLabel = "Baustelle",
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820
        )

        val result = entry.transitionToDayType(dayType = DayType.COMP_TIME, now = now)

        assertEquals(DayType.COMP_TIME, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertEquals(DayType.COMP_TIME.name, result.confirmationSource)
        assertFalse(result.mealIsArrivalDeparture)
        assertFalse(result.mealBreakfastIncluded)
        assertEquals(0, result.mealAllowanceAmountCents)
    }

    @Test
    fun `transitionToDayType clears auto confirmation when leaving comp time`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true,
            confirmationAt = 2_000L,
            confirmationSource = DayType.COMP_TIME.name
        )

        val result = entry.transitionToDayType(dayType = DayType.WORK, now = now)

        assertEquals(DayType.WORK, result.dayType)
        assertFalse(result.confirmedWorkDay)
        assertNull(result.confirmationAt)
        assertNull(result.confirmationSource)
    }

    @Test
    fun `transitionToDayType confirms explicit off days`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.COMP_TIME,
            confirmedWorkDay = true,
            confirmationAt = 2_000L,
            confirmationSource = DayType.COMP_TIME.name
        )

        val result = entry.transitionToDayType(dayType = DayType.OFF, now = now)

        assertEquals(DayType.OFF, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertEquals(DayType.OFF.name, result.confirmationSource)
        assertEquals(0, result.breakMinutes)
    }

    @Test
    fun `transitionToDayType preserves confirmation when toggling OFF back to WORK`() {
        val originalConfirmationAt = 3_000L
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.WORK,
            confirmedWorkDay = true,
            confirmationAt = originalConfirmationAt,
            confirmationSource = "UI"
        )

        val offEntry = entry.transitionToDayType(dayType = DayType.OFF, now = 5_000L)
        val result = offEntry.transitionToDayType(dayType = DayType.WORK, now = 7_000L)

        assertEquals(DayType.WORK, result.dayType)
        assertTrue(result.confirmedWorkDay)
        assertEquals(5_000L, result.confirmationAt)
        assertEquals(DayType.OFF.name, result.confirmationSource)
        assertEquals(7_000L, result.updatedAt)
    }
}
