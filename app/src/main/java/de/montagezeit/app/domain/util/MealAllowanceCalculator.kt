package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs

/**
 * Berechnet die Verpflegungspauschale nach fachlichen Regeln.
 *
 * Regeln:
 * - Gilt nur für DayType.WORK; bei OFF und COMP_TIME immer 0.
 * - Standardbasis: 28,00 € (2800 Cent)
 * - Bei Anreise/Abreise: Basis 14,00 € (1400 Cent)
 * - Frühstück erhalten: Abzug 5,80 € (580 Cent)
 * - Ergebnis nie negativ (Minimum 0)
 * - Kein Einfluss auf Arbeitszeit oder Überstunden.
 */
object MealAllowanceCalculator {

    const val BASE_NORMAL_CENTS = 2800
    const val BASE_ARRIVAL_DEPARTURE_CENTS = 1400
    const val BREAKFAST_DEDUCTION_CENTS = 560

    data class Result(val baseCents: Int, val amountCents: Int)
    data class Snapshot(
        val isArrivalDeparture: Boolean,
        val breakfastIncluded: Boolean,
        val baseCents: Int,
        val amountCents: Int
    )

    fun isEligible(
        dayType: DayType,
        workMinutes: Int,
        travelMinutes: Int
    ): Boolean {
        return dayType == DayType.WORK && (workMinutes > 0 || travelMinutes > 0)
    }

    fun calculate(
        dayType: DayType,
        isArrivalDeparture: Boolean,
        breakfastIncluded: Boolean
    ): Result {
        if (dayType != DayType.WORK) return Result(baseCents = 0, amountCents = 0)
        val base = if (isArrivalDeparture) BASE_ARRIVAL_DEPARTURE_CENTS else BASE_NORMAL_CENTS
        val deduction = if (breakfastIncluded) BREAKFAST_DEDUCTION_CENTS else 0
        val amount = maxOf(0, base - deduction)
        return Result(baseCents = base, amountCents = amount)
    }

    fun resolveForActivity(
        dayType: DayType,
        isArrivalDeparture: Boolean,
        breakfastIncluded: Boolean,
        workMinutes: Int,
        travelMinutes: Int
    ): Snapshot {
        if (!isEligible(dayType = dayType, workMinutes = workMinutes, travelMinutes = travelMinutes)) {
            return Snapshot(
                isArrivalDeparture = false,
                breakfastIncluded = false,
                baseCents = 0,
                amountCents = 0
            )
        }

        val result = calculate(
            dayType = dayType,
            isArrivalDeparture = isArrivalDeparture,
            breakfastIncluded = breakfastIncluded
        )
        return Snapshot(
            isArrivalDeparture = isArrivalDeparture,
            breakfastIncluded = breakfastIncluded,
            baseCents = result.baseCents,
            amountCents = result.amountCents
        )
    }

    fun resolveEffectiveStoredSnapshot(record: WorkEntryWithTravelLegs): Snapshot {
        val workMinutes = TimeCalculator.calculateWorkMinutes(record.workEntry)
        val travelMinutes = TimeCalculator.calculateTravelMinutes(record.orderedTravelLegs)
        if (!isEligible(record.workEntry.dayType, workMinutes, travelMinutes)) {
            return Snapshot(
                isArrivalDeparture = false,
                breakfastIncluded = false,
                baseCents = 0,
                amountCents = 0
            )
        }

        val entry = record.workEntry
        return Snapshot(
            isArrivalDeparture = entry.mealIsArrivalDeparture,
            breakfastIncluded = entry.mealBreakfastIncluded,
            baseCents = entry.mealAllowanceBaseCents,
            amountCents = entry.mealAllowanceAmountCents
        )
    }

    fun formatEuro(cents: Int): String {
        require(cents >= 0) { "cents must be non-negative" }
        val euros = cents / 100
        val remainder = cents % 100
        return "%d,%02d €".format(euros, remainder)
    }
}
