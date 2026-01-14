package de.montagezeit.app.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Data class für Reminder-Settings
 */
data class ReminderSettings(
    // Arbeitszeit Defaults
    val workStart: LocalTime = LocalTime.of(8, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val breakMinutes: Int = 60,
    val locationRadiusKm: Int = 30,
    
    // Morning Window
    val morningReminderEnabled: Boolean = true,
    val morningWindowStart: LocalTime = LocalTime.of(6, 0),
    val morningWindowEnd: LocalTime = LocalTime.of(13, 0),
    val morningCheckIntervalMinutes: Int = 120, // 2 Stunden
    
    // Evening Window
    val eveningReminderEnabled: Boolean = true,
    val eveningWindowStart: LocalTime = LocalTime.of(16, 0),
    val eveningWindowEnd: LocalTime = LocalTime.of(22, 30),
    val eveningCheckIntervalMinutes: Int = 180, // 3 Stunden
    
    // Fallback
    val fallbackEnabled: Boolean = true,
    val fallbackTime: LocalTime = LocalTime.of(22, 30),

    // Tägliche Erinnerung
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: LocalTime = LocalTime.of(18, 0),

    // Arbeitsfreie Tage
    val autoOffWeekends: Boolean = true,
    val autoOffHolidays: Boolean = true,
    val holidayDates: Set<LocalDate> = emptySet(),

    // PDF Export Settings
    val pdfEmployeeName: String? = null,
    val pdfCompany: String? = null,
    val pdfProject: String? = null,
    val pdfPersonnelNumber: String? = null
)

/**
 * Keys für DataStore Preferences
 */
object ReminderSettingsKeys {
    // Arbeitszeit Defaults
    val WORK_START = stringPreferencesKey("work_start")
    val WORK_END = stringPreferencesKey("work_end")
    val BREAK_MINUTES = intPreferencesKey("break_minutes")
    val LOCATION_RADIUS_KM = intPreferencesKey("location_radius_km")

    // Morning Window
    val MORNING_REMINDER_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
    val MORNING_WINDOW_START = stringPreferencesKey("morning_window_start")
    val MORNING_WINDOW_END = stringPreferencesKey("morning_window_end")
    val MORNING_CHECK_INTERVAL = intPreferencesKey("morning_check_interval")

    // Evening Window
    val EVENING_REMINDER_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
    val EVENING_WINDOW_START = stringPreferencesKey("evening_window_start")
    val EVENING_WINDOW_END = stringPreferencesKey("evening_window_end")
    val EVENING_CHECK_INTERVAL = intPreferencesKey("evening_check_interval")

    // Fallback
    val FALLBACK_ENABLED = booleanPreferencesKey("fallback_enabled")
    val FALLBACK_TIME = stringPreferencesKey("fallback_time")

    // Tägliche Erinnerung
    val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
    val DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")

    // Arbeitsfreie Tage
    val AUTO_OFF_WEEKENDS = booleanPreferencesKey("auto_off_weekends")
    val AUTO_OFF_HOLIDAYS = booleanPreferencesKey("auto_off_holidays")
    val HOLIDAY_DATES = stringPreferencesKey("holiday_dates")

    // PDF Export Settings
    val PDF_EMPLOYEE_NAME = stringPreferencesKey("pdf_employee_name")
    val PDF_COMPANY = stringPreferencesKey("pdf_company")
    val PDF_PROJECT = stringPreferencesKey("pdf_project")
    val PDF_PERSONNEL_NUMBER = stringPreferencesKey("pdf_personnel_number")
}

/**
 * Hilfsfunktionen für LocalTime Konvertierung
 */
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

fun LocalTime.toPrefString(): String = this.format(TIME_FORMATTER)

fun String?.toLocalTime(): LocalTime {
    return if (this.isNullOrBlank()) {
        LocalTime.MIDNIGHT
    } else {
        try {
            LocalTime.parse(this, TIME_FORMATTER)
        } catch (e: Exception) {
            LocalTime.MIDNIGHT
        }
    }
}

fun Set<LocalDate>.toPrefString(): String {
    if (this.isEmpty()) return ""
    return this.joinToString(",") { it.format(DATE_FORMATTER) }
}

fun String?.toLocalDateSet(): Set<LocalDate> {
    if (this.isNullOrBlank()) return emptySet()
    return this.split(",")
        .mapNotNull { token ->
            val trimmed = token.trim()
            if (trimmed.isBlank()) null else runCatching {
                LocalDate.parse(trimmed, DATE_FORMATTER)
            }.getOrNull()
        }
        .toSet()
}