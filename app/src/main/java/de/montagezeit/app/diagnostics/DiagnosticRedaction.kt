@file:Suppress("MagicNumber")
package de.montagezeit.app.diagnostics

import de.montagezeit.app.data.local.entity.TravelLeg
import de.montagezeit.app.data.local.entity.WorkEntry
import de.montagezeit.app.data.local.entity.WorkEntryWithTravelLegs
import java.security.MessageDigest

fun Throwable.toDiagnosticPayload(): Map<String, Any?> = mapOf(
    "type" to javaClass.name,
    "message" to message
)

fun WorkEntry.toSanitizedDiagnosticPayload(): Map<String, Any?> = mapOf(
    "date" to date.toString(),
    "dayType" to dayType.name,
    "workStart" to workStart?.toString(),
    "workEnd" to workEnd?.toString(),
    "breakMinutes" to breakMinutes,
    "confirmedWorkDay" to confirmedWorkDay,
    "confirmationSource" to confirmationSource,
    "morningCapturedAt" to morningCapturedAt,
    "eveningCapturedAt" to eveningCapturedAt,
    "mealIsArrivalDeparture" to mealIsArrivalDeparture,
    "mealBreakfastIncluded" to mealBreakfastIncluded,
    "mealAllowanceBaseCents" to mealAllowanceBaseCents,
    "mealAllowanceAmountCents" to mealAllowanceAmountCents,
    "dayLocation" to redactedTextSummary(dayLocationLabel),
    "note" to redactedTextSummary(note),
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

fun TravelLeg.toSanitizedDiagnosticPayload(): Map<String, Any?> = mapOf(
    "id" to id,
    "workEntryDate" to workEntryDate.toString(),
    "sortOrder" to sortOrder,
    "category" to category.name,
    "startAt" to startAt,
    "arriveAt" to arriveAt,
    "paidMinutesOverride" to paidMinutesOverride,
    "source" to source?.name,
    "startLabel" to redactedTextSummary(startLabel),
    "endLabel" to redactedTextSummary(endLabel),
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

fun WorkEntryWithTravelLegs.toSanitizedDiagnosticPayload(): Map<String, Any?> = mapOf(
    "workEntry" to workEntry.toSanitizedDiagnosticPayload(),
    "travelLegCount" to orderedTravelLegs.size,
    "travelLegs" to orderedTravelLegs.map(TravelLeg::toSanitizedDiagnosticPayload)
)

fun redactedTextSummary(value: String?): Map<String, Any?> {
    val normalized = value?.trim().orEmpty()
    if (normalized.isBlank()) {
        return mapOf(
            "present" to false,
            "length" to 0,
            "sha256_12" to null
        )
    }
    return mapOf(
        "present" to true,
        "length" to normalized.length,
        "sha256_12" to normalized.sha256Prefix()
    )
}

private fun String.sha256Prefix(length: Int = 12): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return buildString(digest.size * 2) {
        digest.forEach { byte ->
            append(((byte.toInt() ushr 4) and 0xF).toString(16))
            append((byte.toInt() and 0xF).toString(16))
        }
    }.take(length)
}
