package de.montagezeit.app.ui.screen.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import de.montagezeit.app.ui.components.MZAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.DestructiveActionButton
import de.montagezeit.app.ui.components.MZInlineNotice
import de.montagezeit.app.ui.components.PrimaryActionButton
import de.montagezeit.app.ui.components.StatusType
import de.montagezeit.app.ui.components.TertiaryActionButton

@Composable
internal fun EditValidationCard(
    validationErrors: List<ValidationError>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (validationErrors.isEmpty()) return
    val errorMessages = validationErrors.map { stringResource(it.messageRes) }

    MZInlineNotice(
        title = stringResource(R.string.edit_validation_title),
        message = errorMessages.joinToString("\n"),
        type = StatusType.ERROR,
        modifier = modifier.fillMaxWidth(),
        action = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                errorMessages.forEach { errorMessage ->
                    Text(
                        text = "• $errorMessage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            TertiaryActionButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.edit_action_ok)
                )
            }
        }
    )
}

@Composable
internal fun DeleteDayConfirmDialog(
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MZAlertDialog(
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
internal fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    MZAlertDialog(
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

internal sealed interface EditSheetDialog {
    data object None : EditSheetDialog
    data object CopyDatePicker : EditSheetDialog
    data object NavigateDatePicker : EditSheetDialog
    data object DeleteDayConfirm : EditSheetDialog
    data object DiscardChanges : EditSheetDialog
    data class DiscardChangesForNavigation(val newDate: java.time.LocalDate) : EditSheetDialog
}
