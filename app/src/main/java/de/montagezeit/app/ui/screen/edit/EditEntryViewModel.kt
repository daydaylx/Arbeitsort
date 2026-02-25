package de.montagezeit.app.ui.screen.edit

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.DayLocationSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.usecase.UpdateEntry
import de.montagezeit.app.ui.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val updateEntry: UpdateEntry,
    private val reminderSettingsManager: ReminderSettingsManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private var currentDate: LocalDate =
        savedStateHandle.get<String>("date")?.let { LocalDate.parse(it) } ?: LocalDate.now()
    
    private val _screenState = MutableStateFlow(EditScreenState())
    val screenState: StateFlow<EditScreenState> = _screenState.asStateFlow()
    
    init {
        loadEntry(currentDate)
    }
    
    fun setDate(date: LocalDate) {
        if (date == currentDate) {
            return
        }
        currentDate = date
        savedStateHandle["date"] = date.toString()
        loadEntry(date)
    }
    
    /**
     * Setzt FormData direkt (z.B. beim Kopieren eines Eintrags)
     */
    fun setFormData(data: EditFormData) {
        _screenState.update { it.copy(formData = data) }
        // Prüfe ob Eintrag existiert, wenn nicht, setze NewEntry State
        viewModelScope.launch {
            val entry = workEntryDao.getByDate(currentDate)
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
        viewModelScope.launch {
            _screenState.update { it.copy(uiState = EditUiState.Loading, isSaving = false) }

            try {
                val settings = reminderSettingsManager.settings.first()
                val targetMinutes = (settings.workEnd.toSecondOfDay() - settings.workStart.toSecondOfDay()) / 60 - settings.breakMinutes
                val dailyTargetHours = maxOf(0, targetMinutes) / 60.0

                val entry = workEntryDao.getByDate(date)
                if (entry != null) {
                    val formData = EditFormData.fromEntry(entry)
                    _screenState.update {
                        it.copy(
                            formData = formData,
                            uiState = EditUiState.Success(entry),
                            isSaving = false,
                            dailyTargetHours = dailyTargetHours
                        )
                    }
                } else {
                    val defaultDayType = defaultDayTypeForDate(date, settings)
                    // Neuen Eintrag mit Defaults erstellen
                    val defaultEntry = WorkEntry(
                        date = date,
                        dayType = defaultDayType,
                        workStart = settings.workStart,
                        workEnd = settings.workEnd,
                        breakMinutes = settings.breakMinutes,
                        dayLocationLabel = "",
                        dayLocationSource = DayLocationSource.FALLBACK
                    )
                    val formData = EditFormData.fromEntry(defaultEntry)
                    _screenState.update {
                        it.copy(
                            formData = formData,
                            uiState = EditUiState.NewEntry(date),
                            isSaving = false,
                            dailyTargetHours = dailyTargetHours
                        )
                    }
                }
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
        if (dayType == DayType.COMP_TIME) {
            // Clear snapshot labels to avoid mixing work-day data with comp-time.
            updateFormData {
                it.copy(
                    dayType = dayType,
                    morningLocationLabel = null,
                    eveningLocationLabel = null
                )
            }
        } else {
            updateFormData { it.copy(dayType = dayType) }
        }
    }
    
    fun updateWorkStart(hour: Int, minute: Int) {
        updateFormData { it.copy(workStart = LocalTime.of(hour, minute)) }
    }
    
    fun updateWorkEnd(hour: Int, minute: Int) {
        updateFormData { it.copy(workEnd = LocalTime.of(hour, minute)) }
    }
    
    fun updateBreakMinutes(minutes: Int) {
        updateFormData { it.copy(breakMinutes = minutes) }
    }
    
    fun updateMorningLocationLabel(label: String) {
        updateFormData { it.copy(morningLocationLabel = label.takeIf { it.isNotBlank() }) }
    }
    
    fun updateEveningLocationLabel(label: String) {
        updateFormData { it.copy(eveningLocationLabel = label.takeIf { it.isNotBlank() }) }
    }

    fun updateDayLocationLabel(label: String) {
        updateFormData {
            val trimmed = label.trim()
            if (trimmed.isBlank()) {
                it.copy(dayLocationLabel = null)
            } else {
                it.copy(
                    dayLocationLabel = trimmed,
                    dayLocationSource = DayLocationSource.MANUAL,
                    dayLocationLat = null,
                    dayLocationLon = null,
                    dayLocationAccuracyMeters = null
                )
            }
        }
    }
    
    fun updateNote(note: String) {
        updateFormData { it.copy(note = note.takeIf { it.isNotBlank() }) }
    }

    fun updateTravelStart(time: LocalTime?) {
        updateFormData { it.copy(travelStartTime = time) }
    }

    fun updateTravelArrive(time: LocalTime?) {
        updateFormData { it.copy(travelArriveTime = time) }
    }

    fun updateTravelLabelStart(label: String) {
        updateFormData { it.copy(travelLabelStart = label.takeIf { it.isNotBlank() }) }
    }

    fun updateTravelLabelEnd(label: String) {
        updateFormData { it.copy(travelLabelEnd = label.takeIf { it.isNotBlank() }) }
    }

    fun clearTravel() {
        updateFormData {
            it.copy(
                travelStartTime = null,
                travelArriveTime = null,
                travelLabelStart = null,
                travelLabelEnd = null
            )
        }
    }
    
    fun resetNeedsReview() {
        updateFormData { it.copy(needsReview = false) }
    }

    fun applyDefaultWorkTimes() {
        viewModelScope.launch {
            val settings = reminderSettingsManager.settings.first()
            updateFormData {
                it.copy(
                    workStart = settings.workStart,
                    workEnd = settings.workEnd,
                    breakMinutes = settings.breakMinutes
                )
            }
        }
    }

    fun copyFromPreviousDay(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val previousDate = currentDate.minusDays(1)
            val entry = workEntryDao.getByDate(previousDate)
            withContext(Dispatchers.Main) {
                if (entry != null) {
                    val copied = EditFormData.fromEntry(entry).copy(
                        morningLocationLabel = null,
                        eveningLocationLabel = null,
                        needsReview = false
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
            if (_screenState.value.isSaving) {
                return@launch
            }

            try {
                _screenState.update { it.copy(isSaving = true) }
                val existingEntry = workEntryDao.getByDate(currentDate)
                if (existingEntry == null) {
                    _screenState.update { it.copy(isSaving = false, uiState = EditUiState.NewEntry(currentDate)) }
                    onResult(false)
                    return@launch
                }

                workEntryDao.deleteByDate(currentDate)
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
    
    fun save(confirmBorderzone: Boolean = false) {
        viewModelScope.launch {
            val stateSnapshot = _screenState.value
            if (stateSnapshot.isSaving) {
                return@launch
            }
            val data = stateSnapshot.formData
            val currentState = stateSnapshot.uiState

            // Validate form data before saving
            val validationErrors = data.validate()
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
                        return@launch
                    }
                }
                return@launch
            }

            val entryToSave = when (currentState) {
                is EditUiState.Success -> {
                    val originalEntry = currentState.entry
                    val now = System.currentTimeMillis()

                    // Review-confirmation only for unresolved entries.
                    val requiresConfirm = originalEntry.needsReview
                    if (requiresConfirm && !confirmBorderzone) {
                        _screenState.update { it.copy(uiState = currentState.copy(showConfirmDialog = true)) }
                        return@launch
                    }

                    val isCompTime = data.dayType == DayType.COMP_TIME
                    originalEntry.copy(
                        dayType = data.dayType,
                        workStart = data.workStart,
                        workEnd = data.workEnd,
                        breakMinutes = data.breakMinutes,
                        dayLocationLabel = data.dayLocationLabel ?: "",
                        dayLocationSource = data.dayLocationSource,
                        dayLocationLat = data.dayLocationLat,
                        dayLocationLon = data.dayLocationLon,
                        dayLocationAccuracyMeters = data.dayLocationAccuracyMeters,
                        morningLocationLabel = data.morningLocationLabel,
                        eveningLocationLabel = data.eveningLocationLabel,
                        note = data.note,
                        travelStartAt = data.travelStartTime?.let { toEpochMillis(originalEntry.date, it) },
                        travelArriveAt = data.travelArriveTime?.let { toEpochMillis(originalEntry.date, it) },
                        travelLabelStart = data.travelLabelStart,
                        travelLabelEnd = data.travelLabelEnd,
                        needsReview = data.needsReview,
                        confirmedWorkDay = if (isCompTime) true else originalEntry.confirmedWorkDay,
                        confirmationAt = if (isCompTime && originalEntry.confirmationAt == null) now else originalEntry.confirmationAt,
                        confirmationSource = if (isCompTime && originalEntry.confirmationSource == null) "COMP_TIME" else originalEntry.confirmationSource,
                        updatedAt = now
                    )
                }
                is EditUiState.NewEntry -> {
                    // Neuen Eintrag erstellen
                    val now = System.currentTimeMillis()
                    val isCompTime = data.dayType == DayType.COMP_TIME
                    WorkEntry(
                        date = currentState.date,
                        dayType = data.dayType,
                        workStart = data.workStart,
                        workEnd = data.workEnd,
                        breakMinutes = data.breakMinutes,
                        dayLocationLabel = data.dayLocationLabel ?: "",
                        dayLocationSource = data.dayLocationSource,
                        dayLocationLat = data.dayLocationLat,
                        dayLocationLon = data.dayLocationLon,
                        dayLocationAccuracyMeters = data.dayLocationAccuracyMeters,
                        morningLocationLabel = data.morningLocationLabel,
                        eveningLocationLabel = data.eveningLocationLabel,
                        note = data.note,
                        travelStartAt = data.travelStartTime?.let { toEpochMillis(currentState.date, it) },
                        travelArriveAt = data.travelArriveTime?.let { toEpochMillis(currentState.date, it) },
                        travelLabelStart = data.travelLabelStart,
                        travelLabelEnd = data.travelLabelEnd,
                        needsReview = data.needsReview,
                        confirmedWorkDay = isCompTime,
                        confirmationAt = if (isCompTime) now else null,
                        confirmationSource = if (isCompTime) "COMP_TIME" else null,
                        createdAt = now,
                        updatedAt = now
                    )
                }
                else -> {
                    _screenState.update {
                        it.copy(uiState = EditUiState.Error(UiText.StringResource(R.string.error_cannot_save)))
                    }
                    return@launch
                }
            }

            try {
                _screenState.update { it.copy(isSaving = true) }
                workEntryDao.upsert(entryToSave)
                _screenState.update { it.copy(isSaving = false, uiState = EditUiState.Saved) }
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
        val currentState = _screenState.value.uiState
        when (currentState) {
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
            else -> {}
        }
    }
    
    fun confirmAndSave() {
        save(confirmBorderzone = true)
    }

    fun dismissConfirmDialog() {
        val currentState = _screenState.value.uiState
        if (currentState is EditUiState.Success) {
            _screenState.update { it.copy(uiState = currentState.copy(showConfirmDialog = false)) }
        }
    }
    
    /**
     * Kopiert die aktuellen FormData für einen neuen Eintrag
     * Gibt die kopierten FormData zurück, damit sie für ein neues Datum verwendet werden können
     */
    fun copyEntryData(): EditFormData {
        return _screenState.value.formData.copy()
    }

    private fun toEpochMillis(date: LocalDate, time: LocalTime): Long {
        return date.atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun defaultDayTypeForDate(
        date: LocalDate,
        settings: ReminderSettings
    ): DayType {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val isHoliday = settings.holidayDates.contains(date)
        return if ((settings.autoOffWeekends && isWeekend) || (settings.autoOffHolidays && isHoliday)) {
            DayType.OFF
        } else {
            DayType.WORK
        }
    }

    private inline fun updateFormData(update: (EditFormData) -> EditFormData) {
        _screenState.update { state ->
            state.copy(formData = update(state.formData))
        }
    }
}

data class EditScreenState(
    val uiState: EditUiState = EditUiState.Loading,
    val formData: EditFormData = EditFormData(),
    val isSaving: Boolean = false,
    val dailyTargetHours: Double = 8.0
)

data class EditFormData(
    val dayType: DayType = DayType.WORK,
    val workStart: LocalTime = LocalTime.of(8, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val breakMinutes: Int = 60,
    val dayLocationLabel: String? = null,
    val dayLocationSource: DayLocationSource = DayLocationSource.FALLBACK,
    val dayLocationLat: Double? = null,
    val dayLocationLon: Double? = null,
    val dayLocationAccuracyMeters: Float? = null,
    val morningLocationLabel: String? = null,
    val eveningLocationLabel: String? = null,
    val note: String? = null,
    val needsReview: Boolean = false,
    val travelStartTime: LocalTime? = null,
    val travelArriveTime: LocalTime? = null,
    val travelLabelStart: String? = null,
    val travelLabelEnd: String? = null
) {
    /**
     * Validates the form data and returns a list of validation errors.
     * Returns empty list if validation passes.
     */
    fun validate(): List<ValidationError> {
        // COMP_TIME days have no work times or location requirement.
        if (dayType == DayType.COMP_TIME) return emptyList()

        val errors = mutableListOf<ValidationError>()

        if (dayLocationLabel.isNullOrBlank()) {
            errors.add(ValidationError.MissingDayLocation)
        }

        // 1. Check if workEnd is after workStart
        val workTimeValid = workEnd > workStart
        if (!workTimeValid) {
            errors.add(ValidationError.WorkEndBeforeStart)
        }

        // 2. Check if breakMinutes is non-negative
        if (breakMinutes < 0) {
            errors.add(ValidationError.NegativeBreakMinutes)
        }

        // 3. Check if breakMinutes is not longer than total work time
        // Only check this if work time itself is valid
        if (workTimeValid) {
            val totalWorkMinutes = (workEnd.toSecondOfDay() - workStart.toSecondOfDay()) / 60
            if (breakMinutes > totalWorkMinutes) {
                errors.add(ValidationError.BreakLongerThanWorkTime)
            }
        }

        // 4. Check travel times if both are set
        if (travelStartTime != null && travelArriveTime != null) {
            // Overnight trips are allowed. Only identical times are invalid.
            if (travelArriveTime == travelStartTime) {
                errors.add(ValidationError.TravelArriveBeforeStart)
            }
        }

        return errors
    }

    /**
     * Returns true if the form data is valid (no validation errors).
     */
    fun isValid(): Boolean = validate().isEmpty()

    companion object {
        fun fromEntry(entry: WorkEntry): EditFormData {
            return EditFormData(
                dayType = entry.dayType,
                workStart = entry.workStart,
                workEnd = entry.workEnd,
                breakMinutes = entry.breakMinutes,
                dayLocationLabel = entry.dayLocationLabel,
                dayLocationSource = entry.dayLocationSource,
                dayLocationLat = entry.dayLocationLat,
                dayLocationLon = entry.dayLocationLon,
                dayLocationAccuracyMeters = entry.dayLocationAccuracyMeters,
                morningLocationLabel = entry.morningLocationLabel,
                eveningLocationLabel = entry.eveningLocationLabel,
                note = entry.note,
                needsReview = entry.needsReview,
                travelStartTime = entry.travelStartAt?.let { toLocalTime(it) },
                travelArriveTime = entry.travelArriveAt?.let { toLocalTime(it) },
                travelLabelStart = entry.travelLabelStart,
                travelLabelEnd = entry.travelLabelEnd
            )
        }

        private fun toLocalTime(timestamp: Long): LocalTime {
            return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
    }
}

/**
 * Validation errors for EditFormData
 */
sealed class ValidationError(@StringRes val messageRes: Int) {
    object MissingDayLocation : ValidationError(R.string.edit_validation_missing_day_location)
    object WorkEndBeforeStart : ValidationError(R.string.edit_validation_work_end_before_start)
    object NegativeBreakMinutes : ValidationError(R.string.edit_validation_negative_break)
    object BreakLongerThanWorkTime : ValidationError(R.string.edit_validation_break_longer_than_work)
    object TravelArriveBeforeStart : ValidationError(R.string.edit_validation_travel_arrive_before_start)
}

sealed class EditUiState {
    object Loading : EditUiState()
    data class Success(
        val entry: WorkEntry,
        val showConfirmDialog: Boolean = false,
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
