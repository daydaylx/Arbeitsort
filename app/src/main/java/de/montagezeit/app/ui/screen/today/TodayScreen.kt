package de.montagezeit.app.ui.screen.today

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.domain.util.TimeCalculator
import de.montagezeit.app.R
import de.montagezeit.app.ui.screen.today.TodayUiState
import de.montagezeit.app.ui.screen.today.WeekStats
import de.montagezeit.app.ui.screen.today.MonthStats
import de.montagezeit.app.ui.util.DateTimeUtils
import de.montagezeit.app.ui.util.Formatters
import de.montagezeit.app.ui.util.getReviewReason
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton
import java.time.Duration
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenEditSheet: (java.time.LocalDate) -> Unit,
    onOpenWeekView: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val todayEntry by viewModel.todayEntry.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()
    val monthStats by viewModel.monthStats.collectAsState()
    val showReviewSheet by viewModel.showReviewSheet.collectAsState()
    val reviewScope by viewModel.reviewScope.collectAsState()
    val loadingActions by viewModel.loadingActions.collectAsState()
    
    // Runtime Permission für Standort
    val locationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Permission-Status prüfen (State, damit es sich aktualisiert)
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                locationPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Permission gewährt - Check-in automatisch auslösen
            if (todayEntry?.morningCapturedAt == null) {
                viewModel.onMorningCheckIn()
            } else if (todayEntry?.eveningCapturedAt == null) {
                viewModel.onEveningCheckIn()
            }
        } else {
            // Permission abgelehnt
            showPermissionRationale = true
        }
    }

    val openEditToday = { onOpenEditSheet(java.time.LocalDate.now()) }
    val openEditTodayEnsured = {
        viewModel.ensureTodayEntryThen {
            onOpenEditSheet(java.time.LocalDate.now())
        }
    }
    val currentEntry = (uiState as? TodayUiState.Success)?.entry ?: todayEntry
    val needsReview = currentEntry?.needsReview == true
    var isReviewBannerVisible by rememberSaveable { mutableStateOf(true) }

    val isMorningLoading = loadingActions.contains(TodayAction.MORNING_CHECK_IN)
    val isEveningLoading = loadingActions.contains(TodayAction.EVENING_CHECK_IN)
    val isConfirmWorkdayLoading = loadingActions.contains(TodayAction.CONFIRM_WORKDAY)
    val isConfirmOffdayLoading = loadingActions.contains(TodayAction.CONFIRM_OFFDAY)
    val isReviewResolving = loadingActions.contains(TodayAction.RESOLVE_REVIEW)

    LaunchedEffect(needsReview) {
        if (!needsReview) {
            isReviewBannerVisible = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.today_title)) }
            )
        },
        floatingActionButton = {
            // FAB nur anzeigen wenn nötig (nicht im Standard-Flow)
            if (uiState is TodayUiState.Success && (!needsReview && currentEntry?.confirmedWorkDay == false || currentEntry?.needsReview == true)) {
                ExtendedFloatingActionButton(
                    onClick = openEditToday,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_entry)
                        )
                    },
                    text = { Text(stringResource(R.string.action_edit_entry_manual)) }
                )
            }
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
                    ErrorContent(
                        message = (uiState as TodayUiState.Error).message,
                        onRetry = { viewModel.onResetError() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TodayUiState.LocationError -> {
                    val errorState = uiState as TodayUiState.LocationError
                    LocationErrorContent(
                        message = errorState.message,
                        canRetry = errorState.canRetry,
                        onRetry = {
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
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        TodayContent(
                            entry = currentEntry,
                            weekStats = weekStats,
                            monthStats = monthStats,
                            hasLocationPermission = hasLocationPermission,
                            onRequestLocationPermission = {
                                locationPermissionLauncher.launch(locationPermission)
                            },
                            onMorningCheckIn = { viewModel.onMorningCheckIn() },
                            onEveningCheckIn = { viewModel.onEveningCheckIn() },
                            onConfirmWorkDay = { viewModel.onConfirmWorkDay() },
                            onConfirmOffDay = { viewModel.onConfirmOffDay() },
                            onOpenReviewSheet = { viewModel.onOpenReviewSheet() },
                            showReviewBanner = needsReview && isReviewBannerVisible,
                            onDismissReviewBanner = { isReviewBannerVisible = false },
                            onEditToday = openEditTodayEnsured,
                            onOpenWeekView = onOpenWeekView,
                            showLocationLoading = isLoadingLocation,
                            onSkipLocation = { viewModel.onSkipLocation() },
                            isMorningLoading = isMorningLoading,
                            isEveningLoading = isEveningLoading,
                            isConfirmWorkdayLoading = isConfirmWorkdayLoading,
                            isConfirmOffdayLoading = isConfirmOffdayLoading,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
            
            // Review Sheet
            if (showReviewSheet) {
                val currentScope = reviewScope
                if (currentScope != null) {
                    ReviewSheet(
                        isVisible = showReviewSheet,
                        onDismissRequest = { viewModel.onDismissReviewSheet() },
                        scope = currentScope,
                        reviewReason = getReviewReason(currentEntry),
                        isResolving = isReviewResolving,
                        onResolve = { label, isLeipzig -> 
                            viewModel.onResolveReview(label, isLeipzig)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewBanner(
    entry: WorkEntry?,
    onOpenReviewSheet: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenReviewSheet),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.today_needs_review),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = getReviewReason(entry),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss_review_banner),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingLocationContent(
    onSkipLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = stringResource(R.string.today_location_loading),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TertiaryActionButton(onClick = onSkipLocation) {
            Text(stringResource(R.string.action_save_without_location))
        }
    }
}

@Composable
fun TodayContent(
    entry: WorkEntry?,
    weekStats: WeekStats?,
    monthStats: MonthStats?,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onMorningCheckIn: () -> Unit,
    onEveningCheckIn: () -> Unit,
    onConfirmWorkDay: () -> Unit = {},
    onConfirmOffDay: () -> Unit = {},
    onOpenReviewSheet: () -> Unit = {},
    showReviewBanner: Boolean = false,
    onDismissReviewBanner: () -> Unit = {},
    onEditToday: () -> Unit = {},
    onOpenWeekView: () -> Unit = {},
    showLocationLoading: Boolean = false,
    onSkipLocation: () -> Unit = {},
    isMorningLoading: Boolean = false,
    isEveningLoading: Boolean = false,
    isConfirmWorkdayLoading: Boolean = false,
    isConfirmOffdayLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showReviewBanner) {
            ReviewBanner(
                entry = entry,
                onOpenReviewSheet = onOpenReviewSheet,
                onDismiss = onDismissReviewBanner,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showLocationLoading) {
            LoadingLocationContent(
                onSkipLocation = onSkipLocation,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Status Card
        StatusCard(entry = entry, onEditToday = onEditToday)
        
        // Work Hours Card
        if (entry != null && entry.dayType == DayType.WORK) {
            WorkHoursCard(entry = entry)
        }
        
        // Statistics Dashboard
        if (weekStats != null || monthStats != null) {
            StatisticsDashboard(
                weekStats = weekStats,
                monthStats = monthStats,
                onOpenWeekView = onOpenWeekView
            )
        }

        TodayActionsCard(
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
fun WorkHoursCard(entry: WorkEntry) {
    val workMinutes = TimeCalculator.calculateWorkMinutes(entry)
    val travelMinutes = TimeCalculator.calculateTravelMinutes(entry)
    val totalMinutes = TimeCalculator.calculatePaidTotalMinutes(entry)
    
    // Helper local format function
    fun formatMin(min: Int): String {
        val h = min / 60
        val m = min % 60
        return if (m == 0) "${h} Std." else "${h}h ${m}min"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Gesamt (Bezahlt)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatMin(totalMinutes),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            if (travelMinutes > 0) {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp), 
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Arbeit: ${formatMin(workMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Reise: ${formatMin(travelMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}



@Composable
fun StatisticsDashboard(
    weekStats: WeekStats?,
    monthStats: MonthStats?,
    onOpenWeekView: () -> Unit
) {
    // Woche priorisieren, Monat nur anzeigen wenn Woche fehlt
    val stats = weekStats ?: monthStats
    
    if (stats == null) return
    
    // Stats-Properties basierend auf Typ extrahieren
    val totalHours: Double
    val totalPaidHours: Double
    val targetHours: Double
    val workDaysCount: Int
    val progress: Float
    val isOverTarget: Boolean
    val isUnderTarget: Boolean
    val label: String
    
    when (stats) {
        is WeekStats -> {
            totalHours = stats.totalHours
            totalPaidHours = stats.totalPaidHours
            targetHours = stats.targetHours
            workDaysCount = stats.workDaysCount
            progress = stats.progress
            isOverTarget = stats.isOverTarget
            isUnderTarget = stats.isUnderTarget
            label = stringResource(R.string.today_stats_week)
        }
        is MonthStats -> {
            totalHours = stats.totalHours
            totalPaidHours = stats.totalPaidHours
            targetHours = stats.targetHours
            workDaysCount = stats.workDaysCount
            progress = stats.progress
            isOverTarget = stats.isOverTarget
            isUnderTarget = stats.isUnderTarget
            label = stringResource(R.string.today_stats_month)
        }
        else -> return
    }
    
    val accentColor = when {
        isOverTarget -> MaterialTheme.colorScheme.tertiary
        isUnderTarget -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val progressColor = when {
        isOverTarget -> MaterialTheme.colorScheme.tertiary
        isUnderTarget -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenWeekView),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.today_stats_hours,
                        totalHours,
                        targetHours
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor
                )
                
                if (kotlin.math.abs(totalPaidHours - totalHours) > 0.01) {
                    Text(
                        text = "Bezahlt: " + String.format(java.util.Locale.GERMAN, "%.2f", totalPaidHours) + " h",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Text(
                    text = "$label · ${pluralStringResource(R.plurals.today_workday_count, workDaysCount, workDaysCount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .width(80.dp)
                    .height(6.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
            )
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details anzeigen",
                tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StatusCard(entry: WorkEntry?, onEditToday: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditToday),
        colors = CardDefaults.cardColors(
            containerColor = when {
                entry?.needsReview == true -> MaterialTheme.colorScheme.errorContainer
                entry?.confirmedWorkDay == true -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getCurrentDateString(),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    entry?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        DayTypeText(dayType = it.dayType)
                    }
                }

                val isConfirmed = entry?.confirmedWorkDay == true
                val needsReview = entry?.needsReview == true
                StatusBadge(
                    text = when {
                        needsReview -> getReviewReason(entry)
                        isConfirmed -> stringResource(R.string.today_confirmed)
                        else -> stringResource(R.string.today_unconfirmed)
                    },
                    icon = when {
                        needsReview -> Icons.Default.Warning
                        isConfirmed -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning
                    },
                    containerColor = when {
                        needsReview -> MaterialTheme.colorScheme.errorContainer
                        isConfirmed -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    contentColor = when {
                        needsReview -> MaterialTheme.colorScheme.onErrorContainer
                        isConfirmed -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            if (entry == null) {
                Text(
                    text = stringResource(R.string.today_no_check_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            } else {
                Divider()

                LocationStatusText(entry = entry)
                TravelSummary(entry = entry)

                if (entry.needsReview) {
                    Divider()
                    StatusHintRow(
                        text = getReviewReason(entry),
                        icon = Icons.Default.Warning,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Dekorativ, Text daneben
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun StatusHintRow(
    text: String,
    icon: ImageVector,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Dekorativ, Text daneben
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

@Composable
fun TodayActionsCard(
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

    val handleMorningCheckIn = {
        if (hasLocationPermission) {
            onMorningCheckIn()
        } else {
            onRequestLocationPermission()
        }
    }

    val handleEveningCheckIn = {
        if (hasLocationPermission) {
            onEveningCheckIn()
        } else {
            onRequestLocationPermission()
        }
    }

    val morningActionLabel = if (hasLocationPermission) {
        stringResource(R.string.action_check_in_morning)
    } else {
        stringResource(R.string.action_allow_location)
    }
    val morningSupporting = if (hasLocationPermission) {
        stringResource(R.string.today_morning_support)
    } else {
        stringResource(R.string.today_location_required)
    }

    val eveningActionLabel = if (hasLocationPermission) {
        stringResource(R.string.action_check_in_evening)
    } else {
        stringResource(R.string.action_allow_location)
    }
    val eveningSupporting = if (hasLocationPermission) {
        stringResource(R.string.today_evening_support)
    } else {
        stringResource(R.string.today_location_required)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.today_actions_title),
                    style = MaterialTheme.typography.titleMedium
                )
                if (allDone) {
                    StatusBadge(
                        text = stringResource(R.string.today_actions_done),
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            ActionRow(
                title = stringResource(R.string.today_checkin_morning),
                supportingText = morningSupporting,
                completedAt = entry?.morningCapturedAt,
                icon = Icons.Default.WbSunny,
                actionLabel = morningActionLabel,
                onAction = handleMorningCheckIn,
                isPrimary = !morningCompleted,
                isLoading = isMorningLoading
            )

            Divider()

            ActionRow(
                title = stringResource(R.string.today_checkin_evening),
                supportingText = eveningSupporting,
                completedAt = entry?.eveningCapturedAt,
                icon = Icons.Default.Nightlight,
                actionLabel = eveningActionLabel,
                onAction = handleEveningCheckIn,
                isPrimary = morningCompleted && !eveningCompleted,
                isLoading = isEveningLoading
            )

            Divider()

            if (needsConfirmation) {
                Text(
                    text = stringResource(R.string.today_confirmation_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PrimaryActionButton(
                        onClick = onConfirmWorkDay,
                        modifier = Modifier.weight(1f),
                        enabled = !isConfirmWorkdayLoading
                    ) {
                        if (isConfirmWorkdayLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(stringResource(R.string.action_confirm_workday))
                    }
                    SecondaryActionButton(
                        onClick = onConfirmOffDay,
                        modifier = Modifier.weight(1f),
                        enabled = !isConfirmOffdayLoading
                    ) {
                        if (isConfirmOffdayLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(stringResource(R.string.action_confirm_offday))
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.today_confirmation_done),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    supportingText: String,
    completedAt: Long?,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    isPrimary: Boolean,
    isLoading: Boolean = false
) {
    val completedText = completedAt?.let {
        stringResource(R.string.today_completed_at, getCompletedTimeString(it))
    }
    val isCompleted = completedAt != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (isCompleted) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            val buttonModifier = Modifier.heightIn(min = 40.dp)
            if (isPrimary) {
                PrimaryActionButton(
                    onClick = onAction,
                    modifier = buttonModifier,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(actionLabel)
                }
            } else {
                SecondaryActionButton(
                    onClick = onAction,
                    modifier = buttonModifier,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(actionLabel)
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
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
fun LocationErrorContent(
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
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (canRetry) {
            PrimaryActionButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_retry_location))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        SecondaryActionButton(
            onClick = onSkipLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save_without_location))
        }
    }
}

@Composable
fun DayTypeText(dayType: DayType) {
    val (icon, text) = when (dayType) {
        DayType.WORK -> Icons.Default.Work to stringResource(R.string.day_type_work)
        DayType.OFF -> Icons.Default.FreeBreakfast to stringResource(R.string.day_type_off)
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun LocationStatusText(entry: WorkEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (entry.morningCapturedAt == null && entry.eveningCapturedAt == null) {
            Text(
                text = stringResource(R.string.today_no_check_in),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Morning Location
            if (entry.morningCapturedAt != null) {
                LocationRow(
                    label = stringResource(R.string.today_location_morning),
                    locationStatus = entry.morningLocationStatus,
                    locationLabel = entry.morningLocationLabel
                )
            }

            // Evening Location
            if (entry.eveningCapturedAt != null) {
                LocationRow(
                    label = stringResource(R.string.today_location_evening),
                    locationStatus = entry.eveningLocationStatus,
                    locationLabel = entry.eveningLocationLabel
                )
            }
        }
    }
}

@Composable
fun TravelSummary(entry: WorkEntry) {
    val startAt = entry.travelStartAt
    val arriveAt = entry.travelArriveAt
    val startLabel = entry.travelLabelStart?.takeIf { it.isNotBlank() }
    val endLabel = entry.travelLabelEnd?.takeIf { it.isNotBlank() }
    val labelText = when {
        startLabel != null && endLabel != null ->
            stringResource(R.string.travel_label_from_to, startLabel, endLabel)
        startLabel != null ->
            stringResource(R.string.travel_label_from, startLabel)
        endLabel != null ->
            stringResource(R.string.travel_label_to, endLabel)
        else -> null
    }

    if (startAt == null && arriveAt == null && labelText == null) return

    val timeText = when {
        startAt != null && arriveAt != null ->
            stringResource(
                R.string.travel_time_range,
                getCompletedTimeString(startAt),
                getCompletedTimeString(arriveAt)
            )
        startAt != null -> stringResource(R.string.travel_time_start, getCompletedTimeString(startAt))
        arriveAt != null -> stringResource(R.string.travel_time_arrive, getCompletedTimeString(arriveAt))
        else -> null
    }
    val durationText = DateTimeUtils.calculateTravelDuration(startAt, arriveAt)?.let { Formatters.formatDuration(it) }
    val summaryText = listOfNotNull(
        timeText,
        durationText?.let { stringResource(R.string.travel_duration_label, it) }
    )
        .joinToString(" · ")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            labelText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun LocationRow(
    label: String,
    locationStatus: LocationStatus,
    locationLabel: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        when (locationStatus) {
            LocationStatus.OK -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.location_status_ok),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            LocationStatus.LOW_ACCURACY -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.location_status_low_accuracy),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
            LocationStatus.UNAVAILABLE -> {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = stringResource(R.string.location_status_unavailable),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        locationLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getCurrentDateString(): String {
    return java.time.LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy")
    )
}

private fun getCompletedTimeString(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val time = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

// Removed: calculateTravelDuration - now using DateTimeUtils.calculateTravelDuration

// Removed: formatDuration - now using Formatters.formatDuration
