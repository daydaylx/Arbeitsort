package de.montagezeit.app.ui.screen.export

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPreviewScreen(
    onBack: () -> Unit,
    onOpenEditSheet: (LocalDate, () -> Unit) -> Unit,
    viewModel: ExportPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportvorschau") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val header = when (uiState) {
                is ExportPreviewUiState.Content -> (uiState as ExportPreviewUiState.Content).header
                is ExportPreviewUiState.Empty -> (uiState as ExportPreviewUiState.Empty).header
                else -> null
            }
            if (header != null) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            when (val state = uiState) {
                is ExportPreviewUiState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ExportPreviewUiState.Error -> {
                    ErrorCard(state.message)
                }

                is ExportPreviewUiState.Empty -> {
                    if (state.isNameMissing) {
                        WarningCard("Name fehlt. Bitte zuerst in den Settings eingeben.")
                    }
                    EmptyCard("Keine Einträge im Zeitraum")
                    ExportStatusSection(state.exportStatus, viewModel)
                }

                is ExportPreviewUiState.Content -> {
                    if (state.isNameMissing) {
                        WarningCard("Name fehlt. Bitte zuerst in den Settings eingeben.")
                    }
                    TotalsCard(state.totals)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.rows) { row ->
                            ExportPreviewRowCard(
                                row = row,
                                onClick = {
                                    onOpenEditSheet(row.date) {
                                        viewModel.refresh()
                                    }
                                }
                            )
                        }
                    }
                    ExportStatusSection(state.exportStatus, viewModel)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val canExport = when (val state = uiState) {
                is ExportPreviewUiState.Content -> !state.isNameMissing
                else -> false
            }
            val isExporting = when (val state = uiState) {
                is ExportPreviewUiState.Content -> state.exportStatus is ExportPreviewExportStatus.Exporting
                is ExportPreviewUiState.Empty -> state.exportStatus is ExportPreviewExportStatus.Exporting
                else -> false
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryActionButton(
                    onClick = { viewModel.createPdf() },
                    enabled = canExport && !isExporting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("PDF erstellen")
                }
                SecondaryActionButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Zurück")
                }
            }
        }
    }

    val exportStatus = when (uiState) {
        is ExportPreviewUiState.Content -> (uiState as ExportPreviewUiState.Content).exportStatus
        is ExportPreviewUiState.Empty -> (uiState as ExportPreviewUiState.Empty).exportStatus
        else -> ExportPreviewExportStatus.Idle
    }
    if (exportStatus is ExportPreviewExportStatus.Success) {
        var showShareDialog by remember(exportStatus.fileUri) { mutableStateOf(true) }
        if (showShareDialog) {
            AlertDialog(
                onDismissRequest = {
                    showShareDialog = false
                    viewModel.clearExportStatus()
                },
                title = { Text("PDF Export") },
                text = { Text("Die Datei wurde erstellt. Möchten Sie sie teilen?") },
                confirmButton = {
                    PrimaryActionButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, exportStatus.fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            putExtra(Intent.EXTRA_SUBJECT, "MontageZeit Export (PDF)")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export teilen"))
                        showShareDialog = false
                        viewModel.clearExportStatus()
                    }) {
                        Text("Teilen")
                    }
                },
                dismissButton = {
                    TertiaryActionButton(onClick = {
                        showShareDialog = false
                        viewModel.clearExportStatus()
                    }) {
                        Text("Schließen")
                    }
                }
            )
        }
    }
}

@Composable
private fun TotalsCard(totals: ExportPreviewTotals) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Summen", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Arbeitszeit", style = MaterialTheme.typography.bodySmall)
                    Text(totals.workHours, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Reisezeit", style = MaterialTheme.typography.bodySmall)
                    Text(totals.travelHours, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Gesamt bezahlt", style = MaterialTheme.typography.bodySmall)
                    Text(totals.paidHours, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExportPreviewRowCard(
    row: ExportPreviewRow,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(row.dateLabel, fontWeight = FontWeight.Bold)
                Text("${row.startLabel} – ${row.endLabel}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pause: ${row.breakLabel}")
                Text("Arbeit: ${row.workLabel}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Reise: ${row.travelLabel}")
                Text("Gesamt: ${row.totalLabel}")
            }
            row.locationNote?.let { note ->
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WarningCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Fehler: $message",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun ExportStatusSection(
    exportStatus: ExportPreviewExportStatus,
    viewModel: ExportPreviewViewModel
) {
    when (exportStatus) {
        is ExportPreviewExportStatus.Exporting -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("Exportiere...")
            }
        }

        is ExportPreviewExportStatus.Error -> {
            ErrorCard(exportStatus.message)
        }

        else -> Unit
    }
}
