package de.montagezeit.app.ui.screen.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPreviewBottomSheet(
    range: Pair<LocalDate, LocalDate>,
    onDismiss: () -> Unit,
    onOpenEditSheet: (LocalDate, () -> Unit) -> Unit,
    viewModel: ExportPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(range) {
        viewModel.loadRange(range.first, range.second)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = uiState) {
                is PreviewState.List -> {
                    PreviewHeader(state.header)
                    TotalsCard(state.totals)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.rows,
                            key = { row -> row.date.toEpochDay() }
                        ) { row ->
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PrimaryActionButton(
                            onClick = { viewModel.createPdf() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PDF erstellen")
                        }
                        SecondaryActionButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Schließen")
                        }
                    }
                }

                is PreviewState.Empty -> {
                    PreviewHeader(state.header)
                    EmptyCard(state.message)
                    SecondaryActionButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Schließen")
                    }
                }

                PreviewState.CreatingPdf -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Erstelle PDF...")
                    }
                }

                is PreviewState.PdfReady -> {
                    Text("PDF erstellt", style = MaterialTheme.typography.titleMedium)
                    Text(state.fileName, style = MaterialTheme.typography.bodySmall)
                    PrimaryActionButton(
                        onClick = { openPdf(context, state.fileUri) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PDF öffnen")
                    }
                    SecondaryActionButton(
                        onClick = { sharePdf(context, state.fileUri) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Teilen")
                    }
                    TertiaryActionButton(
                        onClick = { viewModel.returnToPreview() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zurück zur Vorschau")
                    }
                }

                is PreviewState.Error -> {
                    ErrorCard(state.message)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.canReturn) {
                            SecondaryActionButton(
                                onClick = { viewModel.returnToPreview() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Zurück")
                            }
                        }
                        TertiaryActionButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Schließen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewHeader(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
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
            .semantics { contentDescription = "ExportPreviewRow-${row.date}" }
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

private fun openPdf(context: Context, fileUri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(fileUri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Keine App zum Öffnen von PDFs gefunden.", Toast.LENGTH_SHORT).show()
    }
}

private fun sharePdf(context: Context, fileUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(Intent.createChooser(intent, "PDF teilen"))
    } else {
        Toast.makeText(context, "Keine App zum Teilen von PDFs gefunden.", Toast.LENGTH_SHORT).show()
    }
}
