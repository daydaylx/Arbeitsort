package de.montagezeit.app.notification

import android.content.SharedPreferences
import java.time.LocalDate

class ConfirmationReminderLimiter(
    private val prefs: SharedPreferences,
    private val maxRepeatsPerDay: Int = 2
) {
    fun canSchedule(date: LocalDate): Boolean {
        return getCount(date) < maxRepeatsPerDay
    }

    fun increment(date: LocalDate) {
        val newCount = getCount(date) + 1
        prefs.edit()
            .putInt(key(date), newCount)
            .apply()
    }

    fun reset(date: LocalDate) {
        prefs.edit()
            .remove(key(date))
            .apply()
    }

    fun getCount(date: LocalDate): Int {
        return prefs.getInt(key(date), 0)
    }

    fun cleanup(today: LocalDate, keepDays: Int = 7) {
        val cutoff = today.minusDays(keepDays.toLong())
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith("count_") }
            .forEach { key ->
                runCatching { LocalDate.parse(key.removePrefix("count_")) }
                    .getOrNull()
                    ?.let { date -> if (date < cutoff) editor.remove(key) }
            }
        editor.apply()
    }

    private fun key(date: LocalDate): String = "count_${date}"
}
