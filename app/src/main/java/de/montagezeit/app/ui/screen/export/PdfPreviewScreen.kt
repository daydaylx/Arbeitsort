package de.montagezeit.app.ui.screen.export

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.SecondaryActionButton

@Composable
fun PdfPreviewScreen(
    fileUri: Uri,
    onBackToPreview: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Vorschau") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PDF bereit",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Die Datei kann jetzt geöffnet oder geteilt werden.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            PrimaryActionButton(
                onClick = {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(viewIntent)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "PDF öffnen",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("PDF öffnen")
            }

            SecondaryActionButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_SUBJECT, "MontageZeit Export (PDF)")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "PDF teilen"))
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Teilen")
            }

            Spacer(modifier = Modifier.height(8.dp))

            SecondaryActionButton(onClick = onBackToPreview) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Zurück zur Vorschau")
            }
        }
    }
}
