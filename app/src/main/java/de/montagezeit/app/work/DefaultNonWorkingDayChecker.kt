package de.montagezeit.app.work

import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.domain.util.NonWorkingDayChecker
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation delegating to [ReminderWindowEvaluator] auto-off rules
 * (without DB lookup – only checks weekends/holidays).
 */
@Singleton
class DefaultNonWorkingDayChecker @Inject constructor() : NonWorkingDayChecker {
    override fun isNonWorkingDay(date: LocalDate, settings: ReminderSettings): Boolean {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val weekendOff = settings.autoOffWeekends && isWeekend
        val holidayOff = settings.autoOffHolidays && settings.holidayDates.contains(date)
        return weekendOff || holidayOff
    }
}
