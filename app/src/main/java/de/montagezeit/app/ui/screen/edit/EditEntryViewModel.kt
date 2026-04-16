package de.montagezeit.app.ui.screen.edit

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.diagnostics.redactedTextSummary
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.domain.usecase.WorkEntryFactory
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.resolveWorkScheduleDefaults
import de.montagezeit.app.ui.util.UiText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    private val workEntryRepository: WorkEntryRepository,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val draftRules: EditEntryDraftRules,
    private val saveBuilder: EditEntrySaveBuilder,
    private val editEntryDiagnostics: EditEntryDiagnostics,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editTimeZone: ZoneId = ZoneId.systemDefault()
    private var currentDate: LocalDate = try {
        savedStateHandle.get<String>("date")?.let(LocalDate::parse) ?: LocalDate.now()
    } catch (e: Exception) {
        LocalDate.now()
    }

    private val _screenState = MutableStateFlow(EditScreenState())
    val screenState: StateFlow<EditScreenState> = _screenState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<UiText> = _snackbarMessage.asSharedFlow()

    private var loadEntryJob: Job? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val validationErrors: StateFlow<List<ValidationError>> = _screenState
        .map { state -> ValidationInput(state.formData, state.uiState) }
        .distinctUntilChanged()
        .debounce(VALIDATION_DEBOUNCE_MS)
        .mapLatest { input ->
            if (input.uiState is EditUiState.NewEntry || input.uiState is EditUiState.Success) {
                draftRules.validate(input.formData)
            } else {
                emptyList()
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(VALIDATION_SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList()
        )

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
        _screenState.update { state -> state.withFormData(data, draftRules) }
    }

    private fun loadEntry(date: LocalDate) {
        loadEntryJob?.cancel()
        loadEntryJob = viewModelScope.launch {
            _screenState.update { it.copy(uiState = EditUiState.Loading, isSaving = false, originalFormData = null) }

            try {
                val settings = reminderSettingsManager.settings.first()
                val dailyTargetHours = settings.dailyTargetHours

                val record = workEntryRepository.getByDateWithTravel(date)
                if (record != null) {
                    showExistingEntry(record, dailyTargetHours)
                } else {
                    showNewEntry(date, settings, dailyTargetHours)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showErrorState(e, R.string.edit_error_unknown)
            }
        }
    }

    fun updateDayType(dayType: DayType) {
        val previousType = _screenState.value.formData.dayType
        updateFormData {
            when (dayType) {
                DayType.WORK -> it.copy(dayType = dayType)
                DayType.OFF -> it.copy(dayType = dayType, hasWorkTimes = false)
                DayType.COMP_TIME -> it.copy(dayType = dayType, hasWorkTimes = false, travelLegs = emptyList())
            }
        }
        if (previousType == DayType.WORK && dayType != DayType.WORK) {
            viewModelScope.launch {
                _snackbarMessage.emit(UiText.StringResource(R.string.edit_day_type_cleared_times))
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
            val workDefaults = resolveWorkScheduleDefaults(settings)
            updateFormData {
                it.copy(
                    hasWorkTimes = true,
                    workStart = workDefaults.workStart,
                    workEnd = workDefaults.workEnd,
                    breakMinutes = workDefaults.breakMinutes
                )
            }
        }
    }

    fun copyFromPreviousDay(onResult: (Boolean) -> Unit) {
        if (_screenState.value.isSaving) { onResult(false); return }
        viewModelScope.launch(Dispatchers.IO) {
            val previousDate = currentDate.minusDays(1)
            val record = workEntryRepository.getByDateWithTravel(previousDate)
            withContext(Dispatchers.Main) {
                if (record != null) {
                    val copied = EditFormData.fromEntry(
                        entry = record.workEntry,
                        travelLegs = record.orderedTravelLegs
                    )
                    _screenState.update { state -> state.withFormData(copied, draftRules) }
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
                val existingEntry = workEntryRepository.getByDate(currentDate)
                if (existingEntry == null) {
                    _screenState.update { it.copy(isSaving = false, uiState = EditUiState.NewEntry(currentDate)) }
                    onResult(false)
                    return@launch
                }

                workEntryRepository.deleteByDate(currentDate)
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
            val currentState = stateSnapshot.uiState
            val trace = editEntryDiagnostics.startSaveTrace(
                currentState = currentState,
                fallbackDate = currentDate,
                data = data,
                isDirty = stateSnapshot.isDirty
            )

            val validationErrors = draftRules.validate(data)
            if (validationErrors.isNotEmpty()) {
                trace.validationFailed(validationErrors)
                showValidationErrors(currentState, validationErrors)
                return@launch
            }

            try {
                val latestState = resolveLatestSaveState(currentState) ?: return@launch
                val pendingSave = saveBuilder.build(
                    currentState = latestState,
                    data = data,
                    zoneId = editTimeZone
                ) ?: return@launch
                trace.pendingSaveBuilt(pendingSave)
                _screenState.update { it.copy(isSaving = true) }
                workEntryRepository.replaceEntryWithTravelLegs(pendingSave.entry, pendingSave.legs)
                trace.saveSucceeded(pendingSave)
                _screenState.update {
                    it.copy(
                        isSaving = false,
                        uiState = EditUiState.Saved,
                        originalFormData = data,
                        mealAllowancePreviewCents = draftRules.mealAllowancePreviewCents(data)
                    )
                }
            } catch (e: Exception) {
                trace.saveFailed(e)
                showErrorState(e, R.string.edit_error_save_failed)
            }
        }
    }

    fun saveAndNavigate(newDate: LocalDate, onNavigate: (LocalDate) -> Unit) {
        viewModelScope.launch {
            val stateSnapshot = _screenState.value
            if (stateSnapshot.isSaving) return@launch
            if (!stateSnapshot.isDirty) {
                onNavigate(newDate)
                return@launch
            }
            val data = stateSnapshot.formData
            val currentState = stateSnapshot.uiState
            val validationErrors = draftRules.validate(data)
            if (validationErrors.isNotEmpty()) {
                showValidationErrors(currentState, validationErrors)
                return@launch
            }
            try {
                val latestState = resolveLatestSaveState(currentState) ?: run {
                    onNavigate(newDate); return@launch
                }
                val pendingSave = saveBuilder.build(
                    currentState = latestState,
                    data = data,
                    zoneId = editTimeZone
                ) ?: run {
                    onNavigate(newDate); return@launch
                }
                _screenState.update { it.copy(isSaving = true) }
                workEntryRepository.replaceEntryWithTravelLegs(pendingSave.entry, pendingSave.legs)
                _screenState.update {
                    it.copy(
                        isSaving = false,
                        originalFormData = data,
                        mealAllowancePreviewCents = draftRules.mealAllowancePreviewCents(data)
                    )
                }
                onNavigate(newDate)
            } catch (e: Exception) {
                showErrorState(e, R.string.edit_error_save_failed)
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

    private suspend fun resolveLatestSaveState(currentState: EditUiState): EditUiState? {
        val validationErrors = when (currentState) {
            is EditUiState.Success -> currentState.validationErrors
            is EditUiState.NewEntry -> currentState.validationErrors
            else -> return null
        }
        val date = when (currentState) {
            is EditUiState.Success -> currentState.entry.date
            is EditUiState.NewEntry -> currentState.date
            else -> return null
        }
        val latestRecord = workEntryRepository.getByDateWithTravel(date)
        return if (latestRecord != null) {
            EditUiState.Success(latestRecord.workEntry, validationErrors)
        } else {
            EditUiState.NewEntry(date, validationErrors)
        }
    }

    private fun showExistingEntry(
        record: de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs,
        dailyTargetHours: Double
    ) {
        val formData = EditFormData.fromEntry(
            entry = record.workEntry,
            travelLegs = record.orderedTravelLegs,
            zoneId = editTimeZone
        )
        _screenState.update {
            it.withFormData(formData, draftRules).copy(
                originalFormData = formData,
                uiState = EditUiState.Success(record.workEntry),
                isSaving = false,
                dailyTargetHours = dailyTargetHours
            )
        }
    }

    private fun showNewEntry(
        date: LocalDate,
        settings: de.montagezeit.app.data.preferences.ReminderSettings,
        dailyTargetHours: Double
    ) {
        val workDefaults = resolveWorkScheduleDefaults(settings)
        val defaultDayType = WorkEntryFactory.resolveAutoDayType(date, settings)
        val defaultEntry = WorkEntryFactory.createDefaultEntry(
            date = date,
            settings = settings,
            dayType = defaultDayType
        )
        val formData = EditFormData.defaultFor(
            dayType = defaultDayType,
            workStart = workDefaults.workStart,
            workEnd = workDefaults.workEnd,
            breakMinutes = workDefaults.breakMinutes
        )
        val finalFormData = formData.copy(
            dayLocationLabel = defaultEntry.dayLocationLabel.ifBlank { null }
        )
        _screenState.update {
            it.withFormData(finalFormData, draftRules).copy(
                originalFormData = finalFormData,
                uiState = EditUiState.NewEntry(date),
                isSaving = false,
                dailyTargetHours = dailyTargetHours
            )
        }
    }

    private fun showValidationErrors(
        currentState: EditUiState,
        validationErrors: List<ValidationError>
    ) {
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
    }

    private fun showErrorState(error: Throwable, @StringRes fallbackRes: Int) {
        _screenState.update {
            it.copy(
                isSaving = false,
                uiState = EditUiState.Error(toUiText(error.message, fallbackRes))
            )
        }
    }

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
            state.withFormData(update(state.formData), draftRules)
        }
    }

    private data class ValidationInput(
        val formData: EditFormData,
        val uiState: EditUiState
    )

    companion object {
        private const val VALIDATION_DEBOUNCE_MS = 80L
        private const val VALIDATION_SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

data class EditScreenState(
    val uiState: EditUiState = EditUiState.Loading,
    val formData: EditFormData = EditFormData(),
    val mealAllowancePreviewCents: Int = 0,
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
    breakfastIncluded: Boolean,
    workMinutes: Int = 0,
    travelMinutes: Int = 0
): ResolvedMealAllowanceForSave {
    val snapshot = MealAllowanceCalculator.resolveForActivity(
        dayType = dayType,
        isArrivalDeparture = isArrivalDeparture,
        breakfastIncluded = breakfastIncluded,
        workMinutes = workMinutes,
        travelMinutes = travelMinutes
    )
    return ResolvedMealAllowanceForSave(
        isArrivalDeparture = snapshot.isArrivalDeparture,
        breakfastIncluded = snapshot.breakfastIncluded,
        baseCents = snapshot.baseCents,
        amountCents = snapshot.amountCents
    )
}

sealed class ValidationError(@StringRes val messageRes: Int) {
    object MissingWorkOrTravel : ValidationError(R.string.edit_validation_missing_work_or_travel)
    object MissingDayLocation : ValidationError(R.string.edit_validation_missing_day_location)
    object WorkEndBeforeStart : ValidationError(R.string.edit_validation_work_end_before_start)
    object NegativeBreakMinutes : ValidationError(R.string.edit_validation_negative_break)
    object BreakLongerThanWorkTime : ValidationError(R.string.edit_validation_break_longer_than_work)
    object WorkDayTooLong : ValidationError(R.string.edit_validation_work_day_too_long)
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

private fun EditScreenState.withFormData(
    formData: EditFormData,
    draftRules: EditEntryDraftRules
): EditScreenState {
    val mealAllowance = if (affectsMealAllowance(this.formData, formData)) {
        draftRules.mealAllowancePreviewCents(formData)
    } else {
        mealAllowancePreviewCents
    }
    return copy(
        formData = formData,
        mealAllowancePreviewCents = mealAllowance
    )
}

private fun affectsMealAllowance(old: EditFormData, new: EditFormData): Boolean {
    return old.dayType != new.dayType ||
        old.hasWorkTimes != new.hasWorkTimes ||
        old.workStart != new.workStart ||
        old.workEnd != new.workEnd ||
        old.breakMinutes != new.breakMinutes ||
        old.mealIsArrivalDeparture != new.mealIsArrivalDeparture ||
        old.mealBreakfastIncluded != new.mealBreakfastIncluded ||
        !travelLegsEquivalentForMeal(old.travelLegs, new.travelLegs) ||
        old.travelStartTime != new.travelStartTime ||
        old.travelArriveTime != new.travelArriveTime
}

private fun travelLegsEquivalentForMeal(
    old: List<EditTravelLegForm>,
    new: List<EditTravelLegForm>
): Boolean {
    if (old.size != new.size) return false
    for (index in old.indices) {
        val a = old[index]
        val b = new[index]
        if (a.startTime != b.startTime ||
            a.arriveTime != b.arriveTime ||
            a.paidMinutesOverride != b.paidMinutesOverride
        ) {
            return false
        }
    }
    return true
}

internal fun EditFormData.toSanitizedDiagnosticPayload(
    draftRules: EditEntryDraftRules = EditEntryDraftRules.Default
): Map<String, Any?> {
    return mapOf(
        "dayType" to dayType.name,
        "hasWorkTimes" to hasWorkTimes,
        "workStart" to workStart.toString(),
        "workEnd" to workEnd.toString(),
        "breakMinutes" to breakMinutes,
        "dayLocation" to redactedTextSummary(dayLocationLabel),
        "mealIsArrivalDeparture" to mealIsArrivalDeparture,
        "mealBreakfastIncluded" to mealBreakfastIncluded,
        "note" to redactedTextSummary(note),
        "travelLegCount" to draftRules.normalizedTravelLegs(this).size,
        "travelLegs" to draftRules.normalizedTravelLegs(this).mapIndexed { index, leg ->
            mapOf(
                "index" to index,
                "startTime" to leg.startTime?.toString(),
                "arriveTime" to leg.arriveTime?.toString(),
                "startLabel" to redactedTextSummary(leg.startLabel),
                "endLabel" to redactedTextSummary(leg.endLabel),
                "paidMinutesOverride" to leg.paidMinutesOverride
            )
        }
    )
}

internal fun ValidationError.diagnosticCode(): String {
    return when (this) {
        ValidationError.MissingWorkOrTravel -> "VALIDATION_MISSING_WORK_OR_TRAVEL"
        ValidationError.MissingDayLocation -> "VALIDATION_MISSING_DAY_LOCATION"
        ValidationError.WorkEndBeforeStart -> "VALIDATION_WORK_END_BEFORE_START"
        ValidationError.NegativeBreakMinutes -> "VALIDATION_NEGATIVE_BREAK"
        ValidationError.BreakLongerThanWorkTime -> "VALIDATION_BREAK_LONGER_THAN_WORK"
        ValidationError.WorkDayTooLong -> "VALIDATION_WORK_DAY_TOO_LONG"
        is ValidationError.TravelArriveBeforeStart -> "VALIDATION_TRAVEL_ARRIVE_BEFORE_START_${legIndex + 1}"
        is ValidationError.TravelTooLong -> "VALIDATION_TRAVEL_TOO_LONG_${legIndex + 1}"
        is ValidationError.TravelLegIncomplete -> "VALIDATION_TRAVEL_LEG_INCOMPLETE_${legIndex + 1}"
        is ValidationError.TravelLegMissingTimeWindow -> "VALIDATION_TRAVEL_LEG_MISSING_TIME_WINDOW_${legIndex + 1}"
        ValidationError.TravelNotAllowedForCompTime -> "VALIDATION_TRAVEL_NOT_ALLOWED_COMP_TIME"
    }
}
