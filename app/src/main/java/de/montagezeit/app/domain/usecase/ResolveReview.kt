package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

/**
 * Scope für die Prüfung - welche Zeitbereiche müssen überprüft werden?
 */
enum class ReviewScope {
    MORNING,
    EVENING,
    BOTH
}

/**
 * UseCase zum Auflösen einer Prüfung (Review) eines WorkEntry
 * 
 * Setzt needsReview=false und speichert den bestätigten Ort.
 * 
 * @param workEntryDao DAO für Datenbankoperationen
 */
class ResolveReview(
    private val workEntryDao: WorkEntryDao
) {
    
    /**
     * Löst eine Prüfung für den angegebenen Tag auf
     * 
     * @param date Das Datum des WorkEntry
     * @param scope Welcher Bereich soll überprüft werden (MORNING, EVENING, oder BOTH)
     * @param resolvedLabel Der vom Nutzer bestätigte Ort (z.B. "Leipzig" oder "Berlin")
     * @param isLeipzig Ob der Ort in Leipzig liegt
     * @return Der aktualisierte WorkEntry
     */
    suspend operator fun invoke(
        date: LocalDate,
        scope: ReviewScope,
        resolvedLabel: String,
        isLeipzig: Boolean
    ): WorkEntry {
        val now = System.currentTimeMillis()
        val existing = workEntryDao.getByDate(date)
        
        val updatedEntry = if (existing != null) {
            // Bestehenden Entry aktualisieren
            val updated = when (scope) {
                ReviewScope.MORNING -> {
                    existing.copy(
                        morningLocationLabel = resolvedLabel,
                        outsideLeipzigMorning = if (isLeipzig) false else true,
                        dayLocationLabel = resolvedLabel,
                        dayLocationSource = DayLocationSource.MANUAL,
                        dayLocationLat = null,
                        dayLocationLon = null,
                        dayLocationAccuracyMeters = null,
                        needsReview = false,
                        updatedAt = now
                    )
                }
                ReviewScope.EVENING -> {
                    existing.copy(
                        eveningLocationLabel = resolvedLabel,
                        outsideLeipzigEvening = if (isLeipzig) false else true,
                        dayLocationLabel = resolvedLabel,
                        dayLocationSource = DayLocationSource.MANUAL,
                        dayLocationLat = null,
                        dayLocationLon = null,
                        dayLocationAccuracyMeters = null,
                        needsReview = false,
                        updatedAt = now
                    )
                }
                ReviewScope.BOTH -> {
                    existing.copy(
                        morningLocationLabel = resolvedLabel,
                        eveningLocationLabel = resolvedLabel,
                        outsideLeipzigMorning = if (isLeipzig) false else true,
                        outsideLeipzigEvening = if (isLeipzig) false else true,
                        dayLocationLabel = resolvedLabel,
                        dayLocationSource = DayLocationSource.MANUAL,
                        dayLocationLat = null,
                        dayLocationLon = null,
                        dayLocationAccuracyMeters = null,
                        needsReview = false,
                        updatedAt = now
                    )
                }
            }
            updated
        } else {
            // Neuen Entry erstellen (falls nicht vorhanden)
            when (scope) {
                ReviewScope.MORNING -> {
                    WorkEntry(
                        date = date,
                        morningLocationLabel = resolvedLabel,
                        outsideLeipzigMorning = if (isLeipzig) false else true,
                        dayLocationLabel = resolvedLabel,
                        dayLocationSource = DayLocationSource.MANUAL,
                        needsReview = false,
                        createdAt = now,
                        updatedAt = now
                    )
                }
                ReviewScope.EVENING -> {
                    WorkEntry(
                        date = date,
                        eveningLocationLabel = resolvedLabel,
                        outsideLeipzigEvening = if (isLeipzig) false else true,
                        dayLocationLabel = resolvedLabel,
                        dayLocationSource = DayLocationSource.MANUAL,
                        needsReview = false,
                        createdAt = now,
                        updatedAt = now
                    )
                }
                ReviewScope.BOTH -> {
                    WorkEntry(
                        date = date,
                        morningLocationLabel = resolvedLabel,
                        eveningLocationLabel = resolvedLabel,
                        outsideLeipzigMorning = if (isLeipzig) false else true,
                        outsideLeipzigEvening = if (isLeipzig) false else true,
                        dayLocationLabel = resolvedLabel,
                        dayLocationSource = DayLocationSource.MANUAL,
                        needsReview = false,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }
        }
        
        workEntryDao.upsert(updatedEntry)
        return updatedEntry
    }
}
