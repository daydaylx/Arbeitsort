package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import java.time.LocalDate

class DeleteDayEntry(private val workEntryDao: WorkEntryDao) {
    /**
     * Deletes the work entry for [date] from the database.
     * Returns the deleted entry including its travel legs so the caller can offer a full undo,
     * or null if no entry existed for that date.
     */
    suspend operator fun invoke(date: LocalDate): WorkEntryWithTravelLegs? {
        val entryWithLegs = workEntryDao.getByDateWithTravel(date) ?: return null
        workEntryDao.deleteByDate(date)
        return entryWithLegs
    }
}
