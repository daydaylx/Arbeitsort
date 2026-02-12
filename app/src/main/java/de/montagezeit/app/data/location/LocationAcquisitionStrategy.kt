package de.montagezeit.app.data.location

import de.montagezeit.app.domain.model.LocationResult

object LocationAcquisitionStrategy {
    private const val MAX_ALLOWED_ACCURACY_METERS = 3000.0f
    private const val GOOD_STAGE_ONE_ACCURACY_METERS = 1200.0f
    private const val GOOD_LAST_KNOWN_ACCURACY_METERS = 1500.0f
    private const val STAGE_ONE_MAX_TIMEOUT_MS = 7000L
    private const val STAGE_ONE_MIN_TIMEOUT_MS = 2500L
    private const val MIN_STAGE_TWO_TIMEOUT_MS = 3000L
    private const val MIN_REQUEST_TIMEOUT_MS = 1000L

    fun normalizeTotalTimeout(timeoutMs: Long): Long {
        return timeoutMs.coerceAtLeast(MIN_REQUEST_TIMEOUT_MS)
    }

    fun stageOneTimeout(totalTimeoutMs: Long): Long {
        return minOf(
            STAGE_ONE_MAX_TIMEOUT_MS,
            maxOf(STAGE_ONE_MIN_TIMEOUT_MS, totalTimeoutMs / 2),
            totalTimeoutMs
        )
    }

    fun shouldUseLastKnown(lastKnown: LocationResult?): Boolean {
        val success = lastKnown as? LocationResult.Success ?: return false
        return success.accuracyMeters <= GOOD_LAST_KNOWN_ACCURACY_METERS
    }

    fun shouldTryHighAccuracy(result: LocationResult): Boolean {
        return when (result) {
            is LocationResult.Success -> result.accuracyMeters > GOOD_STAGE_ONE_ACCURACY_METERS
            is LocationResult.LowAccuracy -> true
            LocationResult.Timeout -> true
            LocationResult.Unavailable -> true
            LocationResult.SkippedByUser -> false
        }
    }

    fun hasEnoughTimeForStageTwo(remainingTimeoutMs: Long): Boolean {
        return remainingTimeoutMs >= MIN_STAGE_TWO_TIMEOUT_MS
    }

    fun chooseBetterResult(stageOne: LocationResult, stageTwo: LocationResult): LocationResult {
        return when {
            stageTwo is LocationResult.Success -> stageTwo
            stageOne is LocationResult.Success -> stageOne
            stageTwo is LocationResult.LowAccuracy && stageOne is LocationResult.LowAccuracy ->
                if (stageTwo.accuracyMeters <= stageOne.accuracyMeters) stageTwo else stageOne
            stageTwo is LocationResult.LowAccuracy -> stageTwo
            stageOne is LocationResult.LowAccuracy -> stageOne
            stageOne == LocationResult.Unavailable -> stageOne
            else -> stageTwo
        }
    }

    fun toLocationResult(lat: Double, lon: Double, accuracyMeters: Float): LocationResult {
        return if (accuracyMeters > MAX_ALLOWED_ACCURACY_METERS) {
            LocationResult.LowAccuracy(accuracyMeters)
        } else {
            LocationResult.Success(
                lat = lat,
                lon = lon,
                accuracyMeters = accuracyMeters
            )
        }
    }
}
