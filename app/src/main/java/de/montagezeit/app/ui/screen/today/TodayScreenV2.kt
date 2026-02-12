package de.montagezeit.app.ui.screen.today

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.ui.components.*
import de.montagezeit.app.ui.util.LocationPermissionHelper
import de.montagezeit.app.ui.util.getReviewReason
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Verbesserter TodayScreen mit besserer Accessibility und visuellem Design
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TodayScreenV2(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenEditSheet: (LocalDate) -> Unit,
    onOpenWeekView: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayEntry by viewModel.todayEntry.collectAsStateWithLifecycle()
    val weekStats by viewModel.weekStats.collectAsStateWithLifecycle()
    val monthStats by viewModel.monthStats.collectAsStateWithLifecycle()
    val showReviewSheet by viewModel.showReviewSheet.collectAsStateWithLifecycle()
    val reviewScope by viewModel.reviewScope.collectAsStateWithLifecycle()
    val loadingActions by viewModel.loadingActions.collectAsStateWithLifecycle()
    
    var hasLocationPermission by remember {
        mutableStateOf(LocationPermissionHelper.hasAnyLocationPermission(context))
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val isGranted = LocationPermissionHelper.isPermissionGranted(result)
        hasLocationPermission = isGranted
        if (isGranted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (todayEntry?.morningCapturedAt == null) {
                viewModel.onMorningCheckIn()
            } else if (todayEntry?.eveningCapturedAt == null) {
                viewModel.onEveningCheckIn()
            }
        }
    }

    val currentEntry = (uiState as? TodayUiState.Success)?.entry ?: todayEntry
    val needsReview = currentEntry?.needsReview == true
    val reviewReason = getReviewReason(currentEntry)
    var showDayLocationDialog by rememberSaveable { mutableStateOf(false) }
    var dayLocationInput by rememberSaveable { mutableStateOf("") }

    val isMorningLoading = loadingActions.contains(TodayAction.MORNING_CHECK_IN)
    val isEveningLoading = loadingActions.contains(TodayAction.EVENING_CHECK_IN)
    val isConfirmWorkdayLoading = loadingActions.contains(TodayAction.CONFIRM_WORKDAY)
    val isConfirmOffdayLoading = loadingActions.contains(TodayAction.CONFIRM_OFFDAY)
    val isReviewResolving = loadingActions.contains(TodayAction.RESOLVE_REVIEW)

    val onRequestLocationPermissionAction = {
        locationPermissionLauncher.launch(LocationPermissionHelper.locationPermissions)
    }
    val onMorningCheckInAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.onMorningCheckIn()
    }
    val onEveningCheckInAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.onEveningCheckIn()
    }
    val onConfirmWorkDayAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.onConfirmWorkDay()
    }
    val onConfirmOffDayAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.onConfirmOffDay()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.today_title),
                        modifier = Modifier.semantics { heading() }
                    )
                }
            )
        },
        floatingActionButton = {
            if (uiState is TodayUiState.Success &&
                (currentEntry?.confirmedWorkDay == false || currentEntry?.needsReview == true)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenEditSheet(LocalDate.now())
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.semantics { invisibleToUser() }
                        )
                    },
                    text = { Text(stringResource(R.string.action_edit_entry_manual)) }
                )
            }
        },
        bottomBar = {
            StickyTodayActionBarV2(
                entry = currentEntry,
                hasLocationPermission = hasLocationPermission,
                needsReview = needsReview,
                isMorningLoading = isMorningLoading,
                isEveningLoading = isEveningLoading,
                isConfirmWorkdayLoading = isConfirmWorkdayLoading,
                isConfirmOffdayLoading = isConfirmOffdayLoading,
                isReviewResolving = isReviewResolving,
                onRequestLocationPermission = onRequestLocationPermissionAction,
                onMorningCheckIn = onMorningCheckInAction,
                onEveningCheckIn = onEveningCheckInAction,
                onConfirmWorkDay = onConfirmWorkDayAction,
                onConfirmOffDay = onConfirmOffDayAction,
                onOpenReviewSheet = { viewModel.onOpenReviewSheet() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isLoadingLocation = uiState is TodayUiState.LoadingLocation
            val isLoading = uiState is TodayUiState.Loading
            val canShowContent = uiState is TodayUiState.Success ||
                isLoadingLocation ||
                (isLoading && currentEntry != null)

            when (uiState) {
                is TodayUiState.Error -> {
                    MZErrorState(
                        message = (uiState as TodayUiState.Error).message,
                        onRetry = { viewModel.onResetError() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TodayUiState.LocationError -> {
                    val errorState = uiState as TodayUiState.LocationError
                    LocationErrorContentV2(
                        message = errorState.message,
                        canRetry = errorState.canRetry,
                        onRetry = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (todayEntry?.morningCapturedAt == null) {
                                viewModel.onMorningCheckIn()
                            } else {
                                viewModel.onEveningCheckIn()
                            }
                        },
                        onSkipLocation = { viewModel.onSkipLocation() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    if (!canShowContent) {
                        MZLoadingState(
                            message = stringResource(R.string.loading),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        TodayContentV2(
                            entry = currentEntry,
                            weekStats = weekStats,
                            monthStats = monthStats,
                            hasLocationPermission = hasLocationPermission,
                            onRequestLocationPermission = onRequestLocationPermissionAction,
                            onMorningCheckIn = onMorningCheckInAction,
                            onEveningCheckIn = onEveningCheckInAction,
                            onConfirmWorkDay = onConfirmWorkDayAction,
                            onConfirmOffDay = onConfirmOffDayAction,
                            onOpenReviewSheet = { viewModel.onOpenReviewSheet() },
                            onEditDayLocation = {
                                dayLocationInput = currentEntry?.dayLocationLabel.orEmpty()
                                showDayLocationDialog = true
                            },
                            needsReview = needsReview,
                            reviewReason = reviewReason,
                            onEditToday = {
                                viewModel.ensureTodayEntryThen {
                                    onOpenEditSheet(LocalDate.now())
                                }
                            },
                            onOpenWeekView = onOpenWeekView,
                            showLocationLoading = isLoadingLocation,
                            onSkipLocation = { viewModel.onSkipLocation() },
                            isMorningLoading = isMorningLoading,
                            isEveningLoading = isEveningLoading,
                            isConfirmWorkdayLoading = isConfirmWorkdayLoading,
                            isConfirmOffdayLoading = isConfirmOffdayLoading
                        )
                    }
                }
            }
            
            if (showReviewSheet) {
                val currentScope = reviewScope
                if (currentScope != null) {
                    ReviewSheet(
                        isVisible = showReviewSheet,
                        onDismissRequest = { viewModel.onDismissReviewSheet() },
                        scope = currentScope,
                        reviewReason = reviewReason,
                        isResolving = isReviewResolving,
                        onResolve = { label, isLeipzig -> 
                            viewModel.onResolveReview(label, isLeipzig)
                        }
                    )
                }
            }

            if (showDayLocationDialog) {
                DayLocationDialogV2(
                    currentLabel = dayLocationInput,
                    onDismiss = { showDayLocationDialog = false },
                    onConfirm = { label ->
                        viewModel.onUpdateDayLocation(label)
                        showDayLocationDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StickyTodayActionBarV2(
    entry: WorkEntry?,
    hasLocationPermission: Boolean,
    needsReview: Boolean,
    isMorningLoading: Boolean,
    isEveningLoading: Boolean,
    isConfirmWorkdayLoading: Boolean,
    isConfirmOffdayLoading: Boolean,
    isReviewResolving: Boolean,
    onRequestLocationPermission: () -> Unit,
    onMorningCheckIn: () -> Unit,
    onEveningCheckIn: () -> Unit,
    onConfirmWorkDay: () -> Unit,
    onConfirmOffDay: () -> Unit,
    onOpenReviewSheet: () -> Unit
) {
    val morningCompleted = entry?.morningCapturedAt != null
    val eveningCompleted = entry?.eveningCapturedAt != null
    val needsConfirmation = entry?.confirmedWorkDay != true
    val canConfirm = needsConfirmation && (morningCompleted || eveningCompleted)

    if (morningCompleted && eveningCompleted && !needsReview && !canConfirm) {
        return
    }

    val primaryLabel: String
    val primaryLoading: Boolean
    val primaryAction: () -> Unit
    var showOffdayAction = false

    when {
        !morningCompleted -> {
            if (hasLocationPermission) {
                primaryLabel = stringResource(R.string.action_check_in_morning)
                primaryLoading = isMorningLoading
                primaryAction = onMorningCheckIn
            } else {
                primaryLabel = stringResource(R.string.action_allow_location)
                primaryLoading = false
                primaryAction = onRequestLocationPermission
            }
        }

        !eveningCompleted -> {
            if (hasLocationPermission) {
                primaryLabel = stringResource(R.string.action_check_in_evening)
                primaryLoading = isEveningLoading
                primaryAction = onEveningCheckIn
            } else {
                primaryLabel = stringResource(R.string.action_allow_location)
                primaryLoading = false
                primaryAction = onRequestLocationPermission
            }
        }

        needsReview -> {
            primaryLabel = stringResource(R.string.today_needs_review)
            primaryLoading = isReviewResolving
            primaryAction = onOpenReviewSheet
        }

        canConfirm -> {
            primaryLabel = stringResource(R.string.action_confirm_workday)
            primaryLoading = isConfirmWorkdayLoading
            primaryAction = onConfirmWorkDay
            showOffdayAction = true
        }

        else -> return
    }

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showOffdayAction) {
                MZSecondaryButton(
                    onClick = onConfirmOffDay,
                    isLoading = isConfirmOffdayLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_confirm_offday))
                }
            }

            MZPrimaryButton(
                onClick = primaryAction,
                isLoading = primaryLoading,
                modifier = if (showOffdayAction) Modifier.weight(1f) else Modifier.fillMaxWidth()
            ) {
                Text(primaryLabel)
            }
        }
    }
}

@Composable
private fun TodayContentV2(
    entry: WorkEntry?,
    weekStats: WeekStats?,
    monthStats: MonthStats?,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onMorningCheckIn: () -> Unit,
    onEveningCheckIn: () -> Unit,
    onConfirmWorkDay: () -> Unit,
    onConfirmOffDay: () -> Unit,
    onOpenReviewSheet: () -> Unit,
    onEditDayLocation: () -> Unit,
    needsReview: Boolean,
    reviewReason: String,
    onEditToday: () -> Unit,
    onOpenWeekView: () -> Unit,
    showLocationLoading: Boolean,
    onSkipLocation: () -> Unit,
    isMorningLoading: Boolean = false,
    isEveningLoading: Boolean = false,
    isConfirmWorkdayLoading: Boolean = false,
    isConfirmOffdayLoading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = needsReview,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ReviewBannerV2(
                reviewReason = reviewReason,
                onOpenReviewSheet = onOpenReviewSheet
            )
        }

        if (showLocationLoading) {
            LoadingLocationContentV2(onSkipLocation = onSkipLocation)
        }

        StatusCardV2(
            entry = entry,
            onEditToday = onEditToday,
            onEditDayLocation = onEditDayLocation
        )
        
        if (entry != null && entry.dayType == DayType.WORK) {
            WorkHoursCardV2(entry = entry)
        }
        
        if (weekStats != null || monthStats != null) {
            StatisticsDashboardV2(
                weekStats = weekStats,
                monthStats = monthStats,
                onOpenWeekView = onOpenWeekView
            )
        }

        TodayActionsCardV2(
            entry = entry,
            hasLocationPermission = hasLocationPermission,
            onRequestLocationPermission = onRequestLocationPermission,
            onMorningCheckIn = onMorningCheckIn,
            onEveningCheckIn = onEveningCheckIn,
            onConfirmWorkDay = onConfirmWorkDay,
            onConfirmOffDay = onConfirmOffDay,
            isMorningLoading = isMorningLoading,
            isEveningLoading = isEveningLoading,
            isConfirmWorkdayLoading = isConfirmWorkdayLoading,
            isConfirmOffdayLoading = isConfirmOffdayLoading
        )
    }
}

@Composable
private fun ReviewBannerV2(
    reviewReason: String,
    onOpenReviewSheet: () -> Unit
) {
    MZCard(
        modifier = Modifier.clickableWithAccessibility(
            onClick = onOpenReviewSheet,
            contentDescription = stringResource(R.string.today_needs_review)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.today_needs_review),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = reviewReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MZSecondaryButton(
                onClick = onOpenReviewSheet
            ) {
                Text(stringResource(R.string.action_review))
            }
        }
    }
}

@Composable
private fun LoadingLocationContentV2(onSkipLocation: () -> Unit) {
    MZCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.today_location_loading),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MZTertiaryButton(onClick = onSkipLocation) {
                Text(stringResource(R.string.action_save_without_location))
            }
        }
    }
}

@Composable
private fun LocationErrorContentV2(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onSkipLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (canRetry) {
            MZPrimaryButton(
                onClick = onRetry,
                content = { Text(stringResource(R.string.action_retry_location)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        MZSecondaryButton(
            onClick = onSkipLocation,
            content = { Text(stringResource(R.string.action_save_without_location)) }
        )
    }
}

@Composable
private fun StatusCardV2(
    entry: WorkEntry?,
    onEditToday: () -> Unit,
    onEditDayLocation: () -> Unit
) {
    val status = when {
        entry?.needsReview == true -> StatusType.WARNING
        entry?.confirmedWorkDay == true -> StatusType.SUCCESS
        else -> StatusType.INFO
    }

    MZStatusCard(
        title = getCurrentDateString(),
        status = status,
        onClick = onEditToday
    ) {
        entry?.let {
            DayTypeRow(dayType = it.dayType)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (entry != null) {
            MZInfoRow(
                icon = Icons.Default.LocationOn,
                text = entry.dayLocationLabel,
                iconTint = MaterialTheme.colorScheme.primary
            )
            
            TextButton(
                onClick = onEditDayLocation,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.action_change_location))
            }
        } else {
            Text(
                text = stringResource(R.string.today_no_check_in),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (entry?.morningCapturedAt != null || entry?.eveningCapturedAt != null) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            LocationStatusSection(entry = entry)
        }
    }
}

@Composable
private fun DayTypeRow(dayType: DayType) {
    val (icon, text, color) = when (dayType) {
        DayType.WORK -> Triple(
            Icons.Default.Work,
            stringResource(R.string.day_type_work),
            MaterialTheme.colorScheme.primary
        )
        DayType.OFF -> Triple(
            Icons.Default.FreeBreakfast,
            stringResource(R.string.day_type_off),
            MaterialTheme.colorScheme.secondary
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LocationStatusSection(entry: WorkEntry?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entry?.let { e ->
            if (e.morningCapturedAt != null) {
                LocationRowV2(
                    label = stringResource(R.string.today_location_morning),
                    locationStatus = e.morningLocationStatus,
                    locationLabel = e.morningLocationLabel
                )
            }
            if (e.eveningCapturedAt != null) {
                LocationRowV2(
                    label = stringResource(R.string.today_location_evening),
                    locationStatus = e.eveningLocationStatus,
                    locationLabel = e.eveningLocationLabel
                )
            }
        }
    }
}

@Composable
private fun LocationRowV2(
    label: String,
    locationStatus: LocationStatus,
    locationLabel: String?
) {
    val (icon, tint) = when (locationStatus) {
        LocationStatus.OK -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        LocationStatus.LOW_ACCURACY -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        LocationStatus.UNAVAILABLE -> Icons.Default.LocationOff to MaterialTheme.colorScheme.error
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            imageVector = icon,
            contentDescription = when (locationStatus) {
                LocationStatus.OK -> stringResource(R.string.location_status_ok)
                LocationStatus.LOW_ACCURACY -> stringResource(R.string.location_status_low_accuracy)
                LocationStatus.UNAVAILABLE -> stringResource(R.string.location_status_unavailable)
            },
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        locationLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkHoursCardV2(entry: WorkEntry) {
    val (workMinutes, travelMinutes, totalMinutes) = remember(entry) {
        Triple(
            TimeCalculator.calculateWorkMinutes(entry),
            TimeCalculator.calculateTravelMinutes(entry),
            TimeCalculator.calculatePaidTotalMinutes(entry)
        )
    }

    MZCard {
        Column {
            MZSectionHeader(title = stringResource(R.string.work_hours_title))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total_paid_hours),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatMinutes(totalMinutes),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (travelMinutes > 0) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.work_time_breakdown, formatMinutes(workMinutes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.travel_time_breakdown, formatMinutes(travelMinutes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsDashboardV2(
    weekStats: WeekStats?,
    monthStats: MonthStats?,
    onOpenWeekView: () -> Unit
) {
    when {
        weekStats != null -> {
            StatisticsDashboardCardV2(
                label = stringResource(R.string.today_stats_week),
                totalHours = weekStats.totalHours,
                targetHours = weekStats.targetHours,
                workDaysCount = weekStats.workDaysCount,
                progress = weekStats.progress,
                isOverTarget = weekStats.isOverTarget,
                isUnderTarget = weekStats.isUnderTarget,
                onOpenWeekView = onOpenWeekView
            )
        }
        monthStats != null -> {
            StatisticsDashboardCardV2(
                label = stringResource(R.string.today_stats_month),
                totalHours = monthStats.totalHours,
                targetHours = monthStats.targetHours,
                workDaysCount = monthStats.workDaysCount,
                progress = monthStats.progress,
                isOverTarget = monthStats.isOverTarget,
                isUnderTarget = monthStats.isUnderTarget,
                onOpenWeekView = onOpenWeekView
            )
        }
    }
}

@Composable
private fun StatisticsDashboardCardV2(
    label: String,
    totalHours: Double,
    targetHours: Double,
    workDaysCount: Int,
    progress: Float,
    isOverTarget: Boolean,
    isUnderTarget: Boolean,
    onOpenWeekView: () -> Unit
) {
    val status = when {
        isOverTarget -> StatusType.SUCCESS
        isUnderTarget -> StatusType.WARNING
        else -> StatusType.INFO
    }

    MZCard(
        modifier = Modifier.clickableWithAccessibility(
            onClick = onOpenWeekView,
            contentDescription = stringResource(R.string.cd_open_statistics)
        )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.today_stats_hours,
                            totalHours,
                            targetHours
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.today_stats_label_days, label, workDaysCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MZStatusBadge(
                    text = stringResource(
                        R.string.today_stats_percent,
                        (progress * 100).toInt()
                    ),
                    type = status
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (status) {
                    StatusType.SUCCESS -> MaterialTheme.colorScheme.primary
                    StatusType.WARNING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun TodayActionsCardV2(
    entry: WorkEntry?,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onMorningCheckIn: () -> Unit,
    onEveningCheckIn: () -> Unit,
    onConfirmWorkDay: () -> Unit,
    onConfirmOffDay: () -> Unit,
    isMorningLoading: Boolean = false,
    isEveningLoading: Boolean = false,
    isConfirmWorkdayLoading: Boolean = false,
    isConfirmOffdayLoading: Boolean = false
) {
    val morningCompleted = entry?.morningCapturedAt != null
    val eveningCompleted = entry?.eveningCapturedAt != null
    val needsConfirmation = entry?.confirmedWorkDay != true
    val allDone = morningCompleted && eveningCompleted && !needsConfirmation

    MZCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.today_actions_title),
                    style = MaterialTheme.typography.titleLarge
                )
                if (allDone) {
                    MZStatusBadge(
                        text = stringResource(R.string.today_actions_done),
                        type = StatusType.SUCCESS
                    )
                }
            }

            ActionRowV2(
                title = stringResource(R.string.today_checkin_morning),
                supportingText = if (hasLocationPermission) {
                    stringResource(R.string.today_morning_support)
                } else {
                    stringResource(R.string.today_location_required)
                },
                completedAt = entry?.morningCapturedAt,
                icon = Icons.Default.WbSunny,
                actionLabel = if (hasLocationPermission) {
                    stringResource(R.string.action_check_in_morning)
                } else {
                    stringResource(R.string.action_allow_location)
                },
                onAction = if (hasLocationPermission) onMorningCheckIn else onRequestLocationPermission,
                isPrimary = !morningCompleted,
                isLoading = isMorningLoading
            )

            Divider()

            ActionRowV2(
                title = stringResource(R.string.today_checkin_evening),
                supportingText = if (hasLocationPermission) {
                    stringResource(R.string.today_evening_support)
                } else {
                    stringResource(R.string.today_location_required)
                },
                completedAt = entry?.eveningCapturedAt,
                icon = Icons.Default.Nightlight,
                actionLabel = if (hasLocationPermission) {
                    stringResource(R.string.action_check_in_evening)
                } else {
                    stringResource(R.string.action_allow_location)
                },
                onAction = if (hasLocationPermission) onEveningCheckIn else onRequestLocationPermission,
                isPrimary = morningCompleted && !eveningCompleted,
                isLoading = isEveningLoading
            )

            if (needsConfirmation && (morningCompleted || eveningCompleted)) {
                Divider()

                Text(
                    text = stringResource(R.string.today_confirmation_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MZPrimaryButton(
                        onClick = onConfirmWorkDay,
                        isLoading = isConfirmWorkdayLoading,
                        modifier = Modifier.weight(1f),
                        content = { Text(stringResource(R.string.action_confirm_workday)) }
                    )
                    MZSecondaryButton(
                        onClick = onConfirmOffDay,
                        isLoading = isConfirmOffdayLoading,
                        modifier = Modifier.weight(1f),
                        content = { Text(stringResource(R.string.action_confirm_offday)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRowV2(
    title: String,
    supportingText: String,
    completedAt: Long?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    isPrimary: Boolean,
    isLoading: Boolean = false
) {
    val isCompleted = completedAt != null
    val completedText = completedAt?.let {
        stringResource(R.string.today_completed_at, getCompletedTimeString(it))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = completedText ?: supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cd_completed),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            if (isPrimary) {
                MZPrimaryButton(
                    onClick = onAction,
                    isLoading = isLoading,
                    content = { Text(actionLabel) }
                )
            } else {
                MZSecondaryButton(
                    onClick = onAction,
                    isLoading = isLoading,
                    content = { Text(actionLabel) }
                )
            }
        }
    }
}

@Composable
private fun DayLocationDialogV2(
    currentLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf(currentLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.day_location_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.day_location_dialog_label)) },
                    placeholder = { Text(stringResource(R.string.day_location_dialog_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.day_location_dialog_support),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            MZPrimaryButton(
                onClick = { onConfirm(input.trim()) },
                enabled = input.trim().isNotEmpty(),
                content = { Text(stringResource(R.string.action_apply)) }
            )
        },
        dismissButton = {
            MZTertiaryButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun getCurrentDateString(): String {
    return LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", java.util.Locale.GERMAN)
    )
}

private fun getCompletedTimeString(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val localTime = java.time.LocalDateTime.ofInstant(
        instant, 
        java.time.ZoneId.systemDefault()
    )
    return localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) {
        stringResource(R.string.format_hours_short, h)
    } else {
        stringResource(R.string.format_hours_short_minutes, h, m)
    }
}
