package de.montagezeit.app.ui.screen.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.montagezeit.app.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekOverviewRow(
    weekDays: List<WeekDayUi>,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(weekDays, key = { it.date.toString() }) { day ->
            WeekDayChip(
                day = day,
                onClick = { onSelectDay(day.date) }
            )
        }
    }
}

@Composable
private fun WeekDayChip(
    day: WeekDayUi,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(12.dp)

    val backgroundColor: Color
    val contentColor: Color
    val borderColor: Color

    when {
        day.isSelected -> {
            backgroundColor = MaterialTheme.colorScheme.primary
            contentColor = MaterialTheme.colorScheme.onPrimary
            borderColor = Color.Transparent
        }
        day.isToday -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            borderColor = MaterialTheme.colorScheme.primary
        }
        else -> {
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            borderColor = Color.Transparent
        }
    }

    val statusIndicatorColor: Color? = when (day.status) {
        WeekDayStatus.CONFIRMED_WORK -> MaterialTheme.colorScheme.primary
        WeekDayStatus.CONFIRMED_OFF -> MaterialTheme.colorScheme.secondary
        WeekDayStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        WeekDayStatus.EMPTY -> null
    }

    val cdText = if (day.workHours != null) {
        stringResource(R.string.week_chip_cd_with_hours, day.dayLabel, day.dayNumber, day.workHours)
    } else {
        stringResource(R.string.week_chip_cd, day.dayLabel, day.dayNumber)
    }

    Column(
        modifier = Modifier
            .width(44.dp)
            .clip(chipShape)
            .background(backgroundColor)
            .then(
                if (borderColor != Color.Transparent)
                    Modifier.border(1.5.dp, borderColor, chipShape)
                else
                    Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics {
                contentDescription = cdText
                selected = day.isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (day.isToday || day.isSelected) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            text = day.dayNumber,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )

        if (day.workHours != null) {
            val h = day.workHours.toInt()
            val m = ((day.workHours - h) * 60).toInt()
            val hoursText = if (m == 0) "${h}h" else "${h}h${m}"
            Text(
                text = hoursText,
                fontSize = 9.sp,
                color = contentColor.copy(alpha = 0.85f)
            )
        } else {
            Spacer(modifier = Modifier.height(13.dp))
        }

        // Status dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    statusIndicatorColor
                        ?: contentColor.copy(alpha = 0.2f)
                )
        )
    }
}

/**
 * Builds a localized short weekday label (e.g. "Mo", "Di") from a [LocalDate].
 */
fun LocalDate.shortWeekDayLabel(): String =
    dayOfWeek
        .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
        .removeSuffix(".")
        .replaceFirstChar { it.uppercase() }
