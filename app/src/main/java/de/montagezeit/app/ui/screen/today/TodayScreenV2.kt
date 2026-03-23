package de.montagezeit.app.ui.screen.today

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.MealAllowanceCalculator
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
    onOpenEditSheet: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val onOpenDailyCheckInDialogAction = remember {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.openDailyCheckInDialog()
        }
    }
    val onConfirmOffDayAction = remember {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.onConfirmOffDay()
        }
    }

    TodayTransientEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState
    )

    LaunchedEffect(Unit) {
        viewModel.selectDate(LocalDate.now())
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                title = {
                    Text(
                        text = stringResource(R.string.today_title),
                        modifier = Modifier.semantics { heading() }
                    )
                },
                actions = {
                    if (screenState.currentEntry != null) {
                        IconButton(onClick = { viewModel.openDeleteDayDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete_day)
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            StickyTodayActionBarV2(
                entry = screenState.currentEntry,
                isDailyCheckInLoading = screenState.isDailyCheckInLoading,
                isConfirmOffdayLoading = screenState.isConfirmOffdayLoading,
                onOpenDailyCheckInDialog = onOpenDailyCheckInDialogAction,
                onConfirmOffDay = onConfirmOffDayAction
            )
        }
    ) { paddingValues ->
        MZPageBackground(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val errorState = screenState.errorState
                when {
                    screenState.showFullscreenError && errorState != null -> {
                        MZErrorState(
                            message = errorState.message.asString(context),
                            onRetry = { viewModel.onResetError() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    screenState.showInitialLoading -> {
                        MZLoadingState(
                            message = stringResource(R.string.loading),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        TodayContentV2(
                            entry = screenState.currentEntry,
                            travelLegs = screenState.currentTravelLegs,
                            selectedDate = screenState.selectedDate,
                            onEditDayLocation = {
                                viewModel.openDayLocationDialog()
                            },
                            onEditToday = {
                                viewModel.ensureTodayEntryThen {
                                    onOpenEditSheet(screenState.selectedDate)
                                }
                            }
                        )
                    }
                }
            }
        }

        TodayDialogsHost(
            viewModel = viewModel,
            dialogState = dialogState
        )
    }
}

@Composable
private fun TodayTransientEffects(
    viewModel: TodayViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val deletedEntryForUndo by viewModel.deletedEntryForUndo.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message.asString(context))
        viewModel.onSnackbarShown()
    }

    LaunchedEffect(deletedEntryForUndo) {
        deletedEntryForUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.today_delete_success),
            actionLabel = context.getString(R.string.action_undo),
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDeleteDay()
        } else {
            viewModel.onUndoWindowClosed()
        }
    }
}

@Composable
private fun TodayDialogsHost(
    viewModel: TodayViewModel,
    dialogState: TodayDialogState
) {

    if (dialogState.showDeleteDayDialog) {
        DeleteDayConfirmDialog(
            isLoading = dialogState.isDeleteDayLoading,
            onDismiss = { viewModel.dismissDeleteDayDialog() },
            onConfirm = { viewModel.confirmDeleteDay() }
        )
    }

    if (dialogState.showDayLocationDialog) {
        DayLocationDialogV2(
            input = dialogState.dayLocationInput,
            isLoading = dialogState.isUpdateDayLocationLoading,
            onInputChange = { viewModel.onDayLocationInputChanged(it) },
            onDismiss = { viewModel.onDismissDayLocationDialog() },
            onConfirm = { viewModel.submitDayLocationUpdate() }
        )
    }

    if (dialogState.showDailyCheckInDialog) {
        DailyManualCheckInDialogV2(
            input = dialogState.dailyCheckInLocationInput,
            isLoading = dialogState.isDailyCheckInLoading,
            isArrivalDeparture = dialogState.dailyCheckInIsArrivalDeparture,
            breakfastIncluded = dialogState.dailyCheckInBreakfastIncluded,
            allowancePreviewCents = dialogState.dailyCheckInAllowancePreviewCents,
            onInputChange = { viewModel.onDailyCheckInLocationChanged(it) },
            onArrivalDepartureChanged = { viewModel.onDailyCheckInArrivalDepartureChanged(it) },
            onBreakfastIncludedChanged = { viewModel.onDailyCheckInBreakfastIncludedChanged(it) },
            onDismiss = { viewModel.onDismissDailyCheckInDialog() },
            onConfirm = { viewModel.submitDailyManualCheckIn() }
        )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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
}

@Composable
private fun TodayContentV2(
    entry: WorkEntry?,
    travelLegs: List<TravelLeg>,
    selectedDate: LocalDate,
    onEditDayLocation: () -> Unit,
    onEditToday: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusCardV2(
            entry = entry,
            date = selectedDate,
            onEditToday = onEditToday,
            onEditDayLocation = onEditDayLocation
        )

        if (entry != null && entry.dayType == DayType.WORK) {
            WorkHoursCardV2(entry = entry, travelLegs = travelLegs)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun OvertimeCardV2(
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
        onClick = { isExpanded = !isExpanded },
        enabled = isConfigured,
        modifier = Modifier.animateContentSize()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MZSectionHeader(title = stringResource(R.string.overtime_title))
            if (!isConfigured) {
                MZStatusBadge(
                    text = stringResource(R.string.overtime_not_configured),
                    type = StatusType.NEUTRAL
                )
                return@Column
            }

            MZKeyValueRow(
                label = stringResource(R.string.today_stats_month),
                value = monthDisplay ?: formatSignedZeroHours(),
                emphasize = true
            )
            MZKeyValueRow(
                label = stringResource(R.string.today_stats_year),
                value = yearDisplay,
                emphasize = true
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider()
                    MZKeyValueRow(
                        label = stringResource(R.string.history_stat_total),
                        value = yearActualDisplay
                    )
                    MZKeyValueRow(
                        label = stringResource(R.string.history_stat_target),
                        value = yearTargetDisplay
                    )
                    MZKeyValueRow(
                        label = stringResource(R.string.overtime_counted_days),
                        value = yearCountedDays.toString()
                    )
                    if (yearOffDayTravelDays > 0) {
                        MZKeyValueRow(
                            label = stringResource(R.string.history_stat_travel),
                            value = "$yearOffDayTravelDisplay · $yearOffDayTravelDays"
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
        entry != null -> StatusType.WARNING
        else -> StatusType.INFO
    }
    val subtitle = when {
        entry?.confirmedWorkDay == true -> stringResource(R.string.today_dashboard_subtitle_done)
        entry != null -> stringResource(R.string.today_dashboard_subtitle_open)
        else -> stringResource(R.string.today_dashboard_subtitle_empty)
    }

    MZHeroCard(
        title = remember(date) { date.format(todayCurrentDateFormatter) },
        subtitle = subtitle,
        badge = {
            MZStatusBadge(
                text = when (status) {
                    StatusType.SUCCESS -> stringResource(R.string.today_confirmed)
                    StatusType.WARNING -> stringResource(R.string.today_unconfirmed)
                    else -> stringResource(R.string.today_no_check_in)
                },
                type = status
            )
        },
        action = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryActionButton(
                    onClick = onEditDayLocation,
                    modifier = Modifier.weight(1f),
                    enabled = entry != null
                ) {
                    Text(stringResource(R.string.action_change_location))
                }
                PrimaryActionButton(
                    onClick = onEditToday,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_edit_entry_manual))
                }
            }
        }
    ) {
        entry?.let {
            DayTypeRow(dayType = it.dayType)
            Spacer(modifier = Modifier.height(8.dp))
            MZKeyValueRow(
                label = stringResource(R.string.day_location_label),
                value = it.dayLocationLabel.trim().ifEmpty {
                    stringResource(R.string.today_day_location_unset)
                }
            )
            if (it.dayType == DayType.WORK) {
                MZKeyValueRow(
                    label = stringResource(R.string.total_paid_hours),
                    value = formatMinutes(TimeCalculator.calculatePaidTotalMinutes(it)),
                    emphasize = true
                )
            }
        } ?: Text(
            text = stringResource(R.string.today_dashboard_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DayTypeRow(dayType: DayType) {
    val (icon, text, color, containerColor) = when (dayType) {
        DayType.WORK -> listOf(
            Icons.Default.Work,
            stringResource(R.string.day_type_work),
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primaryContainer
        )
        DayType.OFF -> listOf(
            Icons.Default.FreeBreakfast,
            stringResource(R.string.day_type_off),
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
        DayType.COMP_TIME -> listOf(
            Icons.Default.Bedtime,
            stringResource(R.string.day_type_comp_time),
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = containerColor as Color,
            contentColor = color as Color
        ) {
            Box(modifier = Modifier.padding(6.dp)) {
                Icon(
                    imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = text as String,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WorkHoursCardV2(entry: WorkEntry, travelLegs: List<TravelLeg>) {
    val (workMinutes, travelMinutes, totalMinutes) = remember(entry, travelLegs) {
        Triple(
            TimeCalculator.calculateWorkMinutes(entry),
            TimeCalculator.calculateTravelMinutes(travelLegs),
            TimeCalculator.calculatePaidTotalMinutes(entry, travelLegs)
        )
    }

    MZCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MZSectionHeader(title = stringResource(R.string.work_hours_title))
            MZKeyValueRow(
                label = stringResource(R.string.total_paid_hours),
                value = formatMinutes(totalMinutes),
                emphasize = true
            )

            if (travelMinutes > 0) {
                Divider()
                MZKeyValueRow(
                    label = stringResource(R.string.history_stat_total),
                    value = formatMinutes(workMinutes)
                )
                MZKeyValueRow(
                    label = stringResource(R.string.history_stat_travel),
                    value = formatMinutes(travelMinutes)
                )
            }
        }
    }
}

@Composable
private fun StatisticsDashboardV2(
    weekStats: WeekStats?,
    monthStats: MonthStats?,
    monthMealAllowanceCents: Int?,
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
                mealAllowanceCents = monthMealAllowanceCents,
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
                mealAllowanceCents = monthMealAllowanceCents,
                onOpenWeekView = onOpenWeekView
            )
        }
    }
}

@Composable
fun StatisticsDashboardCardV2(
    label: String,
    totalHours: Double,
    targetHours: Double,
    workDaysCount: Int,
    progress: Float,
    isOverTarget: Boolean,
    isUnderTarget: Boolean,
    mealAllowanceCents: Int? = null,
    onOpenWeekView: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val status = when {
        isOverTarget -> StatusType.SUCCESS
        isUnderTarget -> StatusType.WARNING
        else -> StatusType.INFO
    }

    MZCard(
        onClick = onOpenWeekView,
        modifier = Modifier.semantics {
            onClick(label = context.getString(R.string.cd_open_statistics), action = null)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MZSectionHeader(
                title = stringResource(R.string.today_stats_title),
                action = {
                    MZStatusBadge(
                        text = label,
                        type = status,
                        showIcon = false
                    )
                }
            )
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

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = when (status) {
                    StatusType.SUCCESS -> MaterialTheme.colorScheme.primary
                    StatusType.WARNING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (mealAllowanceCents != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.meal_allowance_month_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MealAllowanceCalculator.formatEuro(mealAllowanceCents),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DailyManualCheckInDialogV2(
    input: String,
    isLoading: Boolean,
    isArrivalDeparture: Boolean,
    breakfastIncluded: Boolean,
    allowancePreviewCents: Int,
    onInputChange: (String) -> Unit,
    onArrivalDepartureChanged: (Boolean) -> Unit,
    onBreakfastIncludedChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.daily_check_in_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val keyboardController = LocalSoftwareKeyboardController.current
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.daily_check_in_dialog_label)) },
                    placeholder = { Text(stringResource(R.string.daily_check_in_dialog_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.daily_check_in_dialog_support),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.Checkbox }
                        .clickable { onArrivalDepartureChanged(!isArrivalDeparture) }
                ) {
                    Checkbox(
                        checked = isArrivalDeparture,
                        onCheckedChange = onArrivalDepartureChanged
                    )
                    Text(
                        text = stringResource(R.string.meal_allowance_arrival_departure_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.Checkbox }
                        .clickable { onBreakfastIncludedChanged(!breakfastIncluded) }
                ) {
                    Checkbox(
                        checked = breakfastIncluded,
                        onCheckedChange = onBreakfastIncludedChanged
                    )
                    Text(
                        text = stringResource(R.string.meal_allowance_breakfast_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Text(
                    text = stringResource(
                        R.string.meal_allowance_preview_label,
                        MealAllowanceCalculator.formatEuro(allowancePreviewCents)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
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

@OptIn(ExperimentalComposeUiApi::class)
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
                val keyboardController = LocalSoftwareKeyboardController.current
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.day_location_dialog_label)) },
                    placeholder = { Text(stringResource(R.string.day_location_dialog_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
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

@Composable
private fun DeleteDayConfirmDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_day_title)) },
        text = { Text(stringResource(R.string.dialog_delete_day_message)) },
        confirmButton = {
            PrimaryActionButton(
                onClick = onConfirm,
                isLoading = isLoading,
                content = { Text(stringResource(R.string.action_delete_day)) }
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
