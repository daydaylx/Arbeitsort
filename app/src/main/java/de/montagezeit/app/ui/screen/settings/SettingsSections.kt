package de.montagezeit.app.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.*
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val WORKDAYS_PER_WEEK = 5.0
private const val APPROX_WORKDAYS_PER_MONTH = 20.0
private const val WEEKLY_TOLERANCE_HOURS = 0.1
private const val MONTHLY_TOLERANCE_HOURS = 0.1

@Composable
internal fun WorkTimesSection(
    workStart: LocalTime,
    workEnd: LocalTime,
    breakMinutes: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onUpdateWorkStart: (LocalTime) -> Unit,
    onUpdateWorkEnd: (LocalTime) -> Unit,
    onUpdateBreakMinutes: (Int) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var breakMinutesDraft by rememberSaveable(breakMinutes) {
        mutableFloatStateOf(breakMinutes.toFloat())
    }

    fun commitBreakMinutes() {
        val snappedMinutes = ((breakMinutesDraft / 5f).roundToInt() * 5).coerceIn(0, 180)
        breakMinutesDraft = snappedMinutes.toFloat()
        if (snappedMinutes != breakMinutes) {
            onUpdateBreakMinutes(snappedMinutes)
        }
    }

    CollapsibleSettingsCard(
        title = stringResource(R.string.settings_work_times),
        summary = stringResource(
            R.string.settings_work_times_summary,
            Formatters.formatTime(workStart),
            Formatters.formatTime(workEnd),
            breakMinutes
        ),
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_work_start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecondaryActionButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Formatters.formatTime(workStart))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_work_end),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecondaryActionButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Formatters.formatTime(workEnd))
                }
            }
        }

        Text(
            text = stringResource(
                R.string.settings_break_minutes_value,
                stringResource(R.string.label_break),
                breakMinutesDraft.roundToInt()
            ),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = breakMinutesDraft,
            onValueChange = { breakMinutesDraft = it },
            onValueChangeFinished = { commitBreakMinutes() },
            valueRange = 0f..180f,
            steps = 35,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.work_times_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showStartPicker) {
        TimePickerDialog(
            initialTime = workStart,
            onTimeSelected = {
                onUpdateWorkStart(it)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            initialTime = workEnd,
            onTimeSelected = {
                onUpdateWorkEnd(it)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
internal fun ReminderSettingsSection(
    morningReminderEnabled: Boolean,
    morningWindowStart: LocalTime,
    morningWindowEnd: LocalTime,
    eveningReminderEnabled: Boolean,
    eveningWindowStart: LocalTime,
    eveningWindowEnd: LocalTime,
    fallbackEnabled: Boolean,
    fallbackTime: LocalTime,
    dailyReminderEnabled: Boolean,
    dailyReminderTime: LocalTime,
    onUpdateMorningEnabled: (Boolean) -> Unit,
    onUpdateMorningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateEveningEnabled: (Boolean) -> Unit,
    onUpdateEveningWindow: (Int, Int, Int, Int) -> Unit,
    onUpdateFallbackEnabled: (Boolean) -> Unit,
    onUpdateFallbackTime: (LocalTime) -> Unit,
    onUpdateDailyEnabled: (Boolean) -> Unit,
    onUpdateDailyTime: (LocalTime) -> Unit,
    reminderError: de.montagezeit.app.ui.util.UiText?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    var showFallbackPicker by remember { mutableStateOf(false) }
    var showDailyPicker by remember { mutableStateOf(false) }
    val activeCount = listOf(
        morningReminderEnabled,
        eveningReminderEnabled,
        fallbackEnabled,
        dailyReminderEnabled
    ).count { it }

    CollapsibleSettingsCard(
        title = stringResource(R.string.settings_reminders),
        summary = stringResource(R.string.settings_reminders_summary, activeCount),
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        if (reminderError != null) {
            MZStatusBadge(
                text = reminderError.asString(LocalContext.current),
                type = StatusType.ERROR,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
        }

        SettingsToggleRow(
            title = stringResource(R.string.reminder_morning),
            checked = morningReminderEnabled,
            onCheckedChange = onUpdateMorningEnabled
        )

        AnimatedVisibility(visible = morningReminderEnabled) {
            TimeRangePicker(
                label = stringResource(R.string.label_time_range),
                startTime = morningWindowStart,
                endTime = morningWindowEnd,
                onTimeRangeChanged = { start, end ->
                    onUpdateMorningWindow(start.hour, start.minute, end.hour, end.minute)
                },
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        HorizontalDivider()

        SettingsToggleRow(
            title = stringResource(R.string.reminder_evening),
            checked = eveningReminderEnabled,
            onCheckedChange = onUpdateEveningEnabled
        )

        AnimatedVisibility(visible = eveningReminderEnabled) {
            TimeRangePicker(
                label = stringResource(R.string.label_time_range),
                startTime = eveningWindowStart,
                endTime = eveningWindowEnd,
                onTimeRangeChanged = { start, end ->
                    onUpdateEveningWindow(start.hour, start.minute, end.hour, end.minute)
                },
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        HorizontalDivider()

        SettingsToggleRow(
            title = stringResource(R.string.reminder_fallback),
            checked = fallbackEnabled,
            onCheckedChange = onUpdateFallbackEnabled
        )
        SettingsTimeButtonRow(
            label = stringResource(R.string.label_time),
            time = fallbackTime,
            enabled = fallbackEnabled,
            onClick = { showFallbackPicker = true },
            modifier = Modifier.padding(start = 12.dp)
        )

        HorizontalDivider()

        SettingsToggleRow(
            title = stringResource(R.string.reminder_daily),
            supportingText = stringResource(R.string.reminder_daily_description),
            checked = dailyReminderEnabled,
            onCheckedChange = onUpdateDailyEnabled
        )
        SettingsTimeButtonRow(
            label = stringResource(R.string.label_time),
            time = dailyReminderTime,
            enabled = dailyReminderEnabled,
            onClick = { showDailyPicker = true },
            modifier = Modifier.padding(start = 12.dp)
        )
    }

    if (showFallbackPicker) {
        TimePickerDialog(
            initialTime = fallbackTime,
            onTimeSelected = {
                onUpdateFallbackTime(it)
                showFallbackPicker = false
            },
            onDismiss = { showFallbackPicker = false }
        )
    }

    if (showDailyPicker) {
        TimePickerDialog(
            initialTime = dailyReminderTime,
            onTimeSelected = {
                onUpdateDailyTime(it)
                showDailyPicker = false
            },
            onDismiss = { showDailyPicker = false }
        )
    }
}

@Composable
internal fun NonWorkingDaysSection(
    autoOffWeekends: Boolean,
    autoOffHolidays: Boolean,
    holidayDates: Set<LocalDate>,
    onUpdateAutoOffWeekends: (Boolean) -> Unit,
    onUpdateAutoOffHolidays: (Boolean) -> Unit,
    onAddHolidayDate: (LocalDate) -> Unit,
    onRemoveHolidayDate: (LocalDate) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    var showHolidayPicker by remember { mutableStateOf(false) }
    val sortedHolidays = remember(holidayDates) { holidayDates.sorted() }
    val nonWorkingSummary = buildString {
        append(
            if (autoOffWeekends) {
                stringResource(R.string.settings_summary_weekends_on)
            } else {
                stringResource(R.string.settings_summary_weekends_off)
            }
        )
        append(" · ")
        if (autoOffHolidays) {
            append(
                pluralStringResource(
                    R.plurals.settings_summary_holidays_count,
                    holidayDates.size,
                    holidayDates.size
                )
            )
        } else {
            append(stringResource(R.string.settings_summary_holidays_off))
        }
    }

    CollapsibleSettingsCard(
        title = stringResource(R.string.settings_non_working_days),
        summary = nonWorkingSummary,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.label_auto_off_weekends),
            supportingText = stringResource(R.string.auto_off_weekends_description),
            checked = autoOffWeekends,
            onCheckedChange = onUpdateAutoOffWeekends
        )

        HorizontalDivider()

        SettingsToggleRow(
            title = stringResource(R.string.label_auto_off_holidays),
            supportingText = stringResource(R.string.auto_off_holidays_description),
            checked = autoOffHolidays,
            onCheckedChange = onUpdateAutoOffHolidays
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_holidays),
                style = MaterialTheme.typography.bodyMedium
            )
            SecondaryActionButton(
                onClick = { showHolidayPicker = true },
                enabled = autoOffHolidays
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(stringResource(R.string.action_add))
            }
        }

        if (sortedHolidays.isEmpty()) {
            Text(
                text = stringResource(R.string.no_holidays_added),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sortedHolidays.forEach { date ->
                    HolidayRow(
                        date = date,
                        onRemove = { onRemoveHolidayDate(date) }
                    )
                }
            }
        }
    }

    if (showHolidayPicker) {
        DatePickerDialog(
            initialDate = LocalDate.now(),
            onDateSelected = { date ->
                onAddHolidayDate(date)
                showHolidayPicker = false
            },
            onDismiss = { showHolidayPicker = false }
        )
    }
}

@Composable
private fun HolidayRow(
    date: LocalDate,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Formatters.formatDate(date),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_remove_holiday),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun ExportSection(
    uiState: SettingsUiState,
    onExportPdfCurrentMonth: () -> Unit,
    onExportCsvCurrentMonth: () -> Unit,
    onExportPdfLast30Days: () -> Unit,
    onExportCsvLast30Days: () -> Unit,
    onExportPdfCustomRange: (LocalDate, LocalDate) -> Unit,
    onExportCsvCustomRange: (LocalDate, LocalDate) -> Unit,
    onOpenExportPreview: (LocalDate, LocalDate) -> Unit,
    pdfEmployeeName: String?,
    pdfCompany: String?,
    pdfProject: String?,
    pdfPersonnelNumber: String?,
    onUpdatePdfSettings: (String?, String?, String?, String?) -> Unit,
    onResetState: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    var showPdfSettingsDialog by remember { mutableStateOf(false) }
    var showExportRangeDialog by remember { mutableStateOf(false) }

    val employeeName = pdfEmployeeName.orEmpty()
    val company = pdfCompany.orEmpty()
    val project = pdfProject.orEmpty()
    val personnelNumber = pdfPersonnelNumber.orEmpty()
    val exportSummary = if (employeeName.isBlank()) {
        stringResource(R.string.settings_export_name_missing)
    } else {
        employeeName
    }

    CollapsibleSettingsCard(
        title = stringResource(R.string.settings_export),
        summary = exportSummary,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        action = {
            IconButton(onClick = { showPdfSettingsDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_pdf_settings)
                )
            }
        }
    ) {
        if (employeeName.isBlank()) {
            MZStatusBadge(
                text = stringResource(R.string.warning_name_missing),
                type = StatusType.ERROR,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickExportRow(
                label = stringResource(R.string.export_current_month),
                enabled = employeeName.isNotBlank(),
                onExportPdf = onExportPdfCurrentMonth,
                onExportCsv = onExportCsvCurrentMonth
            )

            QuickExportRow(
                label = stringResource(R.string.export_30_days),
                enabled = employeeName.isNotBlank(),
                onExportPdf = onExportPdfLast30Days,
                onExportCsv = onExportCsvLast30Days
            )

            SecondaryActionButton(
                onClick = { showExportRangeDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = employeeName.isNotBlank()
            ) {
                Text(stringResource(R.string.export_custom))
            }
        }

        when (uiState) {
            is SettingsUiState.Initial -> Unit
            is SettingsUiState.Exporting -> {
                MZLoadingState(message = stringResource(R.string.exporting))
            }

            is SettingsUiState.ExportSuccess -> {
                ExportSuccessCard(
                    format = uiState.format,
                    onCopy = {
                        copyExportUriToClipboard(context, uiState.fileUri)
                        onResetState()
                    },
                    onShare = {
                        shareExport(context, uiState.fileUri, uiState.format)
                        onResetState()
                    },
                    onDismiss = onResetState
                )
            }

            is SettingsUiState.ExportError -> {
                MZStatusBadge(
                    text = uiState.message.asString(context),
                    type = StatusType.ERROR,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is SettingsUiState.ReminderError -> Unit
        }
    }

    if (showPdfSettingsDialog) {
        PdfSettingsDialog(
            initialEmployeeName = employeeName,
            initialCompany = company.ifBlank { null },
            initialProject = project.ifBlank { null },
            initialPersonnelNumber = personnelNumber.ifBlank { null },
            onSave = { name, comp, proj, pers ->
                onUpdatePdfSettings(name, comp, proj, pers)
                showPdfSettingsDialog = false
            },
            onDismiss = { showPdfSettingsDialog = false }
        )
    }

    if (showExportRangeDialog) {
        ExportRangeDialog(
            onPdfRangeSelected = { start, end ->
                onExportPdfCustomRange(start, end)
                showExportRangeDialog = false
            },
            onCsvRangeSelected = { start, end ->
                onExportCsvCustomRange(start, end)
                showExportRangeDialog = false
            },
            onPreviewRangeSelected = { start, end ->
                onOpenExportPreview(start, end)
                showExportRangeDialog = false
            },
            onDismiss = { showExportRangeDialog = false }
        )
    }
}

@Composable
private fun QuickExportRow(
    label: String,
    enabled: Boolean,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryActionButton(
                onClick = onExportPdf,
                modifier = Modifier.weight(1f),
                enabled = enabled
            ) {
                Text(stringResource(R.string.export_format_pdf))
            }
            SecondaryActionButton(
                onClick = onExportCsv,
                modifier = Modifier.weight(1f),
                enabled = enabled
            ) {
                Text(stringResource(R.string.export_format_csv))
            }
        }
    }
}

@Composable
private fun ExportSuccessCard(
    format: ExportFormat,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    MZStatusCard(
        title = stringResource(R.string.export_success_title),
        status = StatusType.SUCCESS
    ) {
        val formatLabel = stringResource(
            when (format) {
                ExportFormat.PDF -> R.string.export_format_pdf
                ExportFormat.CSV -> R.string.export_format_csv
            }
        )
        Text(
            text = stringResource(R.string.export_success_format, formatLabel),
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryActionButton(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                content = { Text(stringResource(R.string.action_share)) }
            )
            SecondaryActionButton(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                content = { Text(stringResource(R.string.action_copy)) }
            )
            TertiaryActionButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}

private fun copyExportUriToClipboard(context: Context, fileUri: Uri) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(
        context.getString(R.string.export_clipboard_label),
        fileUri.toString()
    )
    clipboard.setPrimaryClip(clip)
    Toast.makeText(
        context,
        context.getString(R.string.export_clipboard_copied),
        Toast.LENGTH_SHORT
    ).show()
}

private fun shareExport(context: Context, fileUri: Uri, format: ExportFormat) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = when (format) {
            ExportFormat.PDF -> "application/pdf"
            ExportFormat.CSV -> "text/csv"
        }
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_share_subject))
    }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.share_export))
    )
}

@Composable
internal fun OvertimeTargetsSection(
    dailyTargetHours: Double,
    weeklyTargetHours: Double,
    monthlyTargetHours: Double,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onUpdateDailyTarget: (Double) -> Unit,
    onUpdateWeeklyTarget: (Double) -> Unit,
    onUpdateMonthlyTarget: (Double) -> Unit
) {
    CollapsibleSettingsCard(
        title = stringResource(R.string.settings_overtime_targets),
        summary = stringResource(
            R.string.settings_overtime_targets_summary,
            dailyTargetHours,
            weeklyTargetHours,
            monthlyTargetHours
        ),
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        val expectedWeekly = dailyTargetHours * WORKDAYS_PER_WEEK
        val expectedMonthly = dailyTargetHours * APPROX_WORKDAYS_PER_MONTH
        val isInconsistent = (weeklyTargetHours - expectedWeekly).absoluteValue > WEEKLY_TOLERANCE_HOURS ||
                            (monthlyTargetHours - expectedMonthly).absoluteValue > MONTHLY_TOLERANCE_HOURS

        if (isInconsistent) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.overtime_targets_inconsistent_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.overtime_targets_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        NumericInputRow(
            label = stringResource(R.string.label_daily_target),
            value = dailyTargetHours,
            unit = "Std.",
            onValueChange = onUpdateDailyTarget,
            minValue = 0.5,
            maxValue = 24.0
        )

        Spacer(modifier = Modifier.height(12.dp))

        NumericInputRow(
            label = stringResource(R.string.label_weekly_target),
            value = weeklyTargetHours,
            unit = "Std.",
            onValueChange = onUpdateWeeklyTarget,
            minValue = 1.0,
            maxValue = 168.0
        )

        Spacer(modifier = Modifier.height(12.dp))

        NumericInputRow(
            label = stringResource(R.string.label_monthly_target),
            value = monthlyTargetHours,
            unit = "Std.",
            onValueChange = onUpdateMonthlyTarget,
            minValue = 1.0,
            maxValue = 744.0
        )
    }
}
