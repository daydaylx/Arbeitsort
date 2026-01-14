package de.montagezeit.app.ui.screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.work.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel für ReminderSettingsScreen
 */
@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val settingsManager: ReminderSettingsManager,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    
    val settings = settingsManager.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    fun updateSettings(
        workStart: LocalTime? = null,
        workEnd: LocalTime? = null,
        breakMinutes: Int? = null,
        locationRadiusKm: Int? = null,
        morningReminderEnabled: Boolean? = null,
        morningWindowStart: LocalTime? = null,
        morningWindowEnd: LocalTime? = null,
        morningCheckIntervalMinutes: Int? = null,
        eveningReminderEnabled: Boolean? = null,
        eveningWindowStart: LocalTime? = null,
        eveningWindowEnd: LocalTime? = null,
        eveningCheckIntervalMinutes: Int? = null,
        fallbackEnabled: Boolean? = null,
        fallbackTime: LocalTime? = null,
        dailyReminderEnabled: Boolean? = null,
        dailyReminderTime: LocalTime? = null
    ) {
        viewModelScope.launch {
            settingsManager.updateSettings(
                workStart = workStart,
                workEnd = workEnd,
                breakMinutes = breakMinutes,
                locationRadiusKm = locationRadiusKm,
                morningReminderEnabled = morningReminderEnabled,
                morningWindowStart = morningWindowStart,
                morningWindowEnd = morningWindowEnd,
                morningCheckIntervalMinutes = morningCheckIntervalMinutes,
                eveningReminderEnabled = eveningReminderEnabled,
                eveningWindowStart = eveningWindowStart,
                eveningWindowEnd = eveningWindowEnd,
                eveningCheckIntervalMinutes = eveningCheckIntervalMinutes,
                fallbackEnabled = fallbackEnabled,
                fallbackTime = fallbackTime,
                dailyReminderEnabled = dailyReminderEnabled,
                dailyReminderTime = dailyReminderTime
            )
            reminderScheduler.scheduleAll()
        }
    }
}
