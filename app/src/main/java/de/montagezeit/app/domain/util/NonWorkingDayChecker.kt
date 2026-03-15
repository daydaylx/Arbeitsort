package de.montagezeit.app.domain.util

import de.montagezeit.app.data.preferences.ReminderSettings
import java.time.LocalDate

/**
 * Prüft ob ein Datum ein Nicht-Arbeitstag ist (basierend auf Auto-Off-Regeln).
 * Domain-Layer Interface um direkte Abhängigkeit von work-Package zu vermeiden.
 */
interface NonWorkingDayChecker {
    fun isNonWorkingDay(date: LocalDate, settings: ReminderSettings): Boolean
}
