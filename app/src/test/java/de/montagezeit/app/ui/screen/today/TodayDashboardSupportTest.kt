package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TodayDashboardSupportTest {

    @Test
    fun `buildWeekDayUi marks unconfirmed entry without snapshots as partial`() {
        val selectedDate = LocalDate.of(2026, 4, 6)
        val todayDate = selectedDate
        val entry = WorkEntry(
            date = selectedDate,
            dayType = DayType.WORK,
            workStart = LocalTime.of(8, 0),
            workEnd = LocalTime.of(16, 0),
            breakMinutes = 0,
            confirmedWorkDay = false
        )

        val weekDays = buildWeekDayUi(
            selectedDate = selectedDate,
            todayDate = todayDate,
            entries = listOf(entry)
        )

        val selectedDay = weekDays.find { it.date == selectedDate }
        assertNotNull(selectedDay)
        assertEquals(WeekDayStatus.PARTIAL, selectedDay?.status)
    }
}
