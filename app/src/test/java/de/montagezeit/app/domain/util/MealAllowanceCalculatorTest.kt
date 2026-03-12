package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import org.junit.Assert.assertEquals
import org.junit.Test

class MealAllowanceCalculatorTest {

    @Test
    fun `WORK normal no breakfast returns 2800`() {
        val result = MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = false,
            breakfastIncluded = false
        )
        assertEquals(2800, result.baseCents)
        assertEquals(2800, result.amountCents)
    }

    @Test
    fun `WORK normal with breakfast returns 2220`() {
        val result = MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = false,
            breakfastIncluded = true
        )
        assertEquals(2800, result.baseCents)
        assertEquals(2220, result.amountCents)
    }

    @Test
    fun `WORK arrival no breakfast returns 1400`() {
        val result = MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = true,
            breakfastIncluded = false
        )
        assertEquals(1400, result.baseCents)
        assertEquals(1400, result.amountCents)
    }

    @Test
    fun `WORK arrival with breakfast returns 820`() {
        val result = MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = true,
            breakfastIncluded = true
        )
        assertEquals(1400, result.baseCents)
        assertEquals(820, result.amountCents)
    }

    @Test
    fun `OFF returns zero regardless of flags`() {
        val result = MealAllowanceCalculator.calculate(
            dayType = DayType.OFF,
            isArrivalDeparture = false,
            breakfastIncluded = false
        )
        assertEquals(0, result.baseCents)
        assertEquals(0, result.amountCents)
    }

    @Test
    fun `COMP_TIME returns zero regardless of flags`() {
        val result = MealAllowanceCalculator.calculate(
            dayType = DayType.COMP_TIME,
            isArrivalDeparture = true,
            breakfastIncluded = true
        )
        assertEquals(0, result.baseCents)
        assertEquals(0, result.amountCents)
    }

    @Test
    fun `amount is never negative`() {
        // Even with both deductions applied, result >= 0
        val result = MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = true,
            breakfastIncluded = true
        )
        assertEquals(820, result.amountCents)
        assert(result.amountCents >= 0)
    }

    @Test
    fun `formatEuro formats 2220 as 22 comma 20 euro`() {
        assertEquals("22,20 €", MealAllowanceCalculator.formatEuro(2220))
    }

    @Test
    fun `formatEuro formats 2800 as 28 comma 00 euro`() {
        assertEquals("28,00 €", MealAllowanceCalculator.formatEuro(2800))
    }

    @Test
    fun `formatEuro formats 0 as 0 comma 00 euro`() {
        assertEquals("0,00 €", MealAllowanceCalculator.formatEuro(0))
    }

    @Test
    fun `formatEuro formats 820 as 8 comma 20 euro`() {
        assertEquals("8,20 €", MealAllowanceCalculator.formatEuro(820))
    }
}
