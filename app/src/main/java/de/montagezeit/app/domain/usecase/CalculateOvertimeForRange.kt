package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.OvertimeEntryRow
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator

data class OvertimeResult(
    val totalOvertimeHours: Double,
    val totalActualHours: Double,
    val totalTargetHours: Double,
    val countedDays: Int
)

class CalculateOvertimeForRange {

    operator fun invoke(entries: List<OvertimeEntryRow>, dailyTargetHours: Double): OvertimeResult {
        if (entries.isEmpty()) {
            return OvertimeResult(
                totalOvertimeHours = 0.0,
                totalActualHours = 0.0,
                totalTargetHours = 0.0,
                countedDays = 0
            )
        }

        var totalActualHours = 0.0
        var totalTargetHours = 0.0
        var countedDays = 0

        entries.forEach { entry ->
            if (!entry.confirmedWorkDay) {
                return@forEach
            }

            countedDays += 1

            if (entry.dayType == DayType.WORK) {
                totalTargetHours += dailyTargetHours
            }

            totalActualHours += TimeCalculator.calculatePaidTotalHours(entry.toWorkEntry())
        }

        return OvertimeResult(
            totalOvertimeHours = totalActualHours - totalTargetHours,
            totalActualHours = totalActualHours,
            totalTargetHours = totalTargetHours,
            countedDays = countedDays
        )
    }
}

private fun OvertimeEntryRow.toWorkEntry(): WorkEntry {
    return WorkEntry(
        date = date,
        workStart = workStart,
        workEnd = workEnd,
        breakMinutes = breakMinutes,
        dayType = dayType,
        travelStartAt = travelStartAt,
        travelArriveAt = travelArriveAt,
        travelPaidMinutes = travelPaidMinutes,
        confirmedWorkDay = confirmedWorkDay
    )
}
