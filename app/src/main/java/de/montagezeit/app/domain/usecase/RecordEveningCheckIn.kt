package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

/**
 * UseCase für den Abend-Check-in (ohne GPS-Erfassung)
 */
class RecordEveningCheckIn(
    private val workEntryDao: WorkEntryRepository
) {

    suspend operator fun invoke(date: LocalDate): WorkEntry {
        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val updatedEntry = CheckInEntryBuilder.build(
                date = date,
                existingEntry = existingEntry,
                snapshot = CheckInEntryBuilder.Snapshot.EVENING
            )
            result = updatedEntry
            updatedEntry
        }
        return requireNotNull(result) { "readModifyWrite must return an entry for date $date" }
    }
}
