package de.montagezeit.app.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralizes all reminder-flag SharedPreferences access in one place.
 *
 * These flags are per-date booleans tracking whether a specific reminder
 * type has already been shown today (to avoid duplicate notifications).
 */
@Singleton
class ReminderFlagsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun prefs() = context.getSharedPreferences("reminder_flags", Context.MODE_PRIVATE)

    fun isMorningReminded(date: LocalDate): Boolean =
        prefs().getBoolean("morning_reminded_$date", false)

    fun setMorningReminded(date: LocalDate) =
        prefs().edit().putBoolean("morning_reminded_$date", true).apply()

    fun isEveningReminded(date: LocalDate): Boolean =
        prefs().getBoolean("evening_reminded_$date", false)

    fun setEveningReminded(date: LocalDate) =
        prefs().edit().putBoolean("evening_reminded_$date", true).apply()

    fun isFallbackReminded(date: LocalDate): Boolean =
        prefs().getBoolean("fallback_reminded_$date", false)

    fun setFallbackReminded(date: LocalDate) =
        prefs().edit().putBoolean("fallback_reminded_$date", true).apply()

    fun isDailyReminded(date: LocalDate): Boolean =
        prefs().getBoolean("daily_reminded_$date", false)

    fun setDailyReminded(date: LocalDate) =
        prefs().edit().putBoolean("daily_reminded_$date", true).apply()

    fun setAllReminded(date: LocalDate) {
        prefs().edit()
            .putBoolean("morning_reminded_$date", true)
            .putBoolean("evening_reminded_$date", true)
            .putBoolean("fallback_reminded_$date", true)
            .putBoolean("daily_reminded_$date", true)
            .apply()
    }
}
