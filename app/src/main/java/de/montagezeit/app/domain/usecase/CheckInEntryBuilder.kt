package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

internal object CheckInEntryBuilder {

    enum class Snapshot {
        MORNING,
        EVENING
    }

    /**
     * Baut einen WorkEntry für einen Check-in-Snapshot.
     *
     * - Ist [existingEntry] null, wird ein neuer Eintrag mit [DayType.WORK] angelegt.
     * - Ist [existingEntry] nicht null und hat [DayType.WORK], werden die Felder des
     *   bestehenden Eintrags preserviert und nur der jeweilige Timestamp (morning/evening)
     *   und [dayLocationLabel] aktualisiert.
     * - Ist [existingEntry] nicht null und hat einen anderen DayType (z. B. OFF oder COMP_TIME),
     *   wird eine [IllegalStateException] geworfen.
     * - [dayLocationLabel] wird über [DayLocationResolver.resolve] aufgelöst:
     *   nicht-leerer Wert des Eintrags wird übernommen, andernfalls wird `""` gesetzt.
     */
    fun build(
        date: LocalDate,
        existingEntry: WorkEntry?,
        snapshot: Snapshot
    ): WorkEntry {
        val now = System.currentTimeMillis()
        if (existingEntry != null && existingEntry.dayType != DayType.WORK) {
            throw IllegalStateException("Check-in nicht erlaubt für dayType=${existingEntry.dayType}")
        }
        val normalizedEntry = existingEntry
        val dayLocation = DayLocationResolver.resolve(normalizedEntry)

        return if (snapshot == Snapshot.MORNING) {
            normalizedEntry?.copy(
                morningCapturedAt = now,
                dayLocationLabel = dayLocation,
                updatedAt = now
            ) ?: createDefaultEntry(
                date = date,
                morningCapturedAt = now,
                dayLocationLabel = dayLocation
            )
        } else {
            normalizedEntry?.copy(
                eveningCapturedAt = now,
                dayLocationLabel = dayLocation,
                updatedAt = now
            ) ?: createDefaultEntry(
                date = date,
                eveningCapturedAt = now,
                dayLocationLabel = dayLocation
            )
        }
    }

    private fun createDefaultEntry(
        date: LocalDate,
        morningCapturedAt: Long? = null,
        eveningCapturedAt: Long? = null,
        dayLocationLabel: String = ""
    ): WorkEntry {
        val now = System.currentTimeMillis()
        return WorkEntry(
            date = date,
            dayType = DayType.WORK,
            dayLocationLabel = dayLocationLabel,
            morningCapturedAt = morningCapturedAt,
            eveningCapturedAt = eveningCapturedAt,
            createdAt = now,
            updatedAt = now
        )
    }
}
