package de.montagezeit.app.data.network

sealed class DistanceResult {
    data class Success(val distanceMeters: Double) : DistanceResult()
    object NetworkError : DistanceResult()
    object GeocodeFailed : DistanceResult()
    object ApiError : DistanceResult()
}

interface DistanceService {
    suspend fun fetchRouteDistanceMeters(fromLabel: String, toLabel: String): DistanceResult
}
