package de.montagezeit.app.ui.screen.edit

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.PrimaryActionButton
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
        androidx.compose.material3.IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.edit_cd_prev_day)
            )
        }
        TextButton(onClick = onPickDate) {
            Text(formatShortDate(date))
        }
        androidx.compose.material3.IconButton(onClick = onNext) {
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
internal fun DateNavigationSwipeZone(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditStickySaveBar(
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

private fun formatShortDate(date: LocalDate): String {
    return date.format(editShortDateFormatter)
}

private val editShortDateFormatter = DateTimeFormatter.ofPattern("E, dd.MM.", Locale.GERMAN)
