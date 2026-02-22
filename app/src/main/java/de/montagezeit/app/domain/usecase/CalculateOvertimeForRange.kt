package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.OvertimeEntryRow
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator

data class OvertimeResult(
    val totalOvertimeHours: Double,
    val totalActualHours: Double,
    val totalTargetHours: Double,
    val countedDays: Int,
    val offDayTravelHours: Double,
    val offDayTravelDays: Int
)

class CalculateOvertimeForRange {

    operator fun invoke(entries: List<OvertimeEntryRow>, dailyTargetHours: Double): OvertimeResult {
        if (entries.isEmpty()) {
            return OvertimeResult(
                totalOvertimeHours = 0.0,
                totalActualHours = 0.0,
                totalTargetHours = 0.0,
                countedDays = 0,
                offDayTravelHours = 0.0,
                offDayTravelDays = 0
            )
        }

        var totalActualHours = 0.0
        var totalTargetHours = 0.0
        var countedDays = 0
        var offDayTravelHours = 0.0
        var offDayTravelDays = 0

        entries.forEach { entry ->
            if (!entry.confirmedWorkDay) {
                return@forEach
            }
            val workEntry = entry.toWorkEntry()
            when (entry.dayType) {
                DayType.WORK -> {
                    countedDays += 1
                    totalTargetHours += dailyTargetHours
                    totalActualHours += TimeCalculator.calculatePaidTotalHours(workEntry)
                }
                DayType.OFF -> {
                    val travelHours = TimeCalculator.calculateTravelMinutes(workEntry) / 60.0
                    if (travelHours > 0.0) {
                        offDayTravelHours += travelHours
                        offDayTravelDays += 1
                    }
                }
            }
        }

        return OvertimeResult(
            totalOvertimeHours = totalActualHours - totalTargetHours,
            totalActualHours = totalActualHours,
            totalTargetHours = totalTargetHours,
            countedDays = countedDays,
            offDayTravelHours = offDayTravelHours,
            offDayTravelDays = offDayTravelDays
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
