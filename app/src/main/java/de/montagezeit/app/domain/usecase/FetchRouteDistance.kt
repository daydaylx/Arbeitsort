package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.RouteCacheDao
import de.montagezeit.app.data.local.entity.RouteCacheEntry
import de.montagezeit.app.data.local.entity.TravelSource
import de.montagezeit.app.data.network.DistanceResult
import de.montagezeit.app.data.network.DistanceService
import java.util.Locale

class FetchRouteDistance(
    private val distanceService: DistanceService,
    private val routeCacheDao: RouteCacheDao
) {

    sealed class Result {
        data class Success(val distanceKm: Double, val source: TravelSource) : Result()
        data class Error(val reason: ErrorReason) : Result()
    }

    enum class ErrorReason {
        NETWORK,
        GEOCODE_FAILED,
        API_FAILED
    }

    suspend operator fun invoke(fromLabel: String, toLabel: String): Result {
        val normalizedFrom = normalizeLabel(fromLabel)
        val normalizedTo = normalizeLabel(toLabel)
        if (normalizedFrom.isBlank() || normalizedTo.isBlank()) {
            return Result.Error(ErrorReason.GEOCODE_FAILED)
        }

        val cached = routeCacheDao.getEntry(normalizedFrom, normalizedTo)
        if (cached != null) {
            return Result.Success(cached.distanceKm, TravelSource.ROUTED)
        }

        return when (val distanceResult = distanceService.fetchRouteDistanceMeters(fromLabel, toLabel)) {
            is DistanceResult.Success -> {
                val distanceKm = distanceResult.distanceMeters / 1000.0
                val entry = RouteCacheEntry(
                    fromLabel = normalizedFrom,
                    toLabel = normalizedTo,
                    distanceKm = distanceKm,
                    updatedAt = System.currentTimeMillis()
                )
                routeCacheDao.upsert(entry)
                Result.Success(distanceKm, TravelSource.ROUTED)
            }
            DistanceResult.NetworkError -> Result.Error(ErrorReason.NETWORK)
            DistanceResult.GeocodeFailed -> Result.Error(ErrorReason.GEOCODE_FAILED)
            DistanceResult.ApiError -> Result.Error(ErrorReason.API_FAILED)
        }
    }

    private fun normalizeLabel(label: String): String {
        return label.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }
}
