@file:Suppress("LongMethod", "LongParameterList")

package de.montagezeit.app.ui.screen.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.usecase.EntryStatusResolver
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.ui.components.DatePickerDialog
import de.montagezeit.app.ui.components.MZAlertDialog
import de.montagezeit.app.ui.components.MZContentCard
import de.montagezeit.app.ui.components.MZErrorState
import de.montagezeit.app.ui.components.MZHeroPanel
import de.montagezeit.app.ui.components.MZInlineNotice
import de.montagezeit.app.ui.components.MZKeyValueRow
import de.montagezeit.app.ui.components.MZLoadingState
import de.montagezeit.app.ui.components.MZMetricChip
import de.montagezeit.app.ui.components.MZSectionHeader
import de.montagezeit.app.ui.components.MZSectionIntro
import de.montagezeit.app.ui.components.MZSnackbarHost
import de.montagezeit.app.ui.components.MZStatusBadge
import de.montagezeit.app.ui.components.MZStatusChip
import de.montagezeit.app.ui.components.PrimaryActionButton
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.StatusType
import de.montagezeit.app.ui.components.TertiaryActionButton
import de.montagezeit.app.ui.components.mzOutlinedTextFieldColors
import de.montagezeit.app.ui.components.staggeredAppear
import de.montagezeit.app.ui.screen.edit.DateNavigationRow
import de.montagezeit.app.ui.theme.GlassInfo
import de.montagezeit.app.ui.theme.GlassSuccess
import de.montagezeit.app.ui.theme.GlassWarning
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate

private data class TodayStatusUi(
    val type: StatusType,
    val subtitleRes: Int,
    val badgeTextRes: Int
)

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenEditSheet: (LocalDate) -> Unit,
    onOpenWeekView: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

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
    val onBackToToday = remember(screenState.todayDate) { { viewModel.selectDate(screenState.todayDate) } }
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

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncWithSystemDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                TodayContent(
                    entry = screenState.currentEntry,
                    travelLegs = screenState.currentTravelLegs,
                    selectedDate = screenState.selectedDate,
                    weekDaysUi = screenState.weekDaysUi,
                    isDailyCheckInLoading = screenState.isDailyCheckInLoading,
                    isConfirmOffdayLoading = screenState.isConfirmOffdayLoading,
                    onSelectDay = onSelectDay,
                    onBackToToday = onBackToToday,
                    onOpenDatePicker = { showDatePicker = true },
                    onEditDayLocation = onEditDayLocation,
                    onEditToday = onEditToday,
                    onDeleteDay = onDeleteDay,
                    onOpenDailyCheckInDialog = onOpenDailyCheckInDialogAction,
                    onConfirmOffDay = onConfirmOffDayAction,
                    onOpenWeekView = onOpenWeekView
                )
            }
        }

        MZSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = MZTokens.ScreenPadding, vertical = 12.dp)
        )
    }

    TodayDialogsHost(
        viewModel = viewModel,
        dialogState = dialogState
    )

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = screenState.selectedDate,
            onDateSelected = {
                viewModel.selectDate(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
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
    onBackToToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onEditDayLocation: () -> Unit,
    onEditToday: () -> Unit,
    onDeleteDay: () -> Unit,
    onOpenDailyCheckInDialog: () -> Unit,
    onConfirmOffDay: () -> Unit,
    onOpenWeekView: () -> Unit
) {
    val workMinutes = remember(entry) { entry?.let(TimeCalculator::calculateWorkMinutes) ?: 0 }
    val travelMinutes = remember(travelLegs) { TimeCalculator.calculateTravelMinutes(travelLegs) }
    val totalMinutes = remember(entry, travelLegs) {
        entry?.let { TimeCalculator.calculatePaidTotalMinutes(it, travelLegs) } ?: 0
    }
    val entryStatus = remember(entry, travelLegs) {
        entry?.let { EntryStatusResolver.resolve(it, travelLegs) }
    }
    val statusUi = remember(entry, entryStatus) {
        resolveTodayStatusUi(entry, entryStatus?.isConfirmed == true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        MZHeroPanel(
            modifier = Modifier.padding(MZTokens.ScreenPadding)
        ) {
            MZSectionIntro(
                eyebrow = Formatters.formatDate(selectedDate).uppercase(),
                title = stringResource(R.string.today_title),
                supportingText = stringResource(statusUi.subtitleRes)
            )
            MZStatusBadge(
                text = stringResource(statusUi.badgeTextRes),
                type = statusUi.type,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MZMetricChip(
                    label = stringResource(R.string.total_paid_hours),
                    value = formatMinutes(totalMinutes),
                    modifier = Modifier.weight(1f)
                )
                MZMetricChip(
                    label = stringResource(R.string.label_break),
                    value = formatMinutes(entry?.breakMinutes ?: 0),
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.secondary
                )
            }

            TertiaryActionButton(
                onClick = onOpenWeekView,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.overview_title))
            }
        }

        if (weekDaysUi.isNotEmpty()) {
            WeekOverviewRow(
                weekDays = weekDaysUi,
                onSelectDay = onSelectDay,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        MZContentCard(
            modifier = Modifier.padding(horizontal = MZTokens.ScreenPadding)
        ) {
            DateNavigationRow(
                date = selectedDate,
                onPrevious = { onSelectDay(selectedDate.minusDays(1)) },
                onNext = { onSelectDay(selectedDate.plusDays(1)) },
                onToday = onBackToToday,
                onPickDate = onOpenDatePicker
            )
        }

        Column(
            modifier = Modifier.padding(MZTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MZTokens.CardSpacing)
        ) {
            StatusCard(
                entry = entry,
                travelLegs = travelLegs,
                date = selectedDate,
                isDailyCheckInLoading = isDailyCheckInLoading,
                isConfirmOffdayLoading = isConfirmOffdayLoading,
                onOpenDailyCheckInDialog = onOpenDailyCheckInDialog,
                onConfirmOffDay = onConfirmOffDay,
                onEditToday = onEditToday,
                onEditDayLocation = onEditDayLocation,
                onDeleteDay = onDeleteDay,
                modifier = Modifier.staggeredAppear(index = 0)
            )

            if (entry != null && (entry.dayType == DayType.WORK || travelLegs.isNotEmpty())) {
                WorkHoursCard(
                    entry = entry,
                    travelLegs = travelLegs,
                    workMinutes = workMinutes,
                    travelMinutes = travelMinutes,
                    totalMinutes = totalMinutes,
                    modifier = Modifier.staggeredAppear(index = 1)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    entry: WorkEntry?,
    travelLegs: List<TravelLeg>,
    date: LocalDate,
    isDailyCheckInLoading: Boolean,
    isConfirmOffdayLoading: Boolean,
    onOpenDailyCheckInDialog: () -> Unit,
    onConfirmOffDay: () -> Unit,
    onEditToday: () -> Unit,
    onEditDayLocation: () -> Unit,
    onDeleteDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entryStatus = remember(entry, travelLegs) {
        entry?.let { EntryStatusResolver.resolve(it, travelLegs) }
    }
    val statusUi = remember(entry, entryStatus) {
        resolveTodayStatusUi(entry, entryStatus?.isConfirmed == true)
    }
    val hasEntry = entry != null
    val showOffdayAction = entryStatus?.isConfirmed != true && entry?.dayType != DayType.COMP_TIME
    var showMenu by remember(hasEntry) { mutableStateOf(false) }

    MZContentCard(modifier = modifier) {
        MZSectionHeader(
            title = Formatters.formatDateLong(date),
            supportingText = stringResource(R.string.today_status_panel_support),
            action = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MZStatusChip(
                        text = stringResource(statusUi.badgeTextRes),
                        color = statusColor(statusUi.type)
                    )
                    if (hasEntry) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.cd_today_more_actions)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_change_location)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onEditDayLocation()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete_day)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteDay()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )

        StatusCardContent(entry = entry, travelLegs = travelLegs)

        PrimaryActionButton(
            onClick = if (hasEntry) onEditToday else onOpenDailyCheckInDialog,
            enabled = !isDailyCheckInLoading,
            modifier = Modifier.fillMaxWidth(),
            isLoading = isDailyCheckInLoading,
            icon = if (hasEntry) Icons.Default.Edit else Icons.Default.Add
        ) {
            Text(
                text = if (hasEntry) {
                    stringResource(R.string.action_edit_entry_manual)
                } else {
                    stringResource(R.string.action_daily_manual_check_in)
                }
            )
        }

        StatusCardActions(
            showOffdayAction = showOffdayAction,
            isConfirmOffdayLoading = isConfirmOffdayLoading,
            onConfirmOffDay = onConfirmOffDay
        )
    }
}

private fun resolveTodayStatusUi(entry: WorkEntry?, isConfirmed: Boolean): TodayStatusUi = when {
    entry != null && isConfirmed -> TodayStatusUi(
        type = StatusType.SUCCESS,
        subtitleRes = R.string.today_dashboard_subtitle_done,
        badgeTextRes = R.string.today_confirmed
    )

    entry != null -> TodayStatusUi(
        type = StatusType.WARNING,
        subtitleRes = R.string.today_dashboard_subtitle_open,
        badgeTextRes = R.string.today_unconfirmed
    )

    else -> TodayStatusUi(
        type = StatusType.INFO,
        subtitleRes = R.string.today_dashboard_subtitle_empty,
        badgeTextRes = R.string.today_no_check_in
    )
}

@Composable
private fun StatusCardContent(
    entry: WorkEntry?,
    travelLegs: List<TravelLeg>
) {
    entry?.let {
        MZKeyValueRow(
            label = stringResource(R.string.today_detail_type_label),
            value = dayTypeLabel(it.dayType)
        )
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
        return
    }

    MZInlineNotice(
        title = stringResource(R.string.today_empty_notice_title),
        message = stringResource(R.string.today_dashboard_empty_hint),
        type = StatusType.INFO
    )
}

@Composable
private fun StatusCardActions(
    showOffdayAction: Boolean,
    isConfirmOffdayLoading: Boolean,
    onConfirmOffDay: () -> Unit
) {
    if (showOffdayAction) {
        SecondaryActionButton(
            onClick = onConfirmOffDay,
            enabled = !isConfirmOffdayLoading,
            modifier = Modifier.fillMaxWidth(),
            isLoading = isConfirmOffdayLoading
        ) {
            Text(stringResource(R.string.action_confirm_offday))
        }
    }
}

@Composable
private fun WorkHoursCard(
    entry: WorkEntry,
    travelLegs: List<TravelLeg>,
    workMinutes: Int,
    travelMinutes: Int,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    MZContentCard(modifier = modifier) {
        MZSectionHeader(
            title = stringResource(R.string.work_hours_title),
            supportingText = if (travelLegs.isNotEmpty()) {
                stringResource(R.string.today_work_panel_support)
            } else {
                null
            }
        )
        MZKeyValueRow(
            label = stringResource(R.string.total_paid_hours),
            value = formatMinutes(totalMinutes),
            emphasize = true
        )

        if (travelMinutes > 0 && workMinutes > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MZKeyValueRow(
                label = stringResource(R.string.history_stat_work),
                value = formatMinutes(workMinutes)
            )
            MZKeyValueRow(
                label = stringResource(R.string.history_stat_travel),
                value = formatMinutes(travelMinutes)
            )
        }

        entry.note?.takeIf { it.isNotBlank() }?.let { note ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MZInlineNotice(
                title = stringResource(R.string.edit_section_note_optional),
                message = note,
                type = StatusType.NEUTRAL
            )
        }
    }
}

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
    MZAlertDialog(
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
                    colors = mzOutlinedTextFieldColors(),
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
                enabled = input.trim().isNotEmpty() && !isLoading,
                isLoading = isLoading
            ) {
                Text(stringResource(R.string.action_daily_manual_check_in))
            }
        },
        dismissButton = {
            TertiaryActionButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DayLocationDialog(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    MZAlertDialog(
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
                    colors = mzOutlinedTextFieldColors(),
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
                enabled = input.trim().isNotEmpty() && !isLoading,
                isLoading = isLoading
            ) {
                Text(stringResource(R.string.action_apply))
            }
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
    MZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_day_title)) },
        text = { Text(stringResource(R.string.dialog_delete_day_message)) },
        confirmButton = {
            PrimaryActionButton(
                onClick = onConfirm,
                enabled = !isLoading,
                isLoading = isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.action_delete_day))
            }
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

private fun statusColor(type: StatusType) = when (type) {
    StatusType.SUCCESS -> GlassSuccess
    StatusType.WARNING -> GlassWarning
    StatusType.ERROR -> GlassWarning
    StatusType.INFO, StatusType.NEUTRAL -> GlassInfo
}

@Composable
private fun dayTypeLabel(dayType: DayType): String = when (dayType) {
    DayType.WORK -> stringResource(R.string.day_type_work)
    DayType.OFF -> stringResource(R.string.day_type_off)
    DayType.COMP_TIME -> stringResource(R.string.day_type_comp_time)
    DayType.SCHULUNG -> stringResource(R.string.day_type_schulung)
    DayType.LEHRGANG -> stringResource(R.string.day_type_lehrgang)
}
