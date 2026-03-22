package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.copyWithLegacyTravel
import de.montagezeit.app.data.local.entity.withLegacyTravelFrom
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
        var entry: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val baseEntry = existingEntry?.copy(updatedAt = now)?.withLegacyTravelFrom(existingEntry)
                ?: WorkEntry(
                    date = date,
                    createdAt = now,
                    updatedAt = now
                )
            val updatedEntry = when (type) {
                TravelType.START -> baseEntry.copyWithLegacyTravel(
                    travelStartAt = timestamp,
                    travelLabelStart = label,
                    travelPaidMinutes = null,
                    travelSource = TravelSource.MANUAL,
                    travelUpdatedAt = now
                )
                TravelType.ARRIVE -> baseEntry.copyWithLegacyTravel(
                    travelArriveAt = timestamp,
                    travelLabelEnd = label,
                    travelPaidMinutes = null,
                    travelSource = TravelSource.MANUAL,
                    travelUpdatedAt = now
                )
            }
            entry = updatedEntry
            updatedEntry
        }
        val updatedEntry = requireNotNull(entry)
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
        workEntryDao.replaceEntryWithTravelLegs(updatedEntry, listOf(updatedLeg) + remainingLegs)
        return updatedEntry
    }
    
    /**
     * Löscht alle Travel-Informationen für ein bestimmtes Datum
     * 
     * @param date Datum des WorkEntry
     * @return Aktualisierter WorkEntry
     */
    suspend fun clearTravelEvents(date: LocalDate): WorkEntry {
        val now = System.currentTimeMillis()
        var updated: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { current ->
            val existing = current ?: throw IllegalArgumentException("Kein WorkEntry für Datum $date gefunden")
            existing.withTravelCleared()
                .copy(updatedAt = now)
                .copyWithLegacyTravel(travelUpdatedAt = now)
                .also { updated = it }
        }
        val clearedEntry = requireNotNull(updated)
        workEntryDao.replaceEntryWithTravelLegs(clearedEntry, emptyList())
        return clearedEntry
    }
}
