@file:Suppress("LongMethod", "LongParameterList", "MagicNumber")

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.*
import de.montagezeit.app.ui.screen.export.ExportPreviewBottomSheet
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration

/**
 * Verbesserter SettingsScreen mit besserer Accessibility und neuen Komponenten
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.reminderSettings.collectAsStateWithLifecycle(initialValue = null)
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message.asString(context))
        viewModel.onSnackbarShown()
    }

    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission(context)
                isIgnoringBatteryOptimizations = checkBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MZScreenScaffold(
        title = stringResource(R.string.settings_title),
        navigationIcon = {
            FilledTonalIconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_close)
                )
            }
        },
        snackbarHost = { MZSnackbarHost(hostState = snackbarHostState) }
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
                onUpdateWorkStart = { viewModel.updateWorkStart(it) },
                onUpdateWorkEnd = { viewModel.updateWorkEnd(it) },
                onUpdateBreakMinutes = { viewModel.updateBreakMinutes(it) },
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
                onExportCsvCurrentMonth = { viewModel.exportCsvCurrentMonth() },
                onExportPdfLast30Days = { viewModel.exportPdfLast30Days() },
                onExportCsvLast30Days = { viewModel.exportCsvLast30Days() },
                onExportPdfCustomRange = { start, end -> viewModel.exportPdfCustomRange(start, end) },
                onExportCsvCustomRange = { start, end -> viewModel.exportCsvCustomRange(start, end) },
                onOpenExportPreview = { start, end ->
                    previewRange = start to end
                    showPreviewSheet = true
                },
                onUpdatePdfSettings = { name, company, project, personnel ->
                    viewModel.updatePdfSettings(name, company, project, personnel)
                },
                onUpdateDailyTargetHours = { viewModel.updateDailyTargetHours(it) },
                onResetExportState = { viewModel.resetExportState() },
                hasNotificationPermission = hasNotificationPermission,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenNotificationSettings = { openNotificationSettings(context) },
                onOpenBatterySettings = { openBatterySettings(context) },
                onSendTestReminder = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.sendTestReminder()
                },
                onNavigateToRoute = onNavigateToRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MZLoadingState(message = stringResource(R.string.loading))
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: de.montagezeit.app.data.preferences.ReminderSettings,
    uiState: SettingsUiState,
    onUpdateMorningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateEveningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateWorkStart: (LocalTime) -> Unit,
    onUpdateWorkEnd: (LocalTime) -> Unit,
    onUpdateBreakMinutes: (Int) -> Unit,
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
    onExportCsvCurrentMonth: () -> Unit,
    onExportPdfLast30Days: () -> Unit,
    onExportCsvLast30Days: () -> Unit,
    onExportPdfCustomRange: (LocalDate, LocalDate) -> Unit,
    onExportCsvCustomRange: (LocalDate, LocalDate) -> Unit,
    onOpenExportPreview: (LocalDate, LocalDate) -> Unit,
    onUpdatePdfSettings: (String?, String?, String?, String?) -> Unit,
    onUpdateDailyTargetHours: (Double) -> Unit,
    onResetExportState: () -> Unit,
    hasNotificationPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onSendTestReminder: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var workTimesExpanded by rememberSaveable { mutableStateOf(true) }
    var overtimeExpanded by rememberSaveable { mutableStateOf(false) }
    var remindersExpanded by rememberSaveable { mutableStateOf(true) }
    var nonWorkingExpanded by rememberSaveable { mutableStateOf(false) }
    var exportExpanded by rememberSaveable { mutableStateOf(false) }
    val enabledReminderCount = listOf(
        settings.morningReminderEnabled,
        settings.eveningReminderEnabled,
        settings.fallbackEnabled,
        settings.dailyReminderEnabled
    ).count { it }
    val reminderError = (uiState as? SettingsUiState.ReminderError)?.message
    val defaultWorkMinutes = remember(settings.workStart, settings.workEnd, settings.breakMinutes) {
        (Duration.between(settings.workStart, settings.workEnd).toMinutes().toInt() - settings.breakMinutes)
            .coerceAtLeast(0)
    }
    val exportProfileReady = settings.pdfEmployeeName.orEmpty().isNotBlank()

    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MZHeroPanel {
            MZSectionIntro(
                eyebrow = stringResource(R.string.settings_title),
                title = stringResource(R.string.settings_dashboard_title),
                supportingText = stringResource(R.string.settings_dashboard_subtitle)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MZMetricChip(
                    label = stringResource(R.string.settings_reminders),
                    value = "$enabledReminderCount/4",
                    modifier = Modifier.weight(1f)
                )
                MZMetricChip(
                    label = stringResource(R.string.settings_metric_schedule),
                    value = formatMinutesAsHoursMinutes(defaultWorkMinutes),
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.secondary
                )
                MZMetricChip(
                    label = stringResource(R.string.settings_metric_profile),
                    value = if (exportProfileReady) {
                        stringResource(R.string.status_active)
                    } else {
                        stringResource(R.string.settings_export_name_missing)
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = if (exportProfileReady) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }

        MZSectionHeader(title = stringResource(R.string.settings_group_general))

        SetupSection(
            hasNotificationPermission = hasNotificationPermission,
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
            enabledReminderCount = enabledReminderCount,
            workStartLabel = Formatters.formatTime(settings.workStart),
            workEndLabel = Formatters.formatTime(settings.workEnd),
            onRequestNotificationPermission = onRequestNotificationPermission,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenBatterySettings = onOpenBatterySettings,
            onSendTestReminder = onSendTestReminder
        )

        WorkTimesSection(
            workStart = settings.workStart,
            workEnd = settings.workEnd,
            breakMinutes = settings.breakMinutes,
            expanded = workTimesExpanded,
            onExpandedChange = { workTimesExpanded = it },
            onUpdateWorkStart = onUpdateWorkStart,
            onUpdateWorkEnd = onUpdateWorkEnd,
            onUpdateBreakMinutes = onUpdateBreakMinutes
        )

        OvertimeTargetsSection(
            dailyTargetHours = settings.dailyTargetHours,
            expanded = overtimeExpanded,
            onExpandedChange = { overtimeExpanded = it },
            onUpdateDailyTarget = onUpdateDailyTargetHours
        )

        MZSectionHeader(title = stringResource(R.string.settings_group_automation))

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
            onUpdateDailyTime = onUpdateDailyTime,
            reminderError = reminderError,
            expanded = remindersExpanded,
            onExpandedChange = { remindersExpanded = it }
        )

        NonWorkingDaysSection(
            autoOffWeekends = settings.autoOffWeekends,
            autoOffHolidays = settings.autoOffHolidays,
            holidayDates = settings.holidayDates,
            onUpdateAutoOffWeekends = onUpdateAutoOffWeekends,
            onUpdateAutoOffHolidays = onUpdateAutoOffHolidays,
            onAddHolidayDate = onAddHolidayDate,
            onRemoveHolidayDate = onRemoveHolidayDate,
            expanded = nonWorkingExpanded,
            onExpandedChange = { nonWorkingExpanded = it }
        )

        MZSectionHeader(title = stringResource(R.string.settings_group_data))

        ExportSection(
            uiState = uiState,
            onExportPdfCurrentMonth = onExportPdfCurrentMonth,
            onExportCsvCurrentMonth = onExportCsvCurrentMonth,
            onExportPdfLast30Days = onExportPdfLast30Days,
            onExportCsvLast30Days = onExportCsvLast30Days,
            onExportPdfCustomRange = onExportPdfCustomRange,
            onExportCsvCustomRange = onExportCsvCustomRange,
            onOpenExportPreview = onOpenExportPreview,
            pdfEmployeeName = settings.pdfEmployeeName,
            pdfCompany = settings.pdfCompany,
            pdfProject = settings.pdfProject,
            pdfPersonnelNumber = settings.pdfPersonnelNumber,
            onUpdatePdfSettings = onUpdatePdfSettings,
            onResetState = onResetExportState,
            expanded = exportExpanded,
            onExpandedChange = { exportExpanded = it }
        )

        SettingsDeveloperSection(
            onNavigateToRoute = onNavigateToRoute,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
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

private fun openBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

private fun formatMinutesAsHoursMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0) {
        "${hours}h"
    } else {
        "${hours}h ${minutes}m"
    }
}
