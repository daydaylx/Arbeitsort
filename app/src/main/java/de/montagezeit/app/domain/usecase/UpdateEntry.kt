package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
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
        // Validate entry before saving
        validateEntry(entry)

        val now = System.currentTimeMillis()

        // updatedAt immer aktualisieren
        val updatedEntry = entry.copy(
            updatedAt = now
        )

        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }

    /**
     * Validiert einen WorkEntry und wirft eine Exception bei Fehlern
     */
    private fun validateEntry(entry: WorkEntry) {
        // 1. dayLocationLabel darf nicht leer sein
        if (entry.dayLocationLabel.isBlank()) {
            throw IllegalArgumentException("dayLocationLabel darf nicht leer sein")
        }

        // 2. workEnd muss nach workStart liegen
        if (entry.workEnd <= entry.workStart) {
            throw IllegalArgumentException("workEnd (${entry.workEnd}) muss nach workStart (${entry.workStart}) liegen")
        }

        // 3. breakMinutes darf nicht negativ sein
        if (entry.breakMinutes < 0) {
            throw IllegalArgumentException("breakMinutes (${entry.breakMinutes}) darf nicht negativ sein")
        }

        // 4. breakMinutes darf nicht länger als Arbeitszeit sein
        val workDurationMinutes = (entry.workEnd.hour * 60 + entry.workEnd.minute) -
                                  (entry.workStart.hour * 60 + entry.workStart.minute)
        if (entry.breakMinutes > workDurationMinutes) {
            throw IllegalArgumentException("breakMinutes (${entry.breakMinutes}) darf nicht länger als Arbeitszeit ($workDurationMinutes min) sein")
        }

        // 5. Wenn travelStartAt und travelArriveAt beide gesetzt sind, muss arrive nach start liegen
        val travelStart = entry.travelStartAt
        val travelArrive = entry.travelArriveAt
        if (travelStart != null && travelArrive != null) {
            // Bei gleichen Datum-Timestamps: arrive muss >= start sein (oder Mitternachtsüberschreitung)
            val diff = travelArrive - travelStart
            // Nur validieren wenn keine Mitternachtsüberschreitung (diff >= 0 oder < -20h)
            if (diff < 0 && diff > -20 * 60 * 60 * 1000L) {
                throw IllegalArgumentException("travelArriveAt muss nach travelStartAt liegen")
            }
        }
    }
}
