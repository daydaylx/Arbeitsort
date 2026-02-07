package de.montagezeit.app.work

import java.time.Duration
import java.time.LocalTime

object ReminderScheduleCalculator {

    private const val MIN_PERIODIC_INTERVAL_MINUTES = 15

    fun periodicIntervalMinutes(configuredMinutes: Int): Long {
        return configuredMinutes.coerceAtLeast(MIN_PERIODIC_INTERVAL_MINUTES).toLong()
    }

    fun delayToWindowStart(
        now: LocalTime,
        windowStart: LocalTime,
        windowEnd: LocalTime
    ): Duration {
        return when {
            now.isBefore(windowStart) -> Duration.between(now, windowStart)
            !now.isBefore(windowEnd) -> Duration.between(now, windowStart).plusDays(1)
            else -> Duration.ZERO
        }
    }

    fun delayToTime(now: LocalTime, target: LocalTime): Duration {
        return if (now.isBefore(target)) {
            Duration.between(now, target)
        } else {
            Duration.between(now, target).plusDays(1)
        }
    }
}
