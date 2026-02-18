package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

/**
 * UseCase für den Morgen-Check-in (ohne GPS-Erfassung)
 */
class RecordMorningCheckIn(
    private val workEntryDao: WorkEntryDao
) {

    suspend operator fun invoke(date: LocalDate): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        val updatedEntry = CheckInEntryBuilder.build(
            date = date,
            existingEntry = existingEntry,
            snapshot = CheckInEntryBuilder.Snapshot.MORNING
        )
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
