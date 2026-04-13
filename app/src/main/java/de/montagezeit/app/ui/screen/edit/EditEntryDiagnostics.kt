package de.montagezeit.app.ui.screen.edit

import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.diagnostics.AppDiagnosticsRuntime
import de.montagezeit.app.diagnostics.DiagnosticCategory
import de.montagezeit.app.diagnostics.DiagnosticPhase
import de.montagezeit.app.diagnostics.DiagnosticSeverity
import de.montagezeit.app.diagnostics.DiagnosticStatus
import de.montagezeit.app.diagnostics.DiagnosticTrace
import de.montagezeit.app.diagnostics.DiagnosticTraceRequest
import de.montagezeit.app.diagnostics.DiagnosticWarningCodes
import de.montagezeit.app.diagnostics.toSanitizedDiagnosticPayload
import java.time.LocalDate
import javax.inject.Inject

class EditEntryDiagnostics @Inject constructor(
    private val draftRules: EditEntryDraftRules
) {
    fun startSaveTrace(
        currentState: EditUiState,
        fallbackDate: LocalDate,
        data: EditFormData,
        isDirty: Boolean
    ): EditEntrySaveTrace {
        val traceDate = when (currentState) {
            is EditUiState.Success -> currentState.entry.date
            is EditUiState.NewEntry -> currentState.date
            else -> fallbackDate
        }
        val trace = AppDiagnosticsRuntime.startTrace(
            DiagnosticTraceRequest(
                category = DiagnosticCategory.STATE_MUTATION,
                name = "edit_entry_save",
                sourceClass = "EditEntryViewModel",
                screenOrWorker = "EditEntrySheet",
                entityDate = traceDate,
                payload = mapOf(
                    "formData" to data.toSanitizedDiagnosticPayload(draftRules),
                    "uiState" to currentState::class.simpleName,
                    "isDirty" to isDirty
                )
            )
        )
        return EditEntrySaveTrace(trace = trace, draftRules = draftRules, data = data)
    }
}

class EditEntrySaveTrace(
    private val trace: DiagnosticTrace,
    private val draftRules: EditEntryDraftRules,
    private val data: EditFormData
) {
    init {
        recordOvernightWarnings()
    }

    fun validationFailed(errors: List<ValidationError>) {
        trace.event(
            name = "edit_validation_failed",
            phase = DiagnosticPhase.ANOMALY,
            severity = DiagnosticSeverity.WARNING,
            payload = mapOf("errors" to errors.map { it.diagnosticCode() })
        )
        errors.forEach { error ->
            trace.warning(
                code = error.diagnosticCode(),
                payload = mapOf("messageRes" to error.messageRes)
            )
        }
        trace.finish(status = DiagnosticStatus.WARNING)
    }

    fun pendingSaveBuilt(pendingSave: EditEntryPendingSave) {
        trace.event(
            name = "edit_pending_save_built",
            payload = mapOf(
                "entry" to pendingSave.entry.toSanitizedDiagnosticPayload(),
                "travelLegs" to pendingSave.legs.map(TravelLeg::toSanitizedDiagnosticPayload),
                "mealAllowanceAmountCents" to pendingSave.entry.mealAllowanceAmountCents
            )
        )
        if (pendingSave.entry.dayType == de.montagezeit.app.data.local.entity.DayType.WORK &&
            pendingSave.entry.workStart != null &&
            pendingSave.entry.workEnd != null &&
            pendingSave.entry.breakMinutes > draftRules.calculateEffectiveWorkMinutes(data) + data.breakMinutes
        ) {
            trace.warning(
                DiagnosticWarningCodes.BREAK_EXCEEDS_DURATION,
                payload = mapOf("breakMinutes" to pendingSave.entry.breakMinutes)
            )
        }
    }

    fun saveSucceeded(pendingSave: EditEntryPendingSave) {
        trace.finish(
            payload = mapOf(
                "savedEntry" to pendingSave.entry.toSanitizedDiagnosticPayload(),
                "savedTravelLegs" to pendingSave.legs.map(TravelLeg::toSanitizedDiagnosticPayload)
            )
        )
    }

    fun saveFailed(throwable: Throwable) {
        trace.error(
            name = "edit_entry_save_failed",
            throwable = throwable,
            payload = mapOf("formData" to data.toSanitizedDiagnosticPayload(draftRules))
        )
        trace.finish(status = DiagnosticStatus.ERROR)
    }

    private fun recordOvernightWarnings() {
        if (data.dayType == de.montagezeit.app.data.local.entity.DayType.WORK &&
            data.hasWorkTimes &&
            data.workEnd.isBefore(data.workStart)
        ) {
            trace.warning(
                DiagnosticWarningCodes.WORK_DURATION_OVERNIGHT_NORMALIZED,
                payload = mapOf(
                    "workStart" to data.workStart.toString(),
                    "workEnd" to data.workEnd.toString()
                )
            )
        }
        draftRules.normalizedTravelLegs(data)
            .filter { leg ->
                leg.startTime != null &&
                    leg.arriveTime != null &&
                    requireNotNull(leg.arriveTime).isBefore(requireNotNull(leg.startTime))
            }
            .forEachIndexed { index, leg ->
                trace.warning(
                    DiagnosticWarningCodes.TRAVEL_DURATION_OVERNIGHT_NORMALIZED,
                    payload = mapOf(
                        "index" to index,
                        "startTime" to leg.startTime?.toString(),
                        "arriveTime" to leg.arriveTime?.toString()
                    )
                )
            }
    }
}
