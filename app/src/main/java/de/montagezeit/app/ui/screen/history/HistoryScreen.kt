package de.montagezeit.app.ui.screen.history

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.export.CsvExporter
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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var isBatchEditing by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verlauf") },
                actions = {
                    IconButton(
                        onClick = { onOpenEditSheet(LocalDate.now()) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Heute bearbeiten"
                        )
                    }
                    IconButton(
                        onClick = { showBatchEditDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Mehrfach bearbeiten"
                        )
                    }
                    IconButton(
                        onClick = {
                            isExporting = true
                            viewModel.exportToCsv { uri ->
                                isExporting = false
                                if (uri != null) {
                                    handleShareExport(context, uri)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Export fehlgeschlagen",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = !isExporting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export teilen"
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
                        message = (uiState as HistoryUiState.Error).message,
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
                        Toast.makeText(context, "Bereich aktualisiert", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Bereichs-Update fehlgeschlagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

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

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Datum wählen")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            @OptIn(ExperimentalMaterial3Api::class)
            FilterChip(
                selected = !showCalendar,
                onClick = { showCalendar = false },
                label = { Text("Liste") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            @OptIn(ExperimentalMaterial3Api::class)
            FilterChip(
                selected = showCalendar,
                onClick = { showCalendar = true },
                label = { Text("Kalender") }
            )
        }

        if (showCalendar) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                @OptIn(ExperimentalMaterial3Api::class)
                FilterChip(
                    selected = calendarMode == CalendarMode.WEEK,
                    onClick = { calendarMode = CalendarMode.WEEK },
                    label = { Text("Woche") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                @OptIn(ExperimentalMaterial3Api::class)
                FilterChip(
                    selected = calendarMode == CalendarMode.MONTH,
                    onClick = { calendarMode = CalendarMode.MONTH },
                    label = { Text("Monat") }
                )
            }
        }

        if (!showCalendar && months.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                @OptIn(ExperimentalMaterial3Api::class)
                FilterChip(
                    selected = !showMonths,
                    onClick = { showMonths = false },
                    label = { Text("Wochen") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                @OptIn(ExperimentalMaterial3Api::class)
                FilterChip(
                    selected = showMonths,
                    onClick = { showMonths = true },
                    label = { Text("Monate") }
                )
            }
        }

        when {
            showCalendar -> {
                if (calendarMode == CalendarMode.MONTH) {
                    CalendarView(
                        month = selectedMonth,
                        entriesByDate = entriesByDate,
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
                        entriesByDate = entriesByDate,
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
            weeks.isEmpty() && months.isEmpty() -> {
                EmptyContent(
                    modifier = Modifier.fillMaxWidth(),
                    onAddPastDay = { showDatePicker = true },
                    showAddButton = false
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
                        items(months) { month ->
                            MonthGroupCard(
                                month = month,
                                onEntryClick = onEntryClick
                            )
                        }
                    } else {
                        items(weeks) { week ->
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
}

@Composable
fun CalendarView(
    month: YearMonth,
    entriesByDate: Map<LocalDate, WorkEntry>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val days = remember(month) { buildCalendarDays(month) }
    val weeks = remember(days) { days.chunked(7) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Vorheriger Monat"
                )
            }
            Text(
                text = formatMonth(month),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Naechster Monat"
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
    val weekFields = WeekFields.of(Locale.GERMAN)
    val weekStart = selectedDate.with(weekFields.dayOfWeek(), 1)
    val weekEnd = selectedDate.with(weekFields.dayOfWeek(), 7)
    val weekNumber = selectedDate.get(weekFields.weekOfWeekBasedYear())
    val days = remember(weekStart) {
        (0..6).map { offset ->
            CalendarDay(weekStart.plusDays(offset.toLong()), true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Vorherige Woche"
                )
            }
            Text(
                text = "KW $weekNumber · ${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNextWeek) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Naechste Woche"
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
                    onClick = { onDayClick(day.date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun WeekdayHeader() {
    val labels = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = day.date == LocalDate.now()
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
                    text = "Frei",
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
        title = { Text("Bereich bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Zeitraum", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatShortDate(startDate))
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatShortDate(endDate))
                    }
                }

                if (!validRange) {
                    Text(
                        text = "Das Enddatum muss nach dem Startdatum liegen.",
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
                    Text("Tagtyp setzen")
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
                                            DayType.WORK -> "Arbeitstag"
                                            DayType.OFF -> "Frei"
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
                    Text("Standardzeiten setzen")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = applyNote,
                        onCheckedChange = { applyNote = it }
                    )
                    Text("Notiz setzen")
                }

                if (applyNote) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Notiz (leer = loeschen)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!hasSelection) {
                    Text(
                        text = "Bitte mindestens eine Aktion waehlen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
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
                Text("Anwenden")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Abbrechen")
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
    showAddButton: Boolean = true
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
            text = "Keine Einträge vorhanden",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check-in-Einträge erscheinen hier",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (showAddButton) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onAddPastDay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Vergangenen Tag hinzufügen")
            }
        }
    }
}

@Composable
fun WeekGroupCard(
    week: WeekGroup,
    onEntryClick: (java.time.LocalDate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Week Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = week.displayText,
                    style = MaterialTheme.typography.titleLarge
                )
                week.yearText.takeIf { it.isNotEmpty() }?.let { year ->
                    Text(
                        text = year,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Statistics Section
            if (week.entries.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total and average hours
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gesamt:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "%.1f Std.".format(week.totalHours),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (week.workDaysCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Durchschnitt:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Ø %.1f Std./Tag".format(week.averageHoursPerDay),
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
                                text = "Tage:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${week.workDaysCount} Arbeit${if (week.offDaysCount > 0) ", ${week.offDaysCount} frei" else ""}",
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
                                    text = "${week.entriesNeedingReview} ${if (week.entriesNeedingReview == 1) "Eintrag" else "Einträge"} benötigt Überprüfung",
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
                                    text = "${week.daysOutsideLeipzig} ${if (week.daysOutsideLeipzig == 1) "Tag" else "Tage"} außerhalb Leipzig",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            // Entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                week.entries.forEach { entry ->
                    HistoryEntryItem(
                        entry = entry,
                        onClick = { onEntryClick(entry.date) },
                        onQuickEdit = { onEntryClick(entry.date) } // Öffnet Quick-Edit Dialog
                    )
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
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month.displayText,
                    style = MaterialTheme.typography.titleLarge
                )
                month.yearText.takeIf { it.isNotEmpty() }?.let { year ->
                    Text(
                        text = year,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Statistics Section
            if (month.entries.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total and average hours
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gesamt:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "%.1f Std.".format(month.totalHours),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (month.workDaysCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Durchschnitt:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Ø %.1f Std./Tag".format(month.averageHoursPerDay),
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
                                text = "Tage:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${month.workDaysCount} Arbeit${if (month.offDaysCount > 0) ", ${month.offDaysCount} frei" else ""}",
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
                                    text = "${month.entriesNeedingReview} ${if (month.entriesNeedingReview == 1) "Eintrag" else "Einträge"} benötigt Überprüfung",
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
                                    text = "${month.daysOutsideLeipzig} ${if (month.daysOutsideLeipzig == 1) "Tag" else "Tage"} außerhalb Leipzig",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            // Entries (show first 5, then "X weitere Tage" if more)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val entriesToShow = month.entries.take(5)
                val remainingCount = month.entries.size - entriesToShow.size
                
                entriesToShow.forEach { entry ->
                    HistoryEntryItem(
                        entry = entry,
                        onClick = { onEntryClick(entry.date) },
                        onQuickEdit = { onEntryClick(entry.date) } // Öffnet Quick-Edit Dialog
                    )
                }
                
                if (remainingCount > 0) {
                    Text(
                        text = "$remainingCount weitere ${if (remainingCount == 1) "Tag" else "Tage"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryEntryItem(
    entry: WorkEntry,
    onClick: () -> Unit,
    onQuickEdit: () -> Unit = onClick
) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Date, DayType, Location info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                }
                
                // Location Status Info
                LocationSummary(entry = entry)
                TravelSummaryRow(entry = entry)
                
                // Work Hours (only for work days)
                if (entry.dayType == DayType.WORK) {
                    val workHours = calculateWorkHours(entry)
                    Text(
                        text = formatWorkHours(workHours),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Right side: Warning icon, quick edit button, and edit icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (entry.needsReview) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Überprüfung erforderlich",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Quick Edit Button
                IconButton(
                    onClick = { onQuickEdit() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Schnell bearbeiten",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
        Text(
            text = "Kein Check-in",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        return
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
        val labels = listOfNotNull(
            entry.morningLocationLabel?.takeIf { it != "Leipzig" },
            entry.eveningLocationLabel?.takeIf { it != "Leipzig" }
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
            "${entry.travelLabelStart} → ${entry.travelLabelEnd}"
        !entry.travelLabelStart.isNullOrBlank() -> "Von ${entry.travelLabelStart}"
        !entry.travelLabelEnd.isNullOrBlank() -> "Nach ${entry.travelLabelEnd}"
        else -> null
    }

    if (startAt == null && arriveAt == null && labelText == null) return

    val timeText = when {
        startAt != null && arriveAt != null ->
            "${formatTime(startAt)} – ${formatTime(arriveAt)}"
        startAt != null -> "Start: ${formatTime(startAt)}"
        arriveAt != null -> "Ankunft: ${formatTime(arriveAt)}"
        else -> null
    }
    val durationText = calculateTravelDuration(startAt, arriveAt)?.let { formatDuration(it) }
    val summaryText = listOfNotNull(timeText, durationText?.let { "Fahrzeit $it" })
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
        Button(onClick = onRetry) {
            Text("Wiederholen")
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
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
    return month.atDay(1).format(formatter)
}

private fun formatEntryDate(date: java.time.LocalDate): String {
    return date.format(
        DateTimeFormatter.ofPattern("E, dd.MM.", Locale.GERMAN)
    )
}

private fun formatShortDate(date: java.time.LocalDate): String {
    return date.format(
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
    )
}

private fun formatTime(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val time = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun calculateTravelDuration(startAt: Long?, arriveAt: Long?): Duration? {
    if (startAt == null || arriveAt == null) return null
    var duration = Duration.between(
        java.time.Instant.ofEpochMilli(startAt),
        java.time.Instant.ofEpochMilli(arriveAt)
    )
    if (duration.isNegative) {
        duration = duration.plusDays(1)
    }
    return duration
}

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun calculateWorkHours(entry: WorkEntry): Double {
    if (entry.dayType != DayType.WORK) return 0.0
    val startMinutes = entry.workStart.hour * 60 + entry.workStart.minute
    val endMinutes = entry.workEnd.hour * 60 + entry.workEnd.minute
    val workMinutes = endMinutes - startMinutes - entry.breakMinutes
    return workMinutes / 60.0
}

private fun formatWorkHours(hours: Double): String {
    val totalMinutes = (hours * 60).toInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    
    return if (m == 0) {
        "${h} Std."
    } else {
        "${h}h ${m}min"
    }
}

private fun handleShareExport(context: Context, fileUri: android.net.Uri) {
    val csvExporter = CsvExporter(context)
    val shareIntent = csvExporter.createShareIntent(fileUri)
    context.startActivity(shareIntent)
}

@Composable
fun DatePickerDialog(
    initialDate: java.time.LocalDate,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(initialDate) }
    
    LaunchedEffect(Unit) {
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                onDateSelected(selectedDate)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )
        datePickerDialog.setOnDismissListener {
            onDismiss()
        }
        datePickerDialog.show()
    }
}
