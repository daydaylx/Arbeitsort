@file:Suppress("LongMethod", "MaxLineLength", "LongParameterList")

package de.montagezeit.app.ui.screen.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.TravelLegCategory
import de.montagezeit.app.ui.components.MZSegmentedControl
import de.montagezeit.app.ui.components.MZSegmentedOption
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.TertiaryActionButton
import de.montagezeit.app.ui.components.TimePickerDialog
import de.montagezeit.app.ui.components.mzOutlinedTextFieldColors
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.util.DateTimeUtils
import de.montagezeit.app.ui.util.Formatters
import java.time.LocalTime

private enum class TravelMode { NONE, OUTBOUND, RETURN, BOTH, MANUAL }

private fun deriveTravelMode(legs: List<EditTravelLegForm>): TravelMode = when {
    legs.isEmpty() -> TravelMode.NONE
    legs.size == 1 && legs.single().category == TravelLegCategory.OUTBOUND -> TravelMode.OUTBOUND
    legs.size == 1 && legs.single().category == TravelLegCategory.RETURN -> TravelMode.RETURN
    legs.size == 2 &&
        legs[0].category == TravelLegCategory.OUTBOUND &&
        legs[1].category == TravelLegCategory.RETURN -> TravelMode.BOTH
    else -> TravelMode.MANUAL
}

@Composable
internal fun TravelLegsSection(
    travelLegs: List<EditTravelLegForm>,
    validationErrors: List<ValidationError> = emptyList(),
    onAddTravelLeg: (TravelLegCategory) -> Unit,
    onTravelLegStartChange: (Int, LocalTime?) -> Unit,
    onTravelLegArriveChange: (Int, LocalTime?) -> Unit,
    onTravelLegStartLabelChange: (Int, String) -> Unit,
    onTravelLegEndLabelChange: (Int, String) -> Unit,
    onRemoveTravelLeg: (Int) -> Unit,
    onClearTravel: () -> Unit
) {
    var selectedMode by rememberSaveable { mutableStateOf(deriveTravelMode(travelLegs)) }

    LaunchedEffect(travelLegs) {
        selectedMode = deriveTravelMode(travelLegs)
    }

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
            if (selectedMode != TravelMode.NONE) {
                TertiaryActionButton(onClick = {
                    onClearTravel()
                    selectedMode = TravelMode.NONE
                }) {
                    Text(stringResource(R.string.edit_action_clear_all_travel))
                }
            }
        }

        val modeOptions = listOf(
            MZSegmentedOption(TravelMode.NONE, stringResource(R.string.edit_travel_mode_none)),
            MZSegmentedOption(TravelMode.OUTBOUND, stringResource(R.string.edit_travel_mode_outbound)),
            MZSegmentedOption(TravelMode.RETURN, stringResource(R.string.edit_travel_mode_return)),
            MZSegmentedOption(TravelMode.BOTH, stringResource(R.string.edit_travel_mode_both)),
            MZSegmentedOption(TravelMode.MANUAL, stringResource(R.string.edit_travel_mode_manual))
        )
        MZSegmentedControl(
            options = modeOptions,
            selectedValue = selectedMode,
            onValueSelected = { mode ->
                selectedMode = mode
                when (mode) {
                    TravelMode.NONE -> onClearTravel()
                    TravelMode.OUTBOUND -> {
                        onClearTravel()
                        onAddTravelLeg(TravelLegCategory.OUTBOUND)
                    }
                    TravelMode.RETURN -> {
                        onClearTravel()
                        onAddTravelLeg(TravelLegCategory.RETURN)
                    }
                    TravelMode.BOTH -> {
                        onClearTravel()
                        onAddTravelLeg(TravelLegCategory.OUTBOUND)
                        onAddTravelLeg(TravelLegCategory.RETURN)
                    }
                    TravelMode.MANUAL -> Unit
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (selectedMode != TravelMode.NONE) {
            travelLegs.forEachIndexed { index, leg ->
                key(leg.draftId) {
                    TravelLegCard(
                        index = index,
                        leg = leg,
                        validationErrors = validationErrors,
                        onStartChange = onTravelLegStartChange,
                        onArriveChange = onTravelLegArriveChange,
                        onStartLabelChange = onTravelLegStartLabelChange,
                        onEndLabelChange = onTravelLegEndLabelChange,
                        onRemove = onRemoveTravelLeg
                    )
                }
            }

            if (selectedMode == TravelMode.MANUAL) {
                SecondaryActionButton(
                    onClick = { onAddTravelLeg(TravelLegCategory.OTHER) },
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
    }
}

@Composable
private fun TravelLegCard(
    index: Int,
    leg: EditTravelLegForm,
    validationErrors: List<ValidationError>,
    onStartChange: (Int, LocalTime?) -> Unit,
    onArriveChange: (Int, LocalTime?) -> Unit,
    onStartLabelChange: (Int, String) -> Unit,
    onEndLabelChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showArrivePicker by remember { mutableStateOf(false) }
    val legErrors = validationErrors.filter { error -> error.matchesTravelLeg(index) }
    val hasTravelError = legErrors.isNotEmpty()
    val duration = remember(leg.startTime, leg.arriveTime) {
        DateTimeUtils.calculateTravelDuration(leg.startTime, leg.arriveTime)
    }

    Surface(
        shape = RoundedCornerShape(MZTokens.RadiusCard),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (hasTravelError) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = MZTokens.BorderAlphaSubtle)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = travelLegTitle(index = index, leg = leg),
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_remove_travel_leg)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TravelTimeButton(
                    label = stringResource(R.string.edit_label_travel_start),
                    value = leg.startTime?.let(Formatters::formatTime)
                        ?: stringResource(R.string.edit_time_pick),
                    isError = hasTravelError,
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f)
                )
                TravelTimeButton(
                    label = stringResource(R.string.edit_label_travel_arrival),
                    value = leg.arriveTime?.let(Formatters::formatTime)
                        ?: stringResource(R.string.edit_time_pick),
                    isError = hasTravelError,
                    onClick = { showArrivePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (hasTravelError) {
                legErrors.firstOrNull()?.let { error ->
                    Text(
                        text = stringResource(
                            R.string.edit_error_prefix,
                            stringResource(error.messageRes)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (duration != null || (leg.paidMinutesOverride ?: 0) > 0) {
                Text(
                    text = stringResource(
                        R.string.edit_travel_duration,
                        duration?.let(Formatters::formatDuration)
                            ?: Formatters.formatDuration(java.time.Duration.ofMinutes(leg.paidMinutesOverride!!.toLong()))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = leg.startLabel ?: "",
                onValueChange = { if (it.length <= 100) onStartLabelChange(index, it) },
                label = { Text(stringResource(R.string.edit_label_from_optional)) },
                placeholder = { Text(stringResource(R.string.edit_placeholder_start_location)) },
                colors = mzOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = leg.endLabel ?: "",
                onValueChange = { if (it.length <= 100) onEndLabelChange(index, it) },
                label = { Text(stringResource(R.string.edit_label_to_optional)) },
                placeholder = { Text(stringResource(R.string.edit_placeholder_destination)) },
                colors = mzOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initialTime = leg.startTime ?: LocalTime.of(8, 0),
            onTimeSelected = { onStartChange(index, it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showArrivePicker) {
        TimePickerDialog(
            initialTime = leg.arriveTime ?: LocalTime.of(9, 0),
            onTimeSelected = { onArriveChange(index, it); showArrivePicker = false },
            onDismiss = { showArrivePicker = false }
        )
    }
}

@Composable
private fun travelLegTitle(index: Int, leg: EditTravelLegForm): String =
    when (leg.category) {
        TravelLegCategory.OUTBOUND -> stringResource(R.string.edit_travel_mode_outbound)
        TravelLegCategory.RETURN -> stringResource(R.string.edit_travel_mode_return)
        TravelLegCategory.INTERSITE -> stringResource(R.string.edit_travel_leg_intersite)
        TravelLegCategory.OTHER -> stringResource(R.string.edit_travel_leg_title, index + 1)
    }

@Composable
private fun TravelTimeButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    SecondaryActionButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
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

private fun ValidationError.matchesTravelLeg(index: Int): Boolean = when (this) {
    is ValidationError.TravelArriveBeforeStart -> legIndex == index
    is ValidationError.TravelTooLong -> legIndex == index
    is ValidationError.TravelLegIncomplete -> legIndex == index
    is ValidationError.TravelLegMissingTimeWindow -> legIndex == index
    else -> false
}
