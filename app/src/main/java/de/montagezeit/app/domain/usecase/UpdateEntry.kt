package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry

/**
 * UseCase für manuelle Bearbeitung eines WorkEntry
 *
 * Ermöglicht dem Benutzer, Einträge manuell zu korrigieren oder zu vervollständigen
 */
class UpdateEntry(
    private val workEntryDao: WorkEntryDao
) {

    /**
     * Aktualisiert einen WorkEntry mit den angegebenen Änderungen
     *
     * @param entry Der zu aktualisierende WorkEntry
     * @return Der aktualisierte WorkEntry
     * @throws IllegalArgumentException wenn die Validierung fehlschlägt
     */
    suspend operator fun invoke(entry: WorkEntry): WorkEntry {
        validateEntry(entry)

        val now = System.currentTimeMillis()
        val hasWorkBlock = entry.workStart != null && entry.workEnd != null
        val entryToSave = entry.copy(
            breakMinutes = if (hasWorkBlock) entry.breakMinutes else 0,
            updatedAt = now
        )

        workEntryDao.upsert(entryToSave)
        return entryToSave
    }

    /**
     * Validiert einen WorkEntry und wirft eine Exception bei Fehlern.
     *
     * Fachliche Regeln je DayType:
     * - WORK: dayLocationLabel Pflicht, Arbeitszeit valide
     * - OFF:  dayLocationLabel optional, Arbeitszeit nicht relevant (TimeCalculator gibt 0)
     * - COMP_TIME: keine Pflichtfelder für Ort oder Zeiten
     */
    private fun validateEntry(entry: WorkEntry) {
        if (entry.dayType == DayType.COMP_TIME) return

        val hasWorkBlock = entry.workStart != null && entry.workEnd != null
        if (entry.dayType == DayType.WORK && hasWorkBlock) {
            val workStart = requireNotNull(entry.workStart)
            val workEnd = requireNotNull(entry.workEnd)
            if (workEnd <= workStart) {
                throw IllegalArgumentException("workEnd ($workEnd) muss nach workStart ($workStart) liegen")
            }
            if (entry.breakMinutes < 0) {
                throw IllegalArgumentException("breakMinutes (${entry.breakMinutes}) darf nicht negativ sein")
            }
            val workDurationMinutes = (workEnd.hour * 60 + workEnd.minute) -
                (workStart.hour * 60 + workStart.minute)
            if (entry.breakMinutes > workDurationMinutes) {
                throw IllegalArgumentException("breakMinutes (${entry.breakMinutes}) darf nicht länger als Arbeitszeit ($workDurationMinutes min) sein")
            }
        }
    }
}
