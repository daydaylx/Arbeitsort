package de.montagezeit.app.work

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
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

    /**
     * Prüft ob ein Tag ein Nicht-Arbeitstag ist
     *
     * Priorisierung: Manuelle DayType-Einstellungen überschreiben automatische Regeln
     * 1. Wenn DayType == WORK → immer Arbeitstag (auch an Wochenenden)
     * 2. Wenn DayType == OFF → immer Nicht-Arbeitstag
     * 3. Sonst: Auto-Off Regeln anwenden (Wochenende/Feiertage)
     */
    suspend fun isNonWorkingDay(date: LocalDate, settings: ReminderSettings, workEntryDao: WorkEntryDao? = null): Boolean {
        // Prüfe manuelle Einstellungen zuerst
        if (workEntryDao != null) {
            val entry = workEntryDao.getByDate(date)
            if (entry != null) {
                return entry.dayType == DayType.OFF
            }
        }
        
        // Automatische Regeln
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val weekendOff = settings.autoOffWeekends && isWeekend
        val holidayOff = settings.autoOffHolidays && settings.holidayDates.contains(date)
        return weekendOff || holidayOff
    }
}
