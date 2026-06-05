package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.transitionToDayType
import java.time.LocalDate

/**
 * UseCase zum Setzen des Tagtyps (WORK / OFF / VACATION / COMP_TIME)
 *
 * Für OFF, VACATION und COMP_TIME wird [WorkEntry.confirmedWorkDay] sofort auf `true`
 * gesetzt. Diese Typen sind bewusste Nutzeraktionen und benötigen keine Arbeitszeitdaten.
 *
 * @param workEntryDao DAO für WorkEntry
 */
class SetDayType(
    private val workEntryDao: WorkEntryRepository
) {

    /**
     * Setzt den Tagtyp für ein bestimmtes Datum.
     *
     * @param date Datum
     * @param dayType Tagtyp (WORK, OFF, VACATION oder COMP_TIME)
     * @return Aktualisierter WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        dayType: DayType
    ): WorkEntry {
        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val now = System.currentTimeMillis()

            val updatedEntry = if (existingEntry != null) {
                existingEntry.transitionToDayType(dayType = dayType, now = now)
            } else {
                WorkEntry(
                    date = date,
                    createdAt = now,
                    updatedAt = now
                ).transitionToDayType(dayType = dayType, now = now)
            }

            result = updatedEntry
            updatedEntry
        }
        return workEntryDao.normalizeForPersistence(
            requireNotNull(result) { "readModifyWrite hat kein Ergebnis geliefert" }
        )
    }
}
