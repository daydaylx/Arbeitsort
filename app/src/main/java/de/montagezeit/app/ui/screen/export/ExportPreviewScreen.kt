package de.montagezeit.app.ui.screen.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPreviewBottomSheet(
    range: Pair<LocalDate, LocalDate>,
    onDismiss: () -> Unit,
    onOpenEditSheet: (LocalDate, () -> Unit) -> Unit,
    viewModel: ExportPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                            Text(stringResource(R.string.export_preview_action_create_pdf))
                        }
                        SecondaryActionButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }

                is PreviewState.Empty -> {
                    PreviewHeader(state.header)
                    EmptyCard(state.message.asString(context))
                    SecondaryActionButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_close))
                    }
                }

                PreviewState.CreatingPdf -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.export_preview_creating_pdf))
                    }
                }

                is PreviewState.PdfReady -> {
                    PdfReadyContent(
                        fileUri = state.fileUri,
                        fileName = state.fileName,
                        onOpenPdf = { openPdf(context, state.fileUri) },
                        onSharePdf = { sharePdf(context, state.fileUri) },
                        onCopy = { copyExportUri(context, state.fileUri) },
                        onBackToPreview = { viewModel.returnToPreview() }
                    )
                }

                is PreviewState.Error -> {
                    ErrorCard(state.message.asString(context))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.canReturn) {
                            SecondaryActionButton(
                                onClick = { viewModel.returnToPreview() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.export_preview_action_back))
                            }
                        }
                        TertiaryActionButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_close))
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
private fun PdfReadyContent(
    fileUri: Uri,
    fileName: String,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onCopy: () -> Unit,
    onBackToPreview: () -> Unit
) {
    val context = LocalContext.current
    val rendererState = rememberPdfRendererState(context, fileUri)

    Text(
        stringResource(R.string.export_preview_pdf_ready_title),
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = fileName,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrimaryActionButton(
            onClick = onOpenPdf,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.export_preview_action_open_pdf))
        }
        SecondaryActionButton(
            onClick = onSharePdf,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.action_share))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TertiaryActionButton(
            onClick = onCopy,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.action_copy))
        }
        TertiaryActionButton(
            onClick = onBackToPreview,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.export_preview_action_back_to_preview))
        }
    }

    when (rendererState) {
        null -> {
            ErrorCard(stringResource(R.string.export_preview_pdf_render_failed))
        }

        else -> {
            Text(
                text = stringResource(R.string.export_preview_pdf_view_title, rendererState.renderer.pageCount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = List(rendererState.renderer.pageCount) { it },
                    key = { _, pageIndex -> pageIndex }
                ) { _, pageIndex ->
                    PdfPageCard(
                        renderer = rendererState.renderer,
                        pageIndex = pageIndex
                    )
                }
            }
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
            Text(
                stringResource(R.string.export_preview_totals_title),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.export_preview_totals_work_time),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(totals.workHours, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.export_preview_totals_travel_time),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(totals.travelHours, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.export_preview_totals_paid_total),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(totals.paidHours, fontWeight = FontWeight.Bold)
                }
            }
            Divider(modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.export_preview_totals_meal_allowance),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(totals.mealAllowanceTotal, fontWeight = FontWeight.Bold)
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
            .semantics {
                contentDescription = row.dateLabel
            }
            .clickable(onClick = onClick)
    ) {
        val footerLines = buildList {
            row.locationNote?.let(::add)
            row.mealAllowanceLabel?.let {
                add(stringResource(R.string.export_preview_row_meal_allowance, it))
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(row.dateLabel, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.export_preview_row_time_range, row.startLabel, row.endLabel))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.export_preview_row_break, row.breakLabel))
                Text(stringResource(R.string.export_preview_row_work, row.workLabel))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.export_preview_row_travel, row.travelLabel))
                Text(stringResource(R.string.export_preview_row_total, row.totalLabel))
            }
            if (footerLines.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                footerLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            text = stringResource(R.string.export_preview_error_message, message),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun PdfPageCard(
    renderer: PdfRenderer,
    pageIndex: Int
) {
    val bitmapState by produceState<PdfPageRenderState>(
        initialValue = PdfPageRenderState.Loading,
        renderer,
        pageIndex
    ) {
        value = try {
            val bitmap = withContext(Dispatchers.IO) {
                renderPdfPage(renderer, pageIndex)
            }
            PdfPageRenderState.Success(bitmap)
        } catch (_: Exception) {
            PdfPageRenderState.Error
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.export_preview_pdf_page_label, pageIndex + 1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (val state = bitmapState) {
                PdfPageRenderState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                PdfPageRenderState.Error -> {
                    Text(
                        text = stringResource(R.string.export_preview_pdf_render_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is PdfPageRenderState.Success -> {
                    Image(
                        bitmap = state.bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.export_preview_pdf_page_label, pageIndex + 1),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberPdfRendererState(
    context: Context,
    fileUri: Uri
): PdfRendererState? {
    val rendererState = remember(context, fileUri) {
        runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(fileUri, "r")
                ?: return@runCatching null
            PdfRendererState(
                descriptor = descriptor,
                renderer = PdfRenderer(descriptor)
            )
        }.getOrNull()
    }

    DisposableEffect(rendererState) {
        onDispose {
            rendererState?.close()
        }
    }

    return rendererState
}

private suspend fun renderPdfPage(
    renderer: PdfRenderer,
    pageIndex: Int
): Bitmap {
    synchronized(renderer) {
        val page = renderer.openPage(pageIndex)
        try {
            val targetWidth = (page.width * 1.8f).roundToInt().coerceAtLeast(1)
            val scale = targetWidth.toFloat() / page.width.toFloat()
            val targetHeight = (page.height * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } finally {
            page.close()
        }
    }
}

private sealed interface PdfPageRenderState {
    object Loading : PdfPageRenderState
    object Error : PdfPageRenderState
    data class Success(val bitmap: Bitmap) : PdfPageRenderState
}

private data class PdfRendererState(
    val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer
) {
    fun close() {
        renderer.close()
        descriptor.close()
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
        Toast.makeText(
            context,
            context.getString(R.string.export_preview_no_pdf_app),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun sharePdf(context: Context, fileUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.export_preview_share_chooser_title))
        )
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.export_preview_no_share_app),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun copyExportUri(context: Context, fileUri: Uri) {
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
