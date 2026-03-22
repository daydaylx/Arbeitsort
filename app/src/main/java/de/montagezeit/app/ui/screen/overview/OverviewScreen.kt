package de.montagezeit.app.ui.screen.overview

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.ui.common.DatePickerDialog
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.components.MZCard
import de.montagezeit.app.ui.components.MZErrorState
import de.montagezeit.app.ui.components.MZHeroCard
import de.montagezeit.app.ui.components.MZKeyValueRow
import de.montagezeit.app.ui.components.MZLoadingState
import de.montagezeit.app.ui.components.MZPageBackground
import de.montagezeit.app.ui.components.MZSectionHeader
import de.montagezeit.app.ui.components.MZStatusBadge
import de.montagezeit.app.ui.components.StatusType
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel = hiltViewModel(),
    onOpenToday: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditSheet: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(screenState.errorMessage, screenState.metrics) {
        val error = screenState.errorMessage ?: return@LaunchedEffect
        if (screenState.metrics != null) {
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                title = {
                    Text(
                        text = stringResource(R.string.overview_title),
                        modifier = Modifier.semantics { heading() }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        MZPageBackground(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val errorMessage = screenState.errorMessage
                val metrics = screenState.metrics

                when {
                    screenState.showFullscreenError && errorMessage != null -> {
                        MZErrorState(
                            message = errorMessage.asString(context),
                            onRetry = { viewModel.onResetError() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    screenState.showInitialLoading -> {
                        MZLoadingState(
                            message = stringResource(R.string.loading),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    metrics != null -> {
                        OverviewContent(
                            entry = screenState.currentEntry,
                            selectedDate = screenState.selectedDate,
                            selectedPeriod = screenState.selectedPeriod,
                            metrics = metrics,
                            onPreviousRange = viewModel::goToPreviousRange,
                            onNextRange = viewModel::goToNextRange,
                            onOpenDatePicker = { showDatePicker = true },
                            onSelectPeriod = viewModel::selectPeriod,
                            onOpenToday = onOpenToday,
                            onOpenHistory = onOpenHistory,
                            onOpenSettings = onOpenSettings,
                            onOpenEditSheet = { onOpenEditSheet(screenState.selectedDate) }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = screenState.selectedDate,
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun OverviewContent(
    entry: WorkEntry?,
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod,
    metrics: OverviewMetrics,
    onPreviousRange: () -> Unit,
    onNextRange: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onSelectPeriod: (OverviewPeriod) -> Unit,
    onOpenToday: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OverviewHeroCard(
            entry = entry,
            selectedDate = selectedDate,
            selectedPeriod = selectedPeriod
        )

        OverviewReferenceDateCard(
            selectedDate = selectedDate,
            selectedPeriod = selectedPeriod,
            onPreviousRange = onPreviousRange,
            onNextRange = onNextRange,
            onOpenDatePicker = onOpenDatePicker
        )

        OverviewPeriodWheelCard(
            selectedDate = selectedDate,
            selectedPeriod = selectedPeriod,
            onSelectPeriod = onSelectPeriod
        )

        OverviewOvertimeCard(
            metrics = metrics,
            selectedDate = selectedDate,
            selectedPeriod = selectedPeriod
        )

        OverviewMetricsGrid(metrics = metrics)

        OverviewQuickActionsCard(
            onOpenToday = onOpenToday,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
            onOpenEditSheet = onOpenEditSheet
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun OverviewHeroCard(
    entry: WorkEntry?,
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod
) {
    val statusType = when {
        entry?.confirmedWorkDay == true -> StatusType.SUCCESS
        entry != null -> StatusType.WARNING
        else -> StatusType.NEUTRAL
    }
    val statusText = when {
        entry == null -> stringResource(R.string.overview_status_empty)
        entry.dayType == DayType.WORK && entry.confirmedWorkDay -> stringResource(R.string.overview_status_confirmed)
        entry.dayType == DayType.WORK -> stringResource(R.string.overview_status_unconfirmed)
        entry.dayType == DayType.OFF -> stringResource(R.string.day_type_off)
        else -> stringResource(R.string.day_type_comp_time)
    }

    MZHeroCard(
        title = stringResource(R.string.overview_dashboard_title),
        subtitle = formatPeriodSubtitle(selectedPeriod, selectedDate),
        badge = {
            MZStatusBadge(
                text = statusText,
                type = statusType,
                showIcon = false
            )
        }
    ) {
        MZKeyValueRow(
            label = stringResource(R.string.overview_selected_day_label),
            value = selectedDate.format(overviewDateFormatter),
            emphasize = true
        )
        MZKeyValueRow(
            label = stringResource(R.string.overview_selected_status_label),
            value = entry?.dayLocationLabel?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.today_day_location_unset)
        )
        entry?.let {
            MZKeyValueRow(
                label = stringResource(R.string.overview_selected_total_label),
                value = formatHoursValue(TimeCalculator.calculatePaidTotalHours(it))
            )
        }
    }
}

@Composable
private fun OverviewReferenceDateCard(
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod,
    onPreviousRange: () -> Unit,
    onNextRange: () -> Unit,
    onOpenDatePicker: () -> Unit
) {
    MZCard {
        MZSectionHeader(
            title = stringResource(R.string.overview_reference_title),
            action = {
                MZStatusBadge(
                    text = stringResource(selectedPeriod.labelRes),
                    type = StatusType.INFO,
                    showIcon = false
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onPreviousRange) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.overview_reference_previous)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatPeriodTitle(selectedPeriod, selectedDate),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = selectedDate.format(overviewDateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.overview_reference_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(onClick = onNextRange) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.overview_reference_next)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        SecondaryActionButton(
            onClick = onOpenDatePicker,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.DateRange
        ) {
            Text(stringResource(R.string.overview_reference_pick_date))
        }
    }
}

@Composable
private fun OverviewPeriodWheelCard(
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod,
    onSelectPeriod: (OverviewPeriod) -> Unit
) {
    MZCard {
        MZSectionHeader(title = stringResource(R.string.overview_period_selector_title))
        Spacer(modifier = Modifier.height(12.dp))
        OverviewPeriodWheel(
            selectedDate = selectedDate,
            selectedPeriod = selectedPeriod,
            onSelectPeriod = onSelectPeriod
        )
    }
}

@Composable
private fun OverviewPeriodWheel(
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod,
    onSelectPeriod: (OverviewPeriod) -> Unit
) {
    val periods = OverviewPeriod.values()
    val selectedIndex = periods.indexOf(selectedPeriod)

    val targetRotation = -selectedIndex * 90f
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "wheelRotation"
    )

    var dragAccumDeg by remember { mutableFloatStateOf(0f) }
    var prevAngleRad by remember { mutableFloatStateOf(0f) }

    // Farben außerhalb des Canvas-Lambda (kein @Composable-Kontext dort)
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    val periodLabels = periods.map { stringResource(it.labelRes) }
    val labelTextSizePx = with(LocalDensity.current) { 13.sp.toPx() }
    val selectedLabelTextSizePx = with(LocalDensity.current) { 14.sp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(268.dp)
                .pointerInput(selectedIndex) {
                    val canvasPx = size.width.toFloat()
                    val cx = canvasPx / 2f
                    val cy = canvasPx / 2f

                    coroutineScope {
                    launch {
                        detectTapGestures { offset ->
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = sqrt(dx * dx + dy * dy)
                            val innerRpx = canvasPx * 0.26f
                            val outerRpx = canvasPx * 0.50f
                            if (dist < innerRpx || dist > outerRpx) return@detectTapGestures
                            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val adjusted = ((angleDeg - animatedRotation + 90f) % 360f + 360f) % 360f
                            val tappedIndex = (adjusted / 90f).toInt().coerceIn(0, 3)
                            onSelectPeriod(periods[tappedIndex])
                        }
                    }
                    launch {
                        detectDragGestures(
                            onDragStart = { offset ->
                                prevAngleRad = atan2(offset.y - cy, offset.x - cx)
                                dragAccumDeg = 0f
                            },
                            onDrag = { change, _ ->
                                val cur = atan2(change.position.y - cy, change.position.x - cx)
                                var delta = Math.toDegrees((cur - prevAngleRad).toDouble()).toFloat()
                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f
                                dragAccumDeg += delta
                                prevAngleRad = cur
                                change.consume()
                            },
                            onDragEnd = {
                                val steps = (dragAccumDeg / 90f).roundToInt()
                                val newIndex = ((selectedIndex + steps) % 4 + 4) % 4
                                onSelectPeriod(periods[newIndex])
                                dragAccumDeg = 0f
                            },
                            onDragCancel = {
                                dragAccumDeg = 0f
                            }
                        )
                    }
                    } // end coroutineScope
                }
        ) {
            val liveRotation = animatedRotation + dragAccumDeg
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseOuterR = size.minDimension * 0.46f
            val innerR = size.minDimension * 0.26f
            val gapDeg = 7f
            val sweepDeg = 90f - gapDeg

            val segmentPath = Path()

            periods.forEachIndexed { i, _ ->
                val isSelected = i == selectedIndex
                val outerR = if (isSelected) baseOuterR * 1.06f else baseOuterR
                val startAngle = i * 90f - 90f + liveRotation + gapDeg / 2f

                val outerRect = Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
                val innerRect = Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR)

                segmentPath.reset()
                segmentPath.addArc(outerRect, startAngle, sweepDeg)
                segmentPath.arcTo(innerRect, startAngle + sweepDeg, -sweepDeg, forceMoveTo = false)
                segmentPath.close()

                // Füllung
                drawPath(
                    path = segmentPath,
                    color = if (isSelected) primaryContainerColor else surfaceVariantColor.copy(alpha = 0.85f)
                )
                // Umrandung
                drawPath(
                    path = segmentPath,
                    color = outlineVariantColor.copy(alpha = 0.5f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Label-Text mittig im Segment
                val midAngleDeg = startAngle + sweepDeg / 2f
                val midAngleRad = (midAngleDeg * PI / 180.0).toFloat()
                val labelR = (outerR + innerR) / 2f
                val labelX = cx + labelR * cos(midAngleRad)
                val labelY = cy + labelR * sin(midAngleRad)

                val textSize = if (isSelected) selectedLabelTextSizePx else labelTextSizePx
                val textColor = if (isSelected) onPrimaryContainerColor else onSurfaceVariantColor
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    this.textSize = textSize
                    textAlign = android.graphics.Paint.Align.CENTER
                    color = textColor.toArgb()
                    isFakeBoldText = isSelected
                }
                drawContext.canvas.nativeCanvas.drawText(  // nativeCanvas: android.graphics.Canvas
                    periodLabels[i],
                    labelX,
                    labelY + textSize / 3f,
                    textPaint
                )
            }
        }

        // Center Hub
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(selectedPeriod.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = formatWheelCenterValue(selectedPeriod, selectedDate),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OverviewOvertimeCard(
    metrics: OverviewMetrics,
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod
) {
    val status = when {
        metrics.overtimeHours > 0.05 -> StatusType.SUCCESS
        metrics.overtimeHours < -0.05 -> StatusType.WARNING
        else -> StatusType.INFO
    }

    val progressFraction = if (metrics.targetHours > 0.0) {
        (metrics.actualHours / metrics.targetHours).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    MZHeroCard(
        title = stringResource(R.string.overtime_title),
        subtitle = formatPeriodTitle(selectedPeriod, selectedDate),
        badge = {
            MZStatusBadge(
                text = stringResource(selectedPeriod.labelRes),
                type = status,
                showIcon = false
            )
        }
    ) {
        Text(
            text = formatSignedHoursValue(metrics.overtimeHours),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progressFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatHoursValue(metrics.actualHours)} / ${formatHoursValue(metrics.targetHours)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        MZKeyValueRow(
            label = stringResource(R.string.overview_counted_days_label),
            value = metrics.countedDays.toString()
        )
    }
}

@Composable
private fun OverviewMetricsGrid(metrics: OverviewMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewMetricCard(
                title = stringResource(R.string.overview_kpi_target),
                value = formatHoursValue(metrics.targetHours),
                caption = stringResource(R.string.overview_kpi_target_caption),
                icon = Icons.Default.Schedule,
                modifier = Modifier.weight(1f)
            )
            OverviewMetricCard(
                title = stringResource(R.string.overview_kpi_actual),
                value = formatHoursValue(metrics.actualHours),
                caption = stringResource(R.string.overview_kpi_actual_caption),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewMetricCard(
                title = stringResource(R.string.overview_kpi_travel),
                value = formatHoursValue(metrics.travelHours),
                caption = stringResource(R.string.overview_kpi_travel_caption),
                icon = Icons.Default.DirectionsCar,
                modifier = Modifier.weight(1f)
            )
            OverviewMetricCard(
                title = stringResource(R.string.overview_kpi_meal),
                value = MealAllowanceCalculator.formatEuro(metrics.mealAllowanceCents),
                caption = stringResource(R.string.overview_kpi_meal_caption),
                icon = Icons.Default.Restaurant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewMetricCard(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    MZCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OverviewQuickActionsCard(
    onOpenToday: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditSheet: () -> Unit
) {
    MZCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MZSectionHeader(title = stringResource(R.string.overview_quick_actions_title))

            PrimaryActionButton(
                onClick = onOpenEditSheet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.overview_action_edit_selected))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryActionButton(
                    onClick = onOpenToday,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.today_title))
                }
                SecondaryActionButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.history_title))
                }
            }

            SecondaryActionButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_title))
            }
        }
    }
}

@Composable
private fun formatPeriodTitle(period: OverviewPeriod, selectedDate: LocalDate): String =
    when (period) {
        OverviewPeriod.DAY -> selectedDate.format(shortDateFormatter)
        OverviewPeriod.WEEK -> {
            val weekNumber = selectedDate.get(WeekFields.of(Locale.GERMAN).weekOfWeekBasedYear())
            stringResource(R.string.overview_week_title, weekNumber)
        }
        OverviewPeriod.MONTH -> selectedDate.format(monthTitleFormatter)
        OverviewPeriod.YEAR -> stringResource(R.string.overview_year_title, selectedDate.year)
    }

@Composable
private fun formatPeriodSubtitle(period: OverviewPeriod, selectedDate: LocalDate): String {
    val range = period.rangeFor(selectedDate)
    return when (period) {
        OverviewPeriod.DAY -> selectedDate.format(overviewDateFormatter)
        OverviewPeriod.WEEK -> stringResource(
            R.string.overview_date_range,
            range.startDate.format(shortDateFormatter),
            range.endDate.format(shortDateFormatter)
        )
        OverviewPeriod.MONTH -> selectedDate.format(monthTitleFormatter)
        OverviewPeriod.YEAR -> stringResource(
            R.string.overview_date_range,
            range.startDate.format(shortDateFormatter),
            range.endDate.format(shortDateFormatter)
        )
    }
}

private fun formatWheelCenterValue(period: OverviewPeriod, selectedDate: LocalDate): String =
    when (period) {
        OverviewPeriod.DAY -> selectedDate.format(DateTimeFormatter.ofPattern("dd.MM", Locale.GERMAN))
        OverviewPeriod.WEEK -> "KW ${selectedDate.get(WeekFields.of(Locale.GERMAN).weekOfWeekBasedYear())}"
        OverviewPeriod.MONTH -> selectedDate.format(DateTimeFormatter.ofPattern("MMM", Locale.GERMAN))
        OverviewPeriod.YEAR -> selectedDate.year.toString()
    }

private fun formatHoursValue(hours: Double): String =
    String.format(Locale.GERMAN, "%.1f Std.", hours)

private fun formatSignedHoursValue(hours: Double): String =
    String.format(Locale.GERMAN, "%+.1f Std.", hours)

private val overviewDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)
private val shortDateFormatter = DateTimeFormatter.ofPattern("dd. MMM yyyy", Locale.GERMAN)
private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
