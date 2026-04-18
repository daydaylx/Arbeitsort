package de.montagezeit.app.ui.screen.today

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

class TodayDateCoordinator @Inject constructor(
    private val clock: Clock
) {
    private val _selectedDate = MutableStateFlow(LocalDate.now(clock))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _todayDate = MutableStateFlow(LocalDate.now(clock))
    val todayDate: StateFlow<LocalDate> = _todayDate.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun syncWithSystemDate(systemToday: LocalDate = LocalDate.now(clock)): Boolean {
        if (_todayDate.value == systemToday) return false
        val wasOnToday = _selectedDate.value == _todayDate.value
        _todayDate.value = systemToday
        if (wasOnToday) _selectedDate.value = systemToday
        return true
    }

    fun millisUntilNextDay(now: ZonedDateTime = ZonedDateTime.now(clock)): Long {
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
    }
}
