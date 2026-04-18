package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry

/**
 * UseCase für manuelle Bearbeitung eines WorkEntry
 *
 * Ermöglicht dem Benutzer, Einträge manuell zu korrigieren oder zu vervollständigen
 */
class UpdateEntry(
    private val workEntryDao: WorkEntryRepository
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

        val normalizedLocation = entry.dayLocationLabel.trim()

        val now = System.currentTimeMillis()
        val shouldConfirmWorkDay = EntryStatusResolver.shouldAutoConfirmWorkDay(
            entry = entry.copy(dayLocationLabel = normalizedLocation)
        )
        val entryToSave = entry.copy(
            dayLocationLabel = normalizedLocation,
            breakMinutes = if (entry.workStart != null && entry.workEnd != null) entry.breakMinutes else 0,
            confirmedWorkDay = if (entry.dayType == DayType.WORK) shouldConfirmWorkDay else entry.confirmedWorkDay,
            confirmationAt = when {
                entry.dayType != DayType.WORK -> entry.confirmationAt
                shouldConfirmWorkDay -> entry.confirmationAt ?: now
                else -> null
            },
            confirmationSource = when {
                entry.dayType != DayType.WORK -> entry.confirmationSource
                shouldConfirmWorkDay -> entry.confirmationSource ?: "WORK_BLOCK"
                else -> null
            },
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

        if (entry.dayType == DayType.WORK && entry.dayLocationLabel.isBlank()) {
            throw IllegalArgumentException("dayLocationLabel darf bei WORK nicht leer sein")
        }

        val hasWorkStart = entry.workStart != null
        val hasWorkEnd = entry.workEnd != null
        if (hasWorkStart.xor(hasWorkEnd)) {
            throw IllegalArgumentException("workStart und workEnd muessen zusammen gesetzt werden")
        }

        val hasWorkBlock = hasWorkStart && hasWorkEnd
        if (entry.dayType == DayType.WORK && hasWorkBlock) {
            val workStart = requireNotNull(entry.workStart)
            val workEnd = requireNotNull(entry.workEnd)
            if (workEnd == workStart) {
                throw IllegalArgumentException("workEnd ($workEnd) muss sich von workStart ($workStart) unterscheiden")
            }
            if (entry.breakMinutes < 0) {
                throw IllegalArgumentException("breakMinutes (${entry.breakMinutes}) darf nicht negativ sein")
            }
            // Nachtschicht-Unterstützung: wenn Ende < Start, über Mitternacht gerechnet
            val startMinutes = workStart.hour * 60 + workStart.minute
            val endMinutes = workEnd.hour * 60 + workEnd.minute
            val workDurationMinutes = if (endMinutes < startMinutes) {
                (24 * 60 - startMinutes) + endMinutes
            } else {
                endMinutes - startMinutes
            }
            if (workDurationMinutes > 18 * 60) {
                throw IllegalArgumentException("Arbeitszeit ($workDurationMinutes min) darf nicht mehr als 18 Stunden betragen")
            }
            if (entry.breakMinutes >= workDurationMinutes) {
                throw IllegalArgumentException("breakMinutes (${entry.breakMinutes}) muss kleiner als Arbeitszeit ($workDurationMinutes min) sein")
            }
        }
    }
}
