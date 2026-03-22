package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.copyWithLegacyTravel
import de.montagezeit.app.data.local.entity.withLegacyTravelFrom

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
        val hasLegacyTravelWindow =
            (entry.travelStartAt != null && entry.travelArriveAt != null) ||
                (entry.returnStartAt != null && entry.returnArriveAt != null)
        val entryToSave = entry.copy(
            breakMinutes = if (hasWorkBlock) entry.breakMinutes else 0,
            updatedAt = now
        ).withLegacyTravelFrom(entry).copyWithLegacyTravel(
            travelPaidMinutes = if (hasLegacyTravelWindow) null else entry.travelPaidMinutes
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
        // COMP_TIME: keine Validierung von Ort oder Arbeitszeiten erforderlich
        if (entry.dayType == DayType.COMP_TIME) {
            return
        }

        val hasWorkBlock = entry.workStart != null && entry.workEnd != null
        if (entry.dayType == DayType.WORK) {
            if (!hasWorkBlock) {
                validateLegacyTravelWindow(
                    startAt = entry.travelStartAt,
                    arriveAt = entry.travelArriveAt,
                    tooLongMessage = "Reisezeit darf maximal 16 Stunden betragen"
                )
                validateLegacyTravelWindow(
                    startAt = entry.returnStartAt,
                    arriveAt = entry.returnArriveAt,
                    tooLongMessage = "Rückfahrt darf maximal 16 Stunden betragen"
                )
                return
            }
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

        validateLegacyTravelWindow(
            startAt = entry.travelStartAt,
            arriveAt = entry.travelArriveAt,
            tooLongMessage = "Reisezeit darf maximal 16 Stunden betragen"
        )
        validateLegacyTravelWindow(
            startAt = entry.returnStartAt,
            arriveAt = entry.returnArriveAt,
            tooLongMessage = "Rückfahrt darf maximal 16 Stunden betragen"
        )
    }

    private fun validateLegacyTravelWindow(startAt: Long?, arriveAt: Long?, tooLongMessage: String) {
        if (startAt == null || arriveAt == null) return

        var diffMinutes = ((arriveAt - startAt) / 60_000L).toInt()
        if (diffMinutes < 0) diffMinutes += 24 * 60
        if (diffMinutes > 16 * 60) {
            throw IllegalArgumentException(tooLongMessage)
        }
    }
}
