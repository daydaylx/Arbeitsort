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
        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val updatedEntry = CheckInEntryBuilder.build(
                date = date,
                existingEntry = existingEntry,
                snapshot = CheckInEntryBuilder.Snapshot.MORNING
            )
            result = updatedEntry
            updatedEntry
        }
        return requireNotNull(result) { "readModifyWrite must return an entry for date $date" }
    }
}
