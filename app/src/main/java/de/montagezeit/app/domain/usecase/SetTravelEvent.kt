package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

/**
 * UseCase zum Setzen von Travel-Events (Start/Arrive/Departure)
 * 
 * Ermöglicht das Erfassen von Anreise- und Abreise-Informationen
 */
class SetTravelEvent(
    private val workEntryDao: WorkEntryDao
) {
    
    /**
     * Travel-Typ
     */
    enum class TravelType {
        START,      // Anreise startet
        ARRIVE,     // Ankunft am Ziel
        DEPARTURE   // Abreise vom Ziel
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
        val existingEntry = workEntryDao.getByDate(date)
        val now = System.currentTimeMillis()
        
        val updatedEntry = when (type) {
            TravelType.START -> {
                // Anreise startet
                if (existingEntry != null) {
                    existingEntry.copy(
                        travelStartAt = timestamp,
                        travelLabelStart = label,
                        updatedAt = now
                    )
                } else {
                    WorkEntry(
                        date = date,
                        travelStartAt = timestamp,
                        travelLabelStart = label,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }
            
            TravelType.ARRIVE -> {
                // Ankunft am Ziel
                if (existingEntry != null) {
                    existingEntry.copy(
                        travelArriveAt = timestamp,
                        travelLabelEnd = label,
                        updatedAt = now
                    )
                } else {
                    WorkEntry(
                        date = date,
                        travelArriveAt = timestamp,
                        travelLabelEnd = label,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }
            
            TravelType.DEPARTURE -> {
                // Abreise vom Ziel - kann travelStartAt überschreiben oder neuen Eintrag erstellen
                // Für MVP: Wir überschreiben travelStartAt mit Abreise-Zeit
                if (existingEntry != null) {
                    existingEntry.copy(
                        travelStartAt = timestamp,
                        travelLabelStart = label,
                        updatedAt = now
                    )
                } else {
                    WorkEntry(
                        date = date,
                        travelStartAt = timestamp,
                        travelLabelStart = label,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }
        }
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
    
    /**
     * Löscht alle Travel-Informationen für ein bestimmtes Datum
     * 
     * @param date Datum des WorkEntry
     * @return Aktualisierter WorkEntry
     */
    suspend fun clearTravelEvents(date: LocalDate): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
            ?: throw IllegalArgumentException("Kein WorkEntry für Datum $date gefunden")
        
        val now = System.currentTimeMillis()
        val updatedEntry = existingEntry.copy(
            travelStartAt = null,
            travelArriveAt = null,
            travelLabelStart = null,
            travelLabelEnd = null,
            updatedAt = now
        )
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
