package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.repository.WorkEntryRepository
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
    private val workEntryDao: WorkEntryRepository
) {
    
    companion object {
        private const val CONFIRMATION_SOURCE_NOTIFICATION = "NOTIFICATION"
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
        val now = System.currentTimeMillis()
        val dayLocationLabel = ""

        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
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
            result = updatedEntry
            updatedEntry
        }

        return requireNotNull(result) { "readModifyWrite hat kein Ergebnis geliefert" }
    }
}
