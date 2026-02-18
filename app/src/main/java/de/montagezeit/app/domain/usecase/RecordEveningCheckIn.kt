package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

/**
 * UseCase für den Abend-Check-in (ohne GPS-Erfassung)
 */
class RecordEveningCheckIn(
    private val workEntryDao: WorkEntryDao
) {

    suspend operator fun invoke(date: LocalDate): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        val updatedEntry = CheckInEntryBuilder.build(
            date = date,
            existingEntry = existingEntry,
            snapshot = CheckInEntryBuilder.Snapshot.EVENING
        )
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
