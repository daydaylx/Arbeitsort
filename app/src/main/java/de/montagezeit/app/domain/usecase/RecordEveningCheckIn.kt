package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.location.LocationProvider
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.location.LocationCalculator
import de.montagezeit.app.domain.model.LocationResult
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * UseCase für den Abend-Check-in
 *
 * Logik analog zu RecordMorningCheckIn, aber für Abend-Snapshot.
 */
class RecordEveningCheckIn(
    private val workEntryDao: WorkEntryDao,
    private val locationProvider: LocationProvider,
    private val locationCalculator: LocationCalculator,
    private val reminderSettingsManager: ReminderSettingsManager
) {

    companion object {
        private const val LOCATION_TIMEOUT_MS = 15000L
    }

    /**
     * Führt den Abend-Check-in durch
     *
     * @param date Datum des Check-ins
     * @param forceWithoutLocation true wenn ohne Standort gespeichert werden soll
     * @return Aktualisierter WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        forceWithoutLocation: Boolean = false
    ): WorkEntry {
        val existingEntry = workEntryDao.getByDate(date)
        val settings = reminderSettingsManager.settings.first()
        val locationRadiusKm = settings.locationRadiusKm.toDouble()

        val locationResult = if (forceWithoutLocation || !settings.preferGpsLocation) {
            LocationResult.SkippedByUser
        } else {
            locationProvider.getCurrentLocation(LOCATION_TIMEOUT_MS)
        }

        val updatedEntry = CheckInEntryBuilder.build(
            date = date,
            existingEntry = existingEntry,
            locationResult = locationResult,
            snapshot = CheckInEntryBuilder.Snapshot.EVENING,
            radiusKm = locationRadiusKm,
            settings = settings,
            locationCalculator = locationCalculator
        )

        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
