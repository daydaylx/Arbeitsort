package de.montagezeit.app.ui.screen.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import de.montagezeit.app.ui.common.TimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.reminderSettings.collectAsState(initial = null)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var hasLocationPermission by remember {
        mutableStateOf(checkLocationPermission(context))
    }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkBatteryOptimizations(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission(context)
                hasLocationPermission = checkLocationPermission(context)
                isIgnoringBatteryOptimizations = checkBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
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
                onUpdateMorningEnabled = { viewModel.updateMorningReminderEnabled(it) },
                onUpdateEveningEnabled = { viewModel.updateEveningReminderEnabled(it) },
                onUpdateFallbackEnabled = { viewModel.updateFallbackReminderEnabled(it) },
                onUpdateFallbackTime = { viewModel.updateFallbackTime(it) },
                onUpdateDailyEnabled = { viewModel.updateDailyReminderEnabled(it) },
                onUpdateDailyTime = { viewModel.updateDailyReminderTime(it) },
                onUpdateAutoOffWeekends = { viewModel.updateAutoOffWeekends(it) },
                onUpdateAutoOffHolidays = { viewModel.updateAutoOffHolidays(it) },
                onAddHolidayDate = { viewModel.addHolidayDate(it) },
                onRemoveHolidayDate = { viewModel.removeHolidayDate(it) },
                onExportPdfCurrentMonth = { viewModel.exportPdfCurrentMonth() },
                onExportPdfLast30Days = { viewModel.exportPdfLast30Days() },
                onExportPdfCustomRange = { start, end -> viewModel.exportPdfCustomRange(start, end) },
                onUpdatePdfSettings = { name, company, project, personnel -> 
                    viewModel.updatePdfSettings(name, company, project, personnel) 
                },
                onResetExportState = { viewModel.resetExportState() },
                hasNotificationPermission = hasNotificationPermission,
                hasLocationPermission = hasLocationPermission,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestLocationPermission = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                onOpenNotificationSettings = { openNotificationSettings(context) },
                onOpenAppSettings = { openAppSettings(context) },
                onOpenBatterySettings = { openBatterySettings(context) },
                onSendTestReminder = { viewModel.sendTestReminder() },
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
    onUpdateMorningEnabled: (Boolean) -> Unit,
    onUpdateEveningEnabled: (Boolean) -> Unit,
    onUpdateFallbackEnabled: (Boolean) -> Unit,
    onUpdateFallbackTime: (LocalTime) -> Unit,
    onUpdateDailyEnabled: (Boolean) -> Unit,
    onUpdateDailyTime: (LocalTime) -> Unit,
    onUpdateAutoOffWeekends: (Boolean) -> Unit,
    onUpdateAutoOffHolidays: (Boolean) -> Unit,
    onAddHolidayDate: (LocalDate) -> Unit,
    onRemoveHolidayDate: (LocalDate) -> Unit,
    onExportPdfCurrentMonth: () -> Unit,
    onExportPdfLast30Days: () -> Unit,
    onExportPdfCustomRange: (LocalDate, LocalDate) -> Unit,
    onUpdatePdfSettings: (String?, String?, String?, String?) -> Unit,
    onResetExportState: () -> Unit,
    hasNotificationPermission: Boolean,
    hasLocationPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onSendTestReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SettingsSection(title = "Einrichtung") {
            SetupSection(
                hasNotificationPermission = hasNotificationPermission,
                hasLocationPermission = hasLocationPermission,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onSendTestReminder = onSendTestReminder
            )
        }

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
                morningReminderEnabled = settings.morningReminderEnabled,
                morningWindowStart = settings.morningWindowStart,
                morningWindowEnd = settings.morningWindowEnd,
                eveningReminderEnabled = settings.eveningReminderEnabled,
                eveningWindowStart = settings.eveningWindowStart,
                eveningWindowEnd = settings.eveningWindowEnd,
                fallbackEnabled = settings.fallbackEnabled,
                fallbackTime = settings.fallbackTime,
                dailyReminderEnabled = settings.dailyReminderEnabled,
                dailyReminderTime = settings.dailyReminderTime,
                onUpdateMorningEnabled = onUpdateMorningEnabled,
                onUpdateMorningWindow = onUpdateMorningWindow,
                onUpdateEveningEnabled = onUpdateEveningEnabled,
                onUpdateEveningWindow = onUpdateEveningWindow,
                onUpdateFallbackEnabled = onUpdateFallbackEnabled,
                onUpdateFallbackTime = onUpdateFallbackTime,
                onUpdateDailyEnabled = onUpdateDailyEnabled,
                onUpdateDailyTime = onUpdateDailyTime
            )
        }

        // Location Settings Section
        SettingsSection(title = "Standort") {
            LocationSettingsSection(
                radiusMeters = (settings.locationRadiusKm * 1000),
                locationMode = "check_in_only",
                onUpdateRadius = onUpdateRadius,
                onUpdateLocationMode = onUpdateLocationMode
            )
        }

        // Non-working days section
        SettingsSection(title = "Freie Tage") {
            NonWorkingDaysSection(
                autoOffWeekends = settings.autoOffWeekends,
                autoOffHolidays = settings.autoOffHolidays,
                holidayDates = settings.holidayDates,
                onUpdateAutoOffWeekends = onUpdateAutoOffWeekends,
                onUpdateAutoOffHolidays = onUpdateAutoOffHolidays,
                onAddHolidayDate = onAddHolidayDate,
                onRemoveHolidayDate = onRemoveHolidayDate
            )
        }

        // Export Section
        SettingsSection(title = "Export") {
            ExportSection(
                uiState = uiState,
                onExportPdfCurrentMonth = onExportPdfCurrentMonth,
                onExportPdfLast30Days = onExportPdfLast30Days,
                onExportPdfCustomRange = onExportPdfCustomRange,
                pdfEmployeeName = settings.pdfEmployeeName,
                pdfCompany = settings.pdfCompany,
                pdfProject = settings.pdfProject,
                pdfPersonnelNumber = settings.pdfPersonnelNumber,
                onUpdatePdfSettings = onUpdatePdfSettings,
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
            Divider()
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
    morningReminderEnabled: Boolean,
    morningWindowStart: LocalTime,
    morningWindowEnd: LocalTime,
    eveningReminderEnabled: Boolean,
    eveningWindowStart: LocalTime,
    eveningWindowEnd: LocalTime,
    fallbackEnabled: Boolean,
    fallbackTime: LocalTime,
    dailyReminderEnabled: Boolean,
    dailyReminderTime: LocalTime,
    onUpdateMorningEnabled: (Boolean) -> Unit,
    onUpdateMorningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateEveningEnabled: (Boolean) -> Unit,
    onUpdateEveningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateFallbackEnabled: (Boolean) -> Unit,
    onUpdateFallbackTime: (LocalTime) -> Unit,
    onUpdateDailyEnabled: (Boolean) -> Unit,
    onUpdateDailyTime: (LocalTime) -> Unit
) {
    var showFallbackPicker by remember { mutableStateOf(false) }
    var showDailyPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Morning Window
        SettingsToggleRow(
            title = "Morgen-Erinnerung",
            checked = morningReminderEnabled,
            onCheckedChange = onUpdateMorningEnabled
        )
        
        if (morningReminderEnabled) {
            TimeRangePicker(
                label = "Zeitraum",
                startTime = morningWindowStart,
                endTime = morningWindowEnd,
                onTimeRangeChanged = { start, end ->
                    onUpdateMorningWindow(start.hour, start.minute, end.hour, end.minute)
                },
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        
        Divider()
        
        // Evening Window
        SettingsToggleRow(
            title = "Abend-Erinnerung",
            checked = eveningReminderEnabled,
            onCheckedChange = onUpdateEveningEnabled
        )
        
        if (eveningReminderEnabled) {
            TimeRangePicker(
                label = "Zeitraum",
                startTime = eveningWindowStart,
                endTime = eveningWindowEnd,
                onTimeRangeChanged = { start, end ->
                    onUpdateEveningWindow(start.hour, start.minute, end.hour, end.minute)
                },
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Divider()

        // Fallback Reminder
        SettingsToggleRow(
            title = "Fallback am Abend",
            checked = fallbackEnabled,
            onCheckedChange = onUpdateFallbackEnabled
        )
        SettingsTimeButtonRow(
            label = "Uhrzeit",
            time = fallbackTime,
            enabled = fallbackEnabled,
            onClick = { showFallbackPicker = true },
            modifier = Modifier.padding(start = 12.dp)
        )

        Divider()

        // Daily Reminder
        SettingsToggleRow(
            title = "Tägliche Erinnerung",
            supportingText = "Erinnert jeden Tag, auch wenn bereits erfasst",
            checked = dailyReminderEnabled,
            onCheckedChange = onUpdateDailyEnabled
        )
        SettingsTimeButtonRow(
            label = "Uhrzeit",
            time = dailyReminderTime,
            enabled = dailyReminderEnabled,
            onClick = { showDailyPicker = true },
            modifier = Modifier.padding(start = 12.dp)
        )
    }

    if (showFallbackPicker) {
        TimePickerDialog(
            initialTime = fallbackTime,
            onTimeSelected = {
                onUpdateFallbackTime(it)
                showFallbackPicker = false
            },
            onDismiss = { showFallbackPicker = false }
        )
    }

    if (showDailyPicker) {
        TimePickerDialog(
            initialTime = dailyReminderTime,
            onTimeSelected = {
                onUpdateDailyTime(it)
                showDailyPicker = false
            },
            onDismiss = { showDailyPicker = false }
        )
    }
}

@Composable
fun TimeRangePicker(
    label: String,
    startTime: LocalTime,
    endTime: LocalTime,
    onTimeRangeChanged: (LocalTime, LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecondaryActionButton(
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
            
            SecondaryActionButton(
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

        Divider()

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
fun SetupSection(
    hasNotificationPermission: Boolean,
    hasLocationPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onSendTestReminder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SetupRow(
            title = "Benachrichtigungen",
            status = if (hasNotificationPermission) "Aktiv" else "Nicht erlaubt",
            actionLabel = if (hasNotificationPermission) "Einstellungen" else "Erlauben",
            onAction = if (hasNotificationPermission) onOpenNotificationSettings else onRequestNotificationPermission,
            isOk = hasNotificationPermission
        )

        Divider()

        SetupRow(
            title = "Standort",
            status = if (hasLocationPermission) "Aktiv" else "Nicht erlaubt",
            actionLabel = if (hasLocationPermission) "Einstellungen" else "Erlauben",
            onAction = if (hasLocationPermission) onOpenAppSettings else onRequestLocationPermission,
            isOk = hasLocationPermission
        )

        Divider()

        SetupRow(
            title = "Akku-Optimierung",
            status = if (isIgnoringBatteryOptimizations) "Ausgenommen" else "Optimiert",
            actionLabel = "Optimierung deaktivieren",
            onAction = onOpenBatterySettings,
            isOk = isIgnoringBatteryOptimizations
        )

        Divider()

        SecondaryActionButton(
            onClick = onSendTestReminder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Test-Benachrichtigung")
        }
    }
}

@Composable
fun SetupRow(
    title: String,
    status: String,
    actionLabel: String,
    onAction: () -> Unit,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TertiaryActionButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    supportingText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsTimeButtonRow(
    label: String,
    time: LocalTime,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SecondaryActionButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Text(formatTime(time))
        }
    }
}

@Composable
fun NonWorkingDaysSection(
    autoOffWeekends: Boolean,
    autoOffHolidays: Boolean,
    holidayDates: Set<LocalDate>,
    onUpdateAutoOffWeekends: (Boolean) -> Unit,
    onUpdateAutoOffHolidays: (Boolean) -> Unit,
    onAddHolidayDate: (LocalDate) -> Unit,
    onRemoveHolidayDate: (LocalDate) -> Unit
) {
    var showHolidayPicker by remember { mutableStateOf(false) }
    val sortedHolidays = remember(holidayDates) { holidayDates.sorted() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsToggleRow(
            title = "Wochenenden automatisch frei",
            supportingText = "Setzt Tagtyp auf Frei und pausiert Erinnerungen",
            checked = autoOffWeekends,
            onCheckedChange = onUpdateAutoOffWeekends
        )

        Divider()

        SettingsToggleRow(
            title = "Feiertage automatisch frei",
            supportingText = "Manuelle Feiertage werden als frei behandelt",
            checked = autoOffHolidays,
            onCheckedChange = onUpdateAutoOffHolidays
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Feiertage",
                style = MaterialTheme.typography.bodyMedium
            )
            TertiaryActionButton(
                onClick = { showHolidayPicker = true },
                enabled = autoOffHolidays
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text("Hinzufügen")
            }
        }

        if (sortedHolidays.isEmpty()) {
            Text(
                text = "Keine Feiertage hinzugefügt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sortedHolidays.forEach { date ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(date),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveHolidayDate(date) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Feiertag entfernen"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHolidayPicker) {
        SettingsDatePickerDialog(
            initialDate = LocalDate.now(),
            onDateSelected = { date ->
                onAddHolidayDate(date)
                showHolidayPicker = false
            },
            onDismiss = { showHolidayPicker = false }
        )
    }
}

@Composable
fun ExportSection(
    uiState: SettingsUiState,
    onExportPdfCurrentMonth: () -> Unit,
    onExportPdfLast30Days: () -> Unit,
    onExportPdfCustomRange: (LocalDate, LocalDate) -> Unit,
    pdfEmployeeName: String?,
    pdfCompany: String?,
    pdfProject: String?,
    pdfPersonnelNumber: String?,
    onUpdatePdfSettings: (String?, String?, String?, String?) -> Unit,
    onResetState: () -> Unit
) {
    val context = LocalContext.current
    
    var showPdfSettingsDialog by remember { mutableStateOf(false) }
    var showPdfCustomRangeDialog by remember { mutableStateOf(false) }
    
    val employeeName = pdfEmployeeName.orEmpty()
    val company = pdfCompany.orEmpty()
    val project = pdfProject.orEmpty()
    val personnelNumber = pdfPersonnelNumber.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // PDF Export Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PDF Export",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            IconButton(onClick = { showPdfSettingsDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "PDF Settings"
                )
            }
        }
        
        // Name-Validierungs-Info
        if (employeeName.isBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ Name fehlt! Bitte zuerst in den Settings eingeben.",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // PDF Export Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrimaryActionButton(
                onClick = onExportPdfCurrentMonth,
                modifier = Modifier.weight(1f),
                enabled = employeeName.isNotBlank()
            ) {
                Text("Dieser Monat")
            }
            
            PrimaryActionButton(
                onClick = onExportPdfLast30Days,
                modifier = Modifier.weight(1f),
                enabled = employeeName.isNotBlank()
            ) {
                Text("30 Tage")
            }
            
            PrimaryActionButton(
                onClick = { showPdfCustomRangeDialog = true },
                modifier = Modifier.weight(1f),
                enabled = employeeName.isNotBlank()
            ) {
                Text("Benutzerdef.")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        when (uiState) {
            is SettingsUiState.Initial -> Unit
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
                val fileUri = uiState.fileUri
                val format = uiState.format
                var showShareDialog by remember { mutableStateOf(true) }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Export erfolgreich!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = "Format: ${format.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (showShareDialog) {
                    val mimeType = "application/pdf"
                    AlertDialog(
                        onDismissRequest = {
                            showShareDialog = false
                            onResetState()
                        },
                        title = { Text("${format.name} Export") },
                        text = {
                            Text(
                                "Die Datei wurde erstellt. Möchten Sie sie teilen?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            PrimaryActionButton(onClick = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = mimeType
                                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "MontageZeit Export (${format.name})")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Export teilen"))
                                showShareDialog = false
                                onResetState()
                            }) {
                                Text("Teilen")
                            }
                        },
                        dismissButton = {
                            TertiaryActionButton(onClick = {
                                showShareDialog = false
                                onResetState()
                            }) {
                                Text("Schließen")
                            }
                        }
                    )
                }
            }

            is SettingsUiState.ExportError -> {
                val error = uiState.message
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
    
    // PDF Settings Dialog
    if (showPdfSettingsDialog) {
        PdfSettingsDialog(
            initialEmployeeName = employeeName,
            initialCompany = company.ifBlank { null },
            initialProject = project.ifBlank { null },
            initialPersonnelNumber = personnelNumber.ifBlank { null },
            onSave = { name, comp, proj, pers ->
                onUpdatePdfSettings(name, comp, proj, pers)
                showPdfSettingsDialog = false
            },
            onDismiss = { showPdfSettingsDialog = false }
        )
    }
    
    // PDF Custom Range Dialog
    if (showPdfCustomRangeDialog) {
        PdfCustomRangeDialog(
            onDateRangeSelected = { start, end ->
                onExportPdfCustomRange(start, end)
                showPdfCustomRangeDialog = false
            },
            onDismiss = { showPdfCustomRangeDialog = false }
        )
    }
}



private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun openBatterySettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (directIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(directIntent)
    } else if (fallbackIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(fallbackIntent)
    }
}

private fun formatTime(time: LocalTime): String {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}

@Composable
fun SettingsDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(initialDate) }

    LaunchedEffect(Unit) {
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month +1, dayOfMonth)
                onDateSelected(selectedDate)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )
        datePickerDialog.setOnDismissListener { onDismiss() }
        datePickerDialog.show()
    }
}

@Composable
fun PdfSettingsDialog(
    initialEmployeeName: String,
    initialCompany: String?,
    initialProject: String?,
    initialPersonnelNumber: String?,
    onSave: (String?, String?, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var employeeName by remember { mutableStateOf(initialEmployeeName) }
    var company by remember { mutableStateOf(initialCompany ?: "") }
    var project by remember { mutableStateOf(initialProject ?: "") }
    var personnelNumber by remember { mutableStateOf(initialPersonnelNumber ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = employeeName,
                    onValueChange = { employeeName = it },
                    label = { Text("Mitarbeiter-Name *") },
                    placeholder = { Text("Max Mustermann") },
                    singleLine = true,
                    isError = employeeName.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Firma") },
                    placeholder = { Text("Musterfirma GmbH") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = project,
                    onValueChange = { project = it },
                    label = { Text("Projekt") },
                    placeholder = { Text("Projekt X") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = personnelNumber,
                    onValueChange = { personnelNumber = it },
                    label = { Text("Personalnummer") },
                    placeholder = { Text("12345") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "* Pflichtfeld",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = {
                    onSave(
                        employeeName.ifBlank { null },
                        company.ifBlank { null },
                        project.ifBlank { null },
                        personnelNumber.ifBlank { null }
                    )
                },
                enabled = employeeName.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TertiaryActionButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun PdfCustomRangeDialog(
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Benutzerdefinierter Zeitraum") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Start Date
                Text(
                    text = "Von",
                    style = MaterialTheme.typography.bodyMedium
                )
                SecondaryActionButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(formatDate(startDate))
                }
                
                // End Date
                Text(
                    text = "Bis",
                    style = MaterialTheme.typography.bodyMedium
                )
                SecondaryActionButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(formatDate(endDate))
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = {
                    onDateRangeSelected(startDate, endDate)
                },
                enabled = !startDate.isAfter(endDate)
            ) {
                Text("Exportieren")
            }
        },
        dismissButton = {
            TertiaryActionButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
    
    // Start Date Picker
    if (showStartDatePicker) {
        SettingsDatePickerDialog(
            initialDate = startDate,
            onDateSelected = {
                startDate = it
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    // End Date Picker
    if (showEndDatePicker) {
        SettingsDatePickerDialog(
            initialDate = endDate,
            onDateSelected = {
                endDate = it
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}
