package de.montagezeit.app.work

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ReminderScheduleCalculatorTest {

    @Test
    fun `periodic interval enforces minimum of 15 minutes`() {
        assertEquals(15L, ReminderScheduleCalculator.periodicIntervalMinutes(1))
        assertEquals(15L, ReminderScheduleCalculator.periodicIntervalMinutes(15))
        assertEquals(120L, ReminderScheduleCalculator.periodicIntervalMinutes(120))
    }

    @Test
    fun `delay to window start is zero while inside window`() {
        val delay = ReminderScheduleCalculator.delayToWindowStart(
            now = LocalTime.of(10, 0),
            windowStart = LocalTime.of(6, 0),
            windowEnd = LocalTime.of(13, 0)
        )

        assertEquals(0L, delay.toMinutes())
    }

    @Test
    fun `delay to window start rolls to next day at end boundary`() {
        val delay = ReminderScheduleCalculator.delayToWindowStart(
            now = LocalTime.of(13, 0),
            windowStart = LocalTime.of(6, 0),
            windowEnd = LocalTime.of(13, 0)
        )

        assertEquals(17L * 60L, delay.toMinutes())
    }

    @Test
    fun `delay to daily time schedules next day when now equals target`() {
        val delay = ReminderScheduleCalculator.delayToTime(
            now = LocalTime.of(18, 0),
            target = LocalTime.of(18, 0)
        )

        assertEquals(24L * 60L, delay.toMinutes())
    }
}
