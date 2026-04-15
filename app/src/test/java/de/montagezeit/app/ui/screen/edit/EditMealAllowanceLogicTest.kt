package de.montagezeit.app.ui.screen.edit

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EditMealAllowanceLogicTest {

    @Test
    fun `resolveMealAllowanceForSave keeps work meal flags and calculates amount`() {
        val result = resolveMealAllowanceForSave(
            dayType = DayType.WORK,
            isArrivalDeparture = true,
            breakfastIncluded = true,
            workMinutes = 480,
            travelMinutes = 0
        )

        assertTrue(result.isArrivalDeparture)
        assertTrue(result.breakfastIncluded)
        assertEquals(1400, result.baseCents)
        assertEquals(840, result.amountCents)
    }

    @Test
    fun `resolveMealAllowanceForSave clears meal data for off day`() {
        val result = resolveMealAllowanceForSave(
            dayType = DayType.OFF,
            isArrivalDeparture = true,
            breakfastIncluded = true
        )

        assertFalse(result.isArrivalDeparture)
        assertFalse(result.breakfastIncluded)
        assertEquals(0, result.baseCents)
        assertEquals(0, result.amountCents)
    }

    @Test
    fun `resolveMealAllowanceForSave clears meal data for work day without activity`() {
        val result = resolveMealAllowanceForSave(
            dayType = DayType.WORK,
            isArrivalDeparture = true,
            breakfastIncluded = true,
            workMinutes = 0,
            travelMinutes = 0
        )

        assertFalse(result.isArrivalDeparture)
        assertFalse(result.breakfastIncluded)
        assertEquals(0, result.baseCents)
        assertEquals(0, result.amountCents)
    }

    @Test
    fun `fromEntry keeps meal allowance flags in edit form`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.WORK,
            workStart = java.time.LocalTime.of(8, 0),
            workEnd = java.time.LocalTime.of(17, 0),
            breakMinutes = 60,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 840
        )

        val formData = EditFormData.fromEntry(entry)

        assertTrue(formData.mealIsArrivalDeparture)
        assertTrue(formData.mealBreakfastIncluded)
        assertEquals(840, formData.mealAllowancePreviewCents())
    }
}
