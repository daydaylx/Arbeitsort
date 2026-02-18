package de.montagezeit.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekCalculatorTest {

    @Test
    fun `weekStart - Wednesday returns Monday of the same week`() {
        val wednesday = LocalDate.of(2024, 1, 10) // Wednesday
        val expected = LocalDate.of(2024, 1, 8)   // Monday
        assertEquals(expected, WeekCalculator.weekStart(wednesday))
    }

    @Test
    fun `weekStart - Monday returns itself`() {
        val monday = LocalDate.of(2024, 1, 8)
        assertEquals(monday, WeekCalculator.weekStart(monday))
    }

    @Test
    fun `weekStart - Sunday returns Monday of same ISO week`() {
        val sunday = LocalDate.of(2024, 1, 14)   // Sunday
        val expected = LocalDate.of(2024, 1, 8)   // Monday of same week
        assertEquals(expected, WeekCalculator.weekStart(sunday))
    }

    @Test
    fun `weekStart - year boundary December to January`() {
        val saturday = LocalDate.of(2023, 12, 30) // Saturday in week that crosses year
        val expected = LocalDate.of(2023, 12, 25) // Monday of that week
        assertEquals(expected, WeekCalculator.weekStart(saturday))
    }

    @Test
    fun `weekStart - new year day that falls mid-week`() {
        val newYearsDay2024 = LocalDate.of(2024, 1, 1) // Monday
        assertEquals(newYearsDay2024, WeekCalculator.weekStart(newYearsDay2024))
    }

    @Test
    fun `weekStart - new year day that falls Thursday`() {
        val newYearsDay2015 = LocalDate.of(2015, 1, 1) // Thursday
        val expected = LocalDate.of(2014, 12, 29)       // Monday of that ISO week
        assertEquals(expected, WeekCalculator.weekStart(newYearsDay2015))
    }

    @Test
    fun `weekDays - returns 7 days starting from Monday`() {
        val monday = LocalDate.of(2024, 1, 8)
        val days = WeekCalculator.weekDays(monday)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2024, 1, 8), days[0])  // Mon
        assertEquals(LocalDate.of(2024, 1, 9), days[1])  // Tue
        assertEquals(LocalDate.of(2024, 1, 10), days[2]) // Wed
        assertEquals(LocalDate.of(2024, 1, 11), days[3]) // Thu
        assertEquals(LocalDate.of(2024, 1, 12), days[4]) // Fri
        assertEquals(LocalDate.of(2024, 1, 13), days[5]) // Sat
        assertEquals(LocalDate.of(2024, 1, 14), days[6]) // Sun
    }

    @Test
    fun `weekDays - all days are sequential`() {
        val monday = LocalDate.of(2024, 6, 3)
        val days = WeekCalculator.weekDays(monday)
        for (i in 1 until days.size) {
            assertEquals(days[i - 1].plusDays(1), days[i])
        }
    }

    @Test
    fun `weekDays - crosses month boundary correctly`() {
        val monday = LocalDate.of(2024, 1, 29) // Week crossing Jan->Feb
        val days = WeekCalculator.weekDays(monday)
        assertEquals(LocalDate.of(2024, 1, 29), days[0])
        assertEquals(LocalDate.of(2024, 2, 4), days[6])
    }

    @Test
    fun `weekStart then weekDays roundtrip - any date gives its full ISO week`() {
        val thursday = LocalDate.of(2024, 3, 14)
        val weekStart = WeekCalculator.weekStart(thursday)
        val days = WeekCalculator.weekDays(weekStart)
        assertEquals(true, days.contains(thursday))
        assertEquals(7, days.size)
    }
}
