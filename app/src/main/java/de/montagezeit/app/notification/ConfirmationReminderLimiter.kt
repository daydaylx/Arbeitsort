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

    private fun key(date: LocalDate): String = "count_${date}"
}
