package de.montagezeit.app.ui.screen.today

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
import de.montagezeit.app.ui.components.PrimaryActionButton
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.TertiaryActionButton
import de.montagezeit.app.ui.components.*
import de.montagezeit.app.ui.theme.GlassSuccess
import de.montagezeit.app.ui.theme.GlassWarning
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenEditSheet: (LocalDate) -> Unit,
    onOpenWeekView: () -> Unit = {}
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
    val onDeleteDay = remember { { viewModel.openDeleteDayDialog() } }
    val onBackToToday = remember { { viewModel.selectDate(LocalDate.now()) } }
    val onSelectDay = remember { { date: LocalDate -> viewModel.selectDate(date) } }
    val onEditDayLocation = remember { { viewModel.openDayLocationDialog() } }
    val onEditToday = remember(screenState.selectedDate) {
        {
            viewModel.ensureTodayEntryThen { onOpenEditSheet(screenState.selectedDate) }
        }
    }

    TodayTransientEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState
    )

    LaunchedEffect(Unit) {
        viewModel.selectDate(LocalDate.now())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MZPageBackground(
            modifier = Modifier.fillMaxSize()
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
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Secondary actions row (Delete/Back to Today)
                            if (screenState.currentEntry != null || screenState.isViewingPastDay) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (screenState.isViewingPastDay) {
                                        TextButton(onClick = onBackToToday) {
                                            Text(stringResource(R.string.week_back_to_today))
                                        }
                                    }
                                    if (screenState.currentEntry != null) {
                                        IconButton(onClick = onDeleteDay) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.action_delete_day),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            TodayContent(
                                entry = screenState.currentEntry,
                                travelLegs = screenState.currentTravelLegs,
                                selectedDate = screenState.selectedDate,
                                weekDaysUi = screenState.weekDaysUi,
                                isDailyCheckInLoading = screenState.isDailyCheckInLoading,
                                isConfirmOffdayLoading = screenState.isConfirmOffdayLoading,
                                onSelectDay = onSelectDay,
                                onEditDayLocation = onEditDayLocation,
                                onEditToday = onEditToday,
                                onOpenDailyCheckInDialog = onOpenDailyCheckInDialogAction,
                                onConfirmOffDay = onConfirmOffDayAction
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )

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
        DayLocationDialog(
            input = dialogState.dayLocationInput,
            isLoading = dialogState.isUpdateDayLocationLoading,
            onInputChange = { viewModel.onDayLocationInputChanged(it) },
            onDismiss = { viewModel.onDismissDayLocationDialog() },
            onConfirm = { viewModel.submitDayLocationUpdate() }
        )
    }

    if (dialogState.showDailyCheckInDialog) {
        DailyManualCheckInDialog(
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
private fun TodayContent(
    entry: WorkEntry?,
    travelLegs: List<TravelLeg>,
    selectedDate: LocalDate,
    weekDaysUi: List<WeekDayUi>,
    isDailyCheckInLoading: Boolean,
    isConfirmOffdayLoading: Boolean,
    onSelectDay: (LocalDate) -> Unit,
    onEditDayLocation: () -> Unit,
    onEditToday: () -> Unit,
    onOpenDailyCheckInDialog: () -> Unit,
    onConfirmOffDay: () -> Unit
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
            modifier = Modifier.padding(MZTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                entry = entry,
                travelLegs = travelLegs,
                date = selectedDate,
                onEditToday = onEditToday,
                onEditDayLocation = onEditDayLocation
            )

            val isCompleted = entry?.confirmedWorkDay == true
            if (!isCompleted) {
                TodayActionsCard(
                    entry = entry,
                    isDailyCheckInLoading = isDailyCheckInLoading,
                    isConfirmOffdayLoading = isConfirmOffdayLoading,
                    onOpenDailyCheckInDialog = onOpenDailyCheckInDialog,
                    onConfirmOffDay = onConfirmOffDay
                )
            }

            if (entry != null && (entry.dayType == DayType.WORK || travelLegs.isNotEmpty())) {
                WorkHoursCard(entry = entry, travelLegs = travelLegs)
            }
        }
    }
}

@Composable
private fun TodayActionsCard(
    entry: WorkEntry?,
    isDailyCheckInLoading: Boolean,
    isConfirmOffdayLoading: Boolean,
    onOpenDailyCheckInDialog: () -> Unit,
    onConfirmOffDay: () -> Unit
) {
    val showOffdayAction = entry?.dayType != DayType.COMP_TIME

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.today_action_required),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        MZCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val showPulse = entry == null && !isDailyCheckInLoading
                if (showPulse) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart
                        ),
                        label = "pulseAlpha"
                    )
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.10f,
                        animationSpec = infiniteRepeatable(
                            tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart
                        ),
                        label = "pulseScale"
                    )
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }
                                .clip(RoundedCornerShape(MZTokens.RadiusButton))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                        )
                        PrimaryActionButton(
                            onClick = onOpenDailyCheckInDialog,
                            isLoading = isDailyCheckInLoading,
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(MZTokens.RadiusButton)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                stringResource(R.string.action_daily_manual_check_in),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    PrimaryActionButton(
                        onClick = onOpenDailyCheckInDialog,
                        isLoading = isDailyCheckInLoading,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(MZTokens.RadiusButton)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            stringResource(R.string.action_daily_manual_check_in),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (showOffdayAction) {
                    SecondaryActionButton(
                        onClick = onConfirmOffDay,
                        isLoading = isConfirmOffdayLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_confirm_offday))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    entry: WorkEntry?,
    travelLegs: List<TravelLeg>,
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
        title = remember(date) { Formatters.formatDateLong(date) },
        subtitle = subtitle,
        accentColor = when (status) {
            StatusType.SUCCESS -> GlassSuccess
            StatusType.WARNING -> GlassWarning
            else -> null
        },
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryActionButton(
                    onClick = onEditDayLocation,
                    modifier = Modifier.weight(1f),
                    enabled = entry != null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(MZTokens.RadiusChip)
                ) {
                    Text(stringResource(R.string.action_change_location), style = MaterialTheme.typography.labelLarge)
                }
                SecondaryActionButton(
                    onClick = onEditToday,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(MZTokens.RadiusChip)
                ) {
                    Text(stringResource(R.string.today_action_edit_entry), style = MaterialTheme.typography.labelLarge)
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
                    value = formatMinutes(TimeCalculator.calculatePaidTotalMinutes(it, travelLegs)),
                    emphasize = true
                )
            }
        } ?: Text(
            text = stringResource(R.string.today_dashboard_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun DayTypeRow(dayType: DayType) {
    val icon: ImageVector = when (dayType) {
        DayType.WORK -> Icons.Default.Work
        DayType.OFF -> Icons.Default.FreeBreakfast
        DayType.COMP_TIME -> Icons.Default.Bedtime
    }
    val label: String = when (dayType) {
        DayType.WORK -> stringResource(R.string.day_type_work)
        DayType.OFF -> stringResource(R.string.day_type_off)
        DayType.COMP_TIME -> stringResource(R.string.day_type_comp_time)
    }
    val contentColor: Color = when (dayType) {
        DayType.WORK -> MaterialTheme.colorScheme.onPrimaryContainer
        DayType.OFF -> MaterialTheme.colorScheme.onSecondaryContainer
        DayType.COMP_TIME -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val containerColor: Color = when (dayType) {
        DayType.WORK -> MaterialTheme.colorScheme.primaryContainer
        DayType.OFF -> MaterialTheme.colorScheme.secondaryContainer
        DayType.COMP_TIME -> MaterialTheme.colorScheme.tertiaryContainer
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = containerColor,
            contentColor = contentColor
        ) {
            Box(modifier = Modifier.padding(6.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WorkHoursCard(entry: WorkEntry, travelLegs: List<TravelLeg>) {
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

            if (travelMinutes > 0 && workMinutes > 0) {
                HorizontalDivider()
                MZKeyValueRow(
                    label = stringResource(R.string.history_stat_work),
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DailyManualCheckInDialog(
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
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = { keyboardController?.hide(); onDismiss() },
        title = { Text(stringResource(R.string.daily_check_in_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 100) onInputChange(it) },
                    label = { Text(stringResource(R.string.daily_check_in_dialog_label)) },
                    placeholder = { Text(stringResource(R.string.daily_check_in_dialog_placeholder)) },
                    isError = input.isNotEmpty() && input.isBlank(),
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
                CheckboxRow(
                    checked = isArrivalDeparture,
                    onCheckedChange = onArrivalDepartureChanged,
                    label = stringResource(R.string.meal_allowance_arrival_departure_label)
                )
                CheckboxRow(
                    checked = breakfastIncluded,
                    onCheckedChange = onBreakfastIncludedChanged,
                    label = stringResource(R.string.meal_allowance_breakfast_label)
                )
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
private fun DayLocationDialog(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = { keyboardController?.hide(); onDismiss() },
        title = { Text(stringResource(R.string.day_location_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 100) onInputChange(it) },
                    label = { Text(stringResource(R.string.day_location_dialog_label)) },
                    placeholder = { Text(stringResource(R.string.day_location_dialog_placeholder)) },
                    isError = input.isNotEmpty() && input.isBlank(),
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

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Checkbox }
            .clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
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
