package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

/**
 * UseCase zum Setzen des Tagtyps (WORK / OFF / COMP_TIME)
 *
 * Für COMP_TIME wird [WorkEntry.confirmedWorkDay] sofort auf `true` gesetzt
 * (Option A: auto-confirm). Begründung: COMP_TIME ist eine bewusste Nutzeraktion –
 * ein zweiter manueller Bestätigungsschritt wäre inkonsistent mit ConfirmOffDay,
 * das ebenfalls direkt bestätigt. Es gibt nichts zu "reviewen".
 *
 * @param workEntryDao DAO für WorkEntry
 */
class SetDayType(
    private val workEntryDao: WorkEntryDao
) {

    /**
     * Setzt den Tagtyp für ein bestimmtes Datum.
     *
     * @param date Datum
     * @param dayType Tagtyp (WORK, OFF oder COMP_TIME)
     * @return Aktualisierter WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        dayType: DayType
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        val now = System.currentTimeMillis()

        val updatedEntry = if (existingEntry != null) {
            when (dayType) {
                DayType.COMP_TIME -> existingEntry.copy(
                    dayType = dayType,
                    confirmedWorkDay = true,
                    confirmationAt = now,
                    confirmationSource = "COMP_TIME",
                    updatedAt = now
                )
                else -> existingEntry.copy(
                    dayType = dayType,
                    updatedAt = now
                )
            }
        } else {
            WorkEntry(
                date = date,
                dayType = dayType,
                confirmedWorkDay = dayType == DayType.COMP_TIME,
                confirmationAt = if (dayType == DayType.COMP_TIME) now else null,
                confirmationSource = if (dayType == DayType.COMP_TIME) "COMP_TIME" else null,
                createdAt = now,
                updatedAt = now
            )
        }

        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
