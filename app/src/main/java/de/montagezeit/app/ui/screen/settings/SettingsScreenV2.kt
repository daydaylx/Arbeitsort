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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import de.montagezeit.app.R
import de.montagezeit.app.ui.common.TimePickerDialog
import de.montagezeit.app.ui.components.*
import de.montagezeit.app.ui.screen.export.ExportPreviewBottomSheet
import de.montagezeit.app.ui.util.LocationPermissionHelper
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Verbesserter SettingsScreen mit besserer Accessibility und neuen Komponenten
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenV2(
    viewModel: SettingsViewModel = hiltViewModel(),
    onOpenEditSheet: (LocalDate, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.reminderSettings.collectAsState(initial = null)
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var hasLocationPermission by remember {
        mutableStateOf(LocationPermissionHelper.hasAnyLocationPermission(context))
    }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkBatteryOptimizations(context))
    }
    var previewRange by remember { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var showPreviewSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = LocationPermissionHelper.isPermissionGranted(permissions)
        hasLocationPermission = granted
        if (granted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission(context)
                hasLocationPermission = LocationPermissionHelper.hasAnyLocationPermission(context)
                isIgnoringBatteryOptimizations = checkBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings_title),
                        modifier = Modifier.semantics { heading() }
                    )
                }
            )
        }
    ) { paddingValues ->
        if (settings != null) {
            SettingsContentV2(
                settings = settings!!,
                uiState = uiState,
                onUpdateMorningWindow = { startH, startM, endH, endM ->
                    viewModel.updateMorningWindow(startH, startM, endH, endM)
                },
                onUpdateEveningWindow = { startH, startM, endH, endM ->
                    viewModel.updateEveningWindow(startH, startM, endH, endM)
                },
                onUpdateRadius = { viewModel.updateRadiusMeters(it) },
                onUpdateDefaultDayLocationLabel = { viewModel.updateDefaultDayLocationLabel(it) },
                onUpdatePreferGpsLocation = { viewModel.updatePreferGpsLocation(it) },
                onUpdateFallbackOnLowAccuracy = { viewModel.updateFallbackOnLowAccuracy(it) },
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
                onOpenExportPreview = { start, end ->
                    previewRange = start to end
                    showPreviewSheet = true
                },
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
                    locationPermissionLauncher.launch(LocationPermissionHelper.locationPermissions)
                },
                onOpenNotificationSettings = { openNotificationSettings(context) },
                onOpenAppSettings = { openAppSettings(context) },
                onOpenBatterySettings = { openBatterySettings(context) },
                onSendTestReminder = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.sendTestReminder() 
                },
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            )
        }
    }

    val activeRange = previewRange
    if (showPreviewSheet && activeRange != null) {
        ExportPreviewBottomSheet(
            range = activeRange,
            onDismiss = { showPreviewSheet = false },
            onOpenEditSheet = onOpenEditSheet
        )
    }
}

@Composable
fun SettingsContentV2(
    settings: de.montagezeit.app.data.preferences.ReminderSettings,
    uiState: SettingsUiState,
    onUpdateMorningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateEveningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateRadius: (Int) -> Unit,
    onUpdateDefaultDayLocationLabel: (String) -> Unit,
    onUpdatePreferGpsLocation: (Boolean) -> Unit,
    onUpdateFallbackOnLowAccuracy: (Boolean) -> Unit,
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
    onOpenExportPreview: (LocalDate, LocalDate) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Setup Section
        SetupSectionV2(
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

        // Work Times Section
        WorkTimesSectionV2(
            workStart = settings.workStart,
            workEnd = settings.workEnd,
            breakMinutes = settings.breakMinutes
        )

        // Reminder Settings Section
        ReminderSettingsSectionV2(
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

        // Location Settings Section
        LocationSettingsSectionV2(
            radiusMeters = (settings.locationRadiusKm * 1000),
            defaultDayLocationLabel = settings.defaultDayLocationLabel,
            preferGpsLocation = settings.preferGpsLocation,
            fallbackOnLowAccuracy = settings.fallbackOnLowAccuracy,
            onUpdateRadius = onUpdateRadius,
            onUpdateDefaultDayLocationLabel = onUpdateDefaultDayLocationLabel,
            onUpdatePreferGpsLocation = onUpdatePreferGpsLocation,
            onUpdateFallbackOnLowAccuracy = onUpdateFallbackOnLowAccuracy
        )

        // Non-working days section
        NonWorkingDaysSectionV2(
            autoOffWeekends = settings.autoOffWeekends,
            autoOffHolidays = settings.autoOffHolidays,
            holidayDates = settings.holidayDates,
            onUpdateAutoOffWeekends = onUpdateAutoOffWeekends,
            onUpdateAutoOffHolidays = onUpdateAutoOffHolidays,
            onAddHolidayDate = onAddHolidayDate,
            onRemoveHolidayDate = onRemoveHolidayDate
        )

        // Export Section
        ExportSectionV2(
            uiState = uiState,
            onExportPdfCurrentMonth = onExportPdfCurrentMonth,
            onExportPdfLast30Days = onExportPdfLast30Days,
            onExportPdfCustomRange = onExportPdfCustomRange,
            onOpenExportPreview = onOpenExportPreview,
            pdfEmployeeName = settings.pdfEmployeeName,
            pdfCompany = settings.pdfCompany,
            pdfProject = settings.pdfProject,
            pdfPersonnelNumber = settings.pdfPersonnelNumber,
            onUpdatePdfSettings = onUpdatePdfSettings,
            onResetState = onResetExportState
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SetupSectionV2(
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
    val allPermissionsGranted = hasNotificationPermission && hasLocationPermission
    
    MZStatusCard(
        title = stringResource(R.string.settings_setup_title),
        status = if (allPermissionsGranted) StatusType.SUCCESS else StatusType.WARNING
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SetupRowV2(
                title = stringResource(R.string.settings_notifications),
                status = if (hasNotificationPermission) 
                    stringResource(R.string.status_active) 
                else 
                    stringResource(R.string.status_not_allowed),
                actionLabel = if (hasNotificationPermission) 
                    stringResource(R.string.action_settings) 
                else 
                    stringResource(R.string.action_allow),
                onAction = if (hasNotificationPermission) onOpenNotificationSettings else onRequestNotificationPermission,
                isOk = hasNotificationPermission
            )

            Divider()

            SetupRowV2(
                title = stringResource(R.string.settings_location),
                status = if (hasLocationPermission) 
                    stringResource(R.string.status_active) 
                else 
                    stringResource(R.string.status_not_allowed),
                actionLabel = if (hasLocationPermission) 
                    stringResource(R.string.action_settings) 
                else 
                    stringResource(R.string.action_allow),
                onAction = if (hasLocationPermission) onOpenAppSettings else onRequestLocationPermission,
                isOk = hasLocationPermission
            )

            Divider()

            SetupRowV2(
                title = stringResource(R.string.settings_battery_optimization),
                status = if (isIgnoringBatteryOptimizations) 
                    stringResource(R.string.status_excluded) 
                else 
                    stringResource(R.string.status_optimized),
                actionLabel = stringResource(R.string.action_disable_optimization),
                onAction = onOpenBatterySettings,
                isOk = isIgnoringBatteryOptimizations
            )

            if (hasNotificationPermission) {
                Divider()
                
                MZSecondaryButton(
                    onClick = onSendTestReminder,
                    icon = Icons.Default.Notifications,
                    content = { Text(stringResource(R.string.action_test_notification)) }
                )
            }
        }
    }
}

@Composable
fun SetupRowV2(
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
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        MZTertiaryButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
fun WorkTimesSectionV2(
    workStart: LocalTime,
    workEnd: LocalTime,
    breakMinutes: Int
) {
    MZCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MZSectionHeader(title = stringResource(R.string.settings_work_times))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeDisplay(
                    label = stringResource(R.string.label_work_start),
                    time = formatTime(workStart)
                )
                TimeDisplay(
                    label = stringResource(R.string.label_work_end),
                    time = formatTime(workEnd)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_break),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.format_minutes, breakMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Text(
                text = stringResource(R.string.work_times_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeDisplay(label: String, time: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = time,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ReminderSettingsSectionV2(
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

    MZCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MZSectionHeader(title = stringResource(R.string.settings_reminders))

            // Morning Window
            SettingsToggleRowV2(
                title = stringResource(R.string.reminder_morning),
                checked = morningReminderEnabled,
                onCheckedChange = onUpdateMorningEnabled
            )
            
            AnimatedVisibility(visible = morningReminderEnabled) {
                TimeRangePickerV2(
                    label = stringResource(R.string.label_time_range),
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
            SettingsToggleRowV2(
                title = stringResource(R.string.reminder_evening),
                checked = eveningReminderEnabled,
                onCheckedChange = onUpdateEveningEnabled
            )
            
            AnimatedVisibility(visible = eveningReminderEnabled) {
                TimeRangePickerV2(
                    label = stringResource(R.string.label_time_range),
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
            SettingsToggleRowV2(
                title = stringResource(R.string.reminder_fallback),
                checked = fallbackEnabled,
                onCheckedChange = onUpdateFallbackEnabled
            )
            SettingsTimeButtonRowV2(
                label = stringResource(R.string.label_time),
                time = fallbackTime,
                enabled = fallbackEnabled,
                onClick = { showFallbackPicker = true },
                modifier = Modifier.padding(start = 12.dp)
            )

            Divider()

            // Daily Reminder
            SettingsToggleRowV2(
                title = stringResource(R.string.reminder_daily),
                supportingText = stringResource(R.string.reminder_daily_description),
                checked = dailyReminderEnabled,
                onCheckedChange = onUpdateDailyEnabled
            )
            SettingsTimeButtonRowV2(
                label = stringResource(R.string.label_time),
                time = dailyReminderTime,
                enabled = dailyReminderEnabled,
                onClick = { showDailyPicker = true },
                modifier = Modifier.padding(start = 12.dp)
            )
        }
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
fun TimeRangePickerV2(
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
            MZSecondaryButton(
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
            
            MZSecondaryButton(
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
fun LocationSettingsSectionV2(
    radiusMeters: Int,
    defaultDayLocationLabel: String,
    preferGpsLocation: Boolean,
    fallbackOnLowAccuracy: Boolean,
    onUpdateRadius: (Int) -> Unit,
    onUpdateDefaultDayLocationLabel: (String) -> Unit,
    onUpdatePreferGpsLocation: (Boolean) -> Unit,
    onUpdateFallbackOnLowAccuracy: (Boolean) -> Unit
) {
    MZCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MZSectionHeader(title = stringResource(R.string.settings_location))

            OutlinedTextField(
                value = defaultDayLocationLabel,
                onValueChange = onUpdateDefaultDayLocationLabel,
                label = { Text(stringResource(R.string.label_default_city)) },
                placeholder = { Text(stringResource(R.string.placeholder_city)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = stringResource(R.string.default_city_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsToggleRowV2(
                title = stringResource(R.string.label_prefer_gps),
                supportingText = stringResource(R.string.prefer_gps_description),
                checked = preferGpsLocation,
                onCheckedChange = onUpdatePreferGpsLocation
            )

            SettingsToggleRowV2(
                title = stringResource(R.string.label_fallback_low_accuracy),
                supportingText = stringResource(R.string.fallback_low_accuracy_description),
                checked = fallbackOnLowAccuracy,
                onCheckedChange = onUpdateFallbackOnLowAccuracy
            )

            Divider()

            // Radius
            Text(
                text = stringResource(R.string.label_radius, radiusMeters / 1000),
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
        }
    }
}

@Composable
fun NonWorkingDaysSectionV2(
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

    MZCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MZSectionHeader(title = stringResource(R.string.settings_non_working_days))

            SettingsToggleRowV2(
                title = stringResource(R.string.label_auto_off_weekends),
                supportingText = stringResource(R.string.auto_off_weekends_description),
                checked = autoOffWeekends,
                onCheckedChange = onUpdateAutoOffWeekends
            )

            Divider()

            SettingsToggleRowV2(
                title = stringResource(R.string.label_auto_off_holidays),
                supportingText = stringResource(R.string.auto_off_holidays_description),
                checked = autoOffHolidays,
                onCheckedChange = onUpdateAutoOffHolidays
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_holidays),
                    style = MaterialTheme.typography.bodyMedium
                )
                MZSecondaryButton(
                    onClick = { showHolidayPicker = true },
                    enabled = autoOffHolidays
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(stringResource(R.string.action_add))
                }
            }

            if (sortedHolidays.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_holidays_added),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedHolidays.forEach { date ->
                        HolidayRow(
                            date = date,
                            onRemove = { onRemoveHolidayDate(date) }
                        )
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
fun HolidayRow(
    date: LocalDate,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDate(date),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_remove_holiday)
            )
        }
    }
}

@Composable
fun ExportSectionV2(
    uiState: SettingsUiState,
    onExportPdfCurrentMonth: () -> Unit,
    onExportPdfLast30Days: () -> Unit,
    onExportPdfCustomRange: (LocalDate, LocalDate) -> Unit,
    onOpenExportPreview: (LocalDate, LocalDate) -> Unit,
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

    MZCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MZSectionHeader(
                    title = stringResource(R.string.settings_export),
                    action = {
                        IconButton(onClick = { showPdfSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_pdf_settings)
                            )
                        }
                    }
                )
            }
            
            // Name validation warning
            if (employeeName.isBlank()) {
                MZStatusBadge(
                    text = stringResource(R.string.warning_name_missing),
                    type = StatusType.ERROR,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Export Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MZPrimaryButton(
                    onClick = onExportPdfCurrentMonth,
                    modifier = Modifier.weight(1f),
                    enabled = employeeName.isNotBlank(),
                    content = { Text(stringResource(R.string.export_current_month)) }
                )
                
                MZPrimaryButton(
                    onClick = onExportPdfLast30Days,
                    modifier = Modifier.weight(1f),
                    enabled = employeeName.isNotBlank(),
                    content = { Text(stringResource(R.string.export_30_days)) }
                )
                
                MZSecondaryButton(
                    onClick = { showPdfCustomRangeDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = employeeName.isNotBlank(),
                    content = { Text(stringResource(R.string.export_custom)) }
                )
            }
            
            // Export Status
            when (uiState) {
                is SettingsUiState.Initial -> Unit
                is SettingsUiState.Exporting -> {
                    MZLoadingState(message = stringResource(R.string.exporting))
                }

                is SettingsUiState.ExportSuccess -> {
                    ExportSuccessCard(
                        format = uiState.format,
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uiState.fileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                putExtra(Intent.EXTRA_SUBJECT, "MontageZeit Export")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, 
                                context.getString(R.string.share_export)))
                            onResetState()
                        },
                        onDismiss = onResetState
                    )
                }

                is SettingsUiState.ExportError -> {
                    MZStatusBadge(
                        text = uiState.message,
                        type = StatusType.ERROR,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // Dialogs
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
    
    if (showPdfCustomRangeDialog) {
        PdfCustomRangeDialog(
            onDateRangeSelected = { start, end ->
                onExportPdfCustomRange(start, end)
                showPdfCustomRangeDialog = false
            },
            onPreviewRangeSelected = { start, end ->
                onOpenExportPreview(start, end)
                showPdfCustomRangeDialog = false
            },
            onDismiss = { showPdfCustomRangeDialog = false }
        )
    }
}

@Composable
fun ExportSuccessCard(
    format: ExportFormat,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    MZStatusCard(
        title = stringResource(R.string.export_success_title),
        status = StatusType.SUCCESS
    ) {
        Text(
            text = stringResource(R.string.export_success_format, format.name),
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MZPrimaryButton(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                content = { Text(stringResource(R.string.action_share)) }
            )
            MZTertiaryButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}

@Composable
fun SettingsToggleRowV2(
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
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
fun SettingsTimeButtonRowV2(
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
        MZSecondaryButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Text(formatTime(time))
        }
    }
}

// Helper Functions

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
