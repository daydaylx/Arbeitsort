package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

class RecordCheckIn(
    private val workEntryDao: WorkEntryRepository
) {
    suspend operator fun invoke(date: LocalDate, snapshot: CheckInEntryBuilder.Snapshot): WorkEntry {
        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val updatedEntry = CheckInEntryBuilder.build(
                date = date,
                existingEntry = existingEntry,
                snapshot = snapshot
            )
            result = updatedEntry
            updatedEntry
        }
        return workEntryDao.normalizeForPersistence(
            requireNotNull(result) { "readModifyWrite must return an entry for date $date" }
        )
    }
}
