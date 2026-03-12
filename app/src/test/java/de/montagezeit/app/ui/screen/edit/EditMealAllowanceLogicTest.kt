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
            breakfastIncluded = true
        )

        assertTrue(result.isArrivalDeparture)
        assertTrue(result.breakfastIncluded)
        assertEquals(1400, result.baseCents)
        assertEquals(820, result.amountCents)
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
    fun `fromEntry keeps meal allowance flags in edit form`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 3, 12),
            dayType = DayType.WORK,
            mealIsArrivalDeparture = true,
            mealBreakfastIncluded = true,
            mealAllowanceBaseCents = 1400,
            mealAllowanceAmountCents = 820
        )

        val formData = EditFormData.fromEntry(entry)

        assertTrue(formData.mealIsArrivalDeparture)
        assertTrue(formData.mealBreakfastIncluded)
        assertEquals(820, formData.mealAllowancePreviewCents())
    }
}
