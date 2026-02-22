package de.montagezeit.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.montagezeit.app.R
import de.montagezeit.app.data.local.entity.LocationStatus
import de.montagezeit.app.data.local.entity.WorkEntry

/**
 * Ermittelt den spezifischen Grund warum ein Entry Review benötigt.
 * Gibt dem User klaren Kontext statt generischem "Standort prüfen".
 */
@Composable
fun getReviewReason(entry: WorkEntry?): String {
    if (entry == null || !entry.needsReview) {
        return stringResource(R.string.review_reason_generic)
    }

    val morningLowAccuracy = entry.morningLocationStatus == LocationStatus.LOW_ACCURACY
    val eveningLowAccuracy = entry.eveningLocationStatus == LocationStatus.LOW_ACCURACY

    val morningUnavailable = entry.morningLocationStatus == LocationStatus.UNAVAILABLE && entry.morningCapturedAt != null
    val eveningUnavailable = entry.eveningLocationStatus == LocationStatus.UNAVAILABLE && entry.eveningCapturedAt != null

    return when {
        morningLowAccuracy && eveningLowAccuracy -> stringResource(R.string.review_reason_low_accuracy_both)
        morningLowAccuracy -> stringResource(R.string.review_reason_low_accuracy_morning)
        eveningLowAccuracy -> stringResource(R.string.review_reason_low_accuracy_evening)
        morningUnavailable -> stringResource(R.string.review_reason_unavailable_morning)
        eveningUnavailable -> stringResource(R.string.review_reason_unavailable_evening)
        else -> stringResource(R.string.review_reason_generic)
    }
}
