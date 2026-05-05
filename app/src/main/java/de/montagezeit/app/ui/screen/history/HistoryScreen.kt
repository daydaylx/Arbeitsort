@file:Suppress("LongMethod", "CyclomaticComplexMethod", "MaxLineLength")

package de.montagezeit.app.ui.screen.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import de.montagezeit.app.domain.usecase.EntryStatusResolver
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.ui.components.DatePickerDialog
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
private const val HISTORY_LIST_CONTENT_START_INDEX = 2
private val historyWeekFields = WeekFields.ISO


private val localDateSaver = Saver<LocalDate, String>(
    save = { it.toString() },
    restore = { LocalDate.parse(it) }
)

private val yearMonthSaver = Saver<YearMonth, String>(
    save = { it.toString() },
    restore = { YearMonth.parse(it) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenEditSheet: (java.time.LocalDate) -> Unit,
    openRequest: HistoryOpenRequest? = null,
    onOpenRequestConsumed: () -> Unit = {}
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
                    openRequest = openRequest,
                    onOpenRequestConsumed = onOpenRequestConsumed,
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
    openRequest: HistoryOpenRequest? = null,
    onOpenRequestConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showMonths by rememberSaveable { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var selectedMonth by rememberSaveable(stateSaver = yearMonthSaver) { mutableStateOf(YearMonth.now()) }
    var selectedDate by rememberSaveable(stateSaver = localDateSaver) { mutableStateOf(LocalDate.now()) }
    var pendingScrollRequest by remember { mutableStateOf<HistoryOpenRequest?>(null) }
    val listState = rememberLazyListState()
    val entries = remember(entriesByDate) { entriesByDate.values.sortedByDescending(WorkEntry::date) }
    val pendingEntries = remember(entries, travelLegsByDate) {
        entries.filter { entry ->
            EntryStatusResolver.isPendingWorkDay(
                entry = entry,
                travelLegs = travelLegsByDate[entry.date].orEmpty()
            )
        }
    }
    val workEntryCount = remember(entries) { entries.count { it.dayType == DayType.WORK } }
    val unconfirmedCount = pendingEntries.size
    val hasGroupedContent = weeks.isNotEmpty() || months.isNotEmpty()
    val openEntryForDate: (LocalDate) -> Unit = { date ->
        selectedDate = date
        selectedMonth = YearMonth.from(date)
        onEntryClick(date)
    }

    LaunchedEffect(openRequest?.requestId) {
        val request = openRequest ?: return@LaunchedEffect
        val selectionSeed = historySelectionSeedForRequest(request)
        showCalendar = selectionSeed.showCalendar
        showMonths = selectionSeed.showMonths
        selectedDate = selectionSeed.selectedDate
        selectedMonth = selectionSeed.selectedMonth
        pendingScrollRequest = request
    }

    LaunchedEffect(pendingScrollRequest?.requestId, showCalendar, showMonths, entries, weeks, months, unconfirmedCount) {
        val request = pendingScrollRequest ?: return@LaunchedEffect
        if (showCalendar) return@LaunchedEffect

        val targetIndex = if (showMonths) {
            historyGroupedScrollTargetIndex(
                request = request,
                weeks = weeks,
                months = months
            )
        } else {
            val entryIndex = historyListEntryIndex(request.anchorDate, entries)
            entryIndex?.let { HISTORY_LIST_CONTENT_START_INDEX + (if (unconfirmedCount > 0) 1 else 0) + it }
        }
        targetIndex?.let {
            listState.scrollToItem(targetIndex)
        }

        pendingScrollRequest = null
        onOpenRequestConsumed()
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MZTokens.ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "history-hero") {
            MZAppPanel {
                MZSectionIntro(
                    eyebrow = stringResource(R.string.history_title),
                    title = stringResource(R.string.history_hero_title),
                    supportingText = stringResource(R.string.history_hero_support)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MZStatusChip(
                        text = stringResource(R.string.history_entries_count, entries.size),
                        color = MaterialTheme.colorScheme.primary
                    )
                    MZStatusChip(
                        text = stringResource(R.string.history_workdays_count, workEntryCount),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    MZStatusChip(
                        text = pluralStringResource(
                            R.plurals.overview_kpi_unconfirmed_count,
                            unconfirmedCount,
                            unconfirmedCount
                        ),
                        color = if (unconfirmedCount > 0) {
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
                    .background(MaterialTheme.colorScheme.background.copy(alpha = MZTokens.AlphaOverlay))
                    .padding(bottom = 8.dp)
            ) {
                HistoryMiniFilterBar(
                    showCalendar = showCalendar,
                    showMonths = showMonths,
                    canToggleMonths = months.isNotEmpty(),
                    onShowCalendarChange = { showCalendar = it },
                    onShowMonthsChange = { showMonths = it },
                    onPickDate = { showDatePicker = true },
                    onOpenBatchEdit = onOpenBatchEdit
                )
            }
        }

        if (unconfirmedCount > 0) {
            item(key = "history-open-days") {
                MZInlineNotice(
                    title = stringResource(R.string.overview_action_required_title),
                    message = pluralStringResource(
                        R.plurals.overview_kpi_unconfirmed_count,
                        unconfirmedCount,
                        unconfirmedCount
                    ),
                    type = StatusType.WARNING
                )
            }
        }

        when {
            showCalendar -> {
                item(key = "calendar") {
                    MZAppPanel {
                        CalendarView(
                            month = selectedMonth,
                            entriesByDate = entriesByDate,
                            onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                            onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
                            onDayClick = openEntryForDate
                        )
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

            !showMonths -> {
                items(
                    items = entries,
                    key = { entry -> entry.date }
                ) { entry ->
                    HistoryEntryItem(
                        entry = entry,
                        travelLegs = travelLegsByDate[entry.date].orEmpty(),
                        onEntryClick = openEntryForDate
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

private fun historyListEntryIndex(anchorDate: LocalDate, entries: List<WorkEntry>): Int? {
    if (entries.isEmpty()) return null
    val exactIndex = entries.indexOfFirst { it.date == anchorDate }
    return if (exactIndex >= 0) exactIndex
    else entries.indexOfFirst { !it.date.isAfter(anchorDate) }.takeIf { it >= 0 } ?: 0
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
    showMonths: Boolean,
    canToggleMonths: Boolean,
    onShowCalendarChange: (Boolean) -> Unit,
    onShowMonthsChange: (Boolean) -> Unit,
    onPickDate: () -> Unit,
    onOpenBatchEdit: () -> Unit = {}
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(MZTokens.RadiusCard),
        border = BorderStroke(
            MZTokens.PanelBorderWidth,
            MaterialTheme.colorScheme.outline.copy(alpha = MZTokens.BorderAlphaSubtle)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MZSegmentedControl(
                options = listOf(
                    MZSegmentedOption(false, stringResource(R.string.history_summary_list)),
                    MZSegmentedOption(true, stringResource(R.string.history_summary_calendar))
                ),
                selectedValue = showCalendar,
                onValueSelected = onShowCalendarChange,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPickDate) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.history_action_pick_date)
                )
            }
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_batch_edit)
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    if (!showCalendar && canToggleMonths) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (showMonths) {
                                        stringResource(R.string.history_summary_weeks)
                                    } else {
                                        stringResource(R.string.history_summary_months)
                                    }
                                )
                            },
                            onClick = {
                                onShowMonthsChange(!showMonths)
                                showOverflowMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cd_batch_edit)) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onOpenBatchEdit()
                        }
                    )
                }
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
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MZTokens.AlphaDisabled)
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
    var applyDayLocation by remember { mutableStateOf(false) }
    var dayLocation by remember { mutableStateOf("") }
    var applyNote by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    val validRange = !startDate.isAfter(endDate)
    val hasSelection = applyDayType || applyDefaults || applyDayLocation || applyNote
    val hasRequiredInputs = !applyDayLocation || dayLocation.isNotBlank()
    val canApply = validRange && hasSelection && hasRequiredInputs && !isSubmitting

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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                                            DayType.SCHULUNG -> stringResource(R.string.day_type_schulung)
                                            DayType.LEHRGANG -> stringResource(R.string.day_type_lehrgang)
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
                        checked = applyDayLocation,
                        onCheckedChange = { applyDayLocation = it }
                    )
                    Text(stringResource(R.string.history_batch_set_location))
                }

                if (applyDayLocation) {
                    OutlinedTextField(
                        value = dayLocation,
                        onValueChange = { if (it.length <= 100) dayLocation = it },
                        label = { Text(stringResource(R.string.day_location_label)) },
                        placeholder = { Text(stringResource(R.string.edit_placeholder_work_location)) },
                        colors = mzOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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
                        colors = mzOutlinedTextFieldColors(),
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

                if (applyDayLocation && dayLocation.isBlank()) {
                    Text(
                        text = stringResource(R.string.history_batch_location_required),
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
                            dayLocationLabel = dayLocation,
                            applyDayLocation = applyDayLocation,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = MZTokens.AlphaOverlay))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.history_week_header, week.week, "", "").substringBefore("·").trim(),
                style = MaterialTheme.typography.titleSmall,
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
        Text(
            text = stringResource(R.string.history_hours_decimal, week.totalHours),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        week.yearText.takeIf { it.isNotEmpty() }?.let { year ->
            Text(
                text = year,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MonthGroupHeader(
    month: MonthGroup
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = MZTokens.AlphaOverlay))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = month.displayText,
                style = MaterialTheme.typography.titleSmall,
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
        Text(
            text = stringResource(R.string.history_hours_decimal, month.totalHours),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        month.yearText.takeIf { it.isNotEmpty() }?.let { year ->
            Text(
                text = year,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val isPending = EntryStatusResolver.isPendingWorkDay(entry, travelLegs)
    val statusChipInfo: Pair<String, Color> = when {
        isPending -> stringResource(R.string.today_unconfirmed) to MaterialTheme.colorScheme.error
        else -> stringResource(R.string.today_confirmed) to MaterialTheme.colorScheme.primary
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MZTokens.RadiusCard))
            .clickable { onEntryClick(entry.date) },
        shape = RoundedCornerShape(MZTokens.RadiusCard),
        color = if (isPending) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            MZTokens.PanelBorderWidth,
            if (isPending) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = MZTokens.BorderAlphaSubtle)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = MZTokens.AlphaSecondary),
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

            Text(
                text = locationLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MZStatusChip(
                    text = historyDayTypeLabel(entry.dayType),
                    color = MaterialTheme.colorScheme.primary
                )

                MZStatusChip(
                    text = statusChipInfo.first,
                    color = statusChipInfo.second
                )

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

data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean
)

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

@Composable
private fun historyDayTypeLabel(dayType: DayType): String = when (dayType) {
    DayType.WORK -> stringResource(R.string.edit_day_type_workday)
    DayType.OFF -> stringResource(R.string.history_day_type_off)
    DayType.COMP_TIME -> stringResource(R.string.day_type_comp_time)
    DayType.SCHULUNG -> stringResource(R.string.day_type_schulung)
    DayType.LEHRGANG -> stringResource(R.string.day_type_lehrgang)
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
