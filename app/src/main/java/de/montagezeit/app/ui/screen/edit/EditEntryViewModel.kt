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
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.transitionToDayType
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.usecase.UpdateEntry
import de.montagezeit.app.domain.usecase.WorkEntryFactory
import de.montagezeit.app.ui.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    
    // B12: Cache timezone at ViewModel creation to ensure consistent conversion
    private val editTimeZone: ZoneId = ZoneId.systemDefault()

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
                    val formData = EditFormData.fromEntry(entry, editTimeZone)
                    _screenState.update {
                        it.copy(
                            formData = formData,
                            uiState = EditUiState.Success(entry),
                            isSaving = false,
                            dailyTargetHours = dailyTargetHours
                        )
                    }
                } else {
                    val defaultEntry = WorkEntryFactory.createDefaultEntry(
                        date = date,
                        settings = settings,
                        dayType = WorkEntryFactory.resolveAutoDayType(date, settings)
                    )
                    val formData = EditFormData.fromEntry(defaultEntry, editTimeZone)
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

    fun updateMealArrivalDeparture(isArrivalDeparture: Boolean) {
        updateFormData { it.copy(mealIsArrivalDeparture = isArrivalDeparture) }
    }

    fun updateMealBreakfastIncluded(breakfastIncluded: Boolean) {
        updateFormData { it.copy(mealBreakfastIncluded = breakfastIncluded) }
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

            val mealAllowance = resolveMealAllowanceForSave(
                dayType = data.dayType,
                isArrivalDeparture = data.mealIsArrivalDeparture,
                breakfastIncluded = data.mealBreakfastIncluded
            )

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

                    val baseEntry = originalEntry.transitionToDayType(dayType = data.dayType, now = now)
                    val travelFields = resolveTravelFields(
                        date = originalEntry.date,
                        originalEntry = originalEntry,
                        data = data,
                        now = now
                    )

                    baseEntry.copy(
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
                        travelStartAt = travelFields.startAt,
                        travelArriveAt = travelFields.arriveAt,
                        travelLabelStart = travelFields.labelStart,
                        travelLabelEnd = travelFields.labelEnd,
                        travelFromLabel = travelFields.fromLabel,
                        travelToLabel = travelFields.toLabel,
                        travelDistanceKm = travelFields.distanceKm,
                        travelSource = travelFields.source,
                        travelUpdatedAt = travelFields.updatedAt,
                        mealIsArrivalDeparture = mealAllowance.isArrivalDeparture,
                        mealBreakfastIncluded = mealAllowance.breakfastIncluded,
                        mealAllowanceBaseCents = mealAllowance.baseCents,
                        mealAllowanceAmountCents = mealAllowance.amountCents,
                        needsReview = data.needsReview,
                        updatedAt = now
                    )
                }
                is EditUiState.NewEntry -> {
                    // Neuen Eintrag erstellen
                    val now = System.currentTimeMillis()
                    val travelFields = resolveTravelFields(
                        date = currentState.date,
                        originalEntry = null,
                        data = data,
                        now = now
                    )
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
                        travelStartAt = travelFields.startAt,
                        travelArriveAt = travelFields.arriveAt,
                        travelLabelStart = travelFields.labelStart,
                        travelLabelEnd = travelFields.labelEnd,
                        travelFromLabel = travelFields.fromLabel,
                        travelToLabel = travelFields.toLabel,
                        travelDistanceKm = travelFields.distanceKm,
                        travelSource = travelFields.source,
                        travelUpdatedAt = travelFields.updatedAt,
                        mealIsArrivalDeparture = mealAllowance.isArrivalDeparture,
                        mealBreakfastIncluded = mealAllowance.breakfastIncluded,
                        mealAllowanceBaseCents = mealAllowance.baseCents,
                        mealAllowanceAmountCents = mealAllowance.amountCents,
                        needsReview = data.needsReview,
                        confirmedWorkDay = data.dayType == DayType.COMP_TIME,
                        confirmationAt = if (data.dayType == DayType.COMP_TIME) now else null,
                        confirmationSource = if (data.dayType == DayType.COMP_TIME) DayType.COMP_TIME.name else null,
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
                updateEntry(entryToSave)
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
            .atZone(editTimeZone)
            .toInstant()
            .toEpochMilli()
    }

    private fun resolveTravelFields(
        date: LocalDate,
        originalEntry: WorkEntry?,
        data: EditFormData,
        now: Long
    ): ResolvedTravelFields {
        val shouldPersistTravel = data.dayType != DayType.COMP_TIME
        val startAt = if (shouldPersistTravel) data.travelStartTime?.let { toEpochMillis(date, it) } else null
        val arriveAt = if (shouldPersistTravel) data.travelArriveTime?.let { toEpochMillis(date, it) } else null
        val labelStart = if (shouldPersistTravel) data.travelLabelStart else null
        val labelEnd = if (shouldPersistTravel) data.travelLabelEnd else null
        val hasTravelInput = startAt != null || arriveAt != null || labelStart != null || labelEnd != null

        if (originalEntry == null) {
            return ResolvedTravelFields(
                startAt = startAt,
                arriveAt = arriveAt,
                labelStart = labelStart,
                labelEnd = labelEnd,
                fromLabel = null,
                toLabel = null,
                distanceKm = null,
                source = if (hasTravelInput) TravelSource.MANUAL else null,
                updatedAt = if (hasTravelInput) now else null
            )
        }

        val originalHasTravelData = originalEntry.travelStartAt != null ||
            originalEntry.travelArriveAt != null ||
            originalEntry.travelLabelStart != null ||
            originalEntry.travelLabelEnd != null ||
            originalEntry.travelFromLabel != null ||
            originalEntry.travelToLabel != null ||
            originalEntry.travelDistanceKm != null ||
            originalEntry.travelPaidMinutes != null ||
            originalEntry.travelSource != null

        val travelChanged = startAt != originalEntry.travelStartAt ||
            arriveAt != originalEntry.travelArriveAt ||
            labelStart != originalEntry.travelLabelStart ||
            labelEnd != originalEntry.travelLabelEnd ||
            (!shouldPersistTravel && originalHasTravelData)

        return if (travelChanged) {
            ResolvedTravelFields(
                startAt = startAt,
                arriveAt = arriveAt,
                labelStart = labelStart,
                labelEnd = labelEnd,
                fromLabel = null,
                toLabel = null,
                distanceKm = null,
                source = if (hasTravelInput) TravelSource.MANUAL else null,
                updatedAt = now
            )
        } else {
            ResolvedTravelFields(
                startAt = startAt,
                arriveAt = arriveAt,
                labelStart = labelStart,
                labelEnd = labelEnd,
                fromLabel = originalEntry.travelFromLabel,
                toLabel = originalEntry.travelToLabel,
                distanceKm = originalEntry.travelDistanceKm,
                source = originalEntry.travelSource,
                updatedAt = originalEntry.travelUpdatedAt
            )
        }
    }

    private inline fun updateFormData(update: (EditFormData) -> EditFormData) {
        _screenState.update { state ->
            state.copy(formData = update(state.formData))
        }
    }
}

private data class ResolvedTravelFields(
    val startAt: Long?,
    val arriveAt: Long?,
    val labelStart: String?,
    val labelEnd: String?,
    val fromLabel: String?,
    val toLabel: String?,
    val distanceKm: Double?,
    val source: TravelSource?,
    val updatedAt: Long?
)

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
    val mealIsArrivalDeparture: Boolean = false,
    val mealBreakfastIncluded: Boolean = false,
    val note: String? = null,
    val needsReview: Boolean = false,
    val travelStartTime: LocalTime? = null,
    val travelArriveTime: LocalTime? = null,
    val travelLabelStart: String? = null,
    val travelLabelEnd: String? = null
) {
    fun mealAllowancePreviewCents(): Int {
        return resolveMealAllowanceForSave(
            dayType = dayType,
            isArrivalDeparture = mealIsArrivalDeparture,
            breakfastIncluded = mealBreakfastIncluded
        ).amountCents
    }

    /**
     * Validates the form data and returns a list of validation errors.
     * Returns empty list if validation passes.
     */
    fun validate(): List<ValidationError> {
        // OFF- und COMP_TIME-Tage haben fachlich keine relevanten Arbeitszeiten (TimeCalculator gibt 0).
        // Eine Zeitvalidierung wäre irreführend und würde legitime Einträge blockieren.
        if (dayType == DayType.COMP_TIME || dayType == DayType.OFF) return emptyList()

        val errors = mutableListOf<ValidationError>()

        // dayLocationLabel ist nur für WORK-Tage Pflicht. OFF-Tage können ohne Ort gültig sein
        // (z.B. über ConfirmOffDay bestätigt, wo kein Ort eingegeben wird).
        if (dayType == DayType.WORK && dayLocationLabel.isNullOrBlank()) {
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
            } else {
                var diffMinutes = (travelArriveTime.toSecondOfDay() - travelStartTime.toSecondOfDay()) / 60
                if (diffMinutes < 0) diffMinutes += 24 * 60 // overnight
                if (diffMinutes > 16 * 60) {
                    errors.add(ValidationError.TravelTooLong)
                }
            }
        }

        return errors
    }

    /**
     * Returns true if the form data is valid (no validation errors).
     */
    fun isValid(): Boolean = validate().isEmpty()

    companion object {
        fun fromEntry(entry: WorkEntry, zoneId: ZoneId = ZoneId.systemDefault()): EditFormData {
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
                mealIsArrivalDeparture = entry.mealIsArrivalDeparture,
                mealBreakfastIncluded = entry.mealBreakfastIncluded,
                note = entry.note,
                needsReview = entry.needsReview,
                travelStartTime = entry.travelStartAt?.let { toLocalTime(it, zoneId) },
                travelArriveTime = entry.travelArriveAt?.let { toLocalTime(it, zoneId) },
                travelLabelStart = entry.travelLabelStart,
                travelLabelEnd = entry.travelLabelEnd
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

/**
 * Validation errors for EditFormData
 */
sealed class ValidationError(@StringRes val messageRes: Int) {
    object MissingDayLocation : ValidationError(R.string.edit_validation_missing_day_location)
    object WorkEndBeforeStart : ValidationError(R.string.edit_validation_work_end_before_start)
    object NegativeBreakMinutes : ValidationError(R.string.edit_validation_negative_break)
    object BreakLongerThanWorkTime : ValidationError(R.string.edit_validation_break_longer_than_work)
    object TravelArriveBeforeStart : ValidationError(R.string.edit_validation_travel_arrive_before_start)
    object TravelTooLong : ValidationError(R.string.edit_validation_travel_too_long)
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
