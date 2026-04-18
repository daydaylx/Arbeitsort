package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.model.DayClassification
import de.montagezeit.app.domain.util.TimeCalculator

data class EntryStatus(
    val classification: DayClassification,
    val workMinutes: Int,
    val travelMinutes: Int,
    val hasActivity: Boolean,
    val isConfirmed: Boolean,
    val isStatisticsEligible: Boolean,
    val isReminderTerminal: Boolean
)

object EntryStatusResolver {
    private val classifier = ClassifyDay()

    fun resolve(entry: WorkEntryWithTravelLegs): EntryStatus =
        resolve(entry.workEntry, entry.orderedTravelLegs)

    fun resolve(entry: WorkEntry, travelLegs: List<TravelLeg> = emptyList()): EntryStatus {
        val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
        val travelMinutes = TimeCalculator.calculateTravelMinutes(travelLegs)
        val classification = classifier(
            dayType = entry.dayType,
            workMinutes = workMinutes,
            travelMinutes = travelMinutes
        )
        val hasActivity = when (entry.dayType) {
            DayType.WORK -> workMinutes > 0 || travelMinutes > 0
            DayType.OFF, DayType.COMP_TIME -> true
        }
        val isConfirmed = when (entry.dayType) {
            DayType.WORK -> entry.confirmedWorkDay && hasActivity
            DayType.OFF, DayType.COMP_TIME -> true
        }

        return EntryStatus(
            classification = classification,
            workMinutes = workMinutes,
            travelMinutes = travelMinutes,
            hasActivity = hasActivity,
            isConfirmed = isConfirmed,
            isStatisticsEligible = isConfirmed,
            isReminderTerminal = isConfirmed
        )
    }

    fun shouldAutoConfirmWorkDay(
        entry: WorkEntry,
        travelLegs: List<TravelLeg> = emptyList()
    ): Boolean {
        if (entry.dayType != DayType.WORK) return false
        val status = resolve(entry, travelLegs)
        return status.hasActivity
    }

    fun isPendingWorkDay(
        entry: WorkEntry,
        travelLegs: List<TravelLeg> = emptyList()
    ): Boolean {
        return entry.dayType == DayType.WORK && !resolve(entry, travelLegs).isConfirmed
    }
}
