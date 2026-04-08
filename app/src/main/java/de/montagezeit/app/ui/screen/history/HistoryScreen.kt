@file:Suppress("LongMethod", "CyclomaticComplexMethod", "MaxLineLength")

package de.montagezeit.app.ui.screen.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import de.montagezeit.app.R
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.theme.NumberStyles
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.ui.components.DatePickerDialog
import de.montagezeit.app.ui.util.DateTimeUtils
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.asString
import de.montagezeit.app.ui.components.PrimaryActionButton
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.TertiaryActionButton
import de.montagezeit.app.ui.components.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private const val MONTH_PREVIEW_COUNT = 5
private val historyWeekFields = WeekFields.ISO


private val localDateSaver = Saver<LocalDate, String>(
    save = { it.toString() },
    restore = { LocalDate.parse(it) }
)

private val yearMonthSaver = Saver<YearMonth, String>(
    save = { it.toString() },
    restore = { YearMonth.parse(it) }
)

private val calendarModeSaver = Saver<CalendarMode, String>(
    save = { it.name },
    restore = { CalendarMode.valueOf(it) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenEditSheet: (java.time.LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val batchEditState by viewModel.batchEditState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBatchEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val msgUpdated = stringResource(R.string.history_toast_batch_updated)

    LaunchedEffect(batchEditState) {
        when (batchEditState) {
            is BatchEditState.Success -> {
                showBatchEditDialog = false
                snackbarHostState.showSnackbar(msgUpdated)
                viewModel.onBatchEditResultConsumed()
            }
            is BatchEditState.Failure -> {
                val failure = batchEditState as BatchEditState.Failure
                snackbarHostState.showSnackbar(failure.message.asString(context))
                viewModel.onBatchEditResultConsumed()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is HistoryUiState.Loading -> {
                MZLoadingState(
                    message = stringResource(R.string.loading),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is HistoryUiState.Success -> {
                val successState = uiState as HistoryUiState.Success
                HistoryContent(
                    weeks = successState.weeks,
                    months = successState.months,
                    entriesByDate = successState.entriesByDate,
                    travelLegsByDate = successState.travelLegsByDate,
                    onEntryClick = onOpenEditSheet,
                    onOpenBatchEdit = { showBatchEditDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is HistoryUiState.Error -> {
                MZErrorState(
                    message = (uiState as HistoryUiState.Error).message.asString(context),
                    onRetry = { viewModel.loadHistory() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        MZSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = MZTokens.ScreenPadding, vertical = 12.dp)
        )
    }

    if (showBatchEditDialog) {
        val isBatchEditing = batchEditState is BatchEditState.InProgress
        BatchEditDialog(
            isSubmitting = isBatchEditing,
            onDismiss = { if (!isBatchEditing) showBatchEditDialog = false },
            onApply = { request -> viewModel.applyBatchEdit(request) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    weeks: List<WeekGroup>,
    months: List<MonthGroup>,
    entriesByDate: Map<LocalDate, WorkEntry>,
    travelLegsByDate: Map<LocalDate, List<TravelLeg>>,
    onEntryClick: (java.time.LocalDate) -> Unit,
    onOpenBatchEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showMonths by rememberSaveable { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var calendarMode by rememberSaveable(stateSaver = calendarModeSaver) { mutableStateOf(CalendarMode.MONTH) }
    var selectedMonth by rememberSaveable(stateSaver = yearMonthSaver) { mutableStateOf(YearMonth.now()) }
    var selectedDate by rememberSaveable(stateSaver = localDateSaver) { mutableStateOf(LocalDate.now()) }
    val entries = remember(entriesByDate) { entriesByDate.values.sortedByDescending(WorkEntry::date) }
    val workEntryCount = remember(entries) { entries.count { it.dayType == DayType.WORK } }
    val unconfirmedCount = remember(entries) {
        entries.count { it.dayType == DayType.WORK && !it.confirmedWorkDay }
    }
    val hasGroupedContent = weeks.isNotEmpty() || months.isNotEmpty()
    val openEntryForDate: (LocalDate) -> Unit = { date ->
        selectedDate = date
        selectedMonth = YearMonth.from(date)
        onEntryClick(date)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MZTokens.ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "history-hero") {
            MZHeroPanel {
                MZSectionIntro(
                    eyebrow = stringResource(R.string.history_title),
                    title = stringResource(R.string.history_hero_title),
                    supportingText = stringResource(R.string.history_hero_support)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MZMetricChip(
                        label = stringResource(R.string.history_metric_entries),
                        value = entries.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MZMetricChip(
                        label = stringResource(R.string.history_metric_workdays),
                        value = workEntryCount.toString(),
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                    MZMetricChip(
                        label = stringResource(R.string.history_metric_open),
                        value = unconfirmedCount.toString(),
                        modifier = Modifier.weight(1f),
                        accentColor = if (unconfirmedCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
            }
        }

        stickyHeader(key = "history-controls") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                    .padding(bottom = 8.dp)
            ) {
                HistoryMiniFilterBar(
                    showCalendar = showCalendar,
                    calendarMode = calendarMode,
                    showMonths = showMonths,
                    canToggleMonths = months.isNotEmpty(),
                    onShowCalendarChange = { showCalendar = it },
                    onCalendarModeChange = { calendarMode = it },
                    onShowMonthsChange = { showMonths = it },
                    onPickDate = { showDatePicker = true },
                    onOpenBatchEdit = onOpenBatchEdit
                )
            }
        }

        // selected-day was removed to save space

        when {
            showCalendar -> {
                item(key = "calendar") {
                    MZAppPanel {
                        if (calendarMode == CalendarMode.MONTH) {
                            CalendarView(
                                month = selectedMonth,
                                entriesByDate = entriesByDate,
                                onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                                onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
                                onDayClick = openEntryForDate
                            )
                        } else {
                            WeekCalendarView(
                                selectedDate = selectedDate,
                                entriesByDate = entriesByDate,
                                onPreviousWeek = { selectedDate = selectedDate.minusDays(7) },
                                onNextWeek = { selectedDate = selectedDate.plusDays(7) },
                                onDayClick = openEntryForDate
                            )
                        }
                    }
                }
            }

            !hasGroupedContent -> {
                item(key = "empty-state") {
                    MZEmptyState(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.history_empty_title),
                        subtitle = stringResource(R.string.history_empty_subtitle),
                        icon = Icons.Default.History
                    )
                }
            }

            else -> {
                historyGroupedEntries(
                    showMonths = showMonths,
                    weeks = weeks,
                    months = months,
                    travelLegsByDate = travelLegsByDate,
                    onEntryClick = openEntryForDate
                )
            }
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { date ->
                openEntryForDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.historyGroupedEntries(
    showMonths: Boolean,
    weeks: List<WeekGroup>,
    months: List<MonthGroup>,
    travelLegsByDate: Map<LocalDate, List<TravelLeg>>,
    onEntryClick: (LocalDate) -> Unit
) {
    if (showMonths) {
        months.forEach { month ->
            stickyHeader(key = "header-${month.year}-${month.month}") {
                MonthGroupHeader(month = month)
            }
            items(
                items = month.entries,
                key = { entry -> entry.date }
            ) { entry ->
                HistoryEntryItem(
                    entry = entry,
                    travelLegs = travelLegsByDate[entry.date].orEmpty(),
                    onEntryClick = onEntryClick
                )
            }
        }
    } else {
        weeks.forEach { week ->
            stickyHeader(key = "header-${week.year}-${week.week}") {
                WeekGroupHeader(week = week)
            }
            items(
                items = week.entries,
                key = { entry -> entry.date }
            ) { entry ->
                HistoryEntryItem(
                    entry = entry,
                    travelLegs = travelLegsByDate[entry.date].orEmpty(),
                    onEntryClick = onEntryClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryMiniFilterBar(
    showCalendar: Boolean,
    calendarMode: CalendarMode,
    showMonths: Boolean,
    canToggleMonths: Boolean,
    onShowCalendarChange: (Boolean) -> Unit,
    onCalendarModeChange: (CalendarMode) -> Unit,
    onShowMonthsChange: (Boolean) -> Unit,
    onPickDate: () -> Unit,
    onOpenBatchEdit: () -> Unit = {}
) {
    MZAppPanel {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(190.dp)) {
                MZSegmentedControl(
                    options = listOf(
                        MZSegmentedOption(false, stringResource(R.string.history_summary_list)),
                        MZSegmentedOption(true, stringResource(R.string.history_summary_calendar))
                    ),
                    selectedValue = showCalendar,
                    onValueSelected = onShowCalendarChange
                )
            }

            if (showCalendar) {
                Box(modifier = Modifier.width(190.dp)) {
                    MZSegmentedControl(
                        options = listOf(
                            MZSegmentedOption(CalendarMode.WEEK, stringResource(R.string.history_summary_week)),
                            MZSegmentedOption(CalendarMode.MONTH, stringResource(R.string.history_summary_month))
                        ),
                        selectedValue = calendarMode,
                        onValueSelected = onCalendarModeChange
                    )
                }
            } else if (canToggleMonths) {
                Box(modifier = Modifier.width(210.dp)) {
                    MZSegmentedControl(
                        options = listOf(
                            MZSegmentedOption(false, stringResource(R.string.history_summary_weeks)),
                            MZSegmentedOption(true, stringResource(R.string.history_summary_months))
                        ),
                        selectedValue = showMonths,
                        onValueSelected = onShowMonthsChange
                    )
                }
            }

            SecondaryActionButton(onClick = onPickDate) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(stringResource(R.string.history_action_pick_date))
            }

            TertiaryActionButton(onClick = onOpenBatchEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(stringResource(R.string.cd_batch_edit))
            }
        }
    }
}



@Composable
fun CalendarView(
    month: YearMonth,
    entriesByDate: Map<LocalDate, WorkEntry>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val days = remember(month) { buildCalendarDays(month) }
    val weeks = remember(days) { days.chunked(7) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.history_cd_prev_month)
                )
            }
            Text(
                text = formatMonth(month),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.history_cd_next_month)
                )
            }
        }

        WeekdayHeader()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { day ->
                        CalendarDayCell(
                            day = day,
                            entry = entriesByDate[day.date],
                            isToday = day.date == today,
                            onClick = { if (day.inMonth) onDayClick(day.date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekCalendarView(
    selectedDate: LocalDate,
    entriesByDate: Map<LocalDate, WorkEntry>,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val weekStart = selectedDate.with(historyWeekFields.dayOfWeek(), 1)
    val weekEnd = selectedDate.with(historyWeekFields.dayOfWeek(), 7)
    val weekNumber = selectedDate.get(historyWeekFields.weekOfWeekBasedYear())
    val days = remember(weekStart) {
        (0..6).map { offset ->
            CalendarDay(weekStart.plusDays(offset.toLong()), true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.history_cd_prev_week)
                )
            }
            Text(
                text = stringResource(
                    R.string.history_week_header,
                    weekNumber,
                    Formatters.formatDate(weekStart),
                    Formatters.formatDate(weekEnd)
                ),
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = onNextWeek) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.history_cd_next_week)
                )
            }
        }

        WeekdayHeader()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            days.forEach { day ->
                CalendarDayCell(
                    day = day,
                    entry = entriesByDate[day.date],
                    isToday = day.date == today,
                    onClick = { onDayClick(day.date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun WeekdayHeader() {
    val labels = listOf(
        stringResource(R.string.history_weekday_mo),
        stringResource(R.string.history_weekday_tu),
        stringResource(R.string.history_weekday_we),
        stringResource(R.string.history_weekday_th),
        stringResource(R.string.history_weekday_fr),
        stringResource(R.string.history_weekday_sa),
        stringResource(R.string.history_weekday_su)
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    entry: WorkEntry?,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(MZTokens.RadiusSmall)
    val containerColor = when {
        !day.inMonth -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        entry?.dayType == DayType.OFF -> MaterialTheme.colorScheme.secondaryContainer
        entry?.dayType == DayType.COMP_TIME -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderModifier = if (isToday) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(containerColor)
            .then(borderModifier)
            .clickable(enabled = day.inMonth, onClick = onClick)
            .padding(6.dp)
    ) {
        val compactLayout = maxWidth < 52.dp

        Text(
            text = day.date.dayOfMonth.toString(),
            style = if (compactLayout) {
                MaterialTheme.typography.labelSmall
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = if (day.inMonth) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (entry != null) {
            if (entry.dayType == DayType.OFF) {
                if (compactLayout) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.16f),
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = stringResource(R.string.history_day_type_off_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.history_day_type_off),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            } else if (entry.dayType == DayType.COMP_TIME) {
                if (compactLayout) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.16f),
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = stringResource(R.string.history_day_type_comp_time_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.day_type_comp_time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusDot(
                        active = entry.morningCapturedAt != null,
                        activeColor = MaterialTheme.colorScheme.primary
                    )
                    StatusDot(
                        active = entry.eveningCapturedAt != null,
                        activeColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun StatusDot(
    active: Boolean,
    activeColor: Color
) {
    val color = if (active) {
        activeColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onApply: (BatchEditRequest) -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(6)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var applyDayType by remember { mutableStateOf(false) }
    var selectedDayType by remember { mutableStateOf(DayType.WORK) }
    var applyDefaults by remember { mutableStateOf(false) }
    var applyNote by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    val validRange = !startDate.isAfter(endDate)
    val hasSelection = applyDayType || applyDefaults || applyNote
    val canApply = validRange && hasSelection && !isSubmitting

    MZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_batch_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.history_batch_period), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SecondaryActionButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Formatters.formatDate(startDate))
                    }
                    SecondaryActionButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Formatters.formatDate(endDate))
                    }
                }

                if (!validRange) {
                    Text(
                        text = stringResource(R.string.history_batch_invalid_range),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = applyDayType,
                        onCheckedChange = { applyDayType = it }
                    )
                    Text(stringResource(R.string.history_batch_daytype_set))
                }

                if (applyDayType) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayType.values().forEach { type ->
                            FilterChip(
                                selected = selectedDayType == type,
                                onClick = { selectedDayType = type },
                                label = {
                                    Text(
                                        when (type) {
                                            DayType.WORK -> stringResource(R.string.history_batch_workday)
                                            DayType.OFF -> stringResource(R.string.history_batch_offday)
                                            DayType.COMP_TIME -> stringResource(R.string.history_batch_comp_time)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = applyDefaults,
                        onCheckedChange = { applyDefaults = it }
                    )
                    Text(stringResource(R.string.history_batch_set_defaults))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = applyNote,
                        onCheckedChange = { applyNote = it }
                    )
                    Text(stringResource(R.string.history_batch_set_note))
                }

                if (applyNote) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text(stringResource(R.string.history_batch_note_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!hasSelection) {
                    Text(
                        text = stringResource(R.string.history_batch_select_action),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = {
                    onApply(
                        BatchEditRequest(
                            startDate = startDate,
                            endDate = endDate,
                            dayType = if (applyDayType) selectedDayType else null,
                            applyDefaultTimes = applyDefaults,
                            note = note,
                            applyNote = applyNote
                        )
                    )
                },
                enabled = canApply
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(stringResource(R.string.history_batch_apply))
            }
        },
        dismissButton = {
            TertiaryActionButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    if (showStartPicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { date ->
                startDate = date
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDateSelected = { date ->
                endDate = date
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
fun WeekGroupHeader(
    week: WeekGroup
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MZSectionHeader(
            title = stringResource(R.string.history_week_header, week.week, "", "").substringBefore("·").trim()
        )
        
        MZAppPanel(emphasized = false) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MZStatusChip(
                    text = "KW ${week.week}",
                    color = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_hours_decimal, week.totalHours),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.history_summary_workdays,
                            week.workDaysCount,
                            week.workDaysCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                week.yearText.takeIf { it.isNotEmpty() }?.let { year ->
                    Text(
                        text = year,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MonthGroupHeader(
    month: MonthGroup
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MZSectionHeader(title = month.displayText)
        
        MZAppPanel(emphasized = false) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MZStatusChip(
                    text = month.displayText,
                    color = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_hours_decimal, month.totalHours),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.history_summary_workdays,
                            month.workDaysCount,
                            month.workDaysCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                month.yearText.takeIf { it.isNotEmpty() }?.let { year ->
                    Text(
                        text = year,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryEntryItem(
    entry: WorkEntry,
    travelLegs: List<TravelLeg>,
    onEntryClick: (LocalDate) -> Unit
) {
    val travelMinutes = remember(travelLegs) { TimeCalculator.calculateTravelMinutes(travelLegs) }
    val workHours = remember(entry) { TimeCalculator.calculateWorkHours(entry) }
    val totalPaidHours = remember(entry, travelLegs) { TimeCalculator.calculatePaidTotalHours(entry, travelLegs) }
    val hoursOnlyFormat = stringResource(R.string.history_hours_only)
    val hoursMinutesFormat = stringResource(R.string.history_hours_and_minutes)
    val locationLabel = entry.dayLocationLabel.ifBlank {
        stringResource(R.string.today_day_location_unset)
    }
    var dragAccum by remember(entry.date) { mutableStateOf(0f) }
    
    val statusChipInfo: Pair<String, Color>? = when {
        !entry.confirmedWorkDay && entry.dayType == DayType.WORK -> stringResource(R.string.today_unconfirmed) to MaterialTheme.colorScheme.error
        entry.dayType == DayType.OFF -> stringResource(R.string.history_day_type_off) to MaterialTheme.colorScheme.outline
        entry.dayType == DayType.COMP_TIME -> stringResource(R.string.day_type_comp_time) to MaterialTheme.colorScheme.secondary
        else -> null
    }

    val travelSummary = remember(travelLegs) {
        travelLegs
            .sortedBy(TravelLeg::sortOrder)
            .firstOrNull()
            ?.let { leg ->
                listOfNotNull(
                    leg.startLabel?.takeIf { it.isNotBlank() },
                    leg.endLabel?.takeIf { it.isNotBlank() }
                ).joinToString(" → ")
            }
            .orEmpty()
    }

    MZAppPanel(
        modifier = Modifier
            .clickable { onEntryClick(entry.date) }
            .pointerInput(entry.date) {
                val swipeThresholdPx = 52.dp.toPx()
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (kotlin.math.abs(dragAccum) > swipeThresholdPx) {
                            onEntryClick(entry.date)
                        }
                        dragAccum = 0f
                    },
                    onDragCancel = { dragAccum = 0f }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatEntryDate(entry.date),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (entry.dayType == DayType.WORK) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (entry.dayType == DayType.WORK || travelMinutes > 0) {
                    val displayHours = if (workHours > 0.0) workHours else totalPaidHours
                    Text(
                        text = formatWorkHoursPlain(displayHours, hoursOnlyFormat, hoursMinutesFormat),
                        style = NumberStyles.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MZStatusChip(
                    text = locationLabel,
                    color = MaterialTheme.colorScheme.outline
                )

                statusChipInfo?.let { (text, color) ->
                    MZStatusChip(
                        text = text,
                        color = color
                    )
                }

                if (travelSummary.isNotBlank()) {
                    MZStatusChip(
                        text = travelSummary,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (workHours > 0.0 && kotlin.math.abs(totalPaidHours - workHours) > 0.01 && travelSummary.isBlank()) {
                Text(
                    text = stringResource(
                        R.string.history_entry_total_paid,
                        formatWorkHoursPlain(totalPaidHours, hoursOnlyFormat, hoursMinutesFormat)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun DayTypeIndicator(dayType: DayType) {
    val (icon, color) = when (dayType) {
        DayType.WORK -> Icons.Default.Work to MaterialTheme.colorScheme.primary
        DayType.OFF -> Icons.Default.FreeBreakfast to MaterialTheme.colorScheme.secondary
        DayType.COMP_TIME -> Icons.Default.Bedtime to MaterialTheme.colorScheme.tertiary
    }
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(18.dp)
    )
}

@Composable
fun LocationSummary(entry: WorkEntry) {
    val locationLabel = entry.dayLocationLabel.ifBlank {
        stringResource(R.string.today_day_location_unset)
    }
    val checkInSummary = listOfNotNull(
        entry.morningCapturedAt?.let {
            stringResource(R.string.today_location_morning) + " " + formatTime(it)
        },
        entry.eveningCapturedAt?.let {
            stringResource(R.string.today_location_evening) + " " + formatTime(it)
        }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(
                    R.string.history_day_location_summary,
                    stringResource(R.string.day_location_label),
                    locationLabel
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
        Text(
            text = checkInSummary.ifEmpty {
                listOf(stringResource(R.string.history_label_no_checkin))
            }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TravelSummaryRow(travelLegs: List<TravelLeg>) {
    val orderedLegs = remember(travelLegs) {
        travelLegs
            .sortedBy(TravelLeg::sortOrder)
            .filter { leg ->
                leg.startAt != null ||
                    leg.arriveAt != null ||
                    !leg.startLabel.isNullOrBlank() ||
                    !leg.endLabel.isNullOrBlank() ||
                    (leg.paidMinutesOverride ?: 0) > 0
            }
    }

    if (orderedLegs.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        orderedLegs.forEach { leg ->
            val labelText = when {
                !leg.startLabel.isNullOrBlank() && !leg.endLabel.isNullOrBlank() ->
                    stringResource(
                        R.string.travel_label_from_to,
                        leg.startLabel.orEmpty(),
                        leg.endLabel.orEmpty()
                    )
                !leg.startLabel.isNullOrBlank() ->
                    stringResource(R.string.travel_label_from, leg.startLabel.orEmpty())
                !leg.endLabel.isNullOrBlank() ->
                    stringResource(R.string.travel_label_to, leg.endLabel.orEmpty())
                else -> null
            }
            val timeText = when {
                leg.startAt != null && leg.arriveAt != null ->
                    stringResource(R.string.travel_time_range, formatTime(leg.startAt), formatTime(leg.arriveAt))
                leg.startAt != null -> stringResource(R.string.travel_time_start, formatTime(leg.startAt))
                leg.arriveAt != null -> stringResource(R.string.travel_time_arrive, formatTime(leg.arriveAt))
                else -> null
            }
            val durationText = remember(leg.startAt, leg.arriveAt, leg.paidMinutesOverride) {
                when {
                    leg.startAt != null && leg.arriveAt != null ->
                        DateTimeUtils.calculateTravelDuration(leg.startAt, leg.arriveAt)?.let(Formatters::formatDuration)
                    (leg.paidMinutesOverride ?: 0) > 0 ->
                        Formatters.formatDuration(java.time.Duration.ofMinutes(leg.paidMinutesOverride!!.toLong()))
                    else -> null
                }
            }
            val summaryText = listOfNotNull(
                timeText,
                durationText?.let { stringResource(R.string.history_travel_duration_inline, it) }
            ).joinToString(" · ")

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Column {
                    if (summaryText.isNotBlank()) {
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    labelText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryActionButton(onClick = onRetry) {
            Text(stringResource(R.string.history_action_repeat))
        }
    }
}

data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean
)

enum class CalendarMode {
    WEEK,
    MONTH
}

private fun buildCalendarDays(month: YearMonth): List<CalendarDay> {
    val firstOfMonth = month.atDay(1)
    val firstDayOfWeek = historyWeekFields.firstDayOfWeek
    val offset = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val startDate = firstOfMonth.minusDays(offset.toLong())
    val totalDays = month.lengthOfMonth()
    val totalCells = ((offset + totalDays + 6) / 7) * 7

    return (0 until totalCells).map { index ->
        val date = startDate.plusDays(index.toLong())
        CalendarDay(
            date = date,
            inMonth = date.month == month.month && date.year == month.year
        )
    }
}

private fun formatMonth(month: YearMonth): String {
    return month.atDay(1).format(historyMonthFormatter)
}

private fun formatEntryDate(date: java.time.LocalDate): String {
    return date.format(historyEntryDateFormatter)
}

private fun formatOverviewDate(date: java.time.LocalDate): String {
    return date.format(historyOverviewDateFormatter)
}

private fun formatTime(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val time = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return Formatters.formatTime(time)
}

@Composable
private fun formatWorkHours(hours: Double): String {
    val totalMinutes = (hours * 60).toInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    
    return if (m == 0) {
        stringResource(R.string.history_hours_only, h)
    } else {
        stringResource(R.string.history_hours_and_minutes, h, m)
    }
}

@Composable
private fun formatMinutesAsHoursMinutes(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (m == 0) {
        stringResource(R.string.history_hours_only, h)
    } else {
        stringResource(R.string.history_hours_and_minutes, h, m)
    }
}

/**
 * Non-@Composable helper to format work hours.
 * Use this when calling from string templates or other non-@Composable contexts.
 *
 * @param hours Work hours as decimal (e.g., 8.5 for 8 hours 30 minutes)
 * @param hoursOnlyFormat Format string for full hours (e.g., "%dh")
 * @param hoursMinutesFormat Format string for hours + minutes (e.g., "%dh %dmin")
 * @return Formatted string (e.g., "8h" or "8h 30min")
 */
private fun formatWorkHoursPlain(hours: Double, hoursOnlyFormat: String, hoursMinutesFormat: String): String {
    val totalMinutes = (hours * 60).toInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60

    return if (m == 0) {
        hoursOnlyFormat.format(h)
    } else {
        hoursMinutesFormat.format(h, m)
    }
}

/**
 * Non-@Composable helper to format minutes as hours and minutes.
 * Use this when calling from string templates or other non-@Composable contexts.
 *
 * @param totalMinutes Total minutes to format
 * @param hoursOnlyFormat Format string for full hours (e.g., "%dh")
 * @param hoursMinutesFormat Format string for hours + minutes (e.g., "%dh %dmin")
 * @return Formatted string (e.g., "2h" or "2h 15min")
 */
private fun formatMinutesAsHoursMinutesPlain(totalMinutes: Int, hoursOnlyFormat: String, hoursMinutesFormat: String): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (m == 0) {
        hoursOnlyFormat.format(h)
    } else {
        hoursMinutesFormat.format(h, m)
    }
}

private val historyMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
private val historyEntryDateFormatter = DateTimeFormatter.ofPattern("E, dd.MM.", Locale.GERMAN)
private val historyOverviewDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN)
