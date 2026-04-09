@file:Suppress("LongMethod")

package de.montagezeit.app.ui.screen.diagnostics

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.diagnostics.debug.DebugDiagnosticsJson
import de.montagezeit.app.diagnostics.debug.data.DiagnosticEventEntity
import de.montagezeit.app.diagnostics.debug.data.DiagnosticTraceEntity
import de.montagezeit.app.ui.components.MZLoadingState
import de.montagezeit.app.ui.components.MZScreenScaffold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val diagnosticsDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@Composable
fun DeveloperDiagnosticsScreen(
    onNavigateBack: () -> Unit,
    onOpenTrace: (String) -> Unit,
    viewModel: DeveloperDiagnosticsViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val traces by viewModel.traces.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var warningsOnly by rememberSaveable { mutableStateOf(false) }

    val visibleTraces = remember(traces, searchQuery, selectedCategory, warningsOnly) {
        traces.filter { trace ->
            val matchesCategory = selectedCategory == null || trace.category == selectedCategory
            val matchesWarnings = !warningsOnly || trace.warningCount > 0 || trace.errorCount > 0
            val query = searchQuery.trim()
            val matchesQuery = query.isBlank() || listOfNotNull(
                trace.traceId,
                trace.name,
                trace.sourceClass,
                trace.entityDate,
                trace.rangeStart,
                trace.rangeEnd,
                trace.firstWarningCode
            ).any { it.contains(query, ignoreCase = true) }
            matchesCategory && matchesWarnings && matchesQuery
        }
    }

    MZScreenScaffold(
        title = stringResource(R.string.developer_diagnostics_title),
        navigationIcon = {
            FilledTonalIconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_close)
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.developer_diagnostics_summary_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (summary == null) {
                            MZLoadingState(message = stringResource(R.string.loading))
                        } else {
                            SummaryMetrics(summary = summary!!)
                        }
                        ActionRow(
                            onRunIntegrityScan = {
                                scope.launch {
                                    val result = viewModel.runIntegrityScan()
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.developer_diagnostics_scan_finished,
                                            result.anomalyCount,
                                            result.scannedEntries
                                        )
                                    )
                                    onOpenTrace(result.traceId)
                                }
                            },
                            onExportVisible = {
                                scope.launch {
                                    val uri = viewModel.exportVisible(visibleTraces.map { it.traceId })
                                    if (uri != null) {
                                        shareUri(context, uri)
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.developer_diagnostics_export_empty)
                                        )
                                    }
                                }
                            },
                            onClearAll = {
                                scope.launch {
                                    viewModel.clearAll()
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.developer_diagnostics_cleared)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.developer_diagnostics_search)) },
                    singleLine = true
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AssistChip(
                            onClick = { selectedCategory = null },
                            label = { Text(stringResource(R.string.developer_diagnostics_filter_all)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCategory == null) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { warningsOnly = !warningsOnly },
                            label = { Text(stringResource(R.string.developer_diagnostics_filter_warnings)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (warningsOnly) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        )
                    }
                    items(traces.map { it.category }.distinct().sorted()) { category ->
                        AssistChip(
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCategory == category) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        )
                    }
                }
            }

            if (visibleTraces.isEmpty()) {
                item {
                    Card {
                        Text(
                            text = stringResource(R.string.developer_diagnostics_empty),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(visibleTraces, key = { it.traceId }) { trace ->
                    TraceListItem(
                        trace = trace,
                        onClick = { onOpenTrace(trace.traceId) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeveloperDiagnosticsTraceScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeveloperDiagnosticsTraceViewModel = hiltViewModel()
) {
    val trace by viewModel.trace.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    MZScreenScaffold(
        title = stringResource(R.string.developer_diagnostics_trace_title),
        navigationIcon = {
            FilledTonalIconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_close)
                )
            }
        },
        actions = {
            TextButton(
                enabled = trace != null,
                onClick = {
                    scope.launch {
                        val uri = viewModel.exportTrace()
                        if (uri != null) {
                            shareUri(context, uri)
                        } else {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.developer_diagnostics_export_empty)
                            )
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.developer_diagnostics_export_trace))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val traceValue = trace
        if (traceValue == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MZLoadingState(message = stringResource(R.string.loading))
            }
            return@MZScreenScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TraceMetadataCard(trace = traceValue)
            JsonCard(
                title = stringResource(R.string.developer_diagnostics_inputs_title),
                rawJson = traceValue.rootPayloadJson
            )
            traceValue.resultPayloadJson?.let {
                JsonCard(
                    title = stringResource(R.string.developer_diagnostics_result_title),
                    rawJson = it
                )
            }
            Text(
                text = stringResource(R.string.developer_diagnostics_events_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (events.isEmpty()) {
                Text(text = stringResource(R.string.developer_diagnostics_no_events))
            } else {
                events.forEach { event ->
                    EventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun SummaryMetrics(summary: de.montagezeit.app.diagnostics.debug.DiagnosticSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricRow(
            label = stringResource(R.string.developer_diagnostics_current_session),
            value = summary.sessionId.take(8)
        )
        MetricRow(
            label = stringResource(R.string.developer_diagnostics_total_traces),
            value = summary.totalTraces.toString()
        )
        MetricRow(
            label = stringResource(R.string.developer_diagnostics_warning_traces),
            value = summary.warningTraces.toString()
        )
        MetricRow(
            label = stringResource(R.string.developer_diagnostics_error_traces),
            value = summary.errorTraces.toString()
        )
        MetricRow(
            label = stringResource(R.string.developer_diagnostics_total_events),
            value = summary.totalEvents.toString()
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ActionRow(
    onRunIntegrityScan: () -> Unit,
    onExportVisible: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(onClick = onRunIntegrityScan, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.developer_diagnostics_run_scan))
        }
        FilledTonalButton(onClick = onExportVisible, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.developer_diagnostics_export_visible))
        }
        FilledTonalButton(onClick = onClearAll, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.developer_diagnostics_clear))
        }
    }
}

@Composable
private fun TraceListItem(
    trace: DiagnosticTraceEntity,
    onClick: () -> Unit
) {
    val supportingText = buildString {
        append(formatEpoch(trace.startedAtEpochMs))
        trace.entityDate?.let {
            append(" • ")
            append(it)
        }
        if (trace.warningCount > 0 || trace.errorCount > 0) {
            append(" • ")
            append("w:")
            append(trace.warningCount)
            append(" e:")
            append(trace.errorCount)
        }
    }
    val headlineColor = when (trace.severity) {
        "ERROR" -> MaterialTheme.colorScheme.error
        "WARNING" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (trace.warningCount > 0 || trace.errorCount > 0) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = trace.name,
                    color = headlineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            overlineContent = { Text(trace.category) },
            supportingContent = {
                Text(
                    text = supportingText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                Text(
                    text = trace.status,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )
    }
}

@Composable
private fun TraceMetadataCard(trace: DiagnosticTraceEntity) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = trace.name, style = MaterialTheme.typography.titleLarge)
            MetricRow(label = "traceId", value = trace.traceId)
            MetricRow(label = "sessionId", value = trace.sessionId)
            MetricRow(label = "status", value = trace.status)
            MetricRow(label = "severity", value = trace.severity)
            MetricRow(label = "category", value = trace.category)
            MetricRow(label = "source", value = trace.sourceClass)
            MetricRow(label = "started", value = formatEpoch(trace.startedAtEpochMs))
            MetricRow(
                label = "durationMs",
                value = trace.durationMs?.toString() ?: "-"
            )
            MetricRow(label = "warnings", value = trace.warningCount.toString())
            MetricRow(label = "errors", value = trace.errorCount.toString())
            trace.entityDate?.let { MetricRow(label = "entityDate", value = it) }
            if (trace.rangeStart != null || trace.rangeEnd != null) {
                MetricRow(
                    label = "range",
                    value = "${trace.rangeStart.orEmpty()}..${trace.rangeEnd.orEmpty()}"
                )
            }
            trace.firstWarningCode?.let {
                MetricRow(label = "firstWarning", value = it)
            }
        }
    }
}

@Composable
private fun JsonCard(title: String, rawJson: String) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            SelectionContainer {
                Text(
                    text = DebugDiagnosticsJson.prettyPrint(rawJson),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun EventCard(event: DiagnosticEventEntity) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${event.seq}. ${event.name}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(text = event.severity, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = "${event.phase} • ${formatEpoch(event.createdAtEpochMs)}",
                style = MaterialTheme.typography.bodySmall
            )
            event.warningCode?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            SelectionContainer {
                Text(
                    text = DebugDiagnosticsJson.prettyPrint(event.payloadJson),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun formatEpoch(epochMs: Long): String {
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(diagnosticsDateTimeFormatter)
}

private fun shareUri(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
