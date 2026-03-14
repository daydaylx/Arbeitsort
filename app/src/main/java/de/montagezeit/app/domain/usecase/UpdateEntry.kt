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

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val MAX_TRAVEL_DURATION_MILLIS = 16 * 60 * 60 * 1000L
    }

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

        // Wenn Timestamps gesetzt sind, travelPaidMinutes nullen damit Timestamps die Quelle sind.
        // travelPaidMinutes != null würde als Override wirken und die Timestamps ignorieren.
        val entryToSave = if (entry.travelStartAt != null || entry.travelArriveAt != null) {
            entry.copy(travelPaidMinutes = null, updatedAt = now)
        } else {
            entry.copy(updatedAt = now)
        }

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
        // COMP_TIME: keine Validierung von Ort oder Arbeitszeiten erforderlich
        if (entry.dayType == DayType.COMP_TIME) {
            validateTravel(entry)
            return
        }

        // 1. dayLocationLabel nur für WORK-Tage Pflicht
        if (entry.dayType == DayType.WORK && entry.dayLocationLabel.isBlank()) {
            throw IllegalArgumentException("dayLocationLabel darf nicht leer sein")
        }

        // 2–4. Arbeitszeitvalidierung nur für WORK-Tage (OFF hat fachlich workMinutes = 0)
        if (entry.dayType == DayType.WORK) {
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
        }

        validateTravel(entry)
    }

    private fun validateTravel(entry: WorkEntry) {
        // 5. Wenn travelStartAt und travelArriveAt beide gesetzt sind, muss arrive nach start liegen
        val travelStart = entry.travelStartAt
        val travelArrive = entry.travelArriveAt
        if (travelStart != null && travelArrive != null) {
            var travelDuration = travelArrive - travelStart
            if (travelDuration < 0) {
                // Übernachtreise erlaubt: beide Timestamps für dasselbe Datum erzeugt
                travelDuration += DAY_MILLIS
            }

            if (travelDuration <= 0) {
                throw IllegalArgumentException("travelArriveAt muss nach travelStartAt liegen")
            }

            if (travelDuration > MAX_TRAVEL_DURATION_MILLIS) {
                throw IllegalArgumentException("Reisezeit darf maximal 16 Stunden betragen")
            }
        }
    }
}
