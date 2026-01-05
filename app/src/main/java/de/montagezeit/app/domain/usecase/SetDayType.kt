package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

/**
 * UseCase zum Setzen des Tagtyps (WORK/OFF)
 * 
 * @param workEntryDao DAO für WorkEntry
 */
class SetDayType(
    private val workEntryDao: WorkEntryDao
) {
    
    /**
     * Setzt den Tagtyp für ein bestimmtes Datum
     * 
     * @param date Datum
     * @param dayType Tagtyp (WORK oder OFF)
     * @return Aktualisierter WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        dayType: DayType
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        val now = System.currentTimeMillis()
        
        val updatedEntry = if (existingEntry != null) {
            existingEntry.copy(
                dayType = dayType,
                updatedAt = now
            )
        } else {
            WorkEntry(
                date = date,
                dayType = dayType,
                createdAt = now,
                updatedAt = now
            )
        }
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
