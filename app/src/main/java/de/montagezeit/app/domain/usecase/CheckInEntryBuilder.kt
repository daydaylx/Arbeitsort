package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import java.time.LocalDate

internal object CheckInEntryBuilder {

    enum class Snapshot {
        MORNING,
        EVENING
    }

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
