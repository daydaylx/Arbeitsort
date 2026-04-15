package de.montagezeit.app.ui.screen.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.MZAppPanel
import de.montagezeit.app.ui.components.MZStatusBadge
import de.montagezeit.app.ui.components.PrimaryActionButton
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.TertiaryActionButton
import de.montagezeit.app.ui.components.StatusType
import de.montagezeit.app.ui.theme.MZTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun DateNavigationRow(
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
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            SecondaryActionButton(onClick = onPickDate) {
                Text(
                    text = formatShortDate(date),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        TertiaryActionButton(onClick = onToday) {
            Text(stringResource(R.string.edit_action_today))
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.edit_cd_next_day)
            )
        }
    }
}

@Composable
internal fun EditStickySaveBar(
    isSaving: Boolean,
    enabled: Boolean,
    blockingMessage: String?,
    onSave: () -> Unit
) {
    MZAppPanel(
        emphasized = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blockingMessage?.let {
                MZStatusBadge(
                    text = it,
                    type = StatusType.ERROR,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                PrimaryActionButton(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled && !isSaving
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
                        stringResource(R.string.action_save_entry)
                    )
                }
            }
        }
    }
}

private fun formatShortDate(date: LocalDate): String {
    return date.format(editShortDateFormatter)
}

private val editShortDateFormatter = DateTimeFormatter.ofPattern("E, dd.MM.", Locale.GERMAN)
