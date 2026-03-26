package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.domain.util.TimeCalculator

data class WorkStatsResult(
    val workDays: Int,
    val offDays: Int,
    val totalWorkMinutes: Int,
    val totalTravelMinutes: Int,
    val totalPaidMinutes: Int,
    val mealAllowanceCents: Int
) {
    val averageWorkHoursPerDay: Double
        get() = if (workDays > 0) totalWorkMinutes / 60.0 / workDays else 0.0
}

class AggregateWorkStats {

    operator fun invoke(entries: List<WorkEntryWithTravelLegs>): WorkStatsResult {
        val confirmed = entries.filter { it.workEntry.confirmedWorkDay }
        val workDays = confirmed.count { it.workEntry.dayType == DayType.WORK }
        val offDays = confirmed.count { it.workEntry.dayType == DayType.OFF || it.workEntry.dayType == DayType.COMP_TIME }
        val totalWorkMinutes = confirmed.sumOf { TimeCalculator.calculateWorkMinutes(it.workEntry) }
        val totalTravelMinutes = confirmed.sumOf { TimeCalculator.calculateTravelMinutes(it.orderedTravelLegs) }
        val mealAllowanceCents = confirmed
            .filter { it.workEntry.dayType == DayType.WORK }
            .sumOf { it.workEntry.mealAllowanceAmountCents }
        return WorkStatsResult(
            workDays = workDays,
            offDays = offDays,
            totalWorkMinutes = totalWorkMinutes,
            totalTravelMinutes = totalTravelMinutes,
            totalPaidMinutes = totalWorkMinutes + totalTravelMinutes,
            mealAllowanceCents = mealAllowanceCents
        )
    }
}
