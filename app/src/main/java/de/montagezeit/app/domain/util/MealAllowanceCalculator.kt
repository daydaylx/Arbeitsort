package de.montagezeit.app.domain.util

import de.montagezeit.app.data.local.entity.DayType

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
    const val BREAKFAST_DEDUCTION_CENTS = 580

    data class Result(val baseCents: Int, val amountCents: Int)

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

    fun formatEuro(cents: Int): String {
        require(cents >= 0) { "cents must be non-negative" }
        val euros = cents / 100
        val remainder = cents % 100
        return "%d,%02d €".format(euros, remainder)
    }
}
