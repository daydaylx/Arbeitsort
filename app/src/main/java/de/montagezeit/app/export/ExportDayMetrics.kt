package de.montagezeit.app.export

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.util.TimeCalculator
import kotlin.math.roundToInt

internal const val MINUTES_PER_HOUR = 60

internal data class ExportDayMetrics(
    val isWorkDay: Boolean,
    val travelLegs: List<TravelLeg>,
    val workMinutes: Int,
    val travelMinutes: Int,
    val paidTotalMinutes: Int
) {
    val workHours: Double get() = workMinutes / MINUTES_PER_HOUR.toDouble()
}

internal fun dailyTargetMinutes(dailyTargetHours: Double): Int {
    return (dailyTargetHours * MINUTES_PER_HOUR).roundToInt()
}

internal fun buildExportDayMetrics(
    record: WorkEntryWithTravelLegs,
    dailyTargetHours: Double
): ExportDayMetrics {
    val entry = record.workEntry
    val isWorkDay = entry.dayType.isWorkLike
    val travelLegs = if (isWorkDay) record.orderedTravelLegs else emptyList()
    val targetMinutes = dailyTargetMinutes(dailyTargetHours)
    val workMinutes = when (entry.dayType) {
        DayType.VACATION -> targetMinutes
        DayType.WORK -> TimeCalculator.calculateWorkMinutes(entry)
        DayType.OFF, DayType.COMP_TIME -> 0
    }
    val travelMinutes = if (isWorkDay) TimeCalculator.calculateTravelMinutes(travelLegs) else 0
    val paidTotalMinutes = when (entry.dayType) {
        DayType.VACATION -> targetMinutes
        DayType.WORK -> workMinutes + travelMinutes
        DayType.OFF, DayType.COMP_TIME -> 0
    }

    return ExportDayMetrics(
        isWorkDay = isWorkDay,
        travelLegs = travelLegs,
        workMinutes = workMinutes,
        travelMinutes = travelMinutes,
        paidTotalMinutes = paidTotalMinutes
    )
}
