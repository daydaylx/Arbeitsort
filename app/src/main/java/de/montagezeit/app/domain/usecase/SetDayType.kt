package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.withMealAllowanceCleared
import de.montagezeit.app.data.local.entity.withTravelCleared
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
        var result: WorkEntry? = null
        workEntryDao.readModifyWrite(date) { existingEntry ->
            val now = System.currentTimeMillis()

            val updatedEntry = if (existingEntry != null) {
                // Meal allowance only stays when both old and new type are WORK.
                // OFF→WORK: meal stays 0 (correct – meal is set at check-in, not here).
                val isWorkToWork = dayType == DayType.WORK && existingEntry.dayType == DayType.WORK
                val shouldClearMealAllowance = !isWorkToWork
                when (dayType) {
                    // COMP_TIME→WORK: travel is not restored because COMP_TIME deliberately
                    // clears travel data (it represents a full day off the overtime account).
                    DayType.COMP_TIME -> existingEntry
                        .withTravelCleared(now)
                        .copy(
                            dayType = dayType,
                            confirmedWorkDay = true,
                            confirmationAt = now,
                            confirmationSource = DayType.COMP_TIME.name,
                            updatedAt = now
                        )
                        .withMealAllowanceCleared()
                    else -> {
                        // When changing away from COMP_TIME, clear the auto-confirmation so the
                        // new day type is not counted as confirmed in overtime calculations.
                        val wasCompTime = existingEntry.dayType == DayType.COMP_TIME
                        val baseEntry = existingEntry.copy(
                            dayType = dayType,
                            confirmedWorkDay = if (wasCompTime) false else existingEntry.confirmedWorkDay,
                            confirmationAt = if (wasCompTime) null else existingEntry.confirmationAt,
                            confirmationSource = if (wasCompTime) null else existingEntry.confirmationSource,
                            updatedAt = now
                        )
                        if (shouldClearMealAllowance) baseEntry.withMealAllowanceCleared() else baseEntry
                    }
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

            result = updatedEntry
            updatedEntry
        }
        return result!!
    }
}
