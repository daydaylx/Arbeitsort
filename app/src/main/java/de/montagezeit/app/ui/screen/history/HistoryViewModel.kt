package de.montagezeit.app.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.diagnostics.AppDiagnosticsRuntime
import de.montagezeit.app.diagnostics.DiagnosticCategory
import de.montagezeit.app.diagnostics.DiagnosticStatus
import de.montagezeit.app.diagnostics.DiagnosticTraceRequest
import de.montagezeit.app.domain.usecase.AggregateWorkStats
import de.montagezeit.app.domain.util.transitionToDayType
import android.util.Log
import androidx.compose.runtime.Stable
import de.montagezeit.app.ui.util.UiText
import de.montagezeit.app.ui.util.toUiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workEntryRepository: WorkEntryRepository,
    private val reminderSettingsManager: ReminderSettingsManager
) : ViewModel() {

    private val _batchEditState = MutableStateFlow<BatchEditState>(BatchEditState.Idle)
    val batchEditState: StateFlow<BatchEditState> = _batchEditState.asStateFlow()

    // Incrementing this trigger causes flatMapLatest to cancel the previous flow
    // subscription and start a fresh one — used by loadHistory() for error retry.
    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<HistoryUiState> = _refreshTrigger
        .flatMapLatest {
            workEntryRepository.getAllWithTravelFlow()
                .debounce(HISTORY_DEBOUNCE_MS)
                .distinctUntilChanged()
                .map<List<WorkEntryWithTravelLegs>, HistoryUiState> { entries ->
                    withContext(Dispatchers.Default) {
                        buildSuccessState(entries)
                    }
                }
                .catch { e ->
                    emit(HistoryUiState.Error(message = e.toUiText(R.string.history_error_unknown)))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState.Loading
        )

    /** Retriggers the DB-Observer flow. Call on error to retry loading. */
    fun loadHistory() {
        _refreshTrigger.value++
    }

    private fun buildSuccessState(entries: List<WorkEntryWithTravelLegs>): HistoryUiState.Success {
        val trace = AppDiagnosticsRuntime.startTrace(
            DiagnosticTraceRequest(
                category = DiagnosticCategory.CALCULATION_RUN,
                name = "history_rebuild",
                sourceClass = "HistoryViewModel",
                screenOrWorker = "HistoryScreen",
                payload = mapOf("entryCount" to entries.size)
            )
        )
        try {
            val groupedWeeks = groupByWeek(entries)
            trace.event("weeks_grouped", payload = mapOf("weekCount" to groupedWeeks.size))
            val groupedMonths = groupByMonth(entries)
            trace.event("months_grouped", payload = mapOf("monthCount" to groupedMonths.size))
            val entriesByDate = entries.associate { it.workEntry.date to it.workEntry }
            val travelLegsByDate = entries.associate { it.workEntry.date to it.orderedTravelLegs }
            trace.finish(payload = mapOf(
                "entryCount" to entries.size,
                "weekCount" to groupedWeeks.size,
                "monthCount" to groupedMonths.size
            ))
            return HistoryUiState.Success(
                weeks = groupedWeeks,
                months = groupedMonths,
                entriesByDate = entriesByDate,
                travelLegsByDate = travelLegsByDate
            )
        } catch (e: CancellationException) {
            trace.finish(status = DiagnosticStatus.CANCELLED)
            throw e
        } catch (e: Exception) {
            trace.error("history_rebuild_failed", e)
            trace.finish(status = DiagnosticStatus.ERROR)
            throw e
        }
    }
    
    fun applyBatchEdit(request: BatchEditRequest) {
        val validationError = validateBatchEditRequest(request)
        if (validationError != null) {
            _batchEditState.value = BatchEditState.Failure(validationError)
            return
        }
        if (_batchEditState.value is BatchEditState.InProgress) return

        viewModelScope.launch(Dispatchers.IO) {
            _batchEditState.value = BatchEditState.InProgress
            try {
                val settings = reminderSettingsManager.settings.first()
                val existingEntries = workEntryRepository.getByDateRange(request.startDate, request.endDate)
                val entriesByDate = existingEntries.associateBy { it.date }
                val dates = buildDateRange(request.startDate, request.endDate)
                val missingDates = dates.filterNot(entriesByDate::containsKey)
                if (missingDates.isNotEmpty()) {
                    _batchEditState.value = BatchEditState.Failure(
                        UiText.StringResource(R.string.history_batch_missing_entries)
                    )
                    return@launch
                }
                val now = System.currentTimeMillis()

                val entriesToUpsert = mutableListOf<WorkEntry>()
                val travelLegDatesToDelete = mutableSetOf<LocalDate>()
                for (date in dates) {
                    val existing = requireNotNull(entriesByDate[date])
                    val baseEntry = existing

                    var updated = baseEntry
                    if (request.dayType != null) {
                        updated = updated.transitionToDayType(dayType = request.dayType, now = now)
                    }
                    if (request.applyDayLocation) {
                        updated = updated.copy(dayLocationLabel = request.dayLocationLabel?.trim().orEmpty())
                    }
                    if (request.dayType == DayType.COMP_TIME) {
                        travelLegDatesToDelete += date
                    }
                    if (updated.dayType == DayType.WORK && updated.dayLocationLabel.isBlank()) {
                        _batchEditState.value = BatchEditState.Failure(
                            UiText.StringResource(R.string.history_batch_work_requires_location)
                        )
                        return@launch
                    }
                    if (request.applyDefaultTimes && updated.dayType == DayType.WORK) {
                        updated = updated.copy(
                            workStart = settings.workStart,
                            workEnd = settings.workEnd,
                            breakMinutes = settings.breakMinutes
                        )
                    }
                    if (request.applyNote) {
                        updated = updated.copy(note = request.note?.takeIf { it.isNotBlank() })
                    }

                    if (updated != baseEntry) {
                        updated = updated.copy(updatedAt = now)
                        entriesToUpsert.add(updated)
                    }
                }
                if (entriesToUpsert.isNotEmpty() || travelLegDatesToDelete.isNotEmpty()) {
                    workEntryRepository.upsertAllAndDeleteTravelLegs(
                        entries = entriesToUpsert,
                        travelLegDatesToDelete = travelLegDatesToDelete.toList()
                    )
                } else {
                    _batchEditState.value = BatchEditState.Failure(
                        UiText.StringResource(R.string.history_batch_no_changes)
                    )
                    return@launch
                }

                _batchEditState.value = BatchEditState.Success
            } catch (e: Exception) {
                Log.w("HistoryViewModel", "applyBatchEdit failed", e)
                _batchEditState.value = BatchEditState.Failure(e.toUiText(R.string.history_toast_batch_failed))
            }
        }
    }

    fun onBatchEditResultConsumed() {
        _batchEditState.value = BatchEditState.Idle
    }
    
    private fun groupByWeek(entries: List<WorkEntryWithTravelLegs>): List<WeekGroup> {
        val weekFields = WeekFields.ISO
        
        return entries
            .groupBy { entry ->
                val week = entry.workEntry.date.get(weekFields.weekOfWeekBasedYear())
                val year = entry.workEntry.date.get(weekFields.weekBasedYear())
                Pair(year, week)
            }
            .map { (yearWeek, weekEntries) ->
                val sortedEntries = weekEntries.sortedByDescending { it.workEntry.date }
                val stats = calculateGroupStats(sortedEntries)
                val weekStart = weekEntries.minOf { it.workEntry.date }.with(weekFields.dayOfWeek(), 1)
                WeekGroup(
                    year = yearWeek.first,
                    week = yearWeek.second,
                    weekStart = weekStart,
                    entries = sortedEntries.map { it.workEntry },
                    workDaysCount = stats.workDaysCount,
                    offDaysCount = stats.offDaysCount,
                    totalHours = stats.totalHours,
                    totalPaidHours = stats.totalPaidHours,
                    averageHoursPerDay = stats.averageHoursPerDay
                )
            }
            .sortedByDescending { it.year * 100 + it.week }
    }
    
    private fun groupByMonth(entries: List<WorkEntryWithTravelLegs>): List<MonthGroup> {
        return entries
            .groupBy { entry ->
                Pair(entry.workEntry.date.year, entry.workEntry.date.monthValue)
            }
            .map { (yearMonth, monthEntries) ->
                val sortedEntries = monthEntries.sortedByDescending { it.workEntry.date }
                val stats = calculateGroupStats(sortedEntries)
                MonthGroup(
                    year = yearMonth.first,
                    month = yearMonth.second,
                    entries = sortedEntries.map { it.workEntry },
                    workDaysCount = stats.workDaysCount,
                    offDaysCount = stats.offDaysCount,
                    totalHours = stats.totalHours,
                    totalPaidHours = stats.totalPaidHours,
                    averageHoursPerDay = stats.averageHoursPerDay,
                    totalTravelMinutes = stats.totalTravelMinutes
                )
            }
            .sortedByDescending { it.year * 100 + it.month }
    }

    private val aggregateWorkStats = AggregateWorkStats()

    private fun calculateGroupStats(entries: List<WorkEntryWithTravelLegs>): HistoryGroupStats {
        val stats = aggregateWorkStats(entries)
        return HistoryGroupStats(
            workDaysCount = stats.workDays,
            offDaysCount = stats.freeDaysWithTravel + stats.freeDaysWithoutTravel + stats.compTimeDays,
            totalHours = stats.totalWorkMinutes / 60.0,
            totalPaidHours = stats.totalPaidMinutes / 60.0,
            averageHoursPerDay = stats.averageWorkHoursPerDay,
            totalTravelMinutes = stats.totalTravelMinutes
        )
    }

    private fun buildDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }

    private fun validateBatchEditRequest(request: BatchEditRequest): UiText? {
        if (request.startDate.isAfter(request.endDate)) {
            return UiText.StringResource(R.string.history_batch_invalid_range)
        }
        if (request.dayType == null && !request.applyDefaultTimes && !request.applyDayLocation && !request.applyNote) {
            return UiText.StringResource(R.string.history_batch_select_action)
        }
        if (request.applyDayLocation && request.dayLocationLabel.isNullOrBlank()) {
            return UiText.StringResource(R.string.history_batch_location_required)
        }
        return null
    }

    private companion object {
        private const val HISTORY_DEBOUNCE_MS = 150L
    }
}

@Stable
data class MonthGroup(
    val year: Int,
    val month: Int,
    val entries: List<WorkEntry>,
    val workDaysCount: Int,
    val offDaysCount: Int,
    val totalHours: Double,
    val totalPaidHours: Double,
    val averageHoursPerDay: Double,
    val totalTravelMinutes: Int = 0
) {
    val displayText: String
        get() = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())

    val yearText: String
        get() = if (year == LocalDate.now().year) "" else "$year"
}

@Stable
data class WeekGroup(
    val year: Int,
    val week: Int,
    val weekStart: LocalDate,
    val entries: List<WorkEntry>,
    val workDaysCount: Int,
    val offDaysCount: Int,
    val totalHours: Double,
    val totalPaidHours: Double,
    val averageHoursPerDay: Double
) {
    val yearText: String
        get() = if (year == LocalDate.now().year) "" else "$year"
}

private data class HistoryGroupStats(
    val workDaysCount: Int,
    val offDaysCount: Int,
    val totalHours: Double,
    val totalPaidHours: Double,
    val averageHoursPerDay: Double,
    val totalTravelMinutes: Int
)

data class BatchEditRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dayType: DayType?,
    val applyDefaultTimes: Boolean,
    val dayLocationLabel: String?,
    val applyDayLocation: Boolean,
    val note: String?,
    val applyNote: Boolean
)

sealed class BatchEditState {
    object Idle : BatchEditState()
    object InProgress : BatchEditState()
    object Success : BatchEditState()
    data class Failure(val message: UiText) : BatchEditState()
}

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(
        val weeks: List<WeekGroup>,
        val months: List<MonthGroup> = emptyList(),
        val entriesByDate: Map<LocalDate, WorkEntry> = emptyMap(),
        val travelLegsByDate: Map<LocalDate, List<TravelLeg>> = emptyMap()
    ) : HistoryUiState()
    data class Error(val message: UiText) : HistoryUiState()
}
