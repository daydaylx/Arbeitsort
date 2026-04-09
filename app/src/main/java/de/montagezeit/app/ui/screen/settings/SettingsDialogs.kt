package de.montagezeit.app.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import de.montagezeit.app.ui.components.MZAlertDialog
import de.montagezeit.app.ui.components.mzOutlinedTextFieldColors
import de.montagezeit.app.ui.components.PrimaryActionButton
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.components.TertiaryActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.DatePickerDialog
import de.montagezeit.app.ui.util.Formatters
import java.time.LocalDate

@Composable
fun PdfSettingsDialog(
    initialEmployeeName: String,
    initialCompany: String?,
    initialProject: String?,
    initialPersonnelNumber: String?,
    onSave: (String?, String?, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var employeeName by remember { mutableStateOf(initialEmployeeName) }
    var company by remember { mutableStateOf(initialCompany.orEmpty()) }
    var project by remember { mutableStateOf(initialProject.orEmpty()) }
    var personnelNumber by remember { mutableStateOf(initialPersonnelNumber.orEmpty()) }

    MZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pdf_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = employeeName,
                    onValueChange = { if (it.length <= 100) employeeName = it },
                    label = { Text(stringResource(R.string.pdf_settings_employee_name_label)) },
                    placeholder = { Text(stringResource(R.string.pdf_settings_employee_name_placeholder)) },
                    singleLine = true,
                    isError = employeeName.isBlank(),
                    colors = mzOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { if (it.length <= 100) company = it },
                    label = { Text(stringResource(R.string.pdf_settings_company_label)) },
                    placeholder = { Text(stringResource(R.string.pdf_settings_company_placeholder)) },
                    singleLine = true,
                    colors = mzOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = project,
                    onValueChange = { if (it.length <= 100) project = it },
                    label = { Text(stringResource(R.string.pdf_settings_project_label)) },
                    placeholder = { Text(stringResource(R.string.pdf_settings_project_placeholder)) },
                    singleLine = true,
                    colors = mzOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = personnelNumber,
                    onValueChange = { if (it.length <= 100) personnelNumber = it },
                    label = { Text(stringResource(R.string.pdf_settings_personnel_number_label)) },
                    placeholder = { Text(stringResource(R.string.pdf_settings_personnel_number_placeholder)) },
                    singleLine = true,
                    colors = mzOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.pdf_settings_required_field_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = {
                    onSave(
                        employeeName.ifBlank { null },
                        company.ifBlank { null },
                        project.ifBlank { null },
                        personnelNumber.ifBlank { null }
                    )
                },
                enabled = employeeName.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TertiaryActionButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ExportRangeDialog(
    onPdfRangeSelected: (LocalDate, LocalDate) -> Unit,
    onCsvRangeSelected: (LocalDate, LocalDate) -> Unit,
    onPreviewRangeSelected: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(29)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    MZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pdf_custom_range_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.pdf_range_from),
                    style = MaterialTheme.typography.bodyMedium
                )
                SecondaryActionButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Formatters.formatDate(startDate))
                }

                Text(
                    text = stringResource(R.string.pdf_range_to),
                    style = MaterialTheme.typography.bodyMedium
                )
                SecondaryActionButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Formatters.formatDate(endDate))
                }

                AnimatedVisibility(visible = startDate.isAfter(endDate)) {
                    Text(
                        text = stringResource(R.string.error_invalid_date_range),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryActionButton(
                    onClick = { onPreviewRangeSelected(startDate, endDate) },
                    enabled = !startDate.isAfter(endDate)
                ) {
                    Text(stringResource(R.string.action_preview))
                }
                SecondaryActionButton(
                    onClick = { onCsvRangeSelected(startDate, endDate) },
                    enabled = !startDate.isAfter(endDate)
                ) {
                    Text(stringResource(R.string.export_format_csv))
                }
                PrimaryActionButton(
                    onClick = { onPdfRangeSelected(startDate, endDate) },
                    enabled = !startDate.isAfter(endDate)
                ) {
                    Text(stringResource(R.string.export_format_pdf))
                }
            }
        },
        dismissButton = {
            TertiaryActionButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = {
                startDate = it
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDateSelected = {
                endDate = it
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}
