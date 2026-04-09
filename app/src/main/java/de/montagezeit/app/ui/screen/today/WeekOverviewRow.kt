package de.montagezeit.app.ui.screen.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.theme.NumberStyles
import de.montagezeit.app.ui.theme.glassSelectionBorderBrush
import de.montagezeit.app.ui.theme.glassSelectionBrush
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val SELECTED_CHIP_SCALE = 1.01f

@Composable
fun WeekOverviewRow(
    weekDays: List<WeekDayUi>,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = MZTokens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
    val chipShape = RoundedCornerShape(MZTokens.RadiusChip)

    val targetBg: Color
    val targetContent: Color
    val targetBorder: Color

    when {
        day.isSelected -> {
            targetBg = Color.Transparent
            targetContent = MaterialTheme.colorScheme.onPrimaryContainer
            targetBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        }
        day.isToday -> {
            targetBg = MaterialTheme.colorScheme.surfaceVariant
            targetContent = MaterialTheme.colorScheme.onSurface
            targetBorder = MaterialTheme.colorScheme.outline.copy(alpha = MZTokens.BorderAlphaNormal)
        }
        else -> {
            targetBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            targetContent = MaterialTheme.colorScheme.onSurfaceVariant
            targetBorder = MaterialTheme.colorScheme.outline.copy(alpha = MZTokens.BorderAlphaSubtle)
        }
    }

    val backgroundColor by animateColorAsState(targetBg, label = "chipBg")
    val contentColor by animateColorAsState(targetContent, label = "chipContent")
    val borderColor by animateColorAsState(
        targetBorder, label = "chipBorder"
    )
    val elevation by animateDpAsState(
        targetValue = if (day.isSelected) 1.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipElev"
    )
    val scale by animateFloatAsState(
        targetValue = if (day.isSelected) SELECTED_CHIP_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale"
    )

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

    Surface(
        modifier = Modifier
            .width(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = cdText
                selected = day.isSelected
            }
            .clip(chipShape)
            .then(
                if (day.isSelected) {
                    Modifier.background(
                        brush = MaterialTheme.colorScheme.glassSelectionBrush,
                        shape = chipShape
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (day.isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        brush = MaterialTheme.colorScheme.glassSelectionBorderBrush,
                        shape = chipShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        shape = chipShape,
        color = backgroundColor,
        contentColor = contentColor,
        border = if (!day.isSelected && targetBorder != Color.Transparent) {
            BorderStroke(1.dp, borderColor)
        } else {
            null
        },
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = day.dayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                fontWeight = if (day.isToday || day.isSelected) FontWeight.Bold else FontWeight.Normal
            )

            Text(
                text = day.dayNumber,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )

            if (day.workHours != null) {
                val h = day.workHours.toInt()
                val m = ((day.workHours - h) * 60).toInt()
                val hoursText = if (m == 0) "${h}h" else "${h}h${m}m"
                Text(
                    text = hoursText,
                    style = NumberStyles.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            val glowColor = statusIndicatorColor
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .then(
                        if (glowColor != null && day.status == WeekDayStatus.CONFIRMED_WORK) {
                            Modifier.drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(glowColor.copy(alpha = 0.35f), Color.Transparent),
                                        radius = 12.dp.toPx()
                                    ),
                                    radius = 12.dp.toPx()
                                )
                            }
                        } else Modifier
                    )
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(glowColor ?: contentColor.copy(alpha = 0.15f))
            )
        }
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
