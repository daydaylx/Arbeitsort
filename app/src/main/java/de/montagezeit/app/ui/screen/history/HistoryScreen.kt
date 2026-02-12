package de.montagezeit.app.ui.screen.history

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.ui.common.DatePickerDialog
import de.montagezeit.app.ui.util.DateTimeUtils
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.asString
import de.montagezeit.app.ui.util.getReviewReason
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton
import java.time.Duration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenEditSheet: (java.time.LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var isBatchEditing by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    IconButton(
                        onClick = { onOpenEditSheet(LocalDate.now()) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = stringResource(R.string.cd_edit_today)
                        )
                    }
                    IconButton(
                        onClick = { showBatchEditDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.cd_batch_edit)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is HistoryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is HistoryUiState.Success -> {
                    val successState = uiState as HistoryUiState.Success
                    HistoryContent(
                        weeks = successState.weeks,
                        months = successState.months,
                        entriesByDate = successState.entriesByDate,
                        onEntryClick = onOpenEditSheet,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                is HistoryUiState.Error -> {
                    ErrorContent(
                        message = (uiState as HistoryUiState.Error).message.asString(context),
                        onRetry = { viewModel.loadHistory() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showBatchEditDialog) {
        BatchEditDialog(
            isSubmitting = isBatchEditing,
            onDismiss = { if (!isBatchEditing) showBatchEditDialog = false },
            onApply = { request ->
                isBatchEditing = true
                viewModel.applyBatchEdit(request) { success ->
                    isBatchEditing = false
                    if (success) {
                        showBatchEditDialog = false
                        Toast.makeText(context, context.getString(R.string.history_toast_batch_updated), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.history_toast_batch_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    weeks: List<WeekGroup>,
    months: List<MonthGroup>,
    entriesByDate: Map<LocalDate, WorkEntry>,
    onEntryClick: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showMonths by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var calendarMode by remember { mutableStateOf(CalendarMode.MONTH) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showNeedsReviewOnly by rememberSaveable { mutableStateOf(false) }
    var showAdvancedOptionsSheet by rememberSaveable { mutableStateOf(false) }

    val optionsSummary = buildString {
        if (showCalendar) {
            append(stringResource(R.string.history_summary_calendar))
            append(" · ")
            append(
                stringResource(
                    if (calendarMode == CalendarMode.MONTH) {
                        R.string.history_summary_month
                    } else {
                        R.string.history_summary_week
                    }
                )
            )
        } else {
            append(stringResource(R.string.history_summary_list))
            append(" · ")
            append(
                stringResource(
                    if (showMonths) {
                        R.string.history_summary_months
                    } else {
                        R.string.history_summary_weeks
                    }
                )
            )
        }
        if (showNeedsReviewOnly) {
            append(" · ")
            append(stringResource(R.string.history_summary_review))
        }
    }

    val filteredWeeks = remember(weeks, showNeedsReviewOnly) {
        if (!showNeedsReviewOnly) {
            weeks
        } else {
            weeks.mapNotNull { week ->
                val filteredEntries = week.entries.filter { it.needsReview }
                if (filteredEntries.isEmpty()) null else week.copyWithEntries(filteredEntries)
            }
        }
    }

    val filteredMonths = remember(months, showNeedsReviewOnly) {
        if (!showNeedsReviewOnly) {
            months
        } else {
            months.mapNotNull { month ->
                val filteredEntries = month.entries.filter { it.needsReview }
                if (filteredEntries.isEmpty()) null else month.copyWithEntries(filteredEntries)
            }
        }
    }

    val filteredEntriesByDate = remember(entriesByDate, showNeedsReviewOnly) {
        if (!showNeedsReviewOnly) {
            entriesByDate
        } else {
            entriesByDate.filterValues { it.needsReview }
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HistoryMiniFilterBar(
            showCalendar = showCalendar,
            calendarMode = calendarMode,
            showMonths = showMonths,
            canToggleMonths = months.isNotEmpty(),
            showNeedsReviewOnly = showNeedsReviewOnly,
            onShowCalendarChange = { showCalendar = it },
            onCalendarModeChange = { calendarMode = it },
            onShowMonthsChange = { showMonths = it },
            onShowNeedsReviewOnlyChange = { showNeedsReviewOnly = it },
            onOpenAdvancedOptions = { showAdvancedOptionsSheet = true }
        )

        when {
            showCalendar -> {
                if (calendarMode == CalendarMode.MONTH) {
                    CalendarView(
                        month = selectedMonth,
                        entriesByDate = filteredEntriesByDate,
                        onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                        onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
                        onDayClick = { date ->
                            selectedDate = date
                            selectedMonth = YearMonth.from(date)
                            onEntryClick(date)
                        }
                    )
                } else {
                    WeekCalendarView(
                        selectedDate = selectedDate,
                        entriesByDate = filteredEntriesByDate,
                        onPreviousWeek = { selectedDate = selectedDate.minusDays(7) },
                        onNextWeek = { selectedDate = selectedDate.plusDays(7) },
                        onDayClick = { date ->
                            selectedDate = date
                            selectedMonth = YearMonth.from(date)
                            onEntryClick(date)
                        }
                    )
                }
            }
            filteredWeeks.isEmpty() && filteredMonths.isEmpty() -> {
                EmptyContent(
                    modifier = Modifier.fillMaxWidth(),
                    onAddPastDay = { showDatePicker = true },
                    showAddButton = false,
                    title = if (showNeedsReviewOnly) {
                        stringResource(R.string.history_empty_review_title)
                    } else {
                        stringResource(R.string.history_empty_title)
                    },
                    subtitle = if (showNeedsReviewOnly) {
                        stringResource(R.string.history_empty_review_subtitle)
                    } else {
                        stringResource(R.string.history_empty_subtitle)
                    }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showMonths) {
                        items(
                            items = filteredMonths,
                            key = { month -> "${month.year}-${month.month}" }
                        ) { month ->
                            MonthGroupCard(
                                month = month,
                                onEntryClick = onEntryClick
                            )
                        }
                    } else {
                        items(
                            items = filteredWeeks,
                            key = { week -> "${week.year}-${week.week}" }
                        ) { week ->
                            WeekGroupCard(
                                week = week,
                                onEntryClick = onEntryClick
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = java.time.LocalDate.now(),
            onDateSelected = { date ->
                onEntryClick(date)
                selectedDate = date
                selectedMonth = YearMonth.from(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showAdvancedOptionsSheet) {
        HistoryAdvancedOptionsSheet(
            showCalendar = showCalendar,
            calendarMode = calendarMode,
            showMonths = showMonths,
            canToggleMonths = months.isNotEmpty(),
            showNeedsReviewOnly = showNeedsReviewOnly,
            optionsSummary = optionsSummary,
            onShowCalendarChange = { showCalendar = it },
            onCalendarModeChange = { calendarMode = it },
            onShowMonthsChange = { showMonths = it },
            onShowNeedsReviewOnlyChange = { showNeedsReviewOnly = it },
            onPickDate = {
                showAdvancedOptionsSheet = false
                showDatePicker = true
            },
            onDismiss = { showAdvancedOptionsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryMiniFilterBar(
    showCalendar: Boolean,
    calendarMode: CalendarMode,
    showMonths: Boolean,
    canToggleMonths: Boolean,
    showNeedsReviewOnly: Boolean,
    onShowCalendarChange: (Boolean) -> Unit,
    onCalendarModeChange: (CalendarMode) -> Unit,
    onShowMonthsChange: (Boolean) -> Unit,
    onShowNeedsReviewOnlyChange: (Boolean) -> Unit,
    onOpenAdvancedOptions: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !showCalendar,
                onClick = { onShowCalendarChange(false) },
                label = { Text(stringResource(R.string.history_summary_list)) }
            )
            FilterChip(
                selected = showCalendar,
                onClick = { onShowCalendarChange(true) },
                label = { Text(stringResource(R.string.history_summary_calendar)) }
            )

            if (showCalendar) {
                FilterChip(
                    selected = calendarMode == CalendarMode.WEEK,
                    onClick = { onCalendarModeChange(CalendarMode.WEEK) },
                    label = { Text(stringResource(R.string.history_summary_week)) }
                )
                FilterChip(
                    selected = calendarMode == CalendarMode.MONTH,
                    onClick = { onCalendarModeChange(CalendarMode.MONTH) },
                    label = { Text(stringResource(R.string.history_summary_month)) }
                )
            } else if (canToggleMonths) {
                FilterChip(
                    selected = !showMonths,
                    onClick = { onShowMonthsChange(false) },
                    label = { Text(stringResource(R.string.history_summary_weeks)) }
                )
                FilterChip(
                    selected = showMonths,
                    onClick = { onShowMonthsChange(true) },
                    label = { Text(stringResource(R.string.history_summary_months)) }
                )
            }

            FilterChip(
                selected = showNeedsReviewOnly,
                onClick = { onShowNeedsReviewOnlyChange(!showNeedsReviewOnly) },
                label = { Text(stringResource(R.string.history_chip_review_short)) }
            )

            AssistChip(
                onClick = onOpenAdvancedOptions,
                label = { Text(stringResource(R.string.history_chip_options)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.history_cd_open_options)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryAdvancedOptionsSheet(
    showCalendar: Boolean,
    calendarMode: CalendarMode,
    showMonths: Boolean,
    canToggleMonths: Boolean,
    showNeedsReviewOnly: Boolean,
    optionsSummary: String,
    onShowCalendarChange: (Boolean) -> Unit,
    onCalendarModeChange: (CalendarMode) -> Unit,
    onShowMonthsChange: (Boolean) -> Unit,
    onShowNeedsReviewOnlyChange: (Boolean) -> Unit,
    onPickDate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.history_options_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = optionsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SecondaryActionButton(
                onClick = onPickDate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.history_action_pick_date))
            }

            Divider()

            Text(
                text = stringResource(R.string.history_section_view),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showCalendar,
                    onClick = { onShowCalendarChange(false) },
                    label = { Text(stringResource(R.string.history_summary_list)) }
                )
                FilterChip(
                    selected = showCalendar,
                    onClick = { onShowCalendarChange(true) },
                    label = { Text(stringResource(R.string.history_summary_calendar)) }
                )
            }

            if (showCalendar) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = calendarMode == CalendarMode.WEEK,
                        onClick = { onCalendarModeChange(CalendarMode.WEEK) },
                        label = { Text(stringResource(R.string.history_summary_week)) }
                    )
                    FilterChip(
                        selected = calendarMode == CalendarMode.MONTH,
                        onClick = { onCalendarModeChange(CalendarMode.MONTH) },
                        label = { Text(stringResource(R.string.history_summary_month)) }
                    )
                }
            }

            if (!showCalendar && canToggleMonths) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showMonths,
                        onClick = { onShowMonthsChange(false) },
                        label = { Text(stringResource(R.string.history_summary_weeks)) }
                    )
                    FilterChip(
                        selected = showMonths,
                        onClick = { onShowMonthsChange(true) },
                        label = { Text(stringResource(R.string.history_summary_months)) }
                    )
                }
            }

            Divider()

            Text(
                text = stringResource(R.string.history_section_filter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilterChip(
                selected = showNeedsReviewOnly,
                onClick = { onShowNeedsReviewOnlyChange(!showNeedsReviewOnly) },
                label = { Text(stringResource(R.string.history_chip_review_only)) }
            )

            TertiaryActionButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_close))
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.history_cd_prev_month)
                )
            }
            Text(
                text = formatMonth(month),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
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
    val weekFields = WeekFields.of(Locale.GERMAN)
    val weekStart = selectedDate.with(weekFields.dayOfWeek(), 1)
    val weekEnd = selectedDate.with(weekFields.dayOfWeek(), 7)
    val weekNumber = selectedDate.get(weekFields.weekOfWeekBasedYear())
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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.history_cd_prev_week)
                )
            }
            Text(
                text = stringResource(
                    R.string.history_week_header,
                    weekNumber,
                    formatShortDate(weekStart),
                    formatShortDate(weekEnd)
                ),
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = onNextWeek) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
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
    val shape = RoundedCornerShape(8.dp)
    val containerColor = when {
        !day.inMonth -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        entry?.dayType == DayType.OFF -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderModifier = if (isToday) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(containerColor)
            .then(borderModifier)
            .clickable(enabled = day.inMonth, onClick = onClick)
            .padding(6.dp)
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (day.inMonth) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (entry != null) {
            if (entry.needsReview) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }

            if (entry.dayType == DayType.OFF) {
                Text(
                    text = stringResource(R.string.history_day_type_off),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
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
    activeColor: androidx.compose.ui.graphics.Color
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

    var applyDayType by remember { mutableStateOf(true) }
    var selectedDayType by remember { mutableStateOf(DayType.WORK) }
    var applyDefaults by remember { mutableStateOf(false) }
    var applyNote by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    val validRange = !startDate.isAfter(endDate)
    val hasSelection = applyDayType || applyDefaults || applyNote
    val canApply = validRange && hasSelection && !isSubmitting

    AlertDialog(
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
                        Text(formatShortDate(startDate))
                    }
                    SecondaryActionButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatShortDate(endDate))
                    }
                }

                if (!validRange) {
                    Text(
                        text = stringResource(R.string.history_batch_invalid_range),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Divider()

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
fun EmptyContent(
    modifier: Modifier = Modifier,
    onAddPastDay: () -> Unit = {},
    showAddButton: Boolean = true,
    title: String = stringResource(R.string.history_empty_title),
    subtitle: String = stringResource(R.string.history_empty_subtitle)
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (showAddButton) {
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryActionButton(
                onClick = onAddPastDay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.history_action_add_past_day))
            }
        }
    }
}

@Composable
fun WeekGroupCard(
    week: WeekGroup,
    onEntryClick: (java.time.LocalDate) -> Unit
) {
    var expanded by rememberSaveable(week.year, week.week) { mutableStateOf(false) }
    val summaryText = buildString {
        append(stringResource(R.string.history_hours_decimal, week.totalHours))
        append(" · ")
        append(stringResource(R.string.history_summary_workdays, week.workDaysCount))
        if (week.entriesNeedingReview > 0) {
            append(" · ")
            append(stringResource(R.string.history_summary_review_count, week.entriesNeedingReview))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = week.displayText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    week.yearText.takeIf { it.isNotEmpty() }?.let { year ->
                        Text(
                            text = year,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) {
                        stringResource(R.string.history_cd_collapse_week)
                    } else {
                        stringResource(R.string.history_cd_expand_week)
                    }
                )
            }

            if (expanded && week.entries.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Total and average hours
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.history_stat_total),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.history_hours_decimal, week.totalHours),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (kotlin.math.abs(week.totalPaidHours - week.totalHours) > 0.01) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.history_stat_paid),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.history_hours_decimal, week.totalPaidHours),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (week.workDaysCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.history_stat_average),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.history_average_hours_per_day, week.averageHoursPerDay),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Days breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.history_stat_days),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (week.offDaysCount > 0) {
                                    stringResource(
                                        R.string.history_days_breakdown_work_with_off,
                                        week.workDaysCount,
                                        week.offDaysCount
                                    )
                                } else {
                                    stringResource(
                                        R.string.history_days_breakdown_work_only,
                                        week.workDaysCount
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Warnings/Info
                        if (week.entriesNeedingReview > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.history_entries_need_review,
                                        week.entriesNeedingReview,
                                        week.entriesNeedingReview
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (week.daysOutsideLeipzig > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.history_days_outside_leipzig,
                                        week.daysOutsideLeipzig,
                                        week.daysOutsideLeipzig
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (expanded) {
                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    week.entries.forEach { entry ->
                        key(entry.date) {
                            HistoryEntryItem(
                                entry = entry,
                                onClick = { onEntryClick(entry.date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthGroupCard(
    month: MonthGroup,
    onEntryClick: (java.time.LocalDate) -> Unit
) {
    var expanded by rememberSaveable(month.year, month.month) { mutableStateOf(false) }
    val summaryText = buildString {
        append(stringResource(R.string.history_hours_decimal, month.totalHours))
        append(" · ")
        append(stringResource(R.string.history_summary_workdays, month.workDaysCount))
        if (month.entriesNeedingReview > 0) {
            append(" · ")
            append(stringResource(R.string.history_summary_review_count, month.entriesNeedingReview))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = month.displayText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    month.yearText.takeIf { it.isNotEmpty() }?.let { year ->
                        Text(
                            text = year,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) {
                        stringResource(R.string.history_cd_collapse_month)
                    } else {
                        stringResource(R.string.history_cd_expand_month)
                    }
                )
            }

            if (expanded && month.entries.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Total and average hours
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.history_stat_total),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.history_hours_decimal, month.totalHours),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (kotlin.math.abs(month.totalPaidHours - month.totalHours) > 0.01) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.history_stat_paid),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.history_hours_decimal, month.totalPaidHours),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (month.workDaysCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.history_stat_average),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.history_average_hours_per_day, month.averageHoursPerDay),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Days breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.history_stat_days),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (month.offDaysCount > 0) {
                                    stringResource(
                                        R.string.history_days_breakdown_work_with_off,
                                        month.workDaysCount,
                                        month.offDaysCount
                                    )
                                } else {
                                    stringResource(
                                        R.string.history_days_breakdown_work_only,
                                        month.workDaysCount
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Warnings/Info
                        if (month.entriesNeedingReview > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.history_entries_need_review,
                                        month.entriesNeedingReview,
                                        month.entriesNeedingReview
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (month.daysOutsideLeipzig > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.history_days_outside_leipzig,
                                        month.daysOutsideLeipzig,
                                        month.daysOutsideLeipzig
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (expanded) {
                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val entriesToShow = month.entries.take(5)
                    val remainingCount = month.entries.size - entriesToShow.size

                    entriesToShow.forEach { entry ->
                        key(entry.date) {
                            HistoryEntryItem(
                                entry = entry,
                                onClick = { onEntryClick(entry.date) }
                            )
                        }
                    }

                    if (remainingCount > 0) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.history_remaining_days,
                                remainingCount,
                                remainingCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryEntryItem(
    entry: WorkEntry,
    onClick: () -> Unit
) {
    val travelMinutes = remember(entry) { TimeCalculator.calculateTravelMinutes(entry) }
    val workHours = remember(entry) { TimeCalculator.calculateWorkHours(entry) }
    val totalPaidHours = remember(entry) { TimeCalculator.calculatePaidTotalHours(entry) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.needsReview) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatEntryDate(entry.date),
                        style = MaterialTheme.typography.titleMedium
                    )
                    DayTypeIndicator(dayType = entry.dayType)
                    if (entry.needsReview) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.history_cd_review_required),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (entry.dayType == DayType.WORK || travelMinutes > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatWorkHours(workHours),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        if (kotlin.math.abs(totalPaidHours - workHours) > 0.01) {
                            Text(
                                text = stringResource(
                                    R.string.history_entry_total_paid,
                                    formatWorkHours(totalPaidHours)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (entry.needsReview) {
                Text(
                    text = getReviewReason(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // Location Status Info
            LocationSummary(entry = entry)
            TravelSummaryRow(entry = entry)
        }
    }
}

@Composable
fun DayTypeIndicator(dayType: DayType) {
    val (icon, color) = when (dayType) {
        DayType.WORK -> Icons.Default.Work to MaterialTheme.colorScheme.primary
        DayType.OFF -> Icons.Default.FreeBreakfast to MaterialTheme.colorScheme.secondary
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
    val hasMorning = entry.morningCapturedAt != null
    val hasEvening = entry.eveningCapturedAt != null
    
    if (!hasMorning && !hasEvening) {
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
                    entry.dayLocationLabel.orEmpty()
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
        Text(
            text = stringResource(R.string.history_label_no_checkin),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        return
    }
    
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
                entry.dayLocationLabel.orEmpty()
            ),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasMorning) {
            LocationStatusIcon(status = entry.morningLocationStatus)
        }
        
        if (hasEvening) {
            LocationStatusIcon(status = entry.eveningLocationStatus)
        }
        
        // Location labels (if available and different from default)
        val defaultLabel = entry.dayLocationLabel
        val labels = listOfNotNull(
            entry.morningLocationLabel?.takeIf { it != defaultLabel },
            entry.eveningLocationLabel?.takeIf { it != defaultLabel }
        )
        
        if (labels.isNotEmpty()) {
            Text(
                text = labels.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TravelSummaryRow(entry: WorkEntry) {
    val startAt = entry.travelStartAt
    val arriveAt = entry.travelArriveAt
    val labelText = when {
        !entry.travelLabelStart.isNullOrBlank() && !entry.travelLabelEnd.isNullOrBlank() ->
            stringResource(
                R.string.travel_label_from_to,
                entry.travelLabelStart.orEmpty(),
                entry.travelLabelEnd.orEmpty()
            )
        !entry.travelLabelStart.isNullOrBlank() ->
            stringResource(R.string.travel_label_from, entry.travelLabelStart.orEmpty())
        !entry.travelLabelEnd.isNullOrBlank() ->
            stringResource(R.string.travel_label_to, entry.travelLabelEnd.orEmpty())
        else -> null
    }

    if (startAt == null && arriveAt == null && labelText == null) return

    val timeText = when {
        startAt != null && arriveAt != null ->
            stringResource(R.string.travel_time_range, formatTime(startAt), formatTime(arriveAt))
        startAt != null -> stringResource(R.string.travel_time_start, formatTime(startAt))
        arriveAt != null -> stringResource(R.string.travel_time_arrive, formatTime(arriveAt))
        else -> null
    }
    val durationText = remember(startAt, arriveAt) {
        DateTimeUtils.calculateTravelDuration(startAt, arriveAt)?.let { Formatters.formatDuration(it) }
    }
    val summaryText = listOfNotNull(
        timeText,
        durationText?.let { stringResource(R.string.history_travel_duration_inline, it) }
    )
        .joinToString(" · ")

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Column {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
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

@Composable
fun LocationStatusIcon(status: LocationStatus) {
    val (icon, color) = when (status) {
        LocationStatus.OK -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        LocationStatus.LOW_ACCURACY -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        LocationStatus.UNAVAILABLE -> Icons.Default.LocationOff to MaterialTheme.colorScheme.error
    }
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
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

private fun WeekGroup.copyWithEntries(entries: List<WorkEntry>): WeekGroup {
    val workDaysCount = entries.count { it.dayType == DayType.WORK }
    val totalHours = entries.sumOf { TimeCalculator.calculateWorkHours(it) }
    return copy(
        entries = entries,
        workDaysCount = workDaysCount,
        offDaysCount = entries.count { it.dayType == DayType.OFF },
        totalHours = totalHours,
        totalPaidHours = entries.sumOf { TimeCalculator.calculatePaidTotalHours(it) },
        averageHoursPerDay = if (workDaysCount > 0) totalHours / workDaysCount else 0.0,
        entriesNeedingReview = entries.count { it.needsReview },
        daysOutsideLeipzig = entries.count { entry ->
            entry.outsideLeipzigMorning == true || entry.outsideLeipzigEvening == true
        }
    )
}

private fun MonthGroup.copyWithEntries(entries: List<WorkEntry>): MonthGroup {
    val workDaysCount = entries.count { it.dayType == DayType.WORK }
    val totalHours = entries.sumOf { TimeCalculator.calculateWorkHours(it) }
    return copy(
        entries = entries,
        workDaysCount = workDaysCount,
        offDaysCount = entries.count { it.dayType == DayType.OFF },
        totalHours = totalHours,
        totalPaidHours = entries.sumOf { TimeCalculator.calculatePaidTotalHours(it) },
        averageHoursPerDay = if (workDaysCount > 0) totalHours / workDaysCount else 0.0,
        entriesNeedingReview = entries.count { it.needsReview },
        daysOutsideLeipzig = entries.count { entry ->
            entry.outsideLeipzigMorning == true || entry.outsideLeipzigEvening == true
        }
    )
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
    val weekFields = WeekFields.of(Locale.GERMAN)
    val firstDayOfWeek = weekFields.firstDayOfWeek
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

private fun formatShortDate(date: java.time.LocalDate): String {
    return date.format(historyShortDateFormatter)
}

private fun formatTime(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val time = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return time.format(historyTimeFormatter)
}

// Removed: calculateTravelDuration - now using DateTimeUtils.calculateTravelDuration

// Removed: formatDuration - now using Formatters.formatDuration

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

private val historyMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
private val historyEntryDateFormatter = DateTimeFormatter.ofPattern("E, dd.MM.", Locale.GERMAN)
private val historyShortDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
private val historyTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
