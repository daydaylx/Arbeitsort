package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

data class DeletedDaySnapshot(
    val entry: WorkEntry,
    val travelLegs: List<TravelLeg>
)

class DeleteDayEntry(private val workEntryDao: WorkEntryDao) {
    /**
     * Deletes the work entry for [date] from the database.
     * Returns the deleted snapshot so the caller can offer an undo action,
     * or null if no entry existed for that date.
     */
    suspend operator fun invoke(date: LocalDate): DeletedDaySnapshot? {
        val entryWithTravel = workEntryDao.getByDateWithTravel(date) ?: return null
        workEntryDao.deleteByDate(date)
        return DeletedDaySnapshot(
            entry = entryWithTravel.workEntry,
            travelLegs = entryWithTravel.orderedTravelLegs
        )
    }
}
