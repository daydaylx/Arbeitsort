package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.preferences.ReminderSettings

fun dailyTargetHoursFromSettings(settings: ReminderSettings): Double {
    val startMinutes = settings.workStart.hour * 60 + settings.workStart.minute
    val endMinutes = settings.workEnd.hour * 60 + settings.workEnd.minute
    val durationMinutes = endMinutes - startMinutes
    if (durationMinutes <= 0) return 0.0

    val breakMinutes = settings.breakMinutes.coerceAtLeast(0)
    val targetMinutes = durationMinutes - breakMinutes
    if (targetMinutes <= 0) return 0.0

    return targetMinutes / 60.0
}
