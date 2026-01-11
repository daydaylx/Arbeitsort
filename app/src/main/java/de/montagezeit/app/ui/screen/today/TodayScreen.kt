package de.montagezeit.app.ui.screen.today

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.ui.screen.today.TodayUiState
import de.montagezeit.app.ui.screen.today.WeekStats
import de.montagezeit.app.ui.screen.today.MonthStats
import java.time.Duration
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenEditSheet: (java.time.LocalDate) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val todayEntry by viewModel.todayEntry.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()
    val monthStats by viewModel.monthStats.collectAsState()
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Heute") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is TodayUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is TodayUiState.LoadingLocation -> {
                    LoadingLocationContent(
                        onSkipLocation = { viewModel.onSkipLocation() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is TodayUiState.Success -> {
                    TodayContent(
                        entry = (uiState as TodayUiState.Success).entry ?: todayEntry,
                        weekStats = weekStats,
                        monthStats = monthStats,
                        hasLocationPermission = hasLocationPermission,
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(locationPermission)
                        },
                        onMorningCheckIn = { viewModel.onMorningCheckIn() },
                        onEveningCheckIn = { viewModel.onEveningCheckIn() },
                        onOpenEditSheet = { onOpenEditSheet(java.time.LocalDate.now()) },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                }
                
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
            text = "Standort wird ermittelt...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSkipLocation) {
            Text("Ohne Standort speichern")
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
    onOpenEditSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        StatusCard(entry = entry)
        
        // Work Hours Card
        if (entry != null && entry.dayType == DayType.WORK) {
            WorkHoursCard(entry = entry)
        }
        
        // Statistics Dashboard
        if (weekStats != null || monthStats != null) {
            StatisticsDashboard(
                weekStats = weekStats,
                monthStats = monthStats
            )
        }
        
        // Check-In Buttons
        CheckInSection(
            entry = entry,
            hasLocationPermission = hasLocationPermission,
            onRequestLocationPermission = onRequestLocationPermission,
            onMorningCheckIn = onMorningCheckIn,
            onEveningCheckIn = onEveningCheckIn
        )
        
        // Manual Edit Button
        Button(
            onClick = onOpenEditSheet,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Manuell bearbeiten")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun WorkHoursCard(entry: WorkEntry) {
    val workHours = calculateWorkHours(entry)
    val formattedHours = formatWorkHours(workHours)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        text = "Heute",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formattedHours,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
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

@Composable
fun StatisticsDashboard(
    weekStats: WeekStats?,
    monthStats: MonthStats?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Statistiken",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            // Wochenstatistik
            weekStats?.let { stats ->
                StatisticCard(
                    title = "Diese Woche",
                    currentHours = stats.totalHours,
                    targetHours = stats.targetHours,
                    workDaysCount = stats.workDaysCount,
                    isOverTarget = stats.isOverTarget,
                    isUnderTarget = stats.isUnderTarget,
                    progress = stats.progress
                )
            }
            
            // Monatsstatistik
            monthStats?.let { stats ->
                StatisticCard(
                    title = "Dieser Monat",
                    currentHours = stats.totalHours,
                    targetHours = stats.targetHours,
                    workDaysCount = stats.workDaysCount,
                    isOverTarget = stats.isOverTarget,
                    isUnderTarget = stats.isUnderTarget,
                    progress = stats.progress
                )
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    currentHours: Double,
    targetHours: Double,
    workDaysCount: Int,
    isOverTarget: Boolean,
    isUnderTarget: Boolean,
    progress: Float
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "%.1f / %.1f Std.".format(currentHours, targetHours),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    isOverTarget -> MaterialTheme.colorScheme.error
                    isUnderTarget -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        }
        
        // Fortschrittsbalken
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = when {
                isOverTarget -> MaterialTheme.colorScheme.error
                isUnderTarget -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
        )
        
        // Warnung bei Über-/Unterstunden
        if (isOverTarget) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Überstunden: +%.1f Std.".format(currentHours - targetHours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (isUnderTarget) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Unterstunden: %.1f Std.".format(targetHours - currentHours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // Arbeitstage
        Text(
            text = "$workDaysCount Arbeitstag${if (workDaysCount != 1) "e" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun StatusCard(entry: WorkEntry?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry?.needsReview == true) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = getCurrentDateString(),
                style = MaterialTheme.typography.headlineSmall
            )
            
            if (entry == null) {
                Text(
                    text = "Noch kein Check-in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            } else {
                DayTypeText(dayType = entry.dayType)
                Spacer(modifier = Modifier.height(4.dp))
                LocationStatusText(entry = entry)
                TravelSummary(entry = entry)
                
                if (entry.needsReview) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Überprüfung erforderlich",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckInSection(
    entry: WorkEntry?,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onMorningCheckIn: () -> Unit,
    onEveningCheckIn: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Morning Check-In
        val morningCompleted = entry?.morningCapturedAt != null
        
        // Permission prüfen vor Morning Check-in
        val handleMorningCheckIn = {
            if (hasLocationPermission) {
                onMorningCheckIn()
            } else {
                onRequestLocationPermission()
            }
        }
        
        Button(
            onClick = handleMorningCheckIn,
            enabled = !morningCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (morningCompleted) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Icon(
                imageVector = if (morningCompleted) Icons.Default.CheckCircle else Icons.Default.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Morgens Check-in",
                    style = MaterialTheme.typography.titleMedium
                )
                if (morningCompleted) {
                    Text(
                        text = getCompletedTimeString(entry!!.morningCapturedAt!!),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Arbeitsbeginn protokollieren",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Evening Check-In
        val eveningCompleted = entry?.eveningCapturedAt != null
        
        // Permission prüfen vor Evening Check-in
        val handleEveningCheckIn = {
            if (hasLocationPermission) {
                onEveningCheckIn()
            } else {
                onRequestLocationPermission()
            }
        }
        
        Button(
            onClick = handleEveningCheckIn,
            enabled = !eveningCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (eveningCompleted) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Icon(
                imageVector = if (eveningCompleted) Icons.Default.CheckCircle else Icons.Default.Nightlight,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Abends Check-in",
                    style = MaterialTheme.typography.titleMedium
                )
                if (eveningCompleted) {
                    Text(
                        text = getCompletedTimeString(entry!!.eveningCapturedAt!!),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Arbeitsende protokollieren",
                        style = MaterialTheme.typography.bodySmall
                    )
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
        Button(onClick = onRetry) {
            Text("Wiederholen")
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
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Erneut versuchen")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        OutlinedButton(
            onClick = onSkipLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ohne Standort speichern")
        }
    }
}

@Composable
fun DayTypeText(dayType: DayType) {
    val (icon, text) = when (dayType) {
        DayType.WORK -> Icons.Default.Work to "Arbeitstag"
        DayType.OFF -> Icons.Default.FreeBreakfast to "Frei"
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
        // Morning Location
        if (entry.morningCapturedAt != null) {
            LocationRow(
                label = "Morgens:",
                locationStatus = entry.morningLocationStatus,
                locationLabel = entry.morningLocationLabel
            )
        }
        
        // Evening Location
        if (entry.eveningCapturedAt != null) {
            LocationRow(
                label = "Abends:",
                locationStatus = entry.eveningLocationStatus,
                locationLabel = entry.eveningLocationLabel
            )
        }
    }
}

@Composable
fun TravelSummary(entry: WorkEntry) {
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
            "${getCompletedTimeString(startAt)} – ${getCompletedTimeString(arriveAt)}"
        startAt != null -> "Start: ${getCompletedTimeString(startAt)}"
        arriveAt != null -> "Ankunft: ${getCompletedTimeString(arriveAt)}"
        else -> null
    }
    val durationText = calculateTravelDuration(startAt, arriveAt)?.let { formatDuration(it) }
    val summaryText = listOfNotNull(timeText, durationText?.let { "Fahrzeit $it" })
        .joinToString(" · ")

    Spacer(modifier = Modifier.height(6.dp))
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
                style = MaterialTheme.typography.bodySmall
            )
            labelText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
            style = MaterialTheme.typography.bodySmall
        )
        
        when (locationStatus) {
            LocationStatus.OK -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Standort OK",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            LocationStatus.LOW_ACCURACY -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Ungenaue Position",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
            LocationStatus.UNAVAILABLE -> {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = "Standort nicht verfügbar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        locationLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall
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
