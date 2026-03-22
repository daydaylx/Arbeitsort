package de.montagezeit.app.ui.screen.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.R
import de.montagezeit.app.data.local.dao.WorkEntryDao
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.ui.util.UiText
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val workEntryDao: WorkEntryDao,
    private val reminderSettingsManager: ReminderSettingsManager
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(OverviewPeriod.WEEK)
    val selectedPeriod: StateFlow<OverviewPeriod> = _selectedPeriod.asStateFlow()

    private val _metrics = MutableStateFlow<OverviewMetrics?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<UiText?>(null)

    private var refreshJob: Job? = null

    private data class SelectedEntrySnapshot(
        val requestedDate: LocalDate? = null,
        val entry: WorkEntry? = null
    )

    private data class EntryRefreshKey(
        val selectedDate: LocalDate,
        val updatedAt: Long?
    )

    private data class SelectionPart(
        val selectedDate: LocalDate,
        val selectedPeriod: OverviewPeriod,
        val selectedEntry: WorkEntry?
    )

    private data class LoadingPart(
        val metrics: OverviewMetrics?,
        val isLoading: Boolean,
        val errorMessage: UiText?
    )

    private val selectedEntrySnapshot: StateFlow<SelectedEntrySnapshot> = _selectedDate
        .flatMapLatest { date ->
            workEntryDao.getByDateFlow(date).map { entry ->
                SelectedEntrySnapshot(
                    requestedDate = date,
                    entry = entry
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SelectedEntrySnapshot()
        )

    val screenState: StateFlow<OverviewScreenState> = combine(
        combine(_selectedDate, _selectedPeriod, selectedEntrySnapshot) { selectedDate, selectedPeriod, entrySnapshot ->
            SelectionPart(
                selectedDate = selectedDate,
                selectedPeriod = selectedPeriod,
                selectedEntry = entrySnapshot.entry
            )
        },
        combine(_metrics, _isLoading, _errorMessage) { metrics, isLoading, errorMessage ->
            LoadingPart(
                metrics = metrics,
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }
    ) { selection, loading ->
        OverviewScreenState(
            selectedDate = selection.selectedDate,
            selectedPeriod = selection.selectedPeriod,
            selectedEntry = selection.selectedEntry,
            metrics = loading.metrics,
            isLoading = loading.isLoading,
            errorMessage = loading.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverviewScreenState(
            selectedDate = LocalDate.now(),
            selectedPeriod = OverviewPeriod.WEEK,
            selectedEntry = null,
            metrics = null,
            isLoading = true,
            errorMessage = null
        )
    )

    init {
        observeSelectedEntryUpdates()
        refreshOverview(
            clearMetrics = true,
            showLoading = true,
            resetError = true
        )
    }

    fun selectDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        _selectedDate.value = date
        refreshOverview(
            clearMetrics = true,
            showLoading = true,
            resetError = true
        )
    }

    fun selectPeriod(period: OverviewPeriod) {
        if (_selectedPeriod.value == period) return
        _selectedPeriod.value = period
        refreshOverview(
            clearMetrics = true,
            showLoading = true,
            resetError = true
        )
    }

    fun goToPreviousRange() {
        _selectedDate.value = _selectedPeriod.value.shiftReferenceDate(_selectedDate.value, -1)
        refreshOverview(
            clearMetrics = true,
            showLoading = true,
            resetError = true
        )
    }

    fun goToNextRange() {
        _selectedDate.value = _selectedPeriod.value.shiftReferenceDate(_selectedDate.value, 1)
        refreshOverview(
            clearMetrics = true,
            showLoading = true,
            resetError = true
        )
    }

    fun onResetError() {
        refreshOverview(
            clearMetrics = _metrics.value == null,
            showLoading = true,
            resetError = true
        )
    }

    fun onErrorShown() {
        _errorMessage.value = null
    }

    private fun observeSelectedEntryUpdates() {
        viewModelScope.launch {
            combine(_selectedDate, selectedEntrySnapshot) { selectedDate, snapshot ->
                val updatedAt = if (snapshot.requestedDate == selectedDate) {
                    snapshot.entry?.updatedAt
                } else {
                    null
                }
                EntryRefreshKey(selectedDate = selectedDate, updatedAt = updatedAt)
            }
                .debounce(ENTRY_UPDATE_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect {
                    refreshOverview(
                        clearMetrics = false,
                        showLoading = _metrics.value == null,
                        resetError = false
                    )
                }
        }
    }

    private fun refreshOverview(
        clearMetrics: Boolean,
        showLoading: Boolean,
        resetError: Boolean
    ) {
        refreshJob?.cancel()

        if (clearMetrics) {
            _metrics.value = null
        }
        if (showLoading) {
            _isLoading.value = true
        }
        if (resetError) {
            _errorMessage.value = null
        }

        val selectedDate = _selectedDate.value
        val selectedPeriod = _selectedPeriod.value

        refreshJob = viewModelScope.launch {
            try {
                val settings = reminderSettingsManager.settings.first()
                val range = selectedPeriod.rangeFor(selectedDate)
                val entries = withContext(Dispatchers.IO) {
                    workEntryDao.getByDateRangeWithTravel(range.startDate, range.endDate)
                }

                _metrics.value = buildOverviewMetrics(
                    period = selectedPeriod,
                    entries = entries,
                    settings = settings
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.toUiText(R.string.today_error_unknown)
            } finally {
                if (isActive) {
                    _isLoading.value = false
                }
            }
        }
    }

    private companion object {
        private const val ENTRY_UPDATE_DEBOUNCE_MS = 250L
    }
}

private fun Throwable.toUiText(@androidx.annotation.StringRes fallbackRes: Int): UiText {
    val messageValue = message?.trim().orEmpty()
    return if (messageValue.isNotEmpty()) {
        UiText.DynamicString(messageValue)
    } else {
        UiText.StringResource(fallbackRes)
    }
}
