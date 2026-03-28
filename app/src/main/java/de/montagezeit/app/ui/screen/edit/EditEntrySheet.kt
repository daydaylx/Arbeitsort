package de.montagezeit.app.ui.screen.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.ui.components.DatePickerDialog
import de.montagezeit.app.ui.components.DestructiveActionButton
import de.montagezeit.app.ui.components.PrimaryActionButton
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.TertiaryActionButton
import de.montagezeit.app.ui.components.TimePickerDialog
import de.montagezeit.app.ui.components.MZErrorState
import de.montagezeit.app.ui.util.DateTimeUtils
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntrySheet(
    date: LocalDate,
    viewModel: EditEntryViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    initialFormData: EditFormData? = null,
    onCopyToNewDate: ((LocalDate, EditFormData) -> Unit)? = null,
    onNavigateDate: ((LocalDate) -> Unit)? = null
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val uiState = screenState.uiState
    val formData = screenState.formData
    val isSaving = screenState.isSaving
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val context = LocalContext.current
    var showCopyDatePicker by remember { mutableStateOf(false) }
    var showNavigateDatePicker by remember { mutableStateOf(false) }
    var showDeleteDayConfirmDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }

    LaunchedEffect(date) {
        viewModel.setDate(date)
    }
    
    // Wenn initialFormData vorhanden ist (beim Kopieren), setze es im ViewModel
    LaunchedEffect(initialFormData) {
        if (initialFormData != null) {
            viewModel.setFormData(initialFormData)
        }
    }

    LaunchedEffect(uiState, onDismiss) {
        if (uiState is EditUiState.Saved) {
            onDismiss()
        }
    }

    val isDirty = screenState.isDirty
    val handleDismiss: () -> Unit = {
        if (isDirty && !isSaving) {
            showDiscardChangesDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = isDirty && !isSaving) {
        showDiscardChangesDialog = true
    }

    ModalBottomSheet(
        onDismissRequest = handleDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val showStickySaveBar = uiState is EditUiState.NewEntry || uiState is EditUiState.Success

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                if (showStickySaveBar) {
                    EditStickySaveBar(
                        isSaving = isSaving,
                        isNewEntry = uiState is EditUiState.NewEntry,
                        onSave = { viewModel.save() }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (onNavigateDate != null) {
                    DateNavigationRow(
                        date = date,
                        onPrevious = { onNavigateDate(date.minusDays(1)) },
                        onNext = { onNavigateDate(date.plusDays(1)) },
                        onToday = { onNavigateDate(LocalDate.now()) },
                        onPickDate = { showNavigateDatePicker = true }
                    )
                    DateNavigationSwipeZone(
                        swipeThresholdPx = swipeThresholdPx,
                        onSwipePrevious = { onNavigateDate(date.minusDays(1)) },
                        onSwipeNext = { onNavigateDate(date.plusDays(1)) },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    HorizontalDivider()
                }

                when (val state = uiState) {
                    is EditUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(32.dp)
                        )
                    }

                    is EditUiState.NotFound -> {
                        Text(
                            text = stringResource(R.string.edit_not_found),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

                    is EditUiState.NewEntry -> {
                        val dummyEntry = de.montagezeit.app.data.local.entity.WorkEntry(
                            date = state.date,
                            dayType = formData.dayType,
                            workStart = if (formData.hasWorkTimes) formData.workStart else null,
                            workEnd = if (formData.hasWorkTimes) formData.workEnd else null,
                            breakMinutes = if (formData.hasWorkTimes) formData.breakMinutes else 0,
                            dayLocationLabel = formData.dayLocationLabel.orEmpty(),
                            note = formData.note
                        )
                        EditFormContent(
                            entry = dummyEntry,
                            formData = formData,
                            validationErrors = state.validationErrors,
                            dailyTargetHours = screenState.dailyTargetHours,
                            onDayTypeChange = { viewModel.updateDayType(it) },
                            onHasWorkTimesChange = { viewModel.setHasWorkTimes(it) },
                            onWorkStartChange = { h, m -> viewModel.updateWorkStart(h, m) },
                            onWorkEndChange = { h, m -> viewModel.updateWorkEnd(h, m) },
                            onBreakMinutesChange = { viewModel.updateBreakMinutes(it) },
                            onAddTravelLeg = { viewModel.addTravelLeg() },
                            onTravelLegStartChange = { index, time -> viewModel.updateTravelLegStart(index, time) },
                            onTravelLegArriveChange = { index, time -> viewModel.updateTravelLegArrive(index, time) },
                            onTravelLegStartLabelChange = { index, label -> viewModel.updateTravelLegStartLabel(index, label) },
                            onTravelLegEndLabelChange = { index, label -> viewModel.updateTravelLegEndLabel(index, label) },
                            onRemoveTravelLeg = { index -> viewModel.removeTravelLeg(index) },
                            onTravelClear = { viewModel.clearTravel() },
                            onDayLocationChange = { viewModel.updateDayLocationLabel(it) },
                            onMealArrivalDepartureChange = { viewModel.updateMealArrivalDeparture(it) },
                            onMealBreakfastIncludedChange = { viewModel.updateMealBreakfastIncluded(it) },
                            onNoteChange = { viewModel.updateNote(it) },
                            onSave = { viewModel.save() },
                            onDeleteDay = null,
                            onCopyPrevious = {
                                viewModel.copyFromPreviousDay { success ->
                                    if (!success) {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.edit_toast_no_previous_entry),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onApplyDefaultTimes = { viewModel.applyDefaultWorkTimes() },
                            isSaving = isSaving,
                            isNewEntry = true,
                            showPrimarySaveButton = false
                        )

                        if (state.validationErrors.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = stringResource(R.string.edit_validation_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    state.validationErrors.forEach { error ->
                                        Text(
                                            text = "• ${stringResource(error.messageRes)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    TertiaryActionButton(
                                        onClick = { viewModel.clearValidationErrors() }
                                    ) {
                                        Text(stringResource(R.string.edit_action_ok))
                                    }
                                }
                            }
                        }
                    }

                    is EditUiState.Error -> {
                        MZErrorState(
                            message = state.message.asString(context),
                            onRetry = { viewModel.reloadEntry() }
                        )
                    }

                    is EditUiState.Success -> {
                        val entry = state.entry
                        EditFormContent(
                            entry = entry,
                            formData = formData,
                            validationErrors = state.validationErrors,
                            dailyTargetHours = screenState.dailyTargetHours,
                            onDayTypeChange = { viewModel.updateDayType(it) },
                            onHasWorkTimesChange = { viewModel.setHasWorkTimes(it) },
                            onWorkStartChange = { h, m -> viewModel.updateWorkStart(h, m) },
                            onWorkEndChange = { h, m -> viewModel.updateWorkEnd(h, m) },
                            onBreakMinutesChange = { viewModel.updateBreakMinutes(it) },
                            onAddTravelLeg = { viewModel.addTravelLeg() },
                            onTravelLegStartChange = { index, time -> viewModel.updateTravelLegStart(index, time) },
                            onTravelLegArriveChange = { index, time -> viewModel.updateTravelLegArrive(index, time) },
                            onTravelLegStartLabelChange = { index, label -> viewModel.updateTravelLegStartLabel(index, label) },
                            onTravelLegEndLabelChange = { index, label -> viewModel.updateTravelLegEndLabel(index, label) },
                            onRemoveTravelLeg = { index -> viewModel.removeTravelLeg(index) },
                            onTravelClear = { viewModel.clearTravel() },
                            onDayLocationChange = { viewModel.updateDayLocationLabel(it) },
                            onMealArrivalDepartureChange = { viewModel.updateMealArrivalDeparture(it) },
                            onMealBreakfastIncludedChange = { viewModel.updateMealBreakfastIncluded(it) },
                            onNoteChange = { viewModel.updateNote(it) },
                            onSave = { viewModel.save() },
                            onDeleteDay = { showDeleteDayConfirmDialog = true },
                            onCopyPrevious = {
                                viewModel.copyFromPreviousDay { success ->
                                    if (!success) {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.edit_toast_no_previous_entry),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onApplyDefaultTimes = { viewModel.applyDefaultWorkTimes() },
                            isSaving = isSaving,
                            onCopy = if (onCopyToNewDate != null) {
                                { showCopyDatePicker = true }
                            } else null,
                            showPrimarySaveButton = false
                        )

                        if (state.validationErrors.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = stringResource(R.string.edit_validation_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    state.validationErrors.forEach { error ->
                                        Text(
                                            text = "• ${stringResource(error.messageRes)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    TertiaryActionButton(
                                        onClick = { viewModel.clearValidationErrors() }
                                    ) {
                                        Text(stringResource(R.string.edit_action_ok))
                                    }
                                }
                            }
                        }

                    }

                    is EditUiState.Saved -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            Text(text = stringResource(R.string.edit_saved))
                        }
                    }
                }
            }
        }

        if (showDeleteDayConfirmDialog) {
            DeleteDayConfirmDialog(
                isLoading = isSaving,
                onDismiss = { showDeleteDayConfirmDialog = false },
                onConfirm = {
                    showDeleteDayConfirmDialog = false
                    viewModel.deleteCurrentEntry { deleted ->
                        if (deleted) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.today_delete_success),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }

        if (showDiscardChangesDialog) {
            DiscardChangesDialog(
                onDiscard = {
                    showDiscardChangesDialog = false
                    onDismiss()
                },
                onKeepEditing = { showDiscardChangesDialog = false }
            )
        }
    }
    
    // Copy Date Picker Dialog
    if (showCopyDatePicker && onCopyToNewDate != null) {
        DatePickerDialog(
            initialDate = date,
            onDateSelected = { newDate ->
                val copiedData = viewModel.copyEntryData()
                onCopyToNewDate(newDate, copiedData)
                showCopyDatePicker = false
                onDismiss() // Schließe aktuelles Sheet
            },
            onDismiss = { showCopyDatePicker = false }
        )
    }

    if (showNavigateDatePicker && onNavigateDate != null) {
        DatePickerDialog(
            initialDate = date,
            onDateSelected = { newDate ->
                onNavigateDate(newDate)
                showNavigateDatePicker = false
            },
            onDismiss = { showNavigateDatePicker = false }
        )
    }
}

@Composable
fun DateNavigationRow(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPickDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.edit_cd_prev_day)
            )
        }
        TextButton(onClick = onPickDate) {
            Text(formatShortDate(date))
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.edit_cd_next_day)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onToday) {
            Text(stringResource(R.string.edit_action_today))
        }
    }
}

@Composable
private fun DateNavigationSwipeZone(
    swipeThresholdPx: Float,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(swipeThresholdPx, onSwipePrevious, onSwipeNext) {
                var dragAccum = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum > swipeThresholdPx) {
                            onSwipePrevious()
                        } else if (dragAccum < -swipeThresholdPx) {
                            onSwipeNext()
                        }
                        dragAccum = 0f
                    },
                    onDragCancel = { dragAccum = 0f }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.width(44.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun EditStickySaveBar(
    isSaving: Boolean,
    isNewEntry: Boolean,
    onSave: () -> Unit
) {
    Surface(shadowElevation = 4.dp, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            PrimaryActionButton(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    if (isNewEntry) {
                        stringResource(R.string.action_create)
                    } else {
                        stringResource(R.string.action_save)
                    }
                )
            }
        }
    }
}

@Composable
private fun EditFormSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun EditFormContent(
    entry: de.montagezeit.app.data.local.entity.WorkEntry,
    formData: EditFormData,
    validationErrors: List<ValidationError> = emptyList(),
    dailyTargetHours: Double = 8.0,
    onDayTypeChange: (de.montagezeit.app.data.local.entity.DayType) -> Unit,
    onHasWorkTimesChange: (Boolean) -> Unit,
    onWorkStartChange: (Int, Int) -> Unit,
    onWorkEndChange: (Int, Int) -> Unit,
    onBreakMinutesChange: (Int) -> Unit,
    onAddTravelLeg: () -> Unit,
    onTravelLegStartChange: (Int, java.time.LocalTime?) -> Unit,
    onTravelLegArriveChange: (Int, java.time.LocalTime?) -> Unit,
    onTravelLegStartLabelChange: (Int, String) -> Unit,
    onTravelLegEndLabelChange: (Int, String) -> Unit,
    onRemoveTravelLeg: (Int) -> Unit,
    onTravelClear: () -> Unit,
    onDayLocationChange: (String) -> Unit,
    onMealArrivalDepartureChange: (Boolean) -> Unit,
    onMealBreakfastIncludedChange: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    onApplyDefaultTimes: (() -> Unit)? = null,
    onCopyPrevious: (() -> Unit)? = null,
    onSave: () -> Unit,
    onDeleteDay: (() -> Unit)? = null,
    isSaving: Boolean = false,
    isNewEntry: Boolean = false,
    onCopy: (() -> Unit)? = null,
    showPrimarySaveButton: Boolean = true
) {
    val isCompTime = formData.dayType == de.montagezeit.app.data.local.entity.DayType.COMP_TIME

    Text(
        text = Formatters.formatDateLong(entry.date),
        style = MaterialTheme.typography.headlineSmall
    )

    EditFormSectionCard {
        DayTypeSelector(
            selectedType = formData.dayType,
            onTypeChange = onDayTypeChange
        )
        // Ü-Abbau info banner: shown when COMP_TIME is active.
        if (isCompTime) {
            androidx.compose.material3.SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = stringResource(
                            R.string.edit_comp_time_info,
                            dailyTargetHours.toInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Work times, travel and location sections are hidden for COMP_TIME days
    // to prevent mixing work-day data with comp-time entries.
    if (!isCompTime) {
        if (formData.dayType == de.montagezeit.app.data.local.entity.DayType.WORK) {
            EditFormSectionCard {
                WorkTimesSection(
                    enabled = formData.hasWorkTimes,
                    workStart = formData.workStart,
                    workEnd = formData.workEnd,
                    breakMinutes = formData.breakMinutes,
                    validationErrors = validationErrors,
                    onEnabledChange = onHasWorkTimesChange,
                    onStartChange = onWorkStartChange,
                    onEndChange = onWorkEndChange,
                    onBreakChange = onBreakMinutesChange,
                    onApplyDefaults = onApplyDefaultTimes
                )
            }
        }

        EditFormSectionCard {
            TravelLegsSection(
                travelLegs = formData.travelLegs,
                validationErrors = validationErrors,
                onAddTravelLeg = onAddTravelLeg,
                onTravelLegStartChange = onTravelLegStartChange,
                onTravelLegArriveChange = onTravelLegArriveChange,
                onTravelLegStartLabelChange = onTravelLegStartLabelChange,
                onTravelLegEndLabelChange = onTravelLegEndLabelChange,
                onRemoveTravelLeg = onRemoveTravelLeg,
                onClearTravel = onTravelClear
            )
        }

        EditFormSectionCard {
            LocationLabelsSection(
                dayLocationLabel = formData.dayLocationLabel,
                onDayLocationChange = onDayLocationChange
            )
        }
    }

    if (formData.dayType == de.montagezeit.app.data.local.entity.DayType.WORK) {
        EditFormSectionCard {
            MealAllowanceSection(
                isArrivalDeparture = formData.mealIsArrivalDeparture,
                breakfastIncluded = formData.mealBreakfastIncluded,
                allowancePreviewCents = formData.mealAllowancePreviewCents(),
                onArrivalDepartureChange = onMealArrivalDepartureChange,
                onBreakfastIncludedChange = onMealBreakfastIncludedChange
            )
        }
    }

    EditFormSectionCard {
        NoteSection(
            note = formData.note,
            onNoteChange = onNoteChange
        )
    }

    val showSecondaryActions = onCopyPrevious != null ||
        (onCopy != null && !isNewEntry) ||
        (onDeleteDay != null && !isNewEntry)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (onCopyPrevious != null) {
            SecondaryActionButton(
                onClick = onCopyPrevious,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.edit_action_copy_previous))
            }
        }
        
        if (onCopy != null && !isNewEntry) {
            SecondaryActionButton(
                onClick = onCopy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.edit_action_copy_entry))
            }
        }

        if (onDeleteDay != null && !isNewEntry) {
            DestructiveActionButton(
                onClick = onDeleteDay,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.action_delete_day))
            }
        }

        if (showSecondaryActions && showPrimarySaveButton) {
            HorizontalDivider()
        }

        if (showPrimarySaveButton) {
            PrimaryActionButton(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    if (isNewEntry) {
                        stringResource(R.string.action_create)
                    } else {
                        stringResource(R.string.action_save)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayTypeSelector(
    selectedType: de.montagezeit.app.data.local.entity.DayType,
    onTypeChange: (de.montagezeit.app.data.local.entity.DayType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.edit_section_day_type),
            style = MaterialTheme.typography.titleMedium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            de.montagezeit.app.data.local.entity.DayType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeChange(type) },
                    label = {
                        Text(
                            when (type) {
                                de.montagezeit.app.data.local.entity.DayType.WORK -> stringResource(R.string.edit_day_type_workday)
                                de.montagezeit.app.data.local.entity.DayType.OFF -> stringResource(R.string.edit_day_type_off)
                                de.montagezeit.app.data.local.entity.DayType.COMP_TIME -> stringResource(R.string.edit_day_type_comp_time)
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun WorkTimesSection(
    enabled: Boolean,
    workStart: java.time.LocalTime,
    workEnd: java.time.LocalTime,
    breakMinutes: Int,
    validationErrors: List<ValidationError> = emptyList(),
    onEnabledChange: (Boolean) -> Unit,
    onStartChange: (Int, Int) -> Unit,
    onEndChange: (Int, Int) -> Unit,
    onBreakChange: (Int) -> Unit,
    onApplyDefaults: (() -> Unit)? = null
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    val hasWorkTimeError = validationErrors.any { it is ValidationError.WorkEndBeforeStart }
    val hasBreakError = validationErrors.any { 
        it is ValidationError.NegativeBreakMinutes || it is ValidationError.BreakLongerThanWorkTime 
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.edit_section_work_times),
                style = MaterialTheme.typography.titleMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.edit_toggle_work_times),
                    style = MaterialTheme.typography.bodySmall
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
        }

        if (!enabled) {
            Text(
                text = stringResource(R.string.edit_work_times_disabled_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f),
                    colors = if (hasWorkTimeError) {
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.edit_label_start),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = Formatters.formatTime(workStart),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f),
                    colors = if (hasWorkTimeError) {
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.edit_label_end),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = Formatters.formatTime(workEnd),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            if (hasWorkTimeError) {
                Text(
                    text = stringResource(
                        R.string.edit_error_prefix,
                        stringResource(ValidationError.WorkEndBeforeStart.messageRes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Break Time
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.edit_label_break_minutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasBreakError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.edit_break_minutes_value, breakMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (hasBreakError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Slider(
                    value = breakMinutes.toFloat(),
                    onValueChange = { onBreakChange(it.toInt()) },
                    valueRange = 0f..120f,
                    steps = 120,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (hasBreakError) {
                        SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.error,
                            activeTrackColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        SliderDefaults.colors()
                    }
                )

                if (hasBreakError) {
                    val breakError = validationErrors.firstOrNull {
                        it is ValidationError.NegativeBreakMinutes || it is ValidationError.BreakLongerThanWorkTime
                    }
                    breakError?.let {
                        Text(
                            text = stringResource(
                                R.string.edit_error_prefix,
                                stringResource(it.messageRes)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            if (onApplyDefaults != null) {
                TextButton(
                    onClick = onApplyDefaults,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(stringResource(R.string.edit_action_apply_default_times))
                }
            }
        }
    }
    
    if (showStartPicker) {
TimePickerDialog(
            initialTime = workStart,
            onTimeSelected = { onStartChange(it.hour, it.minute); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    
    if (showEndPicker) {
TimePickerDialog(
            initialTime = workEnd,
            onTimeSelected = { onEndChange(it.hour, it.minute); showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
fun TravelLegsSection(
    travelLegs: List<EditTravelLegForm>,
    validationErrors: List<ValidationError> = emptyList(),
    onAddTravelLeg: () -> Unit,
    onTravelLegStartChange: (Int, java.time.LocalTime?) -> Unit,
    onTravelLegArriveChange: (Int, java.time.LocalTime?) -> Unit,
    onTravelLegStartLabelChange: (Int, String) -> Unit,
    onTravelLegEndLabelChange: (Int, String) -> Unit,
    onRemoveTravelLeg: (Int) -> Unit,
    onClearTravel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.edit_section_travel),
                style = MaterialTheme.typography.titleMedium
            )

            if (travelLegs.isNotEmpty()) {
                TextButton(onClick = onClearTravel) {
                    Text(stringResource(R.string.edit_action_clear_all_travel))
                }
            }
        }

        if (travelLegs.isEmpty()) {
            Text(
                text = stringResource(R.string.edit_travel_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        travelLegs.forEachIndexed { index, leg ->
            TravelLegCard(
                index = index,
                leg = leg,
                validationErrors = validationErrors,
                onStartChange = { onTravelLegStartChange(index, it) },
                onArriveChange = { onTravelLegArriveChange(index, it) },
                onStartLabelChange = { onTravelLegStartLabelChange(index, it) },
                onEndLabelChange = { onTravelLegEndLabelChange(index, it) },
                onRemove = { onRemoveTravelLeg(index) }
            )
        }

        OutlinedButton(
            onClick = onAddTravelLeg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(stringResource(R.string.edit_action_add_travel_leg))
        }
    }
}

@Composable
private fun TravelLegCard(
    index: Int,
    leg: EditTravelLegForm,
    validationErrors: List<ValidationError>,
    onStartChange: (java.time.LocalTime?) -> Unit,
    onArriveChange: (java.time.LocalTime?) -> Unit,
    onStartLabelChange: (String) -> Unit,
    onEndLabelChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showArrivePicker by remember { mutableStateOf(false) }
    val legErrors = remember(validationErrors, index) {
        validationErrors.filter { error -> error.matchesTravelLeg(index) }
    }
    val hasTravelError = legErrors.isNotEmpty()
    val duration = remember(leg.startTime, leg.arriveTime) {
        DateTimeUtils.calculateTravelDuration(leg.startTime, leg.arriveTime)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.edit_travel_leg_title, index + 1),
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_remove_travel_leg)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f),
                    colors = if (hasTravelError) {
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.edit_label_travel_start),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = leg.startTime?.let { Formatters.formatTime(it) } ?: stringResource(R.string.edit_time_pick),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showArrivePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = if (hasTravelError) {
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.edit_label_travel_arrival),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = leg.arriveTime?.let { Formatters.formatTime(it) } ?: stringResource(R.string.edit_time_pick),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            if (hasTravelError) {
                legErrors.firstOrNull()?.let { error ->
                    Text(
                        text = stringResource(
                            R.string.edit_error_prefix,
                            stringResource(error.messageRes)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            duration?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.edit_travel_duration, Formatters.formatDuration(it)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (leg.paidMinutesOverride != null && leg.startTime == null && leg.arriveTime == null) {
                Text(
                    text = stringResource(R.string.edit_travel_legacy_override, leg.paidMinutesOverride),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = leg.startLabel ?: "",
                onValueChange = { if (it.length <= 100) onStartLabelChange(it) },
                label = { Text(stringResource(R.string.edit_label_from_optional)) },
                placeholder = { Text(stringResource(R.string.edit_placeholder_start_location)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = leg.endLabel ?: "",
                onValueChange = { if (it.length <= 100) onEndLabelChange(it) },
                label = { Text(stringResource(R.string.edit_label_to_optional)) },
                placeholder = { Text(stringResource(R.string.edit_placeholder_destination)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initialTime = leg.startTime ?: java.time.LocalTime.of(8, 0),
            onTimeSelected = { onStartChange(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showArrivePicker) {
        TimePickerDialog(
            initialTime = leg.arriveTime ?: java.time.LocalTime.of(9, 0),
            onTimeSelected = { onArriveChange(it); showArrivePicker = false },
            onDismiss = { showArrivePicker = false }
        )
    }
}

private fun ValidationError.matchesTravelLeg(index: Int): Boolean {
    return when (this) {
        is ValidationError.TravelArriveBeforeStart -> legIndex == index
        is ValidationError.TravelTooLong -> legIndex == index
        is ValidationError.TravelLegIncomplete -> legIndex == index
        is ValidationError.TravelLegMissingTimeWindow -> legIndex == index
        else -> false
    }
}

@Composable
fun LocationLabelsSection(
    dayLocationLabel: String?,
    onDayLocationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.edit_section_location),
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = dayLocationLabel ?: "",
            onValueChange = { if (it.length <= 100) onDayLocationChange(it) },
            label = { Text(stringResource(R.string.edit_label_day_location_required)) },
            placeholder = { Text(stringResource(R.string.edit_placeholder_work_location)) },
            isError = dayLocationLabel?.isBlank() == true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun NoteSection(
    note: String?,
    onNoteChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.edit_section_note_optional),
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = note ?: "",
            onValueChange = { if (it.length <= 500) onNoteChange(it) },
            placeholder = { Text(stringResource(R.string.edit_placeholder_note)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            maxLines = 5
        )
    }
}

@Composable
fun MealAllowanceSection(
    isArrivalDeparture: Boolean,
    breakfastIncluded: Boolean,
    allowancePreviewCents: Int,
    onArrivalDepartureChange: (Boolean) -> Unit,
    onBreakfastIncludedChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.edit_section_meal_allowance),
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Checkbox(
                checked = isArrivalDeparture,
                onCheckedChange = onArrivalDepartureChange
            )
            Text(
                text = stringResource(R.string.meal_allowance_arrival_departure_label),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Checkbox(
                checked = breakfastIncluded,
                onCheckedChange = onBreakfastIncludedChange
            )
            Text(
                text = stringResource(R.string.meal_allowance_breakfast_label),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = stringResource(
                R.string.meal_allowance_preview_label,
                MealAllowanceCalculator.formatEuro(allowancePreviewCents)
            ),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DeleteDayConfirmDialog(
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        title = { Text(stringResource(R.string.dialog_delete_day_title)) },
        text = {
            Text(
                text = stringResource(R.string.dialog_delete_day_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = onConfirm,
                enabled = !isLoading,
                isLoading = isLoading,
                icon = Icons.Default.Delete
            ) {
                Text(stringResource(R.string.action_delete_day))
            }
        },
        dismissButton = {
            TertiaryActionButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}



@Composable
private fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.dialog_discard_changes_title)) },
        text = {
            Text(
                text = stringResource(R.string.dialog_discard_changes_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            DestructiveActionButton(onClick = onDiscard) {
                Text(stringResource(R.string.action_discard))
            }
        },
        dismissButton = {
            TertiaryActionButton(onClick = onKeepEditing) {
                Text(stringResource(R.string.action_keep_editing))
            }
        }
    )
}

private fun formatShortDate(date: java.time.LocalDate): String {
    return date.format(editShortDateFormatter)
}

private val editShortDateFormatter = DateTimeFormatter.ofPattern("E, dd.MM.", Locale.GERMAN)
