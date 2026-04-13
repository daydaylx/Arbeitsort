package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.repository.WorkEntryRepository
import de.montagezeit.app.data.local.entity.DayType
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.preferences.ReminderSettingsManager
import de.montagezeit.app.diagnostics.AppDiagnosticsRuntime
import de.montagezeit.app.diagnostics.DiagnosticCategory
import de.montagezeit.app.diagnostics.DiagnosticTraceRequest
import de.montagezeit.app.diagnostics.redactedTextSummary
import de.montagezeit.app.diagnostics.toSanitizedDiagnosticPayload
import de.montagezeit.app.domain.util.MealAllowanceCalculator
import de.montagezeit.app.domain.util.resolveWorkScheduleDefaults
import kotlinx.coroutines.flow.first
import java.time.LocalDate

data class DailyManualCheckInInput(
    val date: LocalDate,
    val dayLocationLabel: String,
    val isArrivalDeparture: Boolean = false,
    val breakfastIncluded: Boolean = false
)

/**
 * Speichert einen einmaligen manuellen Tages-Check-in inkl. Tagesabschluss.
 */
class RecordDailyManualCheckIn(
    private val workEntryDao: WorkEntryRepository,
    private val reminderSettingsManager: ReminderSettingsManager
) {

    companion object {
        private const val CONFIRMATION_SOURCE_UI = "UI"
    }

    suspend operator fun invoke(input: DailyManualCheckInInput): WorkEntry {
        val trace = AppDiagnosticsRuntime.startTrace(
            DiagnosticTraceRequest(
                category = DiagnosticCategory.STATE_MUTATION,
                name = "daily_manual_check_in",
                sourceClass = "RecordDailyManualCheckIn",
                screenOrWorker = "TodayScreen",
                entityDate = input.date,
                payload = mapOf(
                    "dayLocation" to redactedTextSummary(input.dayLocationLabel),
                    "isArrivalDeparture" to input.isArrivalDeparture,
                    "breakfastIncluded" to input.breakfastIncluded
                )
            )
        )
        val resolvedLabel = input.dayLocationLabel.trim()
        if (resolvedLabel.isEmpty()) {
            val exception = IllegalArgumentException("dayLocationLabel darf nicht leer sein")
            trace.error("daily_manual_check_in_invalid_label", exception)
            trace.finish(status = de.montagezeit.app.diagnostics.DiagnosticStatus.ERROR)
            throw exception
        }

        val now = System.currentTimeMillis()
        val settings = reminderSettingsManager.settings.first()
        val defaults = resolveWorkScheduleDefaults(settings)
        trace.event(
            name = "manual_check_in_context",
            payload = mapOf(
                "defaultWorkStart" to defaults.workStart.toString(),
                "defaultWorkEnd" to defaults.workEnd.toString(),
                "defaultBreakMinutes" to defaults.breakMinutes
            )
        )

        val mealResult = MealAllowanceCalculator.calculate(
            dayType = DayType.WORK,
            isArrivalDeparture = input.isArrivalDeparture,
            breakfastIncluded = input.breakfastIncluded
        )
        trace.event(
            name = "manual_check_in_meal_calculated",
            payload = mapOf(
                "baseCents" to mealResult.baseCents,
                "amountCents" to mealResult.amountCents
            )
        )

        var result: WorkEntry? = null
        try {
            workEntryDao.readModifyWrite(input.date) { existingEntry ->
                trace.event(
                    name = "manual_check_in_existing_entry",
                    payload = mapOf(
                        "existingEntry" to existingEntry?.toSanitizedDiagnosticPayload()
                    )
                )
                val updatedEntry = if (existingEntry != null) {
                    val hasAnyWorkTime = existingEntry.workStart != null || existingEntry.workEnd != null
                    existingEntry.copy(
                        dayType = DayType.WORK,
                        workStart = existingEntry.workStart ?: if (!hasAnyWorkTime) defaults.workStart else null,
                        workEnd = existingEntry.workEnd ?: if (!hasAnyWorkTime) defaults.workEnd else null,
                        breakMinutes = if (hasAnyWorkTime) existingEntry.breakMinutes else defaults.breakMinutes,
                        dayLocationLabel = resolvedLabel,
                        mealIsArrivalDeparture = input.isArrivalDeparture,
                        mealBreakfastIncluded = input.breakfastIncluded,
                        mealAllowanceBaseCents = mealResult.baseCents,
                        mealAllowanceAmountCents = mealResult.amountCents,
                        confirmedWorkDay = true,
                        confirmationAt = now,
                        confirmationSource = CONFIRMATION_SOURCE_UI,
                        morningCapturedAt = existingEntry.morningCapturedAt ?: now,
                        eveningCapturedAt = existingEntry.eveningCapturedAt ?: now,
                        updatedAt = now
                    )
                } else {
                    WorkEntryFactory.createDefaultEntry(
                        date = input.date,
                        settings = settings,
                        dayType = DayType.WORK,
                        dayLocationLabel = resolvedLabel,
                        now = now
                    ).copy(
                        mealIsArrivalDeparture = input.isArrivalDeparture,
                        mealBreakfastIncluded = input.breakfastIncluded,
                        mealAllowanceBaseCents = mealResult.baseCents,
                        mealAllowanceAmountCents = mealResult.amountCents,
                        confirmedWorkDay = true,
                        confirmationAt = now,
                        confirmationSource = CONFIRMATION_SOURCE_UI,
                        morningCapturedAt = now,
                        eveningCapturedAt = now
                    )
                }
                result = updatedEntry
                updatedEntry
            }
        } catch (e: Exception) {
            trace.error(
                name = "daily_manual_check_in_failed",
                throwable = e
            )
            trace.finish(status = de.montagezeit.app.diagnostics.DiagnosticStatus.ERROR)
            throw e
        }
        return requireNotNull(result) { "readModifyWrite hat kein Ergebnis geliefert" }.also { updated ->
            trace.finish(
                payload = mapOf(
                    "resultEntry" to updated.toSanitizedDiagnosticPayload()
                )
            )
        }
    }
}
