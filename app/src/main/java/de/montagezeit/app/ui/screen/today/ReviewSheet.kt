package de.montagezeit.app.ui.screen.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.domain.usecase.ReviewScope
import de.montagezeit.app.ui.common.PrimaryActionButton
import de.montagezeit.app.ui.common.TertiaryActionButton

private enum class LocationOption {
    LEIPZIG,
    OUTSIDE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    @Suppress("UNUSED_PARAMETER") scope: ReviewScope,
    reviewReason: String? = null,
    isResolving: Boolean = false,
    onResolve: (label: String, isLeipzig: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    var selectedOption by remember { mutableStateOf<LocationOption>(LocationOption.LEIPZIG) }
    var customLocation by remember { mutableStateOf("") }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            dragHandle = {
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp),
                        shape = RoundedCornerShape(2.dp)
                    ) {}
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.today_needs_review),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    reviewReason?.let { reason ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.review_location_question),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedOption == LocationOption.LEIPZIG,
                        onClick = { selectedOption = LocationOption.LEIPZIG },
                        label = { Text(stringResource(R.string.review_option_leipzig)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedOption == LocationOption.OUTSIDE,
                        onClick = { selectedOption = LocationOption.OUTSIDE },
                        label = { Text(stringResource(R.string.review_option_outside)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (selectedOption == LocationOption.OUTSIDE) {
                    OutlinedTextField(
                        value = customLocation,
                        onValueChange = { customLocation = it },
                        label = { Text(stringResource(R.string.review_location_label)) },
                        placeholder = { Text(stringResource(R.string.review_location_placeholder)) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                val leipzigLabel = stringResource(R.string.location_leipzig)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TertiaryActionButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        enabled = !isResolving
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    
                    PrimaryActionButton(
                        onClick = {
                            val label = if (selectedOption == LocationOption.LEIPZIG) {
                                leipzigLabel
                            } else {
                                customLocation.trim()
                            }
                            onResolve(label, selectedOption == LocationOption.LEIPZIG)
                        },
                        enabled = !isResolving && (selectedOption == LocationOption.LEIPZIG || customLocation.trim().isNotEmpty()),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isResolving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(stringResource(R.string.action_apply))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
