package de.montagezeit.app.ui.screen.today

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton
import de.montagezeit.app.ui.components.*
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Verbesserter TodayScreen mit besserer Accessibility und visuellem Design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreenV2(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenEditSheet: (LocalDate) -> Unit,
    onOpenWeekView: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedEntry by viewModel.selectedEntry.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weekDaysUi by viewModel.weekDaysUi.collectAsStateWithLifecycle()
    val weekStats by viewModel.weekStats.collectAsStateWithLifecycle()
    val monthStats by viewModel.monthStats.collectAsStateWithLifecycle()
    val isOvertimeConfigured by viewModel.isOvertimeConfigured.collectAsStateWithLifecycle()
    val overtimeYearDisplay by viewModel.overtimeYearDisplay.collectAsStateWithLifecycle()
    val overtimeMonthDisplay by viewModel.overtimeMonthDisplay.collectAsStateWithLifecycle()
    val overtimeYearActualDisplay by viewModel.overtimeYearActualDisplay.collectAsStateWithLifecycle()
    val overtimeYearTargetDisplay by viewModel.overtimeYearTargetDisplay.collectAsStateWithLifecycle()
    val overtimeYearCountedDays by viewModel.overtimeYearCountedDays.collectAsStateWithLifecycle()
    val overtimeYearOffDayTravelDisplay by viewModel.overtimeYearOffDayTravelDisplay.collectAsStateWithLifecycle()
    val overtimeYearOffDayTravelDays by viewModel.overtimeYearOffDayTravelDays.collectAsStateWithLifecycle()
    val showDailyCheckInDialog by viewModel.showDailyCheckInDialog.collectAsStateWithLifecycle()
    val dailyCheckInLocationInput by viewModel.dailyCheckInLocationInput.collectAsStateWithLifecycle()
    val showDayLocationDialog by viewModel.showDayLocationDialog.collectAsStateWithLifecycle()
    val dayLocationInput by viewModel.dayLocationInput.collectAsStateWithLifecycle()
    val loadingActions by viewModel.loadingActions.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val currentEntry = (uiState as? TodayUiState.Success)?.entry ?: selectedEntry
    val isViewingPastDay = selectedDate != LocalDate.now()
    val snackbarHostState = remember { SnackbarHostState() }

    val isDailyCheckInLoading = loadingActions.contains(TodayAction.DAILY_MANUAL_CHECK_IN)
    val isConfirmOffdayLoading = loadingActions.contains(TodayAction.CONFIRM_OFFDAY)
    val isUpdateDayLocationLoading = loadingActions.contains(TodayAction.UPDATE_DAY_LOCATION)

    val onOpenDailyCheckInDialogAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.openDailyCheckInDialog()
    }
    val onConfirmOffDayAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.onConfirmOffDay()
    }

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message.asString(context))
        viewModel.onSnackbarShown()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.today_title),
                        modifier = Modifier.semantics { heading() }
                    )
                },
                actions = {
                    if (isViewingPastDay) {
                        TextButton(onClick = { viewModel.selectDate(LocalDate.now()) }) {
                            Text(stringResource(R.string.week_back_to_today))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            StickyTodayActionBarV2(
                entry = currentEntry,
                isDailyCheckInLoading = isDailyCheckInLoading,
                isConfirmOffdayLoading = isConfirmOffdayLoading,
                onOpenDailyCheckInDialog = onOpenDailyCheckInDialogAction,
                onConfirmOffDay = onConfirmOffDayAction
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val errorState = uiState as? TodayUiState.Error
            val showInitialLoading = uiState is TodayUiState.Loading && currentEntry == null
            val showFullscreenError = errorState != null && currentEntry == null

            when {
                showFullscreenError && errorState != null -> {
                    MZErrorState(
                        message = errorState.message.asString(context),
                        onRetry = { viewModel.onResetError() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                showInitialLoading -> {
                    MZLoadingState(
                        message = stringResource(R.string.loading),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    TodayContentV2(
                        entry = currentEntry,
                        selectedDate = selectedDate,
                        weekDaysUi = weekDaysUi,
                        weekStats = weekStats,
                        monthStats = monthStats,
                        isOvertimeConfigured = isOvertimeConfigured,
                        overtimeYearDisplay = overtimeYearDisplay,
                        overtimeMonthDisplay = overtimeMonthDisplay,
                        overtimeYearActualDisplay = overtimeYearActualDisplay,
                        overtimeYearTargetDisplay = overtimeYearTargetDisplay,
                        overtimeYearCountedDays = overtimeYearCountedDays,
                        overtimeYearOffDayTravelDisplay = overtimeYearOffDayTravelDisplay,
                        overtimeYearOffDayTravelDays = overtimeYearOffDayTravelDays,
                        onSelectDay = { viewModel.selectDate(it) },
                        onEditDayLocation = {
                            viewModel.openDayLocationDialog()
                        },
                        onEditToday = {
                            viewModel.ensureTodayEntryThen {
                                onOpenEditSheet(selectedDate)
                            }
                        },
                        onOpenWeekView = onOpenWeekView
                    )
                }
            }

            if (showDayLocationDialog) {
                DayLocationDialogV2(
                    input = dayLocationInput,
                    isLoading = isUpdateDayLocationLoading,
                    onInputChange = { viewModel.onDayLocationInputChanged(it) },
                    onDismiss = { viewModel.onDismissDayLocationDialog() },
                    onConfirm = { viewModel.submitDayLocationUpdate() }
                )
            }

            if (showDailyCheckInDialog) {
                DailyManualCheckInDialogV2(
                    input = dailyCheckInLocationInput,
                    isLoading = isDailyCheckInLoading,
                    onInputChange = { viewModel.onDailyCheckInLocationChanged(it) },
                    onDismiss = { viewModel.onDismissDailyCheckInDialog() },
                    onConfirm = { viewModel.submitDailyManualCheckIn() }
                )
            }
        }
    }
}

@Composable
private fun StickyTodayActionBarV2(
    entry: WorkEntry?,
    isDailyCheckInLoading: Boolean,
    isConfirmOffdayLoading: Boolean,
    onOpenDailyCheckInDialog: () -> Unit,
    onConfirmOffDay: () -> Unit
) {
    val isCompleted = entry?.confirmedWorkDay == true
    if (isCompleted) {
        return
    }
    val showOffdayAction = true

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showOffdayAction) {
                SecondaryActionButton(
                    onClick = onConfirmOffDay,
                    isLoading = isConfirmOffdayLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_confirm_offday))
                }
            }

            PrimaryActionButton(
                onClick = onOpenDailyCheckInDialog,
                isLoading = isDailyCheckInLoading,
                modifier = if (showOffdayAction) Modifier.weight(1f) else Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_daily_manual_check_in))
            }
        }
    }
}

@Composable
private fun TodayContentV2(
    entry: WorkEntry?,
    selectedDate: LocalDate,
    weekDaysUi: List<WeekDayUi>,
    weekStats: WeekStats?,
    monthStats: MonthStats?,
    isOvertimeConfigured: Boolean,
    overtimeYearDisplay: String,
    overtimeMonthDisplay: String?,
    overtimeYearActualDisplay: String,
    overtimeYearTargetDisplay: String,
    overtimeYearCountedDays: Int,
    overtimeYearOffDayTravelDisplay: String,
    overtimeYearOffDayTravelDays: Int,
    onSelectDay: (LocalDate) -> Unit,
    onEditDayLocation: () -> Unit,
    onEditToday: () -> Unit,
    onOpenWeekView: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (weekDaysUi.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            WeekOverviewRow(
                weekDays = weekDaysUi,
                onSelectDay = onSelectDay,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        StatusCardV2(
            entry = entry,
            date = selectedDate,
            onEditToday = onEditToday,
            onEditDayLocation = onEditDayLocation
        )

        OvertimeCardV2(
            isConfigured = isOvertimeConfigured,
            yearDisplay = overtimeYearDisplay,
            monthDisplay = overtimeMonthDisplay,
            yearActualDisplay = overtimeYearActualDisplay,
            yearTargetDisplay = overtimeYearTargetDisplay,
            yearCountedDays = overtimeYearCountedDays,
            yearOffDayTravelDisplay = overtimeYearOffDayTravelDisplay,
            yearOffDayTravelDays = overtimeYearOffDayTravelDays
        )
        
        if (entry != null && entry.dayType == DayType.WORK) {
            WorkHoursCardV2(entry = entry)
        }

        if (weekStats != null || monthStats != null) {
            StatisticsDashboardV2(
                weekStats = weekStats,
                monthStats = monthStats,
                onOpenWeekView = onOpenWeekView
            )
        }

        } // end inner Column
    }
}

@Composable
private fun OvertimeCardV2(
    isConfigured: Boolean,
    yearDisplay: String,
    monthDisplay: String?,
    yearActualDisplay: String,
    yearTargetDisplay: String,
    yearCountedDays: Int,
    yearOffDayTravelDisplay: String,
    yearOffDayTravelDays: Int
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    MZCard(
        modifier = Modifier.clickable(
            enabled = isConfigured,
            onClick = { isExpanded = !isExpanded }
        )
    ) {
        Column {
            if (!isConfigured) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.overtime_not_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            Text(
                text = stringResource(
                    R.string.overtime_month_value,
                    monthDisplay ?: formatSignedZeroHours()
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.overtime_year_value, yearDisplay),
                style = MaterialTheme.typography.bodyLarge
            )

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.overtime_actual_target,
                            yearActualDisplay,
                            yearTargetDisplay
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.overtime_counted_days, yearCountedDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (yearOffDayTravelDays > 0) {
                        Text(
                            text = stringResource(
                                R.string.overtime_offday_travel_year,
                                yearOffDayTravelDisplay,
                                yearOffDayTravelDays
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatSignedZeroHours(): String = String.format(Locale.GERMAN, "%+.2fh", 0.0)

@Composable
private fun StatusCardV2(
    entry: WorkEntry?,
    date: LocalDate,
    onEditToday: () -> Unit,
    onEditDayLocation: () -> Unit
) {
    val status = when {
        entry?.confirmedWorkDay == true -> StatusType.SUCCESS
        else -> StatusType.INFO
    }

    MZStatusCard(
        title = remember(date) { date.format(todayCurrentDateFormatter) },
        status = status,
        onClick = onEditToday
    ) {
        entry?.let {
            DayTypeRow(dayType = it.dayType)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (entry != null) {
            val dayLocationLabel = entry.dayLocationLabel.trim()
            if (dayLocationLabel.isNotEmpty()) {
                MZInfoRow(
                    icon = Icons.Default.LocationOn,
                    text = dayLocationLabel,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.today_day_location_unset),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextButton(
                onClick = onEditDayLocation,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.action_change_location))
            }
        } else {
            Text(
                text = stringResource(R.string.today_no_check_in),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

    }
}

@Composable
private fun DayTypeRow(dayType: DayType) {
    val (icon, text, color) = when (dayType) {
        DayType.WORK -> Triple(
            Icons.Default.Work,
            stringResource(R.string.day_type_work),
            MaterialTheme.colorScheme.primary
        )
        DayType.OFF -> Triple(
            Icons.Default.FreeBreakfast,
            stringResource(R.string.day_type_off),
            MaterialTheme.colorScheme.secondary
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WorkHoursCardV2(entry: WorkEntry) {
    val (workMinutes, travelMinutes, totalMinutes) = remember(entry) {
        Triple(
            TimeCalculator.calculateWorkMinutes(entry),
            TimeCalculator.calculateTravelMinutes(entry),
            TimeCalculator.calculatePaidTotalMinutes(entry)
        )
    }

    MZCard {
        Column {
            MZSectionHeader(title = stringResource(R.string.work_hours_title))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total_paid_hours),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatMinutes(totalMinutes),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (travelMinutes > 0) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.work_time_breakdown, formatMinutes(workMinutes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.travel_time_breakdown, formatMinutes(travelMinutes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsDashboardV2(
    weekStats: WeekStats?,
    monthStats: MonthStats?,
    onOpenWeekView: () -> Unit
) {
    when {
        weekStats != null -> {
            StatisticsDashboardCardV2(
                label = stringResource(R.string.today_stats_week),
                totalHours = weekStats.totalHours,
                targetHours = weekStats.targetHours,
                workDaysCount = weekStats.workDaysCount,
                progress = weekStats.progress,
                isOverTarget = weekStats.isOverTarget,
                isUnderTarget = weekStats.isUnderTarget,
                onOpenWeekView = onOpenWeekView
            )
        }
        monthStats != null -> {
            StatisticsDashboardCardV2(
                label = stringResource(R.string.today_stats_month),
                totalHours = monthStats.totalHours,
                targetHours = monthStats.targetHours,
                workDaysCount = monthStats.workDaysCount,
                progress = monthStats.progress,
                isOverTarget = monthStats.isOverTarget,
                isUnderTarget = monthStats.isUnderTarget,
                onOpenWeekView = onOpenWeekView
            )
        }
    }
}

@Composable
private fun StatisticsDashboardCardV2(
    label: String,
    totalHours: Double,
    targetHours: Double,
    workDaysCount: Int,
    progress: Float,
    isOverTarget: Boolean,
    isUnderTarget: Boolean,
    onOpenWeekView: () -> Unit
) {
    val status = when {
        isOverTarget -> StatusType.SUCCESS
        isUnderTarget -> StatusType.WARNING
        else -> StatusType.INFO
    }

    MZCard(
        modifier = Modifier.clickableWithAccessibility(
            onClick = onOpenWeekView,
            contentDescription = stringResource(R.string.cd_open_statistics)
        )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.today_stats_hours,
                            totalHours,
                            targetHours
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.today_stats_label_days, label, workDaysCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MZStatusBadge(
                    text = stringResource(
                        R.string.today_stats_percent,
                        (progress * 100).toInt()
                    ),
                    type = status
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (status) {
                    StatusType.SUCCESS -> MaterialTheme.colorScheme.primary
                    StatusType.WARNING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun DailyManualCheckInDialogV2(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.daily_check_in_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.daily_check_in_dialog_label)) },
                    placeholder = { Text(stringResource(R.string.daily_check_in_dialog_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.daily_check_in_dialog_support),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = onConfirm,
                isLoading = isLoading,
                enabled = input.trim().isNotEmpty(),
                content = { Text(stringResource(R.string.action_daily_manual_check_in)) }
            )
        },
        dismissButton = {
            TertiaryActionButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DayLocationDialogV2(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.day_location_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.day_location_dialog_label)) },
                    placeholder = { Text(stringResource(R.string.day_location_dialog_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.day_location_dialog_support),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = onConfirm,
                isLoading = isLoading,
                enabled = input.trim().isNotEmpty(),
                content = { Text(stringResource(R.string.action_apply)) }
            )
        },
        dismissButton = {
            TertiaryActionButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun getCurrentDateString(): String {
    return LocalDate.now().format(todayCurrentDateFormatter)
}

@Composable
private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) {
        stringResource(R.string.format_hours_short, h)
    } else {
        stringResource(R.string.format_hours_short_minutes, h, m)
    }
}

private val todayCurrentDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)
