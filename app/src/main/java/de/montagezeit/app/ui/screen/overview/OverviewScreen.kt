package de.montagezeit.app.ui.screen.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.theme.NumberStyles
import de.montagezeit.app.ui.components.DatePickerDialog
import de.montagezeit.app.ui.components.MZCard
import de.montagezeit.app.ui.components.MZErrorState
import de.montagezeit.app.ui.components.MZSegmentedControl
import de.montagezeit.app.ui.components.MZSegmentedOption
import de.montagezeit.app.ui.components.MZLoadingState
import de.montagezeit.app.ui.components.MZPageBackground
import de.montagezeit.app.ui.components.MZSnackbarHost
import de.montagezeit.app.ui.components.AnimatedCounter
import de.montagezeit.app.ui.components.MZStatusBadge
import de.montagezeit.app.ui.components.StatusType
import de.montagezeit.app.ui.components.staggeredAppear
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch

private val overviewWeekFields = WeekFields.ISO

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
    var showPeriodPicker by remember { mutableStateOf(false) }

    LaunchedEffect(screenState.errorMessage, screenState.metrics) {
        val error = screenState.errorMessage ?: return@LaunchedEffect
        if (screenState.metrics != null) {
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.onErrorShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MZPageBackground(modifier = Modifier.fillMaxSize()) {
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
                            selectedDate = screenState.selectedDate,
                            selectedPeriod = screenState.selectedPeriod,
                            metrics = metrics,
                            onPreviousRange = viewModel::goToPreviousRange,
                            onNextRange = viewModel::goToNextRange,
                            onSelectPeriod = viewModel::selectPeriod,
                            onOpenPeriodPicker = { showPeriodPicker = true },
                            onActionNeededClick = onOpenHistory
                        )
                    }
                }
            }
        }

        MZSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showPeriodPicker) {
        OverviewPeriodPickerSheet(
            selectedPeriod = screenState.selectedPeriod,
            selectedDate = screenState.selectedDate,
            onPeriodSelected = { viewModel.selectPeriod(it) },
            onDateSelected = { viewModel.selectDate(it) },
            onDismiss = { showPeriodPicker = false }
        )
    }
}

@Composable
private fun OverviewContent(
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod,
    metrics: OverviewMetrics,
    onPreviousRange: () -> Unit,
    onNextRange: () -> Unit,
    onSelectPeriod: (OverviewPeriod) -> Unit,
    onOpenPeriodPicker: () -> Unit,
    onActionNeededClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MZTokens.ScreenPadding, vertical = MZTokens.CardSpacing),
        verticalArrangement = Arrangement.spacedBy(MZTokens.CardSpacing)
    ) {
        OverviewTopRangeBar(
            selectedDate = selectedDate,
            selectedPeriod = selectedPeriod,
            onPreviousRange = onPreviousRange,
            onNextRange = onNextRange,
            onOpenPicker = onOpenPeriodPicker
        )

        OverviewPeriodQuickSelector(
            selectedPeriod = selectedPeriod,
            onSelectPeriod = onSelectPeriod
        )

        OverviewHeroSection(
            selectedDate = selectedDate,
            selectedPeriod = selectedPeriod,
            metrics = metrics
        )

        OverviewKpiGrid(
            metrics = metrics,
            onActionNeededClick = onActionNeededClick
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OverviewPeriodQuickSelector(
    selectedPeriod: OverviewPeriod,
    onSelectPeriod: (OverviewPeriod) -> Unit
) {
    MZSegmentedControl(
        options = OverviewPeriod.values().map { period ->
            MZSegmentedOption(period, stringResource(period.labelRes))
        },
        selectedValue = selectedPeriod,
        onValueSelected = onSelectPeriod
    )
}

@Composable
private fun OverviewTopRangeBar(
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod,
    onPreviousRange: () -> Unit,
    onNextRange: () -> Unit,
    onOpenPicker: () -> Unit
) {
    var dragAccum = 0f
    MZCard(
        modifier = Modifier.pointerInput(selectedDate, selectedPeriod) {
            val swipeThresholdPx = 56.dp.toPx()
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                onDragEnd = {
                    when {
                        dragAccum > swipeThresholdPx -> onPreviousRange()
                        dragAccum < -swipeThresholdPx -> onNextRange()
                    }
                    dragAccum = 0f
                },
                onDragCancel = { dragAccum = 0f }
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousRange) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.overview_reference_previous)
                )
            }

            Row(
                modifier = Modifier
                    .clickable(
                        onClick = onOpenPicker,
                        onClickLabel = stringResource(R.string.overview_range_selector_hint)
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatPeriodTitle(selectedPeriod, selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(selectedPeriod.labelRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onNextRange) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.overview_reference_next)
                )
            }
        }
    }
}

@Composable
private fun OverviewHeroSection(
    selectedDate: LocalDate,
    selectedPeriod: OverviewPeriod,
    metrics: OverviewMetrics
) {
    val balanceBadge = overviewBalanceBadge(metrics.overtimeHours)
    val progressFraction = if (metrics.targetHours > 0.0) {
        (metrics.actualHours / metrics.targetHours).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    MZCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.overview_balance_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatPeriodSubtitle(selectedPeriod, selectedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MZStatusBadge(
                text = balanceBadge.text,
                type = balanceBadge.type,
                showIcon = false
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedCounter(
            targetValue = metrics.overtimeHours,
            formatter = { formatSignedHoursValue(it) },
            style = NumberStyles.displayLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewMetricColumn(
                label = stringResource(R.string.overview_actual_label),
                value = Formatters.formatHours(metrics.actualHours),
                modifier = Modifier.weight(1f)
            )
            OverviewMetricColumn(
                label = stringResource(R.string.overview_target_label),
                value = Formatters.formatHours(metrics.targetHours),
                modifier = Modifier.weight(1f)
            )
            OverviewMetricColumn(
                label = stringResource(R.string.overview_difference_label),
                value = formatSignedHoursValue(metrics.overtimeHours),
                modifier = Modifier.weight(1f)
            )
        }

        if (metrics.targetHours > 0.0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.overview_progress_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OverviewKpiGrid(
    metrics: OverviewMetrics,
    onActionNeededClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiGridItem(
                modifier = Modifier.weight(1f).staggeredAppear(index = 0),
                icon = Icons.Default.DirectionsCar,
                label = stringResource(R.string.overview_kpi_travel),
                value = Formatters.formatHours(metrics.travelHours),
                tint = MaterialTheme.colorScheme.primary
            )
            KpiGridItem(
                modifier = Modifier.weight(1f).staggeredAppear(index = 1),
                icon = Icons.Default.Restaurant,
                label = stringResource(R.string.overview_kpi_meal),
                value = MealAllowanceCalculator.formatEuro(metrics.mealAllowanceCents),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiGridItem(
                modifier = Modifier.weight(1f).staggeredAppear(index = 2),
                icon = Icons.Default.Today,
                label = stringResource(R.string.overview_counted_days_label),
                value = stringResource(R.string.overtime_counted_days, metrics.countedDays),
                tint = MaterialTheme.colorScheme.tertiary
            )
            val actionNeeded = metrics.unconfirmedDaysCount > 0
            KpiGridItem(
                modifier = Modifier.weight(1f).staggeredAppear(index = 3),
                icon = if (actionNeeded) Icons.Default.ErrorOutline else Icons.Default.CalendarMonth,
                label = stringResource(R.string.overview_kpi_action_needed),
                value = if (actionNeeded) {
                    pluralStringResource(
                        R.plurals.overview_kpi_unconfirmed_count,
                        metrics.unconfirmedDaysCount,
                        metrics.unconfirmedDaysCount
                    )
                } else {
                    stringResource(R.string.overview_kpi_none_unconfirmed)
                },
                tint = if (actionNeeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                onClick = onActionNeededClick
            )
        }
    }
}

@Composable
private fun KpiGridItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    MZCard(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 126.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(MZTokens.RadiusChip),
                color = tint.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverviewMetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewPeriodPickerSheet(
    selectedPeriod: OverviewPeriod,
    selectedDate: LocalDate,
    onPeriodSelected: (OverviewPeriod) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.overview_range_selector_hint),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            OverviewPeriod.values().forEach { period ->
                ListItem(
                    modifier = Modifier.clickable {
                        onPeriodSelected(period)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    headlineContent = { Text(stringResource(period.labelRes)) },
                    trailingContent = {
                        RadioButton(
                            selected = selectedPeriod == period,
                            onClick = null
                        )
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                modifier = Modifier.clickable {
                    showDatePicker = true
                },
                headlineContent = { Text(stringResource(R.string.overview_reference_pick_date)) },
                supportingContent = { Text(Formatters.formatDateLong(selectedDate)) },
                leadingContent = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                }
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun overviewBalanceBadge(overtimeHours: Double): BadgeSpec =
    when {
        overtimeHours > 0.05 -> BadgeSpec(
            text = stringResource(R.string.overview_balance_positive),
            type = StatusType.SUCCESS
        )
        overtimeHours < -0.05 -> BadgeSpec(
            text = stringResource(R.string.overview_balance_negative),
            type = StatusType.WARNING
        )
        else -> BadgeSpec(
            text = stringResource(R.string.overview_balance_even),
            type = StatusType.INFO
        )
    }

private data class BadgeSpec(
    val text: String,
    val type: StatusType
)

@Composable
private fun formatPeriodTitle(period: OverviewPeriod, selectedDate: LocalDate): String =
    when (period) {
        OverviewPeriod.DAY -> selectedDate.format(shortDateFormatter)
        OverviewPeriod.WEEK -> {
            val weekNumber = selectedDate.get(overviewWeekFields.weekOfWeekBasedYear())
            stringResource(R.string.overview_week_title, weekNumber)
        }
        OverviewPeriod.MONTH -> selectedDate.format(monthTitleFormatter)
        OverviewPeriod.YEAR -> stringResource(R.string.overview_year_title, selectedDate.year)
    }

@Composable
private fun formatPeriodSubtitle(period: OverviewPeriod, selectedDate: LocalDate): String {
    val range = period.rangeFor(selectedDate)
    return when (period) {
        OverviewPeriod.DAY -> Formatters.formatDateLong(selectedDate)
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

private fun formatSignedHoursValue(hours: Double): String =
    String.format(Locale.GERMAN, "%+.1f Std.", hours)

private val shortDateFormatter = DateTimeFormatter.ofPattern("dd. MMM yyyy", Locale.GERMAN)
private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
