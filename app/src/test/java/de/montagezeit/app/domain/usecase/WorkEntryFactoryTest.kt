package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.domain.util.AppDefaults
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WorkEntryFactoryTest {

    private val defaultSettings = ReminderSettings(
        autoOffWeekends = true,
        autoOffHolidays = true,
        workStart = LocalTime.of(8, 0),
        workEnd = LocalTime.of(17, 0),
        breakMinutes = 45
    )

    // --- resolveAutoDayType ---

    @Test
    fun `resolveAutoDayType returns WORK for weekday when autoOffWeekends is false`() {
        val monday = LocalDate.of(2024, 6, 3)
        val settings = defaultSettings.copy(autoOffWeekends = false)
        assertEquals(DayType.WORK, WorkEntryFactory.resolveAutoDayType(monday, settings))
    }

    @Test
    fun `resolveAutoDayType returns WORK for weekday when autoOffWeekends is true`() {
        val tuesday = LocalDate.of(2024, 6, 4)
        assertEquals(DayType.WORK, WorkEntryFactory.resolveAutoDayType(tuesday, defaultSettings))
    }

    @Test
    fun `resolveAutoDayType returns OFF for Saturday when autoOffWeekends is true`() {
        val saturday = LocalDate.of(2024, 6, 8)
        assertEquals(DayType.OFF, WorkEntryFactory.resolveAutoDayType(saturday, defaultSettings))
    }

    @Test
    fun `resolveAutoDayType returns OFF for Sunday when autoOffWeekends is true`() {
        val sunday = LocalDate.of(2024, 6, 9)
        assertEquals(DayType.OFF, WorkEntryFactory.resolveAutoDayType(sunday, defaultSettings))
    }

    @Test
    fun `resolveAutoDayType returns WORK for Saturday when autoOffWeekends is false`() {
        val saturday = LocalDate.of(2024, 6, 8)
        val settings = defaultSettings.copy(autoOffWeekends = false, autoOffHolidays = false)
        assertEquals(DayType.WORK, WorkEntryFactory.resolveAutoDayType(saturday, settings))
    }

    @Test
    fun `resolveAutoDayType returns OFF for holiday when autoOffHolidays is true`() {
        val holiday = LocalDate.of(2024, 12, 25)
        val settings = defaultSettings.copy(
            autoOffWeekends = false,
            autoOffHolidays = true,
            holidayDates = setOf(holiday)
        )
        assertEquals(DayType.OFF, WorkEntryFactory.resolveAutoDayType(holiday, settings))
    }

    @Test
    fun `resolveAutoDayType returns WORK for holiday when autoOffHolidays is false`() {
        val holiday = LocalDate.of(2024, 12, 25)
        val settings = defaultSettings.copy(
            autoOffWeekends = false,
            autoOffHolidays = false,
            holidayDates = setOf(holiday)
        )
        assertEquals(DayType.WORK, WorkEntryFactory.resolveAutoDayType(holiday, settings))
    }

    @Test
    fun `resolveAutoDayType returns OFF for weekend holiday when both flags are true`() {
        // Christmas 2021 is a Saturday
        val christmasSaturday = LocalDate.of(2021, 12, 25)
        val settings = defaultSettings.copy(
            autoOffWeekends = true,
            autoOffHolidays = true,
            holidayDates = setOf(christmasSaturday)
        )
        assertEquals(DayType.OFF, WorkEntryFactory.resolveAutoDayType(christmasSaturday, settings))
    }

    // --- createDefaultEntry ---

    @Test
    fun `createDefaultEntry uses resolveAutoDayType when no explicit dayType provided`() {
        val saturday = LocalDate.of(2024, 6, 8)
        val entry = WorkEntryFactory.createDefaultEntry(saturday, defaultSettings)
        assertEquals(DayType.OFF, entry.dayType)
    }

    @Test
    fun `createDefaultEntry uses provided dayType when explicitly specified`() {
        val saturday = LocalDate.of(2024, 6, 8)
        val entry = WorkEntryFactory.createDefaultEntry(saturday, defaultSettings, dayType = DayType.WORK)
        assertEquals(DayType.WORK, entry.dayType)
    }

    @Test
    fun `createDefaultEntry sets workStart workEnd breakMinutes from settings`() {
        val date = LocalDate.of(2024, 6, 3)
        val entry = WorkEntryFactory.createDefaultEntry(date, defaultSettings)
        assertEquals(LocalTime.of(8, 0), entry.workStart)
        assertEquals(LocalTime.of(17, 0), entry.workEnd)
        assertEquals(45, entry.breakMinutes)
    }

    @Test
    fun `createDefaultEntry uses provided now timestamp for createdAt and updatedAt`() {
        val date = LocalDate.of(2024, 6, 3)
        val fixedNow = 1_000_000L
        val entry = WorkEntryFactory.createDefaultEntry(date, defaultSettings, now = fixedNow)
        assertEquals(fixedNow, entry.createdAt)
        assertEquals(fixedNow, entry.updatedAt)
    }

    @Test
    fun `createDefaultEntry falls back to app defaults for invalid work time settings`() {
        val invalidSettings = defaultSettings.copy(
            workStart = LocalTime.of(18, 0),
            workEnd = LocalTime.of(17, 0)
        )

        val entry = WorkEntryFactory.createDefaultEntry(LocalDate.of(2024, 6, 3), invalidSettings)

        assertEquals(AppDefaults.WORK_START, entry.workStart)
        assertEquals(AppDefaults.WORK_END, entry.workEnd)
        assertEquals(45, entry.breakMinutes)
    }
}
