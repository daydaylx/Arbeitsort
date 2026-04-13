package de.montagezeit.app.ui.screen.edit

import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import javax.inject.Inject

class EditEntryDraftRules @Inject constructor() {

    fun normalizedTravelLegs(formData: EditFormData): List<EditTravelLegForm> {
        val explicitLegs = formData.travelLegs.filterNot(EditTravelLegForm::isBlank)
        if (explicitLegs.isNotEmpty()) return explicitLegs

        val fallbackLeg = EditTravelLegForm(
            startTime = formData.travelStartTime,
            arriveTime = formData.travelArriveTime
        )
        return if (fallbackLeg.isBlank()) emptyList() else listOf(fallbackLeg)
    }

    fun mealAllowancePreviewCents(formData: EditFormData): Int {
        return resolveMealAllowanceForSave(
            dayType = formData.dayType,
            isArrivalDeparture = formData.mealIsArrivalDeparture,
            breakfastIncluded = formData.mealBreakfastIncluded,
            workMinutes = calculateEffectiveWorkMinutes(formData),
            travelMinutes = calculateEffectiveTravelMinutes(formData)
        ).amountCents
    }

    fun calculateEffectiveWorkMinutes(formData: EditFormData): Int {
        if (formData.dayType != DayType.WORK || !formData.hasWorkTimes) return 0

        val rawMinutes = durationMinutes(
            start = formData.workStart.toSecondOfDay(),
            end = formData.workEnd.toSecondOfDay()
        )
        return (rawMinutes - formData.breakMinutes).coerceAtLeast(0)
    }

    fun calculateEffectiveTravelMinutes(formData: EditFormData): Int {
        return normalizedTravelLegs(formData).sumOf { leg ->
            when {
                leg.paidMinutesOverride != null -> leg.paidMinutesOverride
                leg.startTime != null && leg.arriveTime != null -> durationMinutes(
                    start = requireNotNull(leg.startTime).toSecondOfDay(),
                    end = requireNotNull(leg.arriveTime).toSecondOfDay()
                )
                else -> 0
            }
        }
    }

    fun validate(formData: EditFormData): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val relevantTravelLegs = normalizedTravelLegs(formData)

        if (formData.dayType == DayType.COMP_TIME) {
            if (relevantTravelLegs.isNotEmpty()) {
                errors += ValidationError.TravelNotAllowedForCompTime
            }
            return errors
        }

        if (formData.dayType == DayType.WORK) {
            if (formData.dayLocationLabel.isNullOrBlank()) {
                errors += ValidationError.MissingDayLocation
            }
            if (!formData.hasWorkTimes && relevantTravelLegs.isEmpty()) {
                errors += ValidationError.MissingWorkOrTravel
            }

            if (formData.hasWorkTimes) {
                val workTimeValid = formData.workEnd != formData.workStart
                if (!workTimeValid) {
                    errors += ValidationError.WorkEndBeforeStart
                }
                if (formData.breakMinutes < 0) {
                    errors += ValidationError.NegativeBreakMinutes
                }
                if (workTimeValid) {
                    val totalWorkMinutes = durationMinutes(
                        start = formData.workStart.toSecondOfDay(),
                        end = formData.workEnd.toSecondOfDay()
                    )
                    if (totalWorkMinutes > MAX_WORKDAY_MINUTES) {
                        errors += ValidationError.WorkDayTooLong
                    }
                    if (formData.breakMinutes > totalWorkMinutes) {
                        errors += ValidationError.BreakLongerThanWorkTime
                    }
                }
            }
        }

        relevantTravelLegs.forEachIndexed { index, leg ->
            val hasStart = leg.startTime != null
            val hasArrive = leg.arriveTime != null
            val hasOverride = leg.paidMinutesOverride != null

            if (hasStart.xor(hasArrive)) {
                errors += ValidationError.TravelLegIncomplete(index)
                return@forEachIndexed
            }
            if (!hasStart && !hasArrive && !hasOverride) {
                errors += ValidationError.TravelLegMissingTimeWindow(index)
                return@forEachIndexed
            }
            if (hasStart && hasArrive) {
                val startTime = requireNotNull(leg.startTime)
                val arriveTime = requireNotNull(leg.arriveTime)
                if (arriveTime == startTime) {
                    errors += ValidationError.TravelArriveBeforeStart(index)
                } else if (durationMinutes(startTime.toSecondOfDay(), arriveTime.toSecondOfDay()) > MAX_TRAVEL_MINUTES) {
                    errors += ValidationError.TravelTooLong(index)
                }
            }
        }

        return errors.distinct()
    }

    companion object {
        internal val Default = EditEntryDraftRules()
        private const val MINUTES_PER_DAY = 24 * 60
        private const val MAX_WORKDAY_MINUTES = 18 * 60
        private const val MAX_TRAVEL_MINUTES = 16 * 60

        private fun durationMinutes(start: Int, end: Int): Int {
            val rawDiffMinutes = (end - start) / 60
            return if (rawDiffMinutes < 0) rawDiffMinutes + MINUTES_PER_DAY else rawDiffMinutes
        }
    }
}

fun EditFormData.normalizedTravelLegs(rules: EditEntryDraftRules = EditEntryDraftRules.Default): List<EditTravelLegForm> =
    rules.normalizedTravelLegs(this)

fun EditFormData.mealAllowancePreviewCents(rules: EditEntryDraftRules = EditEntryDraftRules.Default): Int =
    rules.mealAllowancePreviewCents(this)

fun EditFormData.calculateEffectiveWorkMinutes(rules: EditEntryDraftRules = EditEntryDraftRules.Default): Int =
    rules.calculateEffectiveWorkMinutes(this)

fun EditFormData.calculateEffectiveTravelMinutes(rules: EditEntryDraftRules = EditEntryDraftRules.Default): Int =
    rules.calculateEffectiveTravelMinutes(this)

fun EditFormData.validate(rules: EditEntryDraftRules = EditEntryDraftRules.Default): List<ValidationError> =
    rules.validate(this)

fun EditFormData.isValid(rules: EditEntryDraftRules = EditEntryDraftRules.Default): Boolean =
    rules.validate(this).isEmpty()
