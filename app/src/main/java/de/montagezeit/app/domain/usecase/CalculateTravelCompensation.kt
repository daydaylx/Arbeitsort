package de.montagezeit.app.domain.usecase

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil

class CalculateTravelCompensation {

    data class Result(
        val paidMinutes: Int,
        val paidHoursDisplay: String
    )

    @Suppress("UNUSED_PARAMETER")
    operator fun invoke(
        fromLabel: String?,
        toLabel: String?,
        distanceKm: Double,
        roundingStepMinutes: Int
    ): Result {
        val rawMinutes = distanceKm / 100.0 * 60.0
        val roundedMinutes = if (roundingStepMinutes > 0) {
            val step = roundingStepMinutes.toDouble()
            ceil(rawMinutes / step) * step
        } else {
            rawMinutes
        }
        val paidMinutes = roundedMinutes.toInt()
        val paidHoursDisplay = formatPaidHours(paidMinutes)
        return Result(paidMinutes = paidMinutes, paidHoursDisplay = paidHoursDisplay)
    }

    companion object {
        fun formatPaidHours(minutes: Int): String {
            val hours = minutes / 60.0
            val symbols = DecimalFormatSymbols(Locale.GERMAN)
            val formatter = DecimalFormat("0.00", symbols)
            return "${formatter.format(hours)} h"
        }
    }
}
