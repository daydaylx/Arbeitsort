@file:Suppress("LongParameterList")

package de.montagezeit.app.ui.screen.edit

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.DatePickerDialog
import de.montagezeit.app.ui.components.MZAppPanel
import de.montagezeit.app.ui.components.MZErrorState
import de.montagezeit.app.ui.components.MZHeroPanel
import de.montagezeit.app.ui.components.MZMetricChip
import de.montagezeit.app.ui.components.MZSectionIntro
import de.montagezeit.app.ui.components.MZStatusChip
import de.montagezeit.app.ui.theme.MZTokens
import de.montagezeit.app.ui.util.asString
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntrySheet(
    date: LocalDate,
    viewModel: EditEntryViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    initialFormData: EditFormData? = null,
    onCopyToNewDate: ((LocalDate, EditFormData) -> Unit)? = null,
    onNavigateDate: ((LocalDate) -> Unit)? = null
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var activeDialog by remember { mutableStateOf<EditSheetDialog>(EditSheetDialog.None) }
    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }

    LaunchedEffect(date) {
        viewModel.setDate(date)
    }

    LaunchedEffect(initialFormData) {
        if (initialFormData != null) {
            viewModel.setFormData(initialFormData)
        }
    }

    LaunchedEffect(screenState.uiState, onDismiss) {
        if (screenState.uiState is EditUiState.Saved) {
            onDismiss()
        }
    }

    val handleDismiss: () -> Unit = {
        if (screenState.isDirty && !screenState.isSaving) {
            activeDialog = EditSheetDialog.DiscardChanges
        } else {
            onDismiss()
        }
    }

    val guardedNavigateDate: (LocalDate) -> Unit = { newDate ->
        onNavigateDate?.let { navigate ->
            if (screenState.isDirty && !screenState.isSaving) {
                activeDialog = EditSheetDialog.DiscardChangesForNavigation(newDate)
            } else {
                navigate(newDate)
            }
        }
    }

    BackHandler(enabled = screenState.isDirty && !screenState.isSaving) {
        activeDialog = EditSheetDialog.DiscardChanges
    }

    ModalBottomSheet(
        onDismissRequest = handleDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = MZTokens.RadiusSheet, topEnd = MZTokens.RadiusSheet),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = MZTokens.AlphaGlassSheet)
    ) {
        EditEntrySheetScaffold(
            date = date,
            screenState = screenState,
            swipeThresholdPx = swipeThresholdPx,
            onNavigateDate = if (onNavigateDate != null) guardedNavigateDate else null,
            onOpenNavigateDatePicker = { activeDialog = EditSheetDialog.NavigateDatePicker },
            onDismiss = onDismiss,
            onOpenCopyDatePicker = { activeDialog = EditSheetDialog.CopyDatePicker },
            onOpenDeleteDialog = { activeDialog = EditSheetDialog.DeleteDayConfirm },
            viewModel = viewModel,
            context = context
        )
    }

    EditEntryDialogsHost(
        activeDialog = activeDialog,
        date = date,
        isSaving = screenState.isSaving,
        viewModel = viewModel,
        context = context,
        onDismiss = onDismiss,
        onCopyToNewDate = onCopyToNewDate,
        onNavigateDate = onNavigateDate,
        onRequestNavigateDate = guardedNavigateDate,
        onDialogChange = { activeDialog = it }
    )
}

@Composable
private fun EditEntrySheetScaffold(
    date: LocalDate,
    screenState: EditScreenState,
    swipeThresholdPx: Float,
    onNavigateDate: ((LocalDate) -> Unit)?,
    onOpenNavigateDatePicker: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCopyDatePicker: () -> Unit,
    onOpenDeleteDialog: () -> Unit,
    viewModel: EditEntryViewModel,
    context: Context
) {
    val uiState = screenState.uiState

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (uiState is EditUiState.NewEntry || uiState is EditUiState.Success) {
                EditStickySaveBar(
                    isSaving = screenState.isSaving,
                    onSave = viewModel::save
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditSheetHero(
                date = date,
                isSaving = screenState.isSaving,
                isDirty = screenState.isDirty,
                uiState = uiState
            )

            if (onNavigateDate != null) {
                MZAppPanel {
                    DateNavigationRow(
                        date = date,
                        onPrevious = { onNavigateDate(date.minusDays(1)) },
                        onNext = { onNavigateDate(date.plusDays(1)) },
                        onToday = { onNavigateDate(LocalDate.now()) },
                        onPickDate = onOpenNavigateDatePicker
                    )
                    DateNavigationSwipeZone(
                        swipeThresholdPx = swipeThresholdPx,
                        onSwipePrevious = { onNavigateDate(date.minusDays(1)) },
                        onSwipeNext = { onNavigateDate(date.plusDays(1)) },
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            EditEntryStateContent(
                screenState = screenState,
                viewModel = viewModel,
                context = context,
                onDismiss = onDismiss,
                onOpenDeleteDialog = onOpenDeleteDialog,
                onOpenCopyDatePicker = onOpenCopyDatePicker
            )
        }
    }
}

@Composable
private fun EditSheetHero(
    date: LocalDate,
    isSaving: Boolean,
    isDirty: Boolean,
    uiState: EditUiState
) {
    MZHeroPanel {
        MZSectionIntro(
            eyebrow = date.toString(),
            title = stringResource(
                if (uiState is EditUiState.NewEntry) {
                    R.string.edit_sheet_title_new
                } else {
                    R.string.edit_sheet_title_existing
                }
            ),
            supportingText = stringResource(R.string.edit_sheet_support)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MZMetricChip(
                label = stringResource(R.string.edit_sheet_metric_state),
                value = when {
                    isSaving -> stringResource(R.string.loading)
                    isDirty -> stringResource(R.string.edit_sheet_metric_unsaved)
                    else -> stringResource(R.string.today_confirmed)
                },
                modifier = Modifier.weight(1f)
            )
            MZMetricChip(
                label = stringResource(R.string.edit_sheet_metric_mode),
                value = if (uiState is EditUiState.NewEntry) {
                    stringResource(R.string.action_add)
                } else {
                    stringResource(R.string.action_edit_entry_manual)
                },
                modifier = Modifier.weight(1f),
                accentColor = MaterialTheme.colorScheme.secondary
            )
        }
        if (isDirty) {
            MZStatusChip(
                text = stringResource(R.string.edit_sheet_metric_unsaved),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ColumnScope.EditEntryStateContent(
    screenState: EditScreenState,
    viewModel: EditEntryViewModel,
    context: Context,
    onDismiss: () -> Unit,
    onOpenDeleteDialog: () -> Unit,
    onOpenCopyDatePicker: () -> Unit
) {
    when (val state = screenState.uiState) {
        is EditUiState.Loading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        }

        is EditUiState.NotFound -> {
            Text(
                text = stringResource(R.string.edit_not_found),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        is EditUiState.NewEntry -> {
            EditEntryFormStateContent(
                formData = screenState.formData,
                validationErrors = state.validationErrors,
                dailyTargetHours = screenState.dailyTargetHours,
                mealAllowancePreviewCents = screenState.mealAllowancePreviewCents,
                viewModel = viewModel,
                context = context,
                isSaving = screenState.isSaving,
                isNewEntry = true,
                onDeleteDay = null,
                onCopy = null
            )
        }

        is EditUiState.Error -> {
            MZErrorState(
                message = state.message.asString(context),
                onRetry = { viewModel.reloadEntry() }
            )
        }

        is EditUiState.Success -> {
            EditEntryFormStateContent(
                formData = screenState.formData,
                validationErrors = state.validationErrors,
                dailyTargetHours = screenState.dailyTargetHours,
                mealAllowancePreviewCents = screenState.mealAllowancePreviewCents,
                viewModel = viewModel,
                context = context,
                isSaving = screenState.isSaving,
                isNewEntry = false,
                onDeleteDay = onOpenDeleteDialog,
                onCopy = onOpenCopyDatePicker
            )
        }

        is EditUiState.Saved -> {
            RowSavingState()
        }
    }
}

@Composable
private fun EditEntryFormStateContent(
    formData: EditFormData,
    validationErrors: List<ValidationError>,
    dailyTargetHours: Double,
    mealAllowancePreviewCents: Int,
    viewModel: EditEntryViewModel,
    context: Context,
    isSaving: Boolean,
    isNewEntry: Boolean,
    onDeleteDay: (() -> Unit)?,
    onCopy: (() -> Unit)?
) {
    EditFormContent(
        formData = formData,
        validationErrors = validationErrors,
        dailyTargetHours = dailyTargetHours,
        mealAllowancePreviewCents = mealAllowancePreviewCents,
        onDayTypeChange = viewModel::updateDayType,
        onHasWorkTimesChange = viewModel::setHasWorkTimes,
        onWorkStartChange = viewModel::updateWorkStart,
        onWorkEndChange = viewModel::updateWorkEnd,
        onBreakMinutesChange = viewModel::updateBreakMinutes,
        onAddTravelLeg = viewModel::addTravelLeg,
        onTravelLegStartChange = viewModel::updateTravelLegStart,
        onTravelLegArriveChange = viewModel::updateTravelLegArrive,
        onTravelLegStartLabelChange = viewModel::updateTravelLegStartLabel,
        onTravelLegEndLabelChange = viewModel::updateTravelLegEndLabel,
        onRemoveTravelLeg = viewModel::removeTravelLeg,
        onTravelClear = viewModel::clearTravel,
        onDayLocationChange = viewModel::updateDayLocationLabel,
        onMealArrivalDepartureChange = viewModel::updateMealArrivalDeparture,
        onMealBreakfastIncludedChange = viewModel::updateMealBreakfastIncluded,
        onNoteChange = viewModel::updateNote,
        onApplyDefaultTimes = viewModel::applyDefaultWorkTimes,
        onCopyPrevious = {
            viewModel.copyFromPreviousDay { success ->
                if (!success) {
                    showMissingPreviousEntryToast(context)
                }
            }
        },
        onSave = viewModel::save,
        onDeleteDay = onDeleteDay,
        isSaving = isSaving,
        isNewEntry = isNewEntry,
        onCopy = onCopy,
        showPrimarySaveButton = false
    )

    EditValidationCard(
        validationErrors = validationErrors,
        onDismiss = { viewModel.clearValidationErrors() }
    )
}

@Composable
private fun EditEntryDialogsHost(
    activeDialog: EditSheetDialog,
    date: LocalDate,
    isSaving: Boolean,
    viewModel: EditEntryViewModel,
    context: Context,
    onDismiss: () -> Unit,
    onCopyToNewDate: ((LocalDate, EditFormData) -> Unit)?,
    onNavigateDate: ((LocalDate) -> Unit)?,
    onRequestNavigateDate: (LocalDate) -> Unit,
    onDialogChange: (EditSheetDialog) -> Unit
) {
    if (activeDialog is EditSheetDialog.DeleteDayConfirm) {
        DeleteDayConfirmDialog(
            isLoading = isSaving,
            onDismiss = { onDialogChange(EditSheetDialog.None) },
            onConfirm = {
                onDialogChange(EditSheetDialog.None)
                viewModel.deleteCurrentEntry { deleted ->
                    if (deleted) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.today_delete_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    if (activeDialog is EditSheetDialog.DiscardChanges) {
        DiscardChangesDialog(
            onDiscard = {
                onDialogChange(EditSheetDialog.None)
                onDismiss()
            },
            onKeepEditing = { onDialogChange(EditSheetDialog.None) }
        )
    }

    val navigateDiscardDialog = activeDialog as? EditSheetDialog.DiscardChangesForNavigation
    if (navigateDiscardDialog != null) {
        DiscardChangesDialog(
            onDiscard = {
                onDialogChange(EditSheetDialog.None)
                onRequestNavigateDate(navigateDiscardDialog.newDate)
            },
            onKeepEditing = { onDialogChange(EditSheetDialog.None) }
        )
    }

    if (activeDialog is EditSheetDialog.CopyDatePicker && onCopyToNewDate != null) {
        DatePickerDialog(
            initialDate = date,
            onDateSelected = { newDate ->
                onCopyToNewDate(newDate, viewModel.copyEntryData())
                onDialogChange(EditSheetDialog.None)
                onDismiss()
            },
            onDismiss = { onDialogChange(EditSheetDialog.None) }
        )
    }

    if (activeDialog is EditSheetDialog.NavigateDatePicker && onNavigateDate != null) {
        DatePickerDialog(
            initialDate = date,
            onDateSelected = { newDate ->
                onRequestNavigateDate(newDate)
                onDialogChange(EditSheetDialog.None)
            },
            onDismiss = { onDialogChange(EditSheetDialog.None) }
        )
    }
}

@Composable
private fun RowSavingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            Text(text = stringResource(R.string.edit_saved))
        }
    }
}

private fun showMissingPreviousEntryToast(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.edit_toast_no_previous_entry),
        Toast.LENGTH_SHORT
    ).show()
}
