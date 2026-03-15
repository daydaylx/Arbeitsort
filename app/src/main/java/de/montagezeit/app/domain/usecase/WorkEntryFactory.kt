package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import java.time.DayOfWeek
import java.time.LocalDate

object WorkEntryFactory {

    fun resolveAutoDayType(date: LocalDate, settings: ReminderSettings): DayType {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val isHoliday = settings.holidayDates.contains(date)
        return if ((settings.autoOffWeekends && isWeekend) || (settings.autoOffHolidays && isHoliday)) {
            DayType.OFF
        } else {
            DayType.WORK
        }
    }

    fun createDefaultEntry(
        date: LocalDate,
        settings: ReminderSettings,
        dayType: DayType = resolveAutoDayType(date, settings),
        dayLocationLabel: String = "",
        dayLocationSource: DayLocationSource = DayLocationSource.FALLBACK,
        now: Long = System.currentTimeMillis()
    ): WorkEntry {
        return WorkEntry(
            date = date,
            dayType = dayType,
            workStart = settings.workStart,
            workEnd = settings.workEnd,
            breakMinutes = settings.breakMinutes,
            dayLocationLabel = dayLocationLabel,
            dayLocationSource = dayLocationSource,
            createdAt = now,
            updatedAt = now
        )
    }
}
