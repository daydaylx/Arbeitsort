package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
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
        val baseEntry = workEntryDao.getByDate(date)?.copy(updatedAt = now)
            ?: WorkEntry(date = date, createdAt = now, updatedAt = now)
        val existingLegs = workEntryDao.getTravelLegsByDate(date)
        val baseLeg = existingLegs.firstOrNull() ?: TravelLeg(
            workEntryDate = date,
            sortOrder = 0,
            category = TravelLegCategory.OUTBOUND,
            createdAt = now,
            updatedAt = now
        )
        val updatedLeg = when (type) {
            TravelType.START -> baseLeg.copy(
                startAt = timestamp,
                startLabel = label,
                source = TravelSource.MANUAL,
                updatedAt = now
            )
            TravelType.ARRIVE -> baseLeg.copy(
                arriveAt = timestamp,
                endLabel = label,
                source = TravelSource.MANUAL,
                updatedAt = now
            )
        }
        val remainingLegs = existingLegs.drop(1)
        workEntryDao.replaceEntryWithTravelLegs(baseEntry, listOf(updatedLeg) + remainingLegs)
        return baseEntry
    }
    
    /**
     * Löscht alle Travel-Informationen für ein bestimmtes Datum
     * 
     * @param date Datum des WorkEntry
     * @return Aktualisierter WorkEntry
     */
    suspend fun clearTravelEvents(date: LocalDate): WorkEntry {
        val now = System.currentTimeMillis()
        val existing = workEntryDao.getByDate(date)
            ?: throw IllegalArgumentException("Kein WorkEntry für Datum $date gefunden")
        val cleared = existing.copy(updatedAt = now)
        workEntryDao.replaceEntryWithTravelLegs(cleared, emptyList())
        return cleared
    }
}
