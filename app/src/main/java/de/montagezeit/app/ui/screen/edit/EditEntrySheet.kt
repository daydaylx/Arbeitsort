package de.montagezeit.app.ui.screen.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.Duration
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntrySheet(
    date: LocalDate,
    viewModel: EditEntryViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val formData by viewModel.formData.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(date) {
        viewModel.setDate(date)
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        text = "Eintrag nicht gefunden",
                        style = MaterialTheme.typography.headlineSmall
                    )
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
                        onMorningLabelChange = { viewModel.updateMorningLocationLabel(it) },
                        onEveningLabelChange = { viewModel.updateEveningLocationLabel(it) },
                        onNoteChange = { viewModel.updateNote(it) },
                        onResetReview = { viewModel.resetNeedsReview() },
                        onSave = { viewModel.save() }
                    )

                    // Display validation errors
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
                                        text = "Eingabefehler",
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

                                TextButton(
                                    onClick = { viewModel.clearValidationErrors() }
                                ) {
                                    Text("OK")
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
                            text = "Gespeichert!",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Button(onClick = onDismiss) {
                            Text("Schließen")
                        }
                    }
                }
            }
        }
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
    onMorningLabelChange: (String) -> Unit,
    onEveningLabelChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onResetReview: () -> Unit,
    onSave: () -> Unit
) {
    // Header
    Text(
        text = formatDate(entry.date),
        style = MaterialTheme.typography.headlineSmall
    )
    
    Divider()

    // Day Type
    DayTypeSelector(
        selectedType = formData.dayType,
        onTypeChange = onDayTypeChange
    )

    Divider()

    // Work Times
    WorkTimesSection(
        workStart = formData.workStart,
        workEnd = formData.workEnd,
        breakMinutes = formData.breakMinutes,
        validationErrors = validationErrors,
        onStartChange = onWorkStartChange,
        onEndChange = onWorkEndChange,
        onBreakChange = onBreakMinutesChange
    )

    Divider()

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

    Divider()

    // Location Labels
    LocationLabelsSection(
        entry = entry,
        morningLabel = formData.morningLocationLabel,
        eveningLabel = formData.eveningLocationLabel,
        onMorningLabelChange = onMorningLabelChange,
        onEveningLabelChange = onEveningLabelChange
    )

    Divider()

    // Note
    NoteSection(
        note = formData.note,
        onNoteChange = onNoteChange
    )

    // Reset Needs Review
    if (formData.needsReview || entry.needsReview) {
        Divider()
        OutlinedButton(
            onClick = onResetReview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Überprüfungsflag zurücksetzen")
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Save Button
    Button(
        onClick = onSave,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("Speichern")
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
            text = "Tagtyp",
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
                                de.montagezeit.app.data.local.entity.DayType.WORK -> "Arbeitstag"
                                de.montagezeit.app.data.local.entity.DayType.OFF -> "Frei"
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
    onBreakChange: (Int) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    val hasWorkTimeError = validationErrors.any { it is ValidationError.WorkEndBeforeStart }
    val hasBreakError = validationErrors.any { 
        it is ValidationError.NegativeBreakMinutes || it is ValidationError.BreakLongerThanWorkTime 
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Arbeitszeiten",
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
                        text = "Beginn",
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
                        text = "Ende",
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
                text = "⚠ ${ValidationError.WorkEndBeforeStart.message}",
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
                    text = "Pause (Minuten)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasBreakError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$breakMinutes min",
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
                        text = "⚠ ${it.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
    
    if (showStartPicker) {
        SimpleTimePickerDialog(
            initialTime = workStart,
            onTimeSelected = { onStartChange(it.hour, it.minute); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    
    if (showEndPicker) {
        SimpleTimePickerDialog(
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
        calculateTravelDuration(travelStartTime, travelArriveTime)
    }
    val durationText = duration?.let { formatDuration(it) }
    val hasTravelError = validationErrors.any { it is ValidationError.TravelArriveBeforeStart }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Reise / Fahrzeit",
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
                        text = "Start",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = travelStartTime?.let { formatTime(it) } ?: "Zeit wählen",
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
                        text = "Ankunft",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = travelArriveTime?.let { formatTime(it) } ?: "Zeit wählen",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        if (hasTravelError) {
            Text(
                text = "⚠ ${ValidationError.TravelArriveBeforeStart.message}",
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
                    text = "Fahrzeit: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedTextField(
            value = travelLabelStart ?: "",
            onValueChange = onTravelLabelStartChange,
            label = { Text("Von (Optional)") },
            placeholder = { Text("Startort") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = travelLabelEnd ?: "",
            onValueChange = onTravelLabelEndChange,
            label = { Text("Nach (Optional)") },
            placeholder = { Text("Zielort") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (hasTravelData) {
            TextButton(
                onClick = onClearTravel,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text("Reisedaten löschen")
            }
        }
    }

    if (showStartPicker) {
        SimpleTimePickerDialog(
            initialTime = travelStartTime ?: java.time.LocalTime.of(8, 0),
            onTimeSelected = { onTravelStartChange(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showArrivePicker) {
        SimpleTimePickerDialog(
            initialTime = travelArriveTime ?: java.time.LocalTime.of(9, 0),
            onTimeSelected = { onTravelArriveChange(it); showArrivePicker = false },
            onDismiss = { showArrivePicker = false }
        )
    }
}

@Composable
fun LocationLabelsSection(
    entry: de.montagezeit.app.data.local.entity.WorkEntry,
    morningLabel: String?,
    eveningLabel: String?,
    onMorningLabelChange: (String) -> Unit,
    onEveningLabelChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Standort",
            style = MaterialTheme.typography.titleMedium
        )
        
        if (entry.morningCapturedAt != null) {
            OutlinedTextField(
                value = morningLabel ?: "",
                onValueChange = onMorningLabelChange,
                label = { Text("Morgens (Optional)") },
                placeholder = { Text("z.B. Berlin") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        if (entry.eveningCapturedAt != null) {
            OutlinedTextField(
                value = eveningLabel ?: "",
                onValueChange = onEveningLabelChange,
                label = { Text("Abends (Optional)") },
                placeholder = { Text("z.B. Berlin") },
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
            text = "Notiz (Optional)",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = note ?: "",
            onValueChange = onNoteChange,
            placeholder = { Text("Zusätzliche Informationen...") },
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
        title = { Text("Bestätigung erforderlich") },
        text = {
            Text(
                "Du befindest dich möglicherweise in der Grenzzone zu Leipzig. Bist du sicher, dass du speichern möchtest?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Ja, speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun SimpleTimePickerDialog(
    initialTime: java.time.LocalTime,
    onTimeSelected: (java.time.LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTime by remember { mutableStateOf(initialTime) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zeit wählen") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = formatTime(selectedTime),
                    style = MaterialTheme.typography.headlineLarge
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stunde",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Slider(
                            value = selectedTime.hour.toFloat(),
                            onValueChange = { selectedTime = selectedTime.withHour(it.toInt()) },
                            valueRange = 0f..23f
                        )
                        Text(
                            text = selectedTime.hour.toString(),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Minute",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Slider(
                            value = selectedTime.minute.toFloat(),
                            onValueChange = { selectedTime = selectedTime.withMinute(it.toInt()) },
                            valueRange = 0f..59f,
                            steps = 59
                        )
                        Text(
                            text = selectedTime.minute.toString(),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onTimeSelected(selectedTime) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

private fun formatDate(date: java.time.LocalDate): String {
    return date.format(
        DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", java.util.Locale.GERMAN)
    )
}

private fun formatTime(time: java.time.LocalTime): String {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun calculateTravelDuration(
    start: java.time.LocalTime?,
    arrive: java.time.LocalTime?
): Duration? {
    if (start == null || arrive == null) return null
    var duration = Duration.between(start, arrive)
    if (duration.isNegative) {
        duration = duration.plusHours(24)
    }
    return duration
}

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
