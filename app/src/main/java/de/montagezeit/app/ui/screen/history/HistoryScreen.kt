package de.montagezeit.app.ui.screen.history

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.export.CsvExporter
import java.time.format.DateTimeFormatter
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verlauf") },
                actions = {
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
                    val weeks = (uiState as HistoryUiState.Success).weeks
                    HistoryContent(
                        weeks = weeks,
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
}

@Composable
fun HistoryContent(
    weeks: List<WeekGroup>,
    onEntryClick: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (weeks.isEmpty()) {
        EmptyContent(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(weeks) { week ->
                WeekGroupCard(
                    week = week,
                    onEntryClick = onEntryClick
                )
            }
        }
    }
}

@Composable
fun EmptyContent(modifier: Modifier = Modifier) {
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
            
            HorizontalDivider()
            
            // Entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                week.entries.forEach { entry ->
                    HistoryEntryItem(
                        entry = entry,
                        onClick = { onEntryClick(entry.date) }
                    )
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
            }
            
            // Right side: Warning icon and edit icon
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
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Bearbeiten",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
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

private fun formatEntryDate(date: java.time.LocalDate): String {
    return date.format(
        DateTimeFormatter.ofPattern("E, dd.MM.", Locale.GERMAN)
    )
}

private fun handleShareExport(context: Context, fileUri: android.net.Uri) {
    val csvExporter = CsvExporter(context)
    val shareIntent = csvExporter.createShareIntent(fileUri)
    context.startActivity(shareIntent)
}
