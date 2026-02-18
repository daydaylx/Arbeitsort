package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.createConfirmedOffDayEntry
import de.montagezeit.app.data.local.entity.withConfirmedOffDay
import java.time.LocalDate

/**
 * UseCase für die Daily Confirmation (NEIN - heute frei)
 * 
 * Logik:
 * 1. Tag als OFF markieren
 * 2. confirmedWorkDay=true, confirmationAt=now setzen
 */
class ConfirmOffDay(
    private val workEntryDao: WorkEntryDao
) {
    
    companion object {
        private const val CONFIRMATION_SOURCE_NOTIFICATION = "NOTIFICATION"
        private const val CONFIRMATION_SOURCE_UI = "UI"
    }
    
    /**
     * Bestätigt, dass heute ein freier Tag ist
     * 
     * @param date Datum des Tages
     * @param source "NOTIFICATION" oder "UI"
     * @return Aktualisierter WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        source: String = CONFIRMATION_SOURCE_NOTIFICATION
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        val now = System.currentTimeMillis()
        val dayLocationLabel = DEFAULT_DAY_LOCATION_LABEL
        
        val updatedEntry = existingEntry?.withConfirmedOffDay(
            source = source,
            now = now,
            fallbackDayLocationLabel = dayLocationLabel
        ) ?: createConfirmedOffDayEntry(
            date = date,
            source = source,
            now = now,
            fallbackDayLocationLabel = dayLocationLabel
        )
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
