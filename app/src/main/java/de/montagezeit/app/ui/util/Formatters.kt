package de.montagezeit.app.ui.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Zentrale Formatierungsfunktionen für konsistente Darstellung in der gesamten App.
 *
 * Verhindert Duplikation und Inkonsistenzen durch wiederholte Implementierungen
 * in verschiedenen Screens (TodayScreen, HistoryScreen, EditEntrySheet, SettingsScreen).
 */
object Formatters {

    // Formatter-Instanzen (Thread-safe, wiederverwendbar)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
    private val dateFormatterShort = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN)
    private val dateFormatterLong = DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)

    /**
     * Formatiert LocalTime zu "HH:mm" (z.B. "08:30").
     *
     * @param time Zeit oder null
     * @return Formatierter String oder "--:--" wenn null
     */
    fun formatTime(time: LocalTime?): String {
        return time?.format(timeFormatter) ?: "--:--"
    }

    /**
     * Formatiert LocalDate zu "dd.MM.yyyy" (z.B. "28.01.2026").
     *
     * @param date Datum oder null
     * @return Formatierter String oder "-- " wenn null
     */
    fun formatDate(date: LocalDate?): String {
        return date?.format(dateFormatter) ?: "--"
    }

    /**
     * Formatiert LocalDate zu "dd.MM." (z.B. "28.01.").
     *
     * @param date Datum oder null
     * @return Formatierter String oder "--" wenn null
     */
    fun formatDateShort(date: LocalDate?): String {
        return date?.format(dateFormatterShort) ?: "--"
    }

    /**
     * Formatiert LocalDate zu "EEEE, dd. MMMM yyyy" (z.B. "Mittwoch, 28. Januar 2026").
     *
     * @param date Datum oder null
     * @return Formatierter String oder "--" wenn null
     */
    fun formatDateLong(date: LocalDate?): String {
        return date?.format(dateFormatterLong) ?: "--"
    }

    /**
     * Formatiert Duration zu "Xh Ymin" (z.B. "2h 30min").
     * Zeigt nur Stunden wenn Minuten = 0, nur Minuten wenn Stunden = 0.
     *
     * @param duration Duration oder null
     * @return Formatierter String oder "--" wenn null
     */
    fun formatDuration(duration: Duration?): String {
        if (duration == null) return "--"

        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}min"
            else -> "0min"
        }
    }

    /**
     * Formatiert Minuten zu "Xh Ymin" oder "X,Y Std." abhängig vom Kontext.
     *
     * @param minutes Gesamtminuten
     * @param asDecimalHours true für "2,5 Std.", false für "2h 30min"
     * @return Formatierter String
     */
    fun formatMinutes(minutes: Int, asDecimalHours: Boolean = false): String {
        if (asDecimalHours) {
            val hours = minutes / 60.0
            return "%.1f Std.".format(Locale.GERMAN, hours)
        }

        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}min"
            hours > 0 -> "${hours}h"
            remainingMinutes > 0 -> "${remainingMinutes}min"
            else -> "0min"
        }
    }

    /**
     * Formatiert Stunden (Double) zu "X,Y Std." (z.B. "8,5 Std.").
     *
     * @param hours Stunden als Double
     * @return Formatierter String
     */
    fun formatHours(hours: Double): String {
        return "%.1f Std.".format(Locale.GERMAN, hours)
    }
}
