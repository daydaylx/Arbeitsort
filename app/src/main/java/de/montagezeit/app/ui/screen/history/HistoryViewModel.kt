package de.montagezeit.app.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.value = HistoryUiState.Loading
            }

            try {
                val endDate = LocalDate.now()
                val startDate = endDate.minusDays(365) // Kalenderansicht: letztes Jahr

                val entries = workEntryDao.getByDateRange(startDate, endDate)
                val groupedWeeks = groupByWeek(entries)
                val groupedMonths = groupByMonth(entries)
                val entriesByDate = entries.associateBy { it.date }

                withContext(Dispatchers.Main) {
                    _uiState.value = HistoryUiState.Success(
                        weeks = groupedWeeks,
                        months = groupedMonths,
                        entriesByDate = entriesByDate
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = HistoryUiState.Error(e.message ?: "Unbekannter Fehler")
                }
            }
        }
    }
    
    fun applyBatchEdit(
        request: BatchEditRequest,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = reminderSettingsManager.settings.first()
                val existingEntries = workEntryDao.getByDateRange(request.startDate, request.endDate)
                val entriesByDate = existingEntries.associateBy { it.date }
                val dates = buildDateRange(request.startDate, request.endDate)
                val now = System.currentTimeMillis()

                for (date in dates) {
                    val existing = entriesByDate[date]
                    val baseEntry = existing ?: WorkEntry(
                        date = date,
                        dayType = defaultDayTypeForDate(date, settings),
                        workStart = settings.workStart,
                        workEnd = settings.workEnd,
                        breakMinutes = settings.breakMinutes,
                        createdAt = now,
                        updatedAt = now
                    )

                    var updated = baseEntry
                    if (request.dayType != null) {
                        updated = updated.copy(dayType = request.dayType)
                    }
                    if (request.applyDefaultTimes) {
                        updated = updated.copy(
                            workStart = settings.workStart,
                            workEnd = settings.workEnd,
                            breakMinutes = settings.breakMinutes
                        )
                    }
                    if (request.applyNote) {
                        updated = updated.copy(note = request.note?.takeIf { it.isNotBlank() })
                    }

                    if (updated != baseEntry || existing == null) {
                        updated = updated.copy(updatedAt = now)
                        workEntryDao.upsert(updated)
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(true)
                    loadHistory()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }
    
    private fun groupByWeek(entries: List<WorkEntry>): List<WeekGroup> {
        val weekFields = WeekFields.of(Locale.GERMAN)
        
        return entries
            .groupBy { entry ->
                val week = entry.date.get(weekFields.weekOfWeekBasedYear())
                val year = entry.date.get(weekFields.weekBasedYear())
                Pair(year, week)
            }
            .map { (yearWeek, weekEntries) ->
                WeekGroup(
                    year = yearWeek.first,
                    week = yearWeek.second,
                    entries = weekEntries.sortedByDescending { it.date }
                )
            }
            .sortedByDescending { it.year * 100 + it.week }
    }
    
    private fun groupByMonth(entries: List<WorkEntry>): List<MonthGroup> {
        return entries
            .groupBy { entry ->
                Pair(entry.date.year, entry.date.monthValue)
            }
            .map { (yearMonth, monthEntries) ->
                MonthGroup(
                    year = yearMonth.first,
                    month = yearMonth.second,
                    entries = monthEntries.sortedByDescending { it.date }
                )
            }
            .sortedByDescending { it.year * 100 + it.month }
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

    private fun defaultDayTypeForDate(date: LocalDate, settings: ReminderSettings): DayType {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val isHoliday = settings.holidayDates.contains(date)
        return if ((settings.autoOffWeekends && isWeekend) || (settings.autoOffHolidays && isHoliday)) {
            DayType.OFF
        } else {
            DayType.WORK
        }
    }
}

data class MonthGroup(
    val year: Int,
    val month: Int,
    val entries: List<WorkEntry>
) {
    val displayText: String
        get() {
            val monthNames = arrayOf(
                "Januar", "Februar", "März", "April", "Mai", "Juni",
                "Juli", "August", "September", "Oktober", "November", "Dezember"
            )
            return monthNames.getOrNull(month - 1) ?: "Monat $month"
        }

    val yearText: String
        get() = if (year == LocalDate.now().year) "" else "$year"

    // Statistics
    val workDaysCount: Int
        get() = entries.count { it.dayType == de.montagezeit.app.data.local.entity.DayType.WORK }

    val offDaysCount: Int
        get() = entries.count { it.dayType == de.montagezeit.app.data.local.entity.DayType.OFF }

    val totalHours: Double
        get() = entries
            .filter { it.dayType == de.montagezeit.app.data.local.entity.DayType.WORK }
            .sumOf { entry ->
                val startMinutes = entry.workStart.hour * 60 + entry.workStart.minute
                val endMinutes = entry.workEnd.hour * 60 + entry.workEnd.minute
                val workMinutes = endMinutes - startMinutes - entry.breakMinutes
                workMinutes / 60.0
            }

    val averageHoursPerDay: Double
        get() = if (workDaysCount > 0) totalHours / workDaysCount else 0.0

    val entriesNeedingReview: Int
        get() = entries.count { it.needsReview }

    val daysOutsideLeipzig: Int
        get() = entries.count { entry ->
            entry.outsideLeipzigMorning == true || entry.outsideLeipzigEvening == true
        }
}

data class WeekGroup(
    val year: Int,
    val week: Int,
    val entries: List<WorkEntry>
) {
    val displayText: String
        get() = "KW $week"

    val yearText: String
        get() = if (year == LocalDate.now().year) "" else "$year"

    // Statistics
    val workDaysCount: Int
        get() = entries.count { it.dayType == de.montagezeit.app.data.local.entity.DayType.WORK }

    val offDaysCount: Int
        get() = entries.count { it.dayType == de.montagezeit.app.data.local.entity.DayType.OFF }

    val totalHours: Double
        get() = entries
            .filter { it.dayType == de.montagezeit.app.data.local.entity.DayType.WORK }
            .sumOf { entry ->
                val startMinutes = entry.workStart.hour * 60 + entry.workStart.minute
                val endMinutes = entry.workEnd.hour * 60 + entry.workEnd.minute
                val workMinutes = endMinutes - startMinutes - entry.breakMinutes
                workMinutes / 60.0
            }

    val averageHoursPerDay: Double
        get() = if (workDaysCount > 0) totalHours / workDaysCount else 0.0

    val entriesNeedingReview: Int
        get() = entries.count { it.needsReview }

    val daysOutsideLeipzig: Int
        get() = entries.count { entry ->
            entry.outsideLeipzigMorning == true || entry.outsideLeipzigEvening == true
        }
}

data class BatchEditRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dayType: DayType?,
    val applyDefaultTimes: Boolean,
    val note: String?,
    val applyNote: Boolean
)

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(
        val weeks: List<WeekGroup>,
        val months: List<MonthGroup> = emptyList(),
        val entriesByDate: Map<LocalDate, WorkEntry> = emptyMap()
    ) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
