package de.montagezeit.app.work

import de.montagezeit.app.data.preferences.ReminderSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object ReminderWindowEvaluator {

    fun isInMorningWindow(currentTime: LocalTime, settings: ReminderSettings): Boolean {
        return !currentTime.isBefore(settings.morningWindowStart) &&
            currentTime.isBefore(settings.morningWindowEnd)
    }

    fun isInEveningWindow(currentTime: LocalTime, settings: ReminderSettings): Boolean {
        return !currentTime.isBefore(settings.eveningWindowStart) &&
            currentTime.isBefore(settings.eveningWindowEnd)
    }

    fun isAfterFallbackTime(currentTime: LocalTime, settings: ReminderSettings): Boolean {
        return !currentTime.isBefore(settings.fallbackTime)
    }

    fun isNonWorkingDay(date: LocalDate, settings: ReminderSettings): Boolean {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val weekendOff = settings.autoOffWeekends && isWeekend
        val holidayOff = settings.autoOffHolidays && settings.holidayDates.contains(date)
        return weekendOff || holidayOff
    }
}
