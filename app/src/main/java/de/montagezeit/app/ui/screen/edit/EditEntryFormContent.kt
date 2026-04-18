@file:Suppress("TooManyFunctions", "MaxLineLength", "MagicNumber", "LongMethod", "LongParameterList")

package de.montagezeit.app.ui.screen.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.ui.components.MZContentCard
import de.montagezeit.app.ui.components.MZInlineNotice
import de.montagezeit.app.ui.components.MZSegmentedControl
import de.montagezeit.app.ui.components.MZSegmentedOption
import de.montagezeit.app.ui.components.MZSectionHeader
import de.montagezeit.app.ui.components.MZStatusBadge
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.StatusType
import de.montagezeit.app.ui.components.TertiaryActionButton
import de.montagezeit.app.ui.components.TimePickerDialog
import de.montagezeit.app.ui.components.mzOutlinedTextFieldColors
import de.montagezeit.app.ui.util.Formatters

@Composable
internal fun EditFormSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    MZContentCard(
        modifier = modifier,
        content = content
    )
}

@Composable
internal fun EditFormContent(
    formData: EditFormData,
    validationErrors: List<ValidationError> = emptyList(),
    dailyTargetHours: Double = 8.0,
    mealAllowancePreviewCents: Int = 0,
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
    isSaving: Boolean = false
) {
    val isCompTime = formData.dayType == DayType.COMP_TIME
    val isMealEligibleType = formData.dayType == DayType.WORK &&
        formData.dayLocationLabel?.trim()?.lowercase() != "leipzig"

    val travelHasErrors = validationErrors.any {
        it is ValidationError.TravelArriveBeforeStart ||
            it is ValidationError.TravelTooLong ||
            it is ValidationError.TravelLegIncomplete ||
            it is ValidationError.TravelLegMissingTimeWindow ||
            it is ValidationError.TravelNotAllowedForCompTime ||
            it is ValidationError.MissingWorkOrTravel
    }
    val locationHasErrors = validationErrors.any { it is ValidationError.MissingDayLocation }

    var travelExpanded by rememberSaveable { mutableStateOf(formData.travelLegs.isNotEmpty()) }
    var locationExpanded by rememberSaveable {
        mutableStateOf(
            formData.dayType.isWorkLike &&
                formData.dayLocationLabel.isNullOrBlank()
        )
    }
    var mealExpanded by rememberSaveable {
        mutableStateOf(formData.mealIsArrivalDeparture || formData.mealBreakfastIncluded)
    }
    var noteExpanded by rememberSaveable { mutableStateOf(!formData.note.isNullOrBlank()) }

    LaunchedEffect(travelHasErrors) { if (travelHasErrors) travelExpanded = true }
    LaunchedEffect(locationHasErrors) { if (locationHasErrors) locationExpanded = true }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditFormSectionCard {
            DayTypeSelector(
                selectedType = formData.dayType,
                onTypeChange = onDayTypeChange
            )
            if (isCompTime) {
                MZStatusBadge(
                    text = stringResource(
                        R.string.edit_comp_time_info,
                        dailyTargetHours.toInt()
                    ),
                    type = StatusType.NEUTRAL,
                    modifier = Modifier.fillMaxWidth(),
                    showIcon = false
                )
            }
        }

        if (!isCompTime && formData.dayType.isWorkLike) {
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

        if (!isCompTime) {
            val travelSummary = when {
                travelHasErrors -> stringResource(R.string.edit_validation_title)
                formData.travelLegs.isNotEmpty() -> "${formData.travelLegs.size} Fahrt(en)"
                else -> null
            }
            CollapsibleSectionHeader(
                title = stringResource(R.string.edit_section_travel),
                expanded = travelExpanded,
                onToggle = { travelExpanded = !travelExpanded },
                summary = travelSummary,
                hasError = travelHasErrors
            )
            AnimatedVisibility(visible = travelExpanded) {
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
            }

            val locationSummary = formData.dayLocationLabel?.takeIf { it.isNotBlank() }
            CollapsibleSectionHeader(
                title = stringResource(R.string.edit_section_location),
                expanded = locationExpanded,
                onToggle = { locationExpanded = !locationExpanded },
                summary = locationSummary,
                hasError = locationHasErrors
            )
            AnimatedVisibility(visible = locationExpanded) {
                EditFormSectionCard {
                    LocationLabelsSection(
                        dayLocationLabel = formData.dayLocationLabel,
                        validationErrors = validationErrors,
                        onDayLocationChange = onDayLocationChange
                    )
                }
            }
        }

        if (isMealEligibleType) {
            val mealSummary = if (mealAllowancePreviewCents > 0) {
                MealAllowanceCalculator.formatEuro(mealAllowancePreviewCents)
            } else null
            CollapsibleSectionHeader(
                title = stringResource(R.string.edit_section_meal_allowance),
                expanded = mealExpanded,
                onToggle = { mealExpanded = !mealExpanded },
                summary = mealSummary
            )
            AnimatedVisibility(visible = mealExpanded) {
                EditFormSectionCard {
                    MealAllowanceSection(
                        isArrivalDeparture = formData.mealIsArrivalDeparture,
                        breakfastIncluded = formData.mealBreakfastIncluded,
                        allowancePreviewCents = mealAllowancePreviewCents,
                        onArrivalDepartureChange = onMealArrivalDepartureChange,
                        onBreakfastIncludedChange = onMealBreakfastIncludedChange
                    )
                }
            }
        }

        val noteSummary = if (!formData.note.isNullOrBlank()) {
            stringResource(R.string.edit_note_present_summary)
        } else {
            null
        }
        CollapsibleSectionHeader(
            title = stringResource(R.string.edit_section_note_optional),
            expanded = noteExpanded,
            onToggle = { noteExpanded = !noteExpanded },
            summary = noteSummary
        )
        AnimatedVisibility(visible = noteExpanded) {
            EditFormSectionCard {
                NoteSection(
                    note = formData.note,
                    onNoteChange = onNoteChange
                )
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
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    summary: String? = null,
    hasError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (hasError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
            if (!expanded && summary != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = if (hasError) MaterialTheme.colorScheme.error
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun DayTypeSelector(
    selectedType: DayType,
    onTypeChange: (DayType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = stringResource(R.string.edit_section_day_type))
        MZSegmentedControl(
            options = listOf(
                MZSegmentedOption(DayType.WORK, stringResource(R.string.edit_day_type_workday)),
                MZSegmentedOption(DayType.OFF, stringResource(R.string.edit_day_type_off)),
                MZSegmentedOption(DayType.COMP_TIME, stringResource(R.string.edit_day_type_comp_time))
            ),
            selectedValue = selectedType,
            onValueSelected = onTypeChange
        )
        MZSegmentedControl(
            options = listOf(
                MZSegmentedOption(DayType.SCHULUNG, stringResource(R.string.edit_day_type_schulung)),
                MZSegmentedOption(DayType.LEHRGANG, stringResource(R.string.edit_day_type_lehrgang))
            ),
            selectedValue = selectedType,
            onValueSelected = onTypeChange
        )
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

    val workTimeError = validationErrors.firstOrNull {
        it is ValidationError.WorkEndBeforeStart || it is ValidationError.WorkDayTooLong
    }
    val breakError = validationErrors.firstOrNull {
        it is ValidationError.NegativeBreakMinutes || it is ValidationError.BreakLongerThanWorkTime
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(text = stringResource(R.string.edit_section_work_times))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.edit_toggle_work_times),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimeValueButton(
                    label = stringResource(R.string.edit_label_start),
                    value = Formatters.formatTime(workStart),
                    isError = workTimeError != null,
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f)
                )
                TimeValueButton(
                    label = stringResource(R.string.edit_label_end),
                    value = Formatters.formatTime(workEnd),
                    isError = workTimeError != null,
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            workTimeError?.let {
                ValidationMessage(it)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.edit_label_break_minutes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.edit_break_minutes_value, breakMinutes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (breakError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = breakMinutes.toFloat(),
                    onValueChange = { onBreakChange((it / 5f).toInt() * 5) },
                    valueRange = 0f..120f,
                    steps = 23,
                    colors = if (breakError != null) {
                        SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.error,
                            activeTrackColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        SliderDefaults.colors()
                    }
                )
                breakError?.let {
                    ValidationMessage(it)
                }
            }

            if (onApplyDefaults != null) {
                TertiaryActionButton(
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
    validationErrors: List<ValidationError> = emptyList(),
    onDayLocationChange: (String) -> Unit
) {
    val locationError = validationErrors.firstOrNull { it is ValidationError.MissingDayLocation }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = stringResource(R.string.edit_section_location))
        OutlinedTextField(
            value = dayLocationLabel ?: "",
            onValueChange = { if (it.length <= 100) onDayLocationChange(it) },
            label = { Text(stringResource(R.string.day_location_label)) },
            placeholder = { Text(stringResource(R.string.edit_placeholder_work_location)) },
            isError = locationError != null,
            colors = mzOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        locationError?.let {
            ValidationMessage(it)
        }
    }
}

@Composable
internal fun NoteSection(
    note: String?,
    onNoteChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(text = stringResource(R.string.edit_section_note_optional))
        OutlinedTextField(
            value = note ?: "",
            onValueChange = { if (it.length <= 500) onNoteChange(it) },
            placeholder = { Text(stringResource(R.string.edit_placeholder_note)) },
            colors = mzOutlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
            minLines = 3,
            maxLines = 4
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text = stringResource(R.string.edit_section_meal_allowance))
        MealAllowanceRow(
            checked = isArrivalDeparture,
            label = stringResource(R.string.meal_allowance_arrival_departure_short_label),
            amount = stringResource(R.string.meal_allowance_arrival_departure_amount),
            onCheckedChange = onArrivalDepartureChange
        )
        MealAllowanceRow(
            checked = breakfastIncluded,
            label = stringResource(R.string.meal_allowance_breakfast_short_label),
            amount = stringResource(R.string.meal_allowance_breakfast_amount),
            onCheckedChange = onBreakfastIncludedChange
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.edit_meal_total_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = MealAllowanceCalculator.formatEuro(allowancePreviewCents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    MZSectionHeader(
        title = text
    )
}

@Composable
private fun TimeValueButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    SecondaryActionButton(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ValidationMessage(error: ValidationError) {
    Text(
        text = stringResource(
            R.string.edit_error_prefix,
            stringResource(error.messageRes)
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun MealAllowanceRow(
    checked: Boolean,
    label: String,
    amount: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
