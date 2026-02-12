package de.montagezeit.app.ui.screen.edit

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import de.montagezeit.app.ui.common.DatePickerDialog
import de.montagezeit.app.ui.common.DestructiveActionButton
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton
import de.montagezeit.app.ui.common.TimePickerDialog
import de.montagezeit.app.ui.util.DateTimeUtils
import de.montagezeit.app.ui.util.Formatters
import java.time.LocalDate
import java.time.Duration
import java.time.format.DateTimeFormatter

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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                        onPickDate = { showNavigateDatePicker = true },
                        modifier = Modifier.pointerInput(date, onNavigateDate) {
                            var dragAccum = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                                onDragEnd = {
                                    if (dragAccum > swipeThresholdPx) {
                                        onNavigateDate(date.minusDays(1))
                                    } else if (dragAccum < -swipeThresholdPx) {
                                        onNavigateDate(date.plusDays(1))
                                    }
                                    dragAccum = 0f
                                },
                                onDragCancel = { dragAccum = 0f }
                            )
                        }
                    )
                    Divider()
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
                            workStart = formData.workStart,
                            workEnd = formData.workEnd,
                            breakMinutes = formData.breakMinutes,
                            dayLocationLabel = formData.dayLocationLabel ?: stringResource(R.string.location_leipzig),
                            dayLocationSource = formData.dayLocationSource,
                            dayLocationLat = formData.dayLocationLat,
                            dayLocationLon = formData.dayLocationLon,
                            dayLocationAccuracyMeters = formData.dayLocationAccuracyMeters,
                            morningLocationLabel = formData.morningLocationLabel,
                            eveningLocationLabel = formData.eveningLocationLabel,
                            note = formData.note,
                            needsReview = formData.needsReview
                        )
                        EditFormContent(
                            entry = dummyEntry,
                            formData = formData,
                            validationErrors = state.validationErrors,
                            onDayTypeChange = { viewModel.updateDayType(it) },
                            onWorkStartChange = { h, m -> viewModel.updateWorkStart(h, m) },
                            onWorkEndChange = { h, m -> viewModel.updateWorkEnd(h, m) },
                            onBreakMinutesChange = { viewModel.updateBreakMinutes(it) },
                            onTravelStartChange = { viewModel.updateTravelStart(it) },
                            onTravelArriveChange = { viewModel.updateTravelArrive(it) },
                            onTravelLabelStartChange = { viewModel.updateTravelLabelStart(it) },
                            onTravelLabelEndChange = { viewModel.updateTravelLabelEnd(it) },
                            onTravelClear = { viewModel.clearTravel() },
                            onDayLocationChange = { viewModel.updateDayLocationLabel(it) },
                            onMorningLabelChange = { viewModel.updateMorningLocationLabel(it) },
                            onEveningLabelChange = { viewModel.updateEveningLocationLabel(it) },
                            onNoteChange = { viewModel.updateNote(it) },
                            onResetReview = { viewModel.resetNeedsReview() },
                            onSave = { viewModel.save() },
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
                                            text = "• ${error.message}",
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
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    is EditUiState.Success -> {
                        val entry = state.entry
                        EditFormContent(
                            entry = entry,
                            formData = formData,
                            validationErrors = state.validationErrors,
                            onDayTypeChange = { viewModel.updateDayType(it) },
                            onWorkStartChange = { h, m -> viewModel.updateWorkStart(h, m) },
                            onWorkEndChange = { h, m -> viewModel.updateWorkEnd(h, m) },
                            onBreakMinutesChange = { viewModel.updateBreakMinutes(it) },
                            onTravelStartChange = { viewModel.updateTravelStart(it) },
                            onTravelArriveChange = { viewModel.updateTravelArrive(it) },
                            onTravelLabelStartChange = { viewModel.updateTravelLabelStart(it) },
                            onTravelLabelEndChange = { viewModel.updateTravelLabelEnd(it) },
                            onTravelClear = { viewModel.clearTravel() },
                            onDayLocationChange = { viewModel.updateDayLocationLabel(it) },
                            onMorningLabelChange = { viewModel.updateMorningLocationLabel(it) },
                            onEveningLabelChange = { viewModel.updateEveningLocationLabel(it) },
                            onNoteChange = { viewModel.updateNote(it) },
                            onResetReview = { viewModel.resetNeedsReview() },
                            onSave = { viewModel.save() },
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
                                            text = "• ${error.message}",
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

                        if (state.showConfirmDialog) {
                            BorderzoneConfirmDialog(
                                onConfirm = { viewModel.confirmAndSave() },
                                onDismiss = { viewModel.dismissConfirmDialog() }
                            )
                        }
                    }

                    is EditUiState.Saved -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = stringResource(R.string.edit_saved),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            PrimaryActionButton(onClick = onDismiss) {
                                Text(stringResource(R.string.action_close))
                            }
                        }
                    }
                }
            }
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
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.edit_cd_prev_day)
            )
        }
        TextButton(onClick = onPickDate) {
            Text(formatShortDate(date))
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun EditFormContent(
    entry: de.montagezeit.app.data.local.entity.WorkEntry,
    formData: EditFormData,
    validationErrors: List<ValidationError> = emptyList(),
    onDayTypeChange: (de.montagezeit.app.data.local.entity.DayType) -> Unit,
    onWorkStartChange: (Int, Int) -> Unit,
    onWorkEndChange: (Int, Int) -> Unit,
    onBreakMinutesChange: (Int) -> Unit,
    onTravelStartChange: (java.time.LocalTime?) -> Unit,
    onTravelArriveChange: (java.time.LocalTime?) -> Unit,
    onTravelLabelStartChange: (String) -> Unit,
    onTravelLabelEndChange: (String) -> Unit,
    onTravelClear: () -> Unit,
    onDayLocationChange: (String) -> Unit,
    onMorningLabelChange: (String) -> Unit,
    onEveningLabelChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onResetReview: () -> Unit,
    onApplyDefaultTimes: (() -> Unit)? = null,
    onCopyPrevious: (() -> Unit)? = null,
    onSave: () -> Unit,
    isSaving: Boolean = false,
    isNewEntry: Boolean = false,
    onCopy: (() -> Unit)? = null,
    showPrimarySaveButton: Boolean = true
) {
    Text(
        text = formatDate(entry.date),
        style = MaterialTheme.typography.headlineSmall
    )

    EditFormSectionCard {
        DayTypeSelector(
            selectedType = formData.dayType,
            onTypeChange = onDayTypeChange
        )
    }

    EditFormSectionCard {
        WorkTimesSection(
            workStart = formData.workStart,
            workEnd = formData.workEnd,
            breakMinutes = formData.breakMinutes,
            validationErrors = validationErrors,
            onStartChange = onWorkStartChange,
            onEndChange = onWorkEndChange,
            onBreakChange = onBreakMinutesChange,
            onApplyDefaults = onApplyDefaultTimes
        )
    }

    EditFormSectionCard {
        TravelSection(
            travelStartTime = formData.travelStartTime,
            travelArriveTime = formData.travelArriveTime,
            travelLabelStart = formData.travelLabelStart,
            travelLabelEnd = formData.travelLabelEnd,
            validationErrors = validationErrors,
            onTravelStartChange = onTravelStartChange,
            onTravelArriveChange = onTravelArriveChange,
            onTravelLabelStartChange = onTravelLabelStartChange,
            onTravelLabelEndChange = onTravelLabelEndChange,
            onClearTravel = onTravelClear
        )
    }

    EditFormSectionCard {
        LocationLabelsSection(
            entry = entry,
            dayLocationLabel = formData.dayLocationLabel,
            onDayLocationChange = onDayLocationChange,
            morningLabel = formData.morningLocationLabel,
            eveningLabel = formData.eveningLocationLabel,
            onMorningLabelChange = onMorningLabelChange,
            onEveningLabelChange = onEveningLabelChange
        )
    }

    EditFormSectionCard {
        NoteSection(
            note = formData.note,
            onNoteChange = onNoteChange
        )
    }

    val showSecondaryActions = (formData.needsReview || entry.needsReview) ||
        onCopyPrevious != null ||
        (onCopy != null && !isNewEntry)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (formData.needsReview || entry.needsReview) {
            SecondaryActionButton(
                onClick = onResetReview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.edit_action_reset_review_flag))
            }
        }

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

        if (showSecondaryActions && showPrimarySaveButton) {
            Divider()
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
    workStart: java.time.LocalTime,
    workEnd: java.time.LocalTime,
    breakMinutes: Int,
    validationErrors: List<ValidationError> = emptyList(),
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
        Text(
            text = stringResource(R.string.edit_section_work_times),
            style = MaterialTheme.typography.titleMedium
        )
        
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
                        text = formatTime(workStart),
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
                        text = formatTime(workEnd),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        if (hasWorkTimeError) {
            Text(
                text = stringResource(R.string.edit_error_prefix, ValidationError.WorkEndBeforeStart.message),
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
                        text = stringResource(R.string.edit_error_prefix, it.message),
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
fun TravelSection(
    travelStartTime: java.time.LocalTime?,
    travelArriveTime: java.time.LocalTime?,
    travelLabelStart: String?,
    travelLabelEnd: String?,
    validationErrors: List<ValidationError> = emptyList(),
    onTravelStartChange: (java.time.LocalTime?) -> Unit,
    onTravelArriveChange: (java.time.LocalTime?) -> Unit,
    onTravelLabelStartChange: (String) -> Unit,
    onTravelLabelEndChange: (String) -> Unit,
    onClearTravel: () -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showArrivePicker by remember { mutableStateOf(false) }
    val hasTravelData = travelStartTime != null ||
        travelArriveTime != null ||
        !travelLabelStart.isNullOrBlank() ||
        !travelLabelEnd.isNullOrBlank()
    val duration = remember(travelStartTime, travelArriveTime) {
        DateTimeUtils.calculateTravelDuration(travelStartTime, travelArriveTime)
    }
    val durationText = duration?.let { Formatters.formatDuration(it) }
    val hasTravelError = validationErrors.any { it is ValidationError.TravelArriveBeforeStart }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.edit_section_travel),
            style = MaterialTheme.typography.titleMedium
        )

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
                        text = travelStartTime?.let { formatTime(it) } ?: stringResource(R.string.edit_time_pick),
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
                        text = travelArriveTime?.let { formatTime(it) } ?: stringResource(R.string.edit_time_pick),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        if (hasTravelError) {
            Text(
                text = stringResource(R.string.edit_error_prefix, ValidationError.TravelArriveBeforeStart.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        durationText?.let {
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
                    text = stringResource(R.string.edit_travel_duration, it),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedTextField(
            value = travelLabelStart ?: "",
            onValueChange = onTravelLabelStartChange,
            label = { Text(stringResource(R.string.edit_label_from_optional)) },
            placeholder = { Text(stringResource(R.string.edit_placeholder_start_location)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = travelLabelEnd ?: "",
            onValueChange = onTravelLabelEndChange,
            label = { Text(stringResource(R.string.edit_label_to_optional)) },
            placeholder = { Text(stringResource(R.string.edit_placeholder_destination)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (hasTravelData) {
            DestructiveActionButton(
                onClick = onClearTravel,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(stringResource(R.string.edit_action_clear_travel_data))
            }
        }
    }

    if (showStartPicker) {
TimePickerDialog(
            initialTime = travelStartTime ?: java.time.LocalTime.of(8, 0),
            onTimeSelected = { onTravelStartChange(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showArrivePicker) {
TimePickerDialog(
            initialTime = travelArriveTime ?: java.time.LocalTime.of(9, 0),
            onTimeSelected = { onTravelArriveChange(it); showArrivePicker = false },
            onDismiss = { showArrivePicker = false }
        )
    }
}

@Composable
fun LocationLabelsSection(
    entry: de.montagezeit.app.data.local.entity.WorkEntry,
    dayLocationLabel: String?,
    onDayLocationChange: (String) -> Unit,
    morningLabel: String?,
    eveningLabel: String?,
    onMorningLabelChange: (String) -> Unit,
    onEveningLabelChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.edit_section_location),
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = dayLocationLabel ?: "",
            onValueChange = onDayLocationChange,
            label = { Text(stringResource(R.string.edit_label_day_location_required)) },
            placeholder = { Text(stringResource(R.string.edit_placeholder_city_leipzig)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        if (entry.morningCapturedAt != null) {
            OutlinedTextField(
                value = morningLabel ?: "",
                onValueChange = onMorningLabelChange,
                label = { Text(stringResource(R.string.edit_label_morning_optional)) },
                placeholder = { Text(stringResource(R.string.edit_placeholder_city_berlin)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        if (entry.eveningCapturedAt != null) {
            OutlinedTextField(
                value = eveningLabel ?: "",
                onValueChange = onEveningLabelChange,
                label = { Text(stringResource(R.string.edit_label_evening_optional)) },
                placeholder = { Text(stringResource(R.string.edit_placeholder_city_berlin)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
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
            onValueChange = onNoteChange,
            placeholder = { Text(stringResource(R.string.edit_placeholder_note)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            maxLines = 5
        )
    }
}

@Composable
fun BorderzoneConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_confirm_title)) },
        text = {
            Text(
                stringResource(R.string.edit_confirm_borderzone_text),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            PrimaryActionButton(onClick = onConfirm) {
                Text(stringResource(R.string.edit_confirm_yes_save))
            }
        },
        dismissButton = {
            TertiaryActionButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}



private fun formatDate(date: java.time.LocalDate): String {
    return date.format(
        DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", java.util.Locale.GERMAN)
    )
}

private fun formatShortDate(date: java.time.LocalDate): String {
    return date.format(
        DateTimeFormatter.ofPattern("E, dd.MM.", java.util.Locale.GERMAN)
    )
}

private fun formatTime(time: java.time.LocalTime): String {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

// Removed: calculateTravelDuration - now using DateTimeUtils.calculateTravelDuration

// Removed: formatDuration - now using Formatters.formatDuration
