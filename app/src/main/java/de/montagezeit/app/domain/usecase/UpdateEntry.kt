package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry

/**
 * UseCase für manuelle Bearbeitung eines WorkEntry
 * 
 * Ermöglicht dem Benutzer, Einträge manuell zu korrigieren oder zu vervollständigen
 */
class UpdateEntry(
    private val workEntryDao: WorkEntryDao
) {
    
    /**
     * Aktualisiert einen WorkEntry mit den angegebenen Änderungen
     * 
     * @param entry Der zu aktualisierende WorkEntry
     * @return Der aktualisierte WorkEntry
     */
    suspend operator fun invoke(entry: WorkEntry): WorkEntry {
        val now = System.currentTimeMillis()
        
        // updatedAt immer aktualisieren
        val updatedEntry = entry.copy(
            updatedAt = now
        )
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
    
    /**
     * Aktualisiert spezifische Felder eines WorkEntry
     * 
     * @param date Datum des WorkEntry
     * @param updates Map mit zu aktualisierenden Feldern
     * @return Der aktualisierte WorkEntry
     */
    suspend fun updateFields(
        date: java.time.LocalDate,
        updates: Map<String, Any?>
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
            ?: throw IllegalArgumentException("Kein WorkEntry für Datum $date gefunden")
        
        var updatedEntry = existingEntry.copy(
            updatedAt = System.currentTimeMillis()
        )
        
        // Felder aktualisieren basierend auf der Map
        updates.forEach { (key, value) ->
            updatedEntry = when (key) {
                "dayType" -> updatedEntry.copy(
                    dayType = value as? de.montagezeit.app.data.local.entity.DayType 
                        ?: updatedEntry.dayType
                )
                "workStart" -> updatedEntry.copy(
                    workStart = value as? java.time.LocalTime 
                        ?: updatedEntry.workStart
                )
                "workEnd" -> updatedEntry.copy(
                    workEnd = value as? java.time.LocalTime 
                        ?: updatedEntry.workEnd
                )
                "breakMinutes" -> updatedEntry.copy(
                    breakMinutes = value as? Int 
                        ?: updatedEntry.breakMinutes
                )
                "needsReview" -> updatedEntry.copy(
                    needsReview = value as? Boolean 
                        ?: updatedEntry.needsReview
                )
                "note" -> updatedEntry.copy(
                    note = value as? String
                )
                // Morning Felder
                "morningLocationLabel" -> updatedEntry.copy(
                    morningLocationLabel = value as? String
                )
                "outsideLeipzigMorning" -> updatedEntry.copy(
                    outsideLeipzigMorning = value as? Boolean
                )
                // Evening Felder
                "eveningLocationLabel" -> updatedEntry.copy(
                    eveningLocationLabel = value as? String
                )
                "outsideLeipzigEvening" -> updatedEntry.copy(
                    outsideLeipzigEvening = value as? Boolean
                )
                // Travel Felder
                "travelStartAt" -> updatedEntry.copy(
                    travelStartAt = value as? Long
                )
                "travelArriveAt" -> updatedEntry.copy(
                    travelArriveAt = value as? Long
                )
                "travelLabelStart" -> updatedEntry.copy(
                    travelLabelStart = value as? String
                )
                "travelLabelEnd" -> updatedEntry.copy(
                    travelLabelEnd = value as? String
                )
                "travelFromLabel" -> updatedEntry.copy(
                    travelFromLabel = value as? String
                )
                "travelToLabel" -> updatedEntry.copy(
                    travelToLabel = value as? String
                )
                "travelDistanceKm" -> updatedEntry.copy(
                    travelDistanceKm = value as? Double
                )
                "travelPaidMinutes" -> updatedEntry.copy(
                    travelPaidMinutes = value as? Int
                )
                "travelSource" -> updatedEntry.copy(
                    travelSource = value as? de.montagezeit.app.data.local.entity.TravelSource
                )
                "travelUpdatedAt" -> updatedEntry.copy(
                    travelUpdatedAt = value as? Long
                )
                else -> updatedEntry
            }
        }
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
