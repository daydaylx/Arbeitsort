package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

class DeleteDayEntry(private val workEntryDao: WorkEntryDao) {
    /**
     * Deletes the work entry for [date] from the database.
     * Returns the deleted entry so the caller can offer an undo action,
     * or null if no entry existed for that date.
     */
    suspend operator fun invoke(date: LocalDate): WorkEntry? {
        val entry = workEntryDao.getByDate(date) ?: return null
        workEntryDao.deleteByDate(date)
        return entry
    }
}
