package de.montagezeit.app.ui.screen.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import de.montagezeit.app.ui.components.TimePickerDialog
import de.montagezeit.app.ui.util.DateTimeUtils
import de.montagezeit.app.ui.util.Formatters
import java.time.LocalTime

@Composable
internal fun TravelLegsSection(
    travelLegs: List<EditTravelLegForm>,
    validationErrors: List<ValidationError> = emptyList(),
    onAddTravelLeg: () -> Unit,
    onTravelLegStartChange: (Int, LocalTime?) -> Unit,
    onTravelLegArriveChange: (Int, LocalTime?) -> Unit,
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
                style = MaterialTheme.typography.bodySmall,
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
    onStartChange: (LocalTime?) -> Unit,
    onArriveChange: (LocalTime?) -> Unit,
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
                            text = leg.startTime?.let { Formatters.formatTime(it) }
                                ?: stringResource(R.string.edit_time_pick),
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
                            text = leg.arriveTime?.let { Formatters.formatTime(it) }
                                ?: stringResource(R.string.edit_time_pick),
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
                        text = stringResource(
                            R.string.edit_travel_duration,
                            Formatters.formatDuration(it)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (leg.paidMinutesOverride != null && leg.startTime == null && leg.arriveTime == null) {
                Text(
                    text = stringResource(
                        R.string.edit_travel_legacy_override,
                        leg.paidMinutesOverride
                    ),
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
            initialTime = leg.startTime ?: LocalTime.of(8, 0),
            onTimeSelected = { onStartChange(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showArrivePicker) {
        TimePickerDialog(
            initialTime = leg.arriveTime ?: LocalTime.of(9, 0),
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
