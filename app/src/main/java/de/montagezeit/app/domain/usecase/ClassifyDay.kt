package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.model.DayClassification
import de.montagezeit.app.domain.util.TimeCalculator

/**
 * Klassifiziert einen Tag basierend auf DayType, Arbeitszeit und Reisezeit.
 * 
 * Diese Klassifikation ist wichtig für:
 * - Korrekte Statistikberechnung
 * - Validierung von Verpflegungspauschalen
 * - UI-Darstellung
 */
class ClassifyDay {

    /**
     * Klassifiziert einen Tag anhand seines WorkEntry und der Reisebeine.
     * 
     * @param entry Der WorkEntry mit optionalen TravelLegs
     * @return Die passende DayClassification
     */
    operator fun invoke(entry: WorkEntryWithTravelLegs): DayClassification {
        return classify(
            dayType = entry.workEntry.dayType,
            workMinutes = TimeCalculator.calculateWorkMinutes(entry.workEntry),
            travelMinutes = TimeCalculator.calculateTravelMinutes(entry.orderedTravelLegs)
        )
    }

    /**
     * Klassifiziert einen Tag anhand der Rohdaten.
     * 
     * @param dayType Der Tagtyp (WORK, OFF, COMP_TIME)
     * @param workMinutes Die Arbeitsminuten (berechnet)
     * @param travelMinutes Die Reiseminuten (berechnet)
     * @return Die passende DayClassification
     */
    operator fun invoke(
        dayType: DayType,
        workMinutes: Int,
        travelMinutes: Int
    ): DayClassification {
        return classify(dayType, workMinutes, travelMinutes)
    }

    private fun classify(
        dayType: DayType,
        workMinutes: Int,
        travelMinutes: Int
    ): DayClassification {
        val hasWork = workMinutes > 0
        val hasTravel = travelMinutes > 0

        return when (dayType) {
            DayType.OFF -> {
                if (hasTravel) DayClassification.FREI_MIT_REISE else DayClassification.FREI
            }
            DayType.COMP_TIME -> {
                // COMP_TIME ist ein expliziter Überstundenabbau-Tag
                DayClassification.UEBERSTUNDEN_ABBAU
            }
            DayType.WORK -> {
                when {
                    hasWork -> DayClassification.ARBEITSTAG_MIT_ARBEIT
                    hasTravel -> DayClassification.ARBEITSTAG_NUR_REISE
                    else -> DayClassification.ARBEITSTAG_LEER
                }
            }
        }
    }
}

/**
 * Erweiterungsfunktion für einfachen Zugriff auf die Tagesklassifikation.
 */
fun WorkEntryWithTravelLegs.classifyDay(): DayClassification {
    return ClassifyDay()(this)
}

