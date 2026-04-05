package de.montagezeit.app.ui.screen.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.ui.components.DestructiveActionButton
import de.montagezeit.app.ui.components.PrimaryActionButton
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.TimePickerDialog
import de.montagezeit.app.ui.util.Formatters

@Composable
internal fun EditFormSectionCard(
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
internal fun EditFormContent(
    entry: WorkEntry,
    formData: EditFormData,
    validationErrors: List<ValidationError> = emptyList(),
    dailyTargetHours: Double = 8.0,
    onDayTypeChange: (DayType) -> Unit,
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
    val isCompTime = formData.dayType == DayType.COMP_TIME

    Text(
        text = Formatters.formatDateLong(entry.date),
        style = MaterialTheme.typography.headlineSmall
    )

    EditFormSectionCard {
        DayTypeSelector(
            selectedType = formData.dayType,
            onTypeChange = onDayTypeChange
        )
        if (isCompTime) {
            SuggestionChip(
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

    if (!isCompTime) {
        if (formData.dayType == DayType.WORK) {
            EditFormSectionCard {
                EditWorkTimesSection(
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

    if (formData.dayType == DayType.WORK) {
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
internal fun DayTypeSelector(
    selectedType: DayType,
    onTypeChange: (DayType) -> Unit
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
            DayType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeChange(type) },
                    label = {
                        Text(
                            when (type) {
                                DayType.WORK -> stringResource(R.string.edit_day_type_workday)
                                DayType.OFF -> stringResource(R.string.edit_day_type_off)
                                DayType.COMP_TIME -> stringResource(R.string.edit_day_type_comp_time)
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
internal fun EditWorkTimesSection(
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

    val hasWorkTimeError = validationErrors.any {
        it is ValidationError.WorkEndBeforeStart || it is ValidationError.WorkDayTooLong
    }
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
internal fun LocationLabelsSection(
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
internal fun NoteSection(
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
internal fun MealAllowanceSection(
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
