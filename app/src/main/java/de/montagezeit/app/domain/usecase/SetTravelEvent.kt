package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.withTravelCleared
import java.time.LocalDate

/**
 * UseCase zum Setzen eines einzelnen manuellen Travel-Fensters (Start/Arrive)
 * 
 * Ermöglicht das Erfassen von Anreise- und Abreise-Informationen
 */
class SetTravelEvent(
    private val workEntryDao: WorkEntryDao
) {
    
    /**
     * Travel-Typ für das einzelne Fahrtfenster des Tages.
     */
    enum class TravelType {
        START,      // Anreise startet
        ARRIVE      // Ankunft am Ziel
    }
    
    /**
     * Setzt ein Travel-Event für ein bestimmtes Datum
     * 
     * @param date Datum des WorkEntry
     * @param type Typ des Travel-Events
     * @param timestamp Zeitstempel des Events
     * @param label Optionaler Ortslabel
     * @return Aktualisierter WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        type: TravelType,
        timestamp: Long,
        label: String? = null
    ): WorkEntry {
        val now = System.currentTimeMillis()

        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val updatedEntry = when (type) {
                TravelType.START -> {
                    if (existingEntry != null) {
                        existingEntry.copy(
                            travelStartAt = timestamp,
                            travelLabelStart = label,
                            travelPaidMinutes = null,
                            travelSource = TravelSource.MANUAL,
                            travelUpdatedAt = now,
                            updatedAt = now
                        )
                    } else {
                        WorkEntry(
                            date = date,
                            travelStartAt = timestamp,
                            travelLabelStart = label,
                            travelSource = TravelSource.MANUAL,
                            travelUpdatedAt = now,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                }

                TravelType.ARRIVE -> {
                    if (existingEntry != null) {
                        existingEntry.copy(
                            travelArriveAt = timestamp,
                            travelLabelEnd = label,
                            travelPaidMinutes = null,
                            travelSource = TravelSource.MANUAL,
                            travelUpdatedAt = now,
                            updatedAt = now
                        )
                    } else {
                        WorkEntry(
                            date = date,
                            travelArriveAt = timestamp,
                            travelLabelEnd = label,
                            travelSource = TravelSource.MANUAL,
                            travelUpdatedAt = now,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                }
            }
            result = updatedEntry
            updatedEntry
        }

        return result!!
    }
    
    /**
     * Löscht alle Travel-Informationen für ein bestimmtes Datum
     * 
     * @param date Datum des WorkEntry
     * @return Aktualisierter WorkEntry
     */
    suspend fun clearTravelEvents(date: LocalDate): WorkEntry {
        val now = System.currentTimeMillis()
        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val current = existingEntry
                ?: throw IllegalArgumentException("Kein WorkEntry für Datum $date gefunden")
            val updatedEntry = current.withTravelCleared(now)
            result = updatedEntry
            updatedEntry
        }
        return result!!
    }
}
