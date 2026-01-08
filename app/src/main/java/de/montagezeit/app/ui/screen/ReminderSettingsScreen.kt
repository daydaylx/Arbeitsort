package de.montagezeit.app.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.montagezeit.app.data.preferences.ReminderSettings
import de.montagezeit.app.ui.screen.viewmodel.ReminderSettingsViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Settings Screen für Reminder-Einstellungen
 * 
 * Enthält:
 * - Reminder-Fenster Einstellungen
 * - Samsung/One UI Hardening Info
 * - Buttons zu Battery Optimization / App Details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    viewModel: ReminderSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState(initial = ReminderSettings())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Erinnerungseinstellungen") },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Samsung Hardening Info
            SamsungHardeningCard(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))

            // Morning Settings
            SettingsSection(title = "Morgendliche Erinnerungen") {
                SwitchSetting(
                    title = "Aktiviert",
                    checked = settings?.morningReminderEnabled ?: true,
                    onCheckedChange = { enabled ->
                        viewModel.updateSettings(morningReminderEnabled = enabled)
                    }
                )

                if (settings?.morningReminderEnabled == true) {
                    TimeRangeSetting(
                        title = "Zeitfenster",
                        startTime = settings?.morningWindowStart ?: LocalTime.of(6, 0),
                        endTime = settings?.morningWindowEnd ?: LocalTime.of(13, 0),
                        onStartTimeChange = { time ->
                            viewModel.updateSettings(morningWindowStart = time)
                        },
                        onEndTimeChange = { time ->
                            viewModel.updateSettings(morningWindowEnd = time)
                        }
                    )

                    IntSetting(
                        title = "Intervall (Minuten)",
                        value = settings?.morningCheckIntervalMinutes ?: 120,
                        onValueChange = { value ->
                            viewModel.updateSettings(morningCheckIntervalMinutes = value)
                        },
                        min = 30,
                        max = 360
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Evening Settings
            SettingsSection(title = "Abendliche Erinnerungen") {
                SwitchSetting(
                    title = "Aktiviert",
                    checked = settings?.eveningReminderEnabled ?: true,
                    onCheckedChange = { enabled ->
                        viewModel.updateSettings(eveningReminderEnabled = enabled)
                    }
                )

                if (settings?.eveningReminderEnabled == true) {
                    TimeRangeSetting(
                        title = "Zeitfenster",
                        startTime = settings?.eveningWindowStart ?: LocalTime.of(16, 0),
                        endTime = settings?.eveningWindowEnd ?: LocalTime.of(22, 30),
                        onStartTimeChange = { time ->
                            viewModel.updateSettings(eveningWindowStart = time)
                        },
                        onEndTimeChange = { time ->
                            viewModel.updateSettings(eveningWindowEnd = time)
                        }
                    )

                    IntSetting(
                        title = "Intervall (Minuten)",
                        value = settings?.eveningCheckIntervalMinutes ?: 180,
                        onValueChange = { value ->
                            viewModel.updateSettings(eveningCheckIntervalMinutes = value)
                        },
                        min = 30,
                        max = 360
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fallback Settings
            SettingsSection(title = "Fallback-Erinnerung") {
                SwitchSetting(
                    title = "Aktiviert",
                    checked = settings?.fallbackEnabled ?: true,
                    onCheckedChange = { enabled ->
                        viewModel.updateSettings(fallbackEnabled = enabled)
                    }
                )

                if (settings?.fallbackEnabled == true) {
                    TimeSetting(
                        title = "Zeit",
                        time = settings?.fallbackTime ?: LocalTime.of(22, 30),
                        onTimeChange = { time ->
                            viewModel.updateSettings(fallbackTime = time)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Work Defaults
            SettingsSection(title = "Arbeitszeit-Defaults") {
                TimeRangeSetting(
                    title = "Arbeitszeit",
                    startTime = settings?.workStart ?: LocalTime.of(8, 0),
                    endTime = settings?.workEnd ?: LocalTime.of(19, 0),
                    onStartTimeChange = { time ->
                        viewModel.updateSettings(workStart = time)
                    },
                    onEndTimeChange = { time ->
                        viewModel.updateSettings(workEnd = time)
                    }
                )

                IntSetting(
                    title = "Pause (Minuten)",
                    value = settings?.breakMinutes ?: 60,
                    onValueChange = { value ->
                        viewModel.updateSettings(breakMinutes = value)
                    },
                    min = 0,
                    max = 180
                )

                IntSetting(
                    title = "Radius (km)",
                    value = settings?.locationRadiusKm ?: 30,
                    onValueChange = { value ->
                        viewModel.updateSettings(locationRadiusKm = value)
                    },
                    min = 5,
                    max = 100
                )
            }
        }
    }
}

@Composable
fun SamsungHardeningCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Warnung",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Samsung One UI Hinweis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Samsung One UI killt Hintergrund-Prozesse aggressiv. Damit Reminder zuverlässig funktionieren:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "1. Öffne \"Batterieoptimierung\"\n2. Suche \"MontageZeit\"\n3. Setze auf \"Unrestricted\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { openBatteryOptimizationSettings(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batterieoptimierung")
                }
                
                OutlinedButton(
                    onClick = { openAppDetailsSettings(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("App-Details")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun TimeSetting(
    title: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title)
        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(time.format(formatter))
        }
    }
    
    // TODO: TimePickerDialog implementieren
    if (showDialog) {
        // Placeholder - in production implement TimePicker
        onTimeChange(time)
        showDialog = false
    }
}

@Composable
fun TimeRangeSetting(
    title: String,
    startTime: LocalTime,
    endTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit,
    onEndTimeChange: (LocalTime) -> Unit
) {
    var showStartDialog by remember { mutableStateOf(false) }
    var showEndDialog by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showStartDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(startTime.format(formatter))
            }
            
            Text(
                text = "-",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            
            OutlinedButton(
                onClick = { showEndDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(endTime.format(formatter))
            }
        }
    }
    
    // TODO: TimePickerDialog implementieren
    if (showStartDialog) {
        // Placeholder
        showStartDialog = false
    }
    if (showEndDialog) {
        // Placeholder
        showEndDialog = false
    }
}

@Composable
fun IntSetting(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 100
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$title: $value")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = max - min - 1
        )
    }
}

/**
 * Öffnet die Batterieoptimierung-Einstellungen
 */
private fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent().apply {
        action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback zu App-Details
        openAppDetailsSettings(context)
    }
}

/**
 * Öffnet die App-Details-Einstellungen
 */
private fun openAppDetailsSettings(context: Context) {
    val intent = Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
