package de.montagezeit.app.work

import de.montagezeit.app.data.preferences.ReminderSettings
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
}
