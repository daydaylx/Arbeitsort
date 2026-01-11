package de.montagezeit.app.ui.screen.travel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelSection(
    title: String,
    travelState: TravelUiState,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onCalculateDistance: () -> Unit,
    onManualDistanceChange: (String) -> Unit,
    onSaveManualDistance: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = travelState.fromLabel,
            onValueChange = onFromChange,
            label = { Text("Von") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = travelState.toLabel,
            onValueChange = onToChange,
            label = { Text("Nach") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onCalculateDistance,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Km berechnen")
        }

        when (travelState.status) {
            TravelStatus.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                    Text("Route wird berechnet...")
                }
            }
            TravelStatus.Error -> {
                Text(
                    text = travelState.errorMessage ?: "Fehler bei der Routenberechnung",
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> Unit
        }

        if (travelState.distanceKm != null) {
            val distanceLabel = String.format(Locale.GERMAN, "%.2f km", travelState.distanceKm)
            Text("Distanz: $distanceLabel")
        }

        if (!travelState.paidHoursDisplay.isNullOrBlank()) {
            Text("Bezahlte Fahrzeit: ${travelState.paidHoursDisplay}")
        }

        if (travelState.source != null) {
            Text("Quelle: ${travelState.source.name}")
        }

        Divider()

        OutlinedTextField(
            value = travelState.manualDistanceKm,
            onValueChange = onManualDistanceChange,
            label = { Text("Km manuell") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors()
        )

        TextButton(
            onClick = onSaveManualDistance,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Manuelle Km speichern")
        }
    }
}
