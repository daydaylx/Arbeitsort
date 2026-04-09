package de.montagezeit.app.ui.screen.diagnostics

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.montagezeit.app.diagnostics.debug.DebugDiagnosticsController
import de.montagezeit.app.diagnostics.debug.DiagnosticSummary
import de.montagezeit.app.diagnostics.debug.DiagnosticsIntegrityScanner
import de.montagezeit.app.diagnostics.debug.data.DiagnosticEventEntity
import de.montagezeit.app.diagnostics.debug.data.DiagnosticTraceEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DeveloperDiagnosticsViewModel @Inject constructor(
    private val diagnosticsController: DebugDiagnosticsController,
    private val integrityScanner: DiagnosticsIntegrityScanner
) : ViewModel() {

    val summary: StateFlow<DiagnosticSummary?> = diagnosticsController.observeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val traces: StateFlow<List<DiagnosticTraceEntity>> = diagnosticsController.observeTraces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun exportVisible(traceIds: List<String>): Uri? {
        return diagnosticsController.exportBundle(traceIds, prefix = "diagnostics_visible")
    }

    suspend fun clearAll() {
        diagnosticsController.clearAll()
    }

    suspend fun runIntegrityScan() = integrityScanner.scanRecentDays()
}

@HiltViewModel
class DeveloperDiagnosticsTraceViewModel @Inject constructor(
    private val diagnosticsController: DebugDiagnosticsController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val traceId: String = checkNotNull(savedStateHandle["traceId"])

    val trace: StateFlow<DiagnosticTraceEntity?> = diagnosticsController.observeTrace(traceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val events: StateFlow<List<DiagnosticEventEntity>> = diagnosticsController.observeEvents(traceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun exportTrace(): Uri? {
        return diagnosticsController.exportBundle(listOf(traceId), prefix = "diagnostics_trace")
    }
}
