package de.montagezeit.app.ui.screen.today

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class TodayDateCoordinator @Inject constructor() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _todayDate = MutableStateFlow(LocalDate.now())
    val todayDate: StateFlow<LocalDate> = _todayDate.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun syncWithSystemDate(systemToday: LocalDate = LocalDate.now()): Boolean {
        if (_todayDate.value == systemToday) return false
        val wasOnToday = _selectedDate.value == _todayDate.value
        _todayDate.value = systemToday
        if (wasOnToday) _selectedDate.value = systemToday
        return true
    }

    fun millisUntilNextDay(now: LocalDateTime = LocalDateTime.now()): Long {
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
    }
}
