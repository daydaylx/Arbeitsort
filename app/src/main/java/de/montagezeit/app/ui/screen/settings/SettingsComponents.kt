@file:Suppress("LongParameterList", "MaxLineLength")

package de.montagezeit.app.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.*
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.UiText
import de.montagezeit.app.ui.util.asString
import java.time.LocalTime

@Composable
internal fun CollapsibleSettingsCard(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val expandLabel = stringResource(R.string.cd_expand_section, title)
    val collapseLabel = stringResource(R.string.cd_collapse_section, title)

    MZAppPanel(
        modifier = modifier.animateContentSize()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (expanded) collapseLabel else expandLabel
                    ) { onExpandedChange(!expanded) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = context.getString(
                            R.string.settings_section_content_desc,
                            title,
                            if (expanded) {
                                context.getString(R.string.settings_state_expanded)
                            } else {
                                context.getString(R.string.settings_state_collapsed)
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                action?.invoke()

                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) {
                        stringResource(R.string.cd_collapse_section, title)
                    } else {
                        stringResource(R.string.cd_expand_section, title)
                    }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    content()
                }
            }
        }
    }
}

@Composable
internal fun SetupSection(
    hasNotificationPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    enabledReminderCount: Int,
    workStartLabel: String,
    workEndLabel: String,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onSendTestReminder: () -> Unit
) {
    val allHealthy = hasNotificationPermission && isIgnoringBatteryOptimizations

    MZAppPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_setup_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            MZStatusChip(
                text = if (allHealthy) stringResource(R.string.status_active) else stringResource(R.string.today_action_required),
                color = if (allHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(
                    R.string.settings_setup_summary,
                    enabledReminderCount,
                    workStartLabel,
                    workEndLabel
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SetupRow(
                title = stringResource(R.string.settings_notifications),
                status = if (hasNotificationPermission) {
                    stringResource(R.string.settings_notifications_ready)
                } else {
                    stringResource(R.string.settings_notifications_missing)
                },
                supportingText = if (hasNotificationPermission) {
                    stringResource(R.string.settings_notifications_ready_support)
                } else {
                    stringResource(R.string.settings_notifications_missing_support)
                },
                actionLabel = if (hasNotificationPermission)
                    stringResource(R.string.action_settings)
                else
                    stringResource(R.string.action_allow),
                onAction = if (hasNotificationPermission) onOpenNotificationSettings else onRequestNotificationPermission,
                isOk = hasNotificationPermission
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SetupRow(
                title = stringResource(R.string.settings_battery_optimization),
                status = if (isIgnoringBatteryOptimizations) {
                    stringResource(R.string.settings_battery_optimization_disabled)
                } else {
                    stringResource(R.string.settings_battery_optimization_active)
                },
                supportingText = if (isIgnoringBatteryOptimizations) {
                    stringResource(R.string.settings_battery_optimization_disabled_support)
                } else {
                    stringResource(R.string.settings_battery_optimization_active_support)
                },
                actionLabel = stringResource(R.string.action_disable_optimization),
                onAction = onOpenBatterySettings,
                isOk = isIgnoringBatteryOptimizations
            )

            if (hasNotificationPermission) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SecondaryActionButton(
                    onClick = onSendTestReminder,
                    icon = Icons.Default.Notifications,
                    content = { Text(stringResource(R.string.action_test_notification)) }
                )
            }
        }
    }
}

@Composable
private fun SetupRow(
    title: String,
    status: String,
    supportingText: String,
    actionLabel: String,
    onAction: () -> Unit,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TertiaryActionButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
internal fun SettingsToggleRow(
    title: String,
    supportingText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.semantics {
                contentDescription = context.getString(
                    R.string.settings_toggle_content_desc,
                    title,
                    if (checked) {
                        context.getString(R.string.settings_toggle_active)
                    } else {
                        context.getString(R.string.settings_toggle_inactive)
                    }
                )
            }
        )
    }
}

@Composable
internal fun SettingsTimeButtonRow(
    label: String,
    time: LocalTime,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabledReason: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!enabled && disabledReason != null) {
                Text(
                    text = disabledReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        SecondaryActionButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Text(Formatters.formatTime(time))
        }
    }
}

@Composable
internal fun TimeRangePicker(
    label: String,
    startTime: LocalTime,
    endTime: LocalTime,
    onTimeRangeChanged: (LocalTime, LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val isInvalid = !startTime.isBefore(endTime)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecondaryActionButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(Formatters.formatTime(startTime))
            }

            Text(
                text = "–",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            SecondaryActionButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(Formatters.formatTime(endTime))
            }
        }

        if (isInvalid) {
            Text(
                text = stringResource(R.string.error_time_range_invalid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showStartPicker) {
        de.montagezeit.app.ui.components.TimePickerDialog(
            initialTime = startTime,
            onTimeSelected = {
                onTimeRangeChanged(it, endTime)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        de.montagezeit.app.ui.components.TimePickerDialog(
            initialTime = endTime,
            onTimeSelected = {
                onTimeRangeChanged(startTime, it)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
internal fun NumericInputRow(
    label: String,
    value: Double,
    unit: String,
    onValueChange: (Double) -> Unit,
    minValue: Double,
    maxValue: Double
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    var isError by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun parseDraftValue(): Double? {
        return textValue
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it in minValue..maxValue }
    }

    fun commitDraft() {
        val parsedValue = parseDraftValue()
        if (parsedValue != null) {
            isError = false
            if (parsedValue != value) {
                onValueChange(parsedValue)
            }
        } else {
            isError = textValue.isNotBlank()
        }
    }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    isError = newValue.isNotBlank() && parseDraftValue() == null
                },
                isError = isError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        commitDraft()
                        keyboardController?.hide()
                    }
                ),
                singleLine = true,
                colors = mzOutlinedTextFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        val lostFocus = hadFocus && !focusState.isFocused
                        hadFocus = focusState.isFocused
                        if (lostFocus) {
                            commitDraft()
                        }
                    }
            )

            Text(
                text = unit,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (isError) {
            Text(
                text = stringResource(R.string.error_value_range, minValue, maxValue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
