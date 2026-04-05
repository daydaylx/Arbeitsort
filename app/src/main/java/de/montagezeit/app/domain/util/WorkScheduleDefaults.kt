package de.montagezeit.app.domain.util

import de.montagezeit.app.data.preferences.ReminderSettings
import java.time.LocalTime

data class WorkScheduleDefaults(
    val workStart: LocalTime,
    val workEnd: LocalTime,
    val breakMinutes: Int
)

fun isValidWorkTimeRange(workStart: LocalTime, workEnd: LocalTime): Boolean {
    return workEnd.isAfter(workStart)
}

private fun calculateRawWorkDurationMinutes(workStart: LocalTime, workEnd: LocalTime): Int {
    val startMinutes = workStart.hour * 60 + workStart.minute
    val endMinutes = workEnd.hour * 60 + workEnd.minute
    return endMinutes - startMinutes
}

fun hasPositiveNetWorkDuration(
    workStart: LocalTime,
    workEnd: LocalTime,
    breakMinutes: Int
): Boolean {
    if (!isValidWorkTimeRange(workStart, workEnd)) return false
    val durationMinutes = calculateRawWorkDurationMinutes(workStart, workEnd)
    return breakMinutes in 0 until durationMinutes
}

fun resolveWorkScheduleDefaults(
    workStart: LocalTime,
    workEnd: LocalTime,
    breakMinutes: Int
): WorkScheduleDefaults {
    val normalizedBreakMinutes = breakMinutes.coerceIn(0, 180)
    return if (hasPositiveNetWorkDuration(workStart, workEnd, normalizedBreakMinutes)) {
        WorkScheduleDefaults(
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = normalizedBreakMinutes
        )
    } else {
        WorkScheduleDefaults(
            workStart = AppDefaults.WORK_START,
            workEnd = AppDefaults.WORK_END,
            breakMinutes = AppDefaults.BREAK_MINUTES
        )
    }
}

fun resolveWorkScheduleDefaults(settings: ReminderSettings): WorkScheduleDefaults {
    return resolveWorkScheduleDefaults(
        workStart = settings.workStart,
        workEnd = settings.workEnd,
        breakMinutes = settings.breakMinutes
    )
}
