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
                val startDate = endDate.minusDays(30) // Letzte 30 Tage
                
                val entries = workEntryDao.getByDateRange(startDate, endDate)
                val groupedEntries = groupByWeek(entries)
                
                _uiState.value = HistoryUiState.Success(groupedEntries)
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
    data class Success(val weeks: List<WeekGroup>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
