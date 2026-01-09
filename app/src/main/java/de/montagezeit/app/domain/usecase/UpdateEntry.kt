package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate
import java.time.LocalTime

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
     * @param updates Liste mit zu aktualisierenden Feldern
     * @return Der aktualisierte WorkEntry
     */
    suspend fun updateFields(
        date: LocalDate,
        updates: List<UpdateField>
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
            ?: throw IllegalArgumentException("Kein WorkEntry für Datum $date gefunden")

        val updatedEntry = updates.fold(existingEntry) { entry, update ->
            when (update) {
                is UpdateField.DayType -> entry.copy(dayType = update.value)
                is UpdateField.WorkStart -> entry.copy(workStart = update.value)
                is UpdateField.WorkEnd -> entry.copy(workEnd = update.value)
                is UpdateField.BreakMinutes -> entry.copy(breakMinutes = update.value)
                is UpdateField.NeedsReview -> entry.copy(needsReview = update.value)
                is UpdateField.Note -> entry.copy(note = update.value)
                is UpdateField.MorningLocationLabel -> entry.copy(morningLocationLabel = update.value)
                is UpdateField.OutsideLeipzigMorning -> entry.copy(outsideLeipzigMorning = update.value)
                is UpdateField.EveningLocationLabel -> entry.copy(eveningLocationLabel = update.value)
                is UpdateField.OutsideLeipzigEvening -> entry.copy(outsideLeipzigEvening = update.value)
                is UpdateField.TravelStartAt -> entry.copy(travelStartAt = update.value)
                is UpdateField.TravelArriveAt -> entry.copy(travelArriveAt = update.value)
                is UpdateField.TravelLabelStart -> entry.copy(travelLabelStart = update.value)
                is UpdateField.TravelLabelEnd -> entry.copy(travelLabelEnd = update.value)
                is UpdateField.TravelFromLabel -> entry.copy(travelFromLabel = update.value)
                is UpdateField.TravelToLabel -> entry.copy(travelToLabel = update.value)
                is UpdateField.TravelDistanceKm -> entry.copy(travelDistanceKm = update.value)
                is UpdateField.TravelPaidMinutes -> entry.copy(travelPaidMinutes = update.value)
                is UpdateField.TravelSource -> entry.copy(travelSource = update.value)
                is UpdateField.TravelUpdatedAt -> entry.copy(travelUpdatedAt = update.value)
            }
        }

        val finalEntry = updatedEntry.copy(updatedAt = System.currentTimeMillis())
        workEntryDao.upsert(finalEntry)
        return finalEntry
    }
}

sealed class UpdateField {
    data class DayType(val value: DayType) : UpdateField()
    data class WorkStart(val value: LocalTime?) : UpdateField()
    data class WorkEnd(val value: LocalTime?) : UpdateField()
    data class BreakMinutes(val value: Int) : UpdateField()
    data class NeedsReview(val value: Boolean) : UpdateField()
    data class Note(val value: String?) : UpdateField()
    data class MorningLocationLabel(val value: String?) : UpdateField()
    data class OutsideLeipzigMorning(val value: Boolean?) : UpdateField()
    data class EveningLocationLabel(val value: String?) : UpdateField()
    data class OutsideLeipzigEvening(val value: Boolean?) : UpdateField()
    data class TravelStartAt(val value: Long?) : UpdateField()
    data class TravelArriveAt(val value: Long?) : UpdateField()
    data class TravelLabelStart(val value: String?) : UpdateField()
    data class TravelLabelEnd(val value: String?) : UpdateField()
    data class TravelFromLabel(val value: String?) : UpdateField()
    data class TravelToLabel(val value: String?) : UpdateField()
    data class TravelDistanceKm(val value: Double?) : UpdateField()
    data class TravelPaidMinutes(val value: Int?) : UpdateField()
    data class TravelSource(val value: TravelSource?) : UpdateField()
    data class TravelUpdatedAt(val value: Long?) : UpdateField()
}
