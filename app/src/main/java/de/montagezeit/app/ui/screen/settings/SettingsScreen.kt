package de.montagezeit.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.reminderSettings.collectAsState(initial = null)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") }
            )
        }
    ) { paddingValues ->
        if (settings != null) {
            SettingsContent(
                settings = settings!!,
                uiState = uiState,
                onUpdateMorningWindow = { startH, startM, endH, endM ->
                    viewModel.updateMorningWindow(startH, startM, endH, endM)
                },
                onUpdateEveningWindow = { startH, startM, endH, endM ->
                    viewModel.updateEveningWindow(startH, startM, endH, endM)
                },
                onUpdateRadius = { viewModel.updateRadiusMeters(it) },
                onUpdateLocationMode = { viewModel.updateLocationMode(it) },
                onExport = { viewModel.exportToCsv() },
                onResetExportState = { viewModel.resetExportState() },
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
fun SettingsContent(
    settings: de.montagezeit.app.data.preferences.ReminderSettings,
    uiState: SettingsUiState,
    onUpdateMorningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateEveningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateRadius: (Int) -> Unit,
    onUpdateLocationMode: (LocationMode) -> Unit,
    onExport: () -> Unit,
    onResetExportState: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Default Times Section
        SettingsSection(title = "Arbeitszeiten") {
            DefaultTimesSection(
                workStart = settings.workStart,
                workEnd = settings.workEnd,
                breakMinutes = settings.breakMinutes
            )
        }
        
        // Reminder Settings Section
        SettingsSection(title = "Erinnerungen") {
            ReminderSettingsSection(
                morningWindowStart = settings.morningReminderStart,
                morningWindowEnd = settings.morningReminderEnd,
                eveningWindowStart = settings.eveningReminderStart,
                eveningWindowEnd = settings.eveningReminderEnd,
                onUpdateMorningWindow = onUpdateMorningWindow,
                onUpdateEveningWindow = onUpdateEveningWindow
            )
        }
        
        // Location Settings Section
        SettingsSection(title = "Standort") {
            LocationSettingsSection(
                radiusMeters = settings.radiusMeters,
                locationMode = settings.locationMode,
                onUpdateRadius = onUpdateRadius,
                onUpdateLocationMode = onUpdateLocationMode
            )
        }
        
        // Export Section
        SettingsSection(title = "Export") {
            ExportSection(
                uiState = uiState,
                onExport = onExport,
                onResetState = onResetExportState
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
fun DefaultTimesSection(
    workStart: LocalTime,
    workEnd: LocalTime,
    breakMinutes: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Arbeitsbeginn",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatTime(workStart),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Column {
                Text(
                    text = "Arbeitsende",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatTime(workEnd),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pause",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$breakMinutes min",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        Text(
            text = "Wird für die automatische Berechnung verwendet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReminderSettingsSection(
    morningWindowStart: LocalTime,
    morningWindowEnd: LocalTime,
    eveningWindowStart: LocalTime,
    eveningWindowEnd: LocalTime,
    onUpdateMorningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateEveningWindow: (Int, Int, Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Morning Window
        Text(
            text = "Morgen-Erinnerung",
            style = MaterialTheme.typography.titleMedium
        )
        
        TimeRangePicker(
            label = "Erinnerungszeitraum",
            startTime = morningWindowStart,
            endTime = morningWindowEnd,
            onTimeRangeChanged = { start, end ->
                onUpdateMorningWindow(start.hour, start.minute, end.hour, end.minute)
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Evening Window
        Text(
            text = "Abend-Erinnerung",
            style = MaterialTheme.typography.titleMedium
        )
        
        TimeRangePicker(
            label = "Erinnerungszeitraum",
            startTime = eveningWindowStart,
            endTime = eveningWindowEnd,
            onTimeRangeChanged = { start, end ->
                onUpdateEveningWindow(start.hour, start.minute, end.hour, end.minute)
            }
        )
    }
}

@Composable
fun TimeRangePicker(
    label: String,
    startTime: LocalTime,
    endTime: LocalTime,
    onTimeRangeChanged: (LocalTime, LocalTime) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(formatTime(startTime))
            }
            
            Text(
                text = "–",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            
            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(formatTime(endTime))
            }
        }
    }
    
    if (showStartPicker) {
        TimePickerDialog(
            initialTime = startTime,
            onTimeSelected = {
                onTimeRangeChanged(it, endTime)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    
    if (showEndPicker) {
        TimePickerDialog(
            initialTime = endTime,
            onTimeSelected = {
                onTimeRangeChanged(startTime, it)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
fun LocationSettingsSection(
    radiusMeters: Int,
    locationMode: String,
    onUpdateRadius: (Int) -> Unit,
    onUpdateLocationMode: (LocationMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Radius
        Text(
            text = "Radius um Leipzig (km)",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Slider(
            value = (radiusMeters / 1000f),
            onValueChange = { onUpdateRadius((it * 1000).toInt()) },
            valueRange = 1f..50f,
            steps = 49,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "%.0f km".format(radiusMeters / 1000f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        HorizontalDivider()
        
        // Location Mode
        Text(
            text = "Standortmodus",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LocationMode.values().forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpdateLocationMode(mode) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = locationMode == mode.value,
                        onClick = { onUpdateLocationMode(mode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = mode.displayName)
                }
            }
        }
    }
}

@Composable
fun ExportSection(
    uiState: SettingsUiState,
    onExport: () -> Unit,
    onResetState: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Alle Einträge als CSV exportieren",
            style = MaterialTheme.typography.bodyMedium
        )
        
        when (uiState) {
            is SettingsUiState.Initial -> {
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Exportieren")
                }
            }
            
            is SettingsUiState.Exporting -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text("Exportiere...")
                }
            }
            
            is SettingsUiState.ExportSuccess -> {
                val csv = (uiState as SettingsUiState.ExportSuccess).csv
                var showCopyDialog by remember { mutableStateOf(true) }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Export erfolgreich!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (showCopyDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            showCopyDialog = false
                            onResetState()
                        },
                        title = { Text("CSV kopieren") },
                        text = {
                            Text(
                                "Die CSV-Daten wurden generiert. Möchten Sie sie in die Zwischenablage kopieren?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                // TODO: Implement clipboard copy
                                showCopyDialog = false
                                onResetState()
                            }) {
                                Text("Kopieren")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showCopyDialog = false
                                onResetState()
                            }) {
                                Text("Schließen")
                            }
                        }
                    )
                }
            }
            
            is SettingsUiState.ExportError -> {
                val error = (uiState as SettingsUiState.ExportError).message
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Fehler: $error",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTime by remember { mutableStateOf(initialTime) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zeit wählen") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Simple time picker with sliders
                Text(
                    text = formatTime(selectedTime),
                    style = MaterialTheme.typography.headlineLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Stunde",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = selectedTime.hour.toFloat(),
                    onValueChange = { selectedTime = selectedTime.withHour(it.toInt()) },
                    valueRange = 0f..23f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Minute",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = selectedTime.minute.toFloat(),
                    onValueChange = { selectedTime = selectedTime.withMinute(it.toInt()) },
                    valueRange = 0f..59f,
                    steps = 59,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onTimeSelected(selectedTime) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

private fun formatTime(time: LocalTime): String {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}
