package de.montagezeit.app.ui.screen.today

import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class TodayDialogsStateHolder @Inject constructor() {
    private val _showDailyCheckInDialog = MutableStateFlow(false)
    val showDailyCheckInDialog: StateFlow<Boolean> = _showDailyCheckInDialog.asStateFlow()

    private val _dailyCheckInLocationInput = MutableStateFlow("")
    val dailyCheckInLocationInput: StateFlow<String> = _dailyCheckInLocationInput.asStateFlow()

    private val _dailyCheckInIsArrivalDeparture = MutableStateFlow(false)
    val dailyCheckInIsArrivalDeparture: StateFlow<Boolean> = _dailyCheckInIsArrivalDeparture.asStateFlow()

    private val _dailyCheckInBreakfastIncluded = MutableStateFlow(false)
    val dailyCheckInBreakfastIncluded: StateFlow<Boolean> = _dailyCheckInBreakfastIncluded.asStateFlow()

    private val _showDayLocationDialog = MutableStateFlow(false)
    val showDayLocationDialog: StateFlow<Boolean> = _showDayLocationDialog.asStateFlow()

    private val _dayLocationInput = MutableStateFlow("")
    val dayLocationInput: StateFlow<String> = _dayLocationInput.asStateFlow()

    private val _showDeleteDayDialog = MutableStateFlow(false)
    val showDeleteDayDialog: StateFlow<Boolean> = _showDeleteDayDialog.asStateFlow()

    private val _dailyCheckInDate = MutableStateFlow(LocalDate.now())
    val dailyCheckInDate: StateFlow<LocalDate> = _dailyCheckInDate.asStateFlow()

    val dailyCheckInAllowancePreviewCents: Flow<Int> = combine(
        _dailyCheckInIsArrivalDeparture,
        _dailyCheckInBreakfastIncluded
    ) { arrival, breakfast ->
        MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = arrival,
            breakfastIncluded = breakfast
        ).amountCents
    }

    fun openDailyCheckInDialog(
        selectedDate: LocalDate,
        locationPrefill: String,
        isArrivalDeparture: Boolean,
        breakfastIncluded: Boolean
    ) {
        _dailyCheckInDate.value = selectedDate
        _dailyCheckInLocationInput.value = locationPrefill
        _dailyCheckInIsArrivalDeparture.value = isArrivalDeparture
        _dailyCheckInBreakfastIncluded.value = breakfastIncluded
        _showDailyCheckInDialog.value = true
    }

    fun updateDailyCheckInLocation(label: String) {
        _dailyCheckInLocationInput.value = label
    }

    fun updateDailyCheckInArrivalDeparture(value: Boolean) {
        _dailyCheckInIsArrivalDeparture.value = value
    }

    fun updateDailyCheckInBreakfastIncluded(value: Boolean) {
        _dailyCheckInBreakfastIncluded.value = value
    }

    fun dismissDailyCheckInDialog() {
        _showDailyCheckInDialog.value = false
    }

    fun openDayLocationDialog(locationPrefill: String) {
        _dayLocationInput.value = locationPrefill
        _showDayLocationDialog.value = true
    }

    fun updateDayLocationInput(label: String) {
        _dayLocationInput.value = label
    }

    fun dismissDayLocationDialog() {
        _showDayLocationDialog.value = false
    }

    fun openDeleteDayDialog() {
        _showDeleteDayDialog.value = true
    }

    fun dismissDeleteDayDialog() {
        _showDeleteDayDialog.value = false
    }
}
