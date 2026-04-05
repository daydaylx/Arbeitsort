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

fun resolveWorkScheduleDefaults(
    workStart: LocalTime,
    workEnd: LocalTime,
    breakMinutes: Int
): WorkScheduleDefaults {
    val normalizedBreakMinutes = breakMinutes.coerceIn(0, 180)
    return if (isValidWorkTimeRange(workStart, workEnd)) {
        WorkScheduleDefaults(
            workStart = workStart,
            workEnd = workEnd,
            breakMinutes = normalizedBreakMinutes
        )
    } else {
        WorkScheduleDefaults(
            workStart = AppDefaults.WORK_START,
            workEnd = AppDefaults.WORK_END,
            breakMinutes = normalizedBreakMinutes
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
