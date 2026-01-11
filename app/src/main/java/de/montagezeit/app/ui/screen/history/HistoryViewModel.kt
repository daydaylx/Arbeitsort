package de.montagezeit.app.ui.screen.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.usecase.ExportDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val exportDataUseCase: ExportDataUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            
            try {
                val endDate = LocalDate.now()
                val startDate = endDate.minusDays(90) // Letzte 90 Tage für Monatsübersicht
                
                val entries = workEntryDao.getByDateRange(startDate, endDate)
                val groupedWeeks = groupByWeek(entries)
                val groupedMonths = groupByMonth(entries)
                
                _uiState.value = HistoryUiState.Success(
                    weeks = groupedWeeks,
                    months = groupedMonths
                )
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
    
    fun exportToCsv(onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = exportDataUseCase()
                onResult(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
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

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(
        val weeks: List<WeekGroup>,
        val months: List<MonthGroup> = emptyList()
    ) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
