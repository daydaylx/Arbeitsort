package de.montagezeit.app.ui.screen.edit

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.transitionToDayType
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.DeleteWorkEntryByDate
import de.montagezeit.app.domain.usecase.GetWorkEntryByDate
import de.montagezeit.app.domain.usecase.GetWorkEntryWithTravelByDate
import de.montagezeit.app.domain.usecase.ReplaceWorkEntryWithTravelLegs
import de.montagezeit.app.domain.usecase.WorkEntryFactory
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.ui.util.UiText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    private val getWorkEntryByDate: GetWorkEntryByDate,
    private val getWorkEntryWithTravelByDate: GetWorkEntryWithTravelByDate,
    private val deleteWorkEntryByDate: DeleteWorkEntryByDate,
    private val replaceWorkEntryWithTravelLegs: ReplaceWorkEntryWithTravelLegs,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editTimeZone: ZoneId = ZoneId.systemDefault()
    private var currentDate: LocalDate =
        savedStateHandle.get<String>("date")?.let(LocalDate::parse) ?: LocalDate.now()

    private val _screenState = MutableStateFlow(EditScreenState())
    val screenState: StateFlow<EditScreenState> = _screenState.asStateFlow()

    private var loadEntryJob: Job? = null

    init {
        loadEntry(currentDate)
    }

    fun setDate(date: LocalDate) {
        if (date == currentDate) return
        currentDate = date
        savedStateHandle["date"] = date.toString()
        loadEntry(date)
    }

    fun setFormData(data: EditFormData) {
        _screenState.update { it.copy(formData = data) }
        viewModelScope.launch {
            val entry = getWorkEntryByDate(currentDate)
            _screenState.update { state ->
                state.copy(
                    uiState = if (entry == null) {
                        EditUiState.NewEntry(currentDate)
                    } else {
                        EditUiState.Success(entry)
                    }
                )
            }
        }
    }

    private fun loadEntry(date: LocalDate) {
        loadEntryJob?.cancel()
        loadEntryJob = viewModelScope.launch {
            _screenState.update { it.copy(uiState = EditUiState.Loading, isSaving = false, originalFormData = null) }

            try {
                val settings = reminderSettingsManager.settings.first()
                val dailyTargetHours = settings.dailyTargetHours

                val record = getWorkEntryWithTravelByDate(date)
                if (record != null) {
                    val formData = EditFormData.fromEntry(
                        entry = record.workEntry,
                        travelLegs = record.orderedTravelLegs,
                        zoneId = editTimeZone
                    )
                    _screenState.update {
                        it.copy(
                            formData = formData,
                            originalFormData = formData,
                            uiState = EditUiState.Success(record.workEntry),
                            isSaving = false,
                            dailyTargetHours = dailyTargetHours
                        )
                    }
                } else {
                    val defaultDayType = WorkEntryFactory.resolveAutoDayType(date, settings)
                    val defaultEntry = WorkEntryFactory.createDefaultEntry(
                        date = date,
                        settings = settings,
                        dayType = defaultDayType
                    )
                    val formData = EditFormData.defaultFor(
                        dayType = defaultDayType,
                        workStart = settings.workStart,
                        workEnd = settings.workEnd,
                        breakMinutes = settings.breakMinutes
                    )
                    val finalFormData = formData.copy(dayLocationLabel = defaultEntry.dayLocationLabel.ifBlank { null })
                    _screenState.update {
                        it.copy(
                            formData = finalFormData,
                            originalFormData = finalFormData,
                            uiState = EditUiState.NewEntry(date),
                            isSaving = false,
                            dailyTargetHours = dailyTargetHours
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _screenState.update {
                    it.copy(
                        uiState = EditUiState.Error(toUiText(e.message, R.string.edit_error_unknown)),
                        isSaving = false
                    )
                }
            }
        }
    }

    fun updateDayType(dayType: DayType) {
        updateFormData {
            when (dayType) {
                DayType.WORK -> it.copy(dayType = dayType)
                DayType.OFF -> it.copy(dayType = dayType, hasWorkTimes = false)
                DayType.COMP_TIME -> it.copy(dayType = dayType, hasWorkTimes = false, travelLegs = emptyList())
            }
        }
    }

    fun setHasWorkTimes(enabled: Boolean) {
        updateFormData { form ->
            form.copy(
                hasWorkTimes = enabled,
                breakMinutes = if (enabled) form.breakMinutes else 0
            )
        }
    }

    fun updateWorkStart(hour: Int, minute: Int) {
        updateFormData { it.copy(hasWorkTimes = true, workStart = LocalTime.of(hour, minute)) }
    }

    fun updateWorkEnd(hour: Int, minute: Int) {
        updateFormData { it.copy(hasWorkTimes = true, workEnd = LocalTime.of(hour, minute)) }
    }

    fun updateBreakMinutes(minutes: Int) {
        updateFormData { it.copy(breakMinutes = minutes) }
    }

    fun updateDayLocationLabel(label: String) {
        updateFormData {
            val trimmed = label.trim()
            it.copy(dayLocationLabel = trimmed.takeIf(String::isNotBlank))
        }
    }

    fun updateNote(note: String) {
        updateFormData { it.copy(note = note.takeIf(String::isNotBlank)) }
    }

    fun updateMealArrivalDeparture(isArrivalDeparture: Boolean) {
        updateFormData { it.copy(mealIsArrivalDeparture = isArrivalDeparture) }
    }

    fun updateMealBreakfastIncluded(breakfastIncluded: Boolean) {
        updateFormData { it.copy(mealBreakfastIncluded = breakfastIncluded) }
    }

    fun addTravelLeg() {
        updateFormData { it.copy(travelLegs = it.travelLegs + EditTravelLegForm()) }
    }

    fun removeTravelLeg(index: Int) {
        updateFormData { state ->
            state.copy(travelLegs = state.travelLegs.filterIndexed { currentIndex, _ -> currentIndex != index })
        }
    }

    fun updateTravelLegStart(index: Int, time: LocalTime?) {
        updateTravelLeg(index) { it.copy(startTime = time) }
    }

    fun updateTravelLegArrive(index: Int, time: LocalTime?) {
        updateTravelLeg(index) { it.copy(arriveTime = time) }
    }

    fun updateTravelLegStartLabel(index: Int, label: String) {
        updateTravelLeg(index) { it.copy(startLabel = label.takeIf(String::isNotBlank)) }
    }

    fun updateTravelLegEndLabel(index: Int, label: String) {
        updateTravelLeg(index) { it.copy(endLabel = label.takeIf(String::isNotBlank)) }
    }

    fun clearTravel() {
        updateFormData { it.copy(travelLegs = emptyList()) }
    }

    fun applyDefaultWorkTimes() {
        viewModelScope.launch {
            val settings = reminderSettingsManager.settings.first()
            updateFormData {
                it.copy(
                    hasWorkTimes = true,
                    workStart = settings.workStart,
                    workEnd = settings.workEnd,
                    breakMinutes = settings.breakMinutes
                )
            }
        }
    }

    fun copyFromPreviousDay(onResult: (Boolean) -> Unit) {
        if (_screenState.value.isSaving) { onResult(false); return }
        viewModelScope.launch(Dispatchers.IO) {
            val previousDate = currentDate.minusDays(1)
            val record = getWorkEntryWithTravelByDate(previousDate)
            withContext(Dispatchers.Main) {
                if (record != null) {
                    val copied = EditFormData.fromEntry(
                        entry = record.workEntry,
                        travelLegs = record.orderedTravelLegs
                    )
                    _screenState.update { it.copy(formData = copied) }
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
        }
    }

    fun deleteCurrentEntry(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (_screenState.value.isSaving) return@launch

            try {
                _screenState.update { it.copy(isSaving = true) }
                val existingEntry = getWorkEntryByDate(currentDate)
                if (existingEntry == null) {
                    _screenState.update { it.copy(isSaving = false, uiState = EditUiState.NewEntry(currentDate)) }
                    onResult(false)
                    return@launch
                }

                deleteWorkEntryByDate(currentDate)
                _screenState.update { it.copy(isSaving = false, uiState = EditUiState.Saved) }
                onResult(true)
            } catch (e: Exception) {
                _screenState.update {
                    it.copy(
                        isSaving = false,
                        uiState = EditUiState.Error(
                            toUiText(e.message, R.string.today_error_delete_failed)
                        )
                    )
                }
                onResult(false)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val stateSnapshot = _screenState.value
            if (stateSnapshot.isSaving) return@launch

            val data = stateSnapshot.formData
            val validationErrors = data.validate()
            val currentState = stateSnapshot.uiState
            if (validationErrors.isNotEmpty()) {
                when (currentState) {
                    is EditUiState.Success -> {
                        _screenState.update { it.copy(uiState = currentState.copy(validationErrors = validationErrors)) }
                    }
                    is EditUiState.NewEntry -> {
                        _screenState.update { it.copy(uiState = currentState.copy(validationErrors = validationErrors)) }
                    }
                    else -> {
                        _screenState.update {
                            it.copy(uiState = EditUiState.Error(UiText.StringResource(R.string.error_validation_failed)))
                        }
                    }
                }
                return@launch
            }

            val mealAllowance = resolveMealAllowanceForSave(
                dayType = data.dayType,
                isArrivalDeparture = data.mealIsArrivalDeparture,
                breakfastIncluded = data.mealBreakfastIncluded
            )
            val now = System.currentTimeMillis()
            val date = when (currentState) {
                is EditUiState.Success -> currentState.entry.date
                is EditUiState.NewEntry -> currentState.date
                else -> return@launch
            }
            val originalEntry = (currentState as? EditUiState.Success)?.entry
            val baseEntry = originalEntry?.transitionToDayType(dayType = data.dayType, now = now)
                ?: WorkEntry(
                    date = date,
                    createdAt = now,
                    updatedAt = now
                ).transitionToDayType(dayType = data.dayType, now = now)

            val persistWorkBlock = data.dayType == DayType.WORK && data.hasWorkTimes
            val entryToSave = baseEntry.copy(
                dayType = data.dayType,
                workStart = if (persistWorkBlock) data.workStart else null,
                workEnd = if (persistWorkBlock) data.workEnd else null,
                breakMinutes = if (persistWorkBlock) data.breakMinutes else 0,
                dayLocationLabel = data.dayLocationLabel.orEmpty(),
                note = data.note,
                mealIsArrivalDeparture = mealAllowance.isArrivalDeparture,
                mealBreakfastIncluded = mealAllowance.breakfastIncluded,
                mealAllowanceBaseCents = mealAllowance.baseCents,
                mealAllowanceAmountCents = mealAllowance.amountCents,
                updatedAt = now
            )

            val legsToSave = if (data.dayType == DayType.COMP_TIME) {
                emptyList()
            } else {
                data.normalizedTravelLegs().mapIndexed { index, leg ->
                    TravelLeg(
                        workEntryDate = date,
                        sortOrder = index,
                        category = inferCategory(index, data.normalizedTravelLegs().size),
                        startAt = leg.startTime?.let { toEpochMillis(date, it) },
                        arriveAt = leg.arriveTime?.let { toEpochMillis(date, it) },
                        startLabel = leg.startLabel,
                        endLabel = leg.endLabel,
                        paidMinutesOverride = leg.paidMinutesOverride?.takeIf { leg.startTime == null && leg.arriveTime == null },
                        source = TravelSource.MANUAL,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }

            try {
                _screenState.update { it.copy(isSaving = true) }
                replaceWorkEntryWithTravelLegs(entryToSave, legsToSave)
                _screenState.update { it.copy(isSaving = false, uiState = EditUiState.Saved, originalFormData = data) }
            } catch (e: Exception) {
                _screenState.update {
                    it.copy(
                        isSaving = false,
                        uiState = EditUiState.Error(
                            toUiText(e.message, R.string.edit_error_save_failed)
                        )
                    )
                }
            }
        }
    }

    fun clearValidationErrors() {
        when (val currentState = _screenState.value.uiState) {
            is EditUiState.Success -> {
                if (currentState.validationErrors.isNotEmpty()) {
                    _screenState.update { it.copy(uiState = currentState.copy(validationErrors = emptyList())) }
                }
            }
            is EditUiState.NewEntry -> {
                if (currentState.validationErrors.isNotEmpty()) {
                    _screenState.update { it.copy(uiState = currentState.copy(validationErrors = emptyList())) }
                }
            }
            else -> Unit
        }
    }

    fun reloadEntry() {
        loadEntry(currentDate)
    }

    fun copyEntryData(): EditFormData = _screenState.value.formData.copy()

    private fun updateTravelLeg(index: Int, update: (EditTravelLegForm) -> EditTravelLegForm) {
        updateFormData { state ->
            state.copy(
                travelLegs = state.travelLegs.mapIndexed { currentIndex, leg ->
                    if (currentIndex == index) update(leg) else leg
                }
            )
        }
    }

    private inline fun updateFormData(update: (EditFormData) -> EditFormData) {
        _screenState.update { state ->
            state.copy(formData = update(state.formData))
        }
    }

    private fun toEpochMillis(date: LocalDate, time: LocalTime): Long {
        return date.atTime(time)
            .atZone(editTimeZone)
            .toInstant()
            .toEpochMilli()
    }

    private fun inferCategory(index: Int, totalSize: Int): TravelLegCategory {
        return when {
            totalSize == 1 -> TravelLegCategory.OTHER
            index == 0 -> TravelLegCategory.OUTBOUND
            index == totalSize - 1 -> TravelLegCategory.RETURN
            else -> TravelLegCategory.INTERSITE
        }
    }
}

data class EditScreenState(
    val uiState: EditUiState = EditUiState.Loading,
    val formData: EditFormData = EditFormData(),
    val isSaving: Boolean = false,
    val dailyTargetHours: Double = 8.0,
    val originalFormData: EditFormData? = null
) {
    val isDirty: Boolean
        get() = originalFormData != null && formData != originalFormData
}

data class EditTravelLegForm(
    val startTime: LocalTime? = null,
    val arriveTime: LocalTime? = null,
    val startLabel: String? = null,
    val endLabel: String? = null,
    val paidMinutesOverride: Int? = null
) {
    fun isBlank(): Boolean {
        return startTime == null &&
            arriveTime == null &&
            startLabel.isNullOrBlank() &&
            endLabel.isNullOrBlank() &&
            paidMinutesOverride == null
    }
}

data class EditFormData(
    val dayType: DayType = DayType.WORK,
    val hasWorkTimes: Boolean = true,
    val workStart: LocalTime = LocalTime.of(8, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val breakMinutes: Int = 60,
    val dayLocationLabel: String? = null,
    val mealIsArrivalDeparture: Boolean = false,
    val mealBreakfastIncluded: Boolean = false,
    val note: String? = null,
    val travelStartTime: LocalTime? = null,
    val travelArriveTime: LocalTime? = null,
    val travelLegs: List<EditTravelLegForm> = emptyList()
) {
    fun normalizedTravelLegs(): List<EditTravelLegForm> {
        val explicitLegs = travelLegs.filterNot(EditTravelLegForm::isBlank)
        if (explicitLegs.isNotEmpty()) return explicitLegs

        val fallbackLeg = EditTravelLegForm(
            startTime = travelStartTime,
            arriveTime = travelArriveTime
        )
        return if (fallbackLeg.isBlank()) emptyList() else listOf(fallbackLeg)
    }

    fun mealAllowancePreviewCents(): Int {
        return resolveMealAllowanceForSave(
            dayType = dayType,
            isArrivalDeparture = mealIsArrivalDeparture,
            breakfastIncluded = mealBreakfastIncluded
        ).amountCents
    }

    fun validate(): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val relevantTravelLegs = normalizedTravelLegs()

        if (dayType == DayType.COMP_TIME) {
            if (relevantTravelLegs.isNotEmpty()) {
                errors += ValidationError.TravelNotAllowedForCompTime
            }
            return errors
        }

        if (dayType == DayType.WORK) {
            if (dayLocationLabel.isNullOrBlank()) {
                errors += ValidationError.MissingDayLocation
            }
            if (!hasWorkTimes && relevantTravelLegs.isEmpty()) {
                errors += ValidationError.MissingWorkOrTravel
            }

            if (hasWorkTimes) {
                val workTimeValid = workEnd != workStart
                if (!workTimeValid) {
                    errors += ValidationError.WorkEndBeforeStart
                }
                if (breakMinutes < 0) {
                    errors += ValidationError.NegativeBreakMinutes
                }
                if (workTimeValid) {
                    // Nachtschicht-Unterstützung: wenn Ende < Start, über Mitternacht
                    val startSec = workStart.toSecondOfDay()
                    val endSec = workEnd.toSecondOfDay()
                    val totalWorkMinutes = if (endSec < startSec) {
                        (24 * 3600 - startSec + endSec) / 60
                    } else {
                        (endSec - startSec) / 60
                    }
                    if (totalWorkMinutes > 18 * 60) {
                        errors += ValidationError.WorkEndBeforeStart
                    }
                    if (breakMinutes > totalWorkMinutes) {
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
                val startTime = leg.startTime
                val arriveTime = leg.arriveTime
                if (arriveTime == startTime) {
                    errors += ValidationError.TravelArriveBeforeStart(index)
                } else {
                    var diffMinutes =
                        (requireNotNull(arriveTime).toSecondOfDay() - requireNotNull(startTime).toSecondOfDay()) / 60
                    if (diffMinutes < 0) diffMinutes += 24 * 60
                    if (diffMinutes > 16 * 60) {
                        errors += ValidationError.TravelTooLong(index)
                    }
                }
            }
        }

        return errors.distinct()
    }

    fun isValid(): Boolean = validate().isEmpty()

    companion object {
        fun defaultFor(
            dayType: DayType,
            workStart: LocalTime,
            workEnd: LocalTime,
            breakMinutes: Int
        ): EditFormData {
            val hasWorkTimes = dayType == DayType.WORK
            return EditFormData(
                dayType = dayType,
                hasWorkTimes = hasWorkTimes,
                workStart = workStart,
                workEnd = workEnd,
                breakMinutes = if (hasWorkTimes) breakMinutes else 0
            )
        }

        fun fromEntry(
            entry: WorkEntry,
            travelLegs: List<TravelLeg> = emptyList(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): EditFormData {
            val hasWorkTimes = entry.workStart != null && entry.workEnd != null
            return EditFormData(
                dayType = entry.dayType,
                hasWorkTimes = hasWorkTimes && entry.dayType == DayType.WORK,
                workStart = entry.workStart ?: LocalTime.of(8, 0),
                workEnd = entry.workEnd ?: LocalTime.of(19, 0),
                breakMinutes = if (hasWorkTimes) entry.breakMinutes else 0,
                dayLocationLabel = entry.dayLocationLabel.takeIf(String::isNotBlank),
                mealIsArrivalDeparture = entry.mealIsArrivalDeparture,
                mealBreakfastIncluded = entry.mealBreakfastIncluded,
                note = entry.note,
                travelStartTime = travelLegs.firstOrNull()?.startAt?.let { toLocalTime(it, zoneId) },
                travelArriveTime = travelLegs.firstOrNull()?.arriveAt?.let { toLocalTime(it, zoneId) },
                travelLegs = travelLegs.sortedBy(TravelLeg::sortOrder).map { leg ->
                    EditTravelLegForm(
                        startTime = leg.startAt?.let { toLocalTime(it, zoneId) },
                        arriveTime = leg.arriveAt?.let { toLocalTime(it, zoneId) },
                        startLabel = leg.startLabel,
                        endLabel = leg.endLabel,
                        paidMinutesOverride = leg.paidMinutesOverride
                    )
                }
            )
        }

        private fun toLocalTime(timestamp: Long, zoneId: ZoneId): LocalTime {
            return Instant.ofEpochMilli(timestamp)
                .atZone(zoneId)
                .toLocalTime()
        }
    }
}

data class ResolvedMealAllowanceForSave(
    val isArrivalDeparture: Boolean,
    val breakfastIncluded: Boolean,
    val baseCents: Int,
    val amountCents: Int
)

fun resolveMealAllowanceForSave(
    dayType: DayType,
    isArrivalDeparture: Boolean,
    breakfastIncluded: Boolean
): ResolvedMealAllowanceForSave {
    if (dayType != DayType.WORK) {
        return ResolvedMealAllowanceForSave(
            isArrivalDeparture = false,
            breakfastIncluded = false,
            baseCents = 0,
            amountCents = 0
        )
    }

    val result = MealAllowanceCalculator.calculate(
        dayType = dayType,
        isArrivalDeparture = isArrivalDeparture,
        breakfastIncluded = breakfastIncluded
    )
    return ResolvedMealAllowanceForSave(
        isArrivalDeparture = isArrivalDeparture,
        breakfastIncluded = breakfastIncluded,
        baseCents = result.baseCents,
        amountCents = result.amountCents
    )
}

sealed class ValidationError(@StringRes val messageRes: Int) {
    object MissingWorkOrTravel : ValidationError(R.string.edit_validation_missing_work_or_travel)
    object MissingDayLocation : ValidationError(R.string.edit_validation_missing_day_location)
    object WorkEndBeforeStart : ValidationError(R.string.edit_validation_work_end_before_start)
    object NegativeBreakMinutes : ValidationError(R.string.edit_validation_negative_break)
    object BreakLongerThanWorkTime : ValidationError(R.string.edit_validation_break_longer_than_work)
    data class TravelArriveBeforeStart(val legIndex: Int) : ValidationError(R.string.edit_validation_travel_arrive_before_start)
    data class TravelTooLong(val legIndex: Int) : ValidationError(R.string.edit_validation_travel_too_long)
    data class TravelLegIncomplete(val legIndex: Int) : ValidationError(R.string.edit_validation_travel_incomplete)
    data class TravelLegMissingTimeWindow(val legIndex: Int) : ValidationError(R.string.edit_validation_travel_missing_time_window)
    object TravelNotAllowedForCompTime : ValidationError(R.string.edit_validation_travel_not_allowed_comp_time)
}

sealed class EditUiState {
    object Loading : EditUiState()
    data class Success(
        val entry: WorkEntry,
        val validationErrors: List<ValidationError> = emptyList()
    ) : EditUiState()
    data class NewEntry(
        val date: LocalDate,
        val validationErrors: List<ValidationError> = emptyList()
    ) : EditUiState()
    object NotFound : EditUiState()
    object Saved : EditUiState()
    data class Error(val message: UiText) : EditUiState()
}

private fun toUiText(message: String?, @StringRes fallbackRes: Int): UiText {
    return message
        ?.takeIf { it.isNotBlank() }
        ?.let(UiText::DynamicString)
        ?: UiText.StringResource(fallbackRes)
}
