package de.montagezeit.app.data.network

import com.squareup.moshi.Moshi
import de.montagezeit.app.data.preferences.RoutingSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

class OpenRouteServiceDistanceService @Inject constructor(
    private val routingSettingsManager: RoutingSettingsManager,
    private val okHttpClient: OkHttpClient,
    moshi: Moshi
) : DistanceService {

    private val geocodeAdapter = moshi.adapter(GeocodeResponse::class.java)
    private val routeAdapter = moshi.adapter(RouteResponse::class.java)

    override suspend fun fetchRouteDistanceMeters(
        fromLabel: String,
        toLabel: String
    ): DistanceResult = withContext(Dispatchers.IO) {
        val apiKey = routingSettingsManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext DistanceResult.ApiError
        }

        val fromCoordinates = when (val result = geocodeAddress(fromLabel, apiKey)) {
            is GeocodeResult.Success -> result.coordinates
            GeocodeResult.NetworkError -> return@withContext DistanceResult.NetworkError
            GeocodeResult.ApiError -> return@withContext DistanceResult.ApiError
            GeocodeResult.NoResults -> return@withContext DistanceResult.GeocodeFailed
        }
        val toCoordinates = when (val result = geocodeAddress(toLabel, apiKey)) {
            is GeocodeResult.Success -> result.coordinates
            GeocodeResult.NetworkError -> return@withContext DistanceResult.NetworkError
            GeocodeResult.ApiError -> return@withContext DistanceResult.ApiError
            GeocodeResult.NoResults -> return@withContext DistanceResult.GeocodeFailed
        }

        return@withContext fetchRouteDistance(fromCoordinates, toCoordinates, apiKey)
    }

    private fun geocodeAddress(label: String, apiKey: String): GeocodeResult {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("api.openrouteservice.org")
            .addPathSegments("geocode/search")
            .addQueryParameter("text", label)
            .addQueryParameter("size", "1")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", apiKey)
            .build()

        return when (val result = executeRequest(request)) {
            is NetworkCallResult.Success -> {
                val response = geocodeAdapter.fromJson(result.body)
                val feature = response?.features?.firstOrNull()
                val coords = feature?.geometry?.coordinates
                if (coords == null || coords.size < 2) {
                    GeocodeResult.NoResults
                } else {
                    GeocodeResult.Success(Coordinates(lat = coords[1], lon = coords[0]))
                }
            }
            NetworkCallResult.NetworkError -> GeocodeResult.NetworkError
            NetworkCallResult.ApiError -> GeocodeResult.ApiError
        }
    }

    private fun fetchRouteDistance(
        from: Coordinates,
        to: Coordinates,
        apiKey: String
    ): DistanceResult {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("api.openrouteservice.org")
            .addPathSegments("v2/directions/driving-car")
            .addQueryParameter("start", "${from.lon},${from.lat}")
            .addQueryParameter("end", "${to.lon},${to.lat}")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", apiKey)
            .build()

        return when (val result = executeRequest(request)) {
            is NetworkCallResult.Success -> {
                val response = routeAdapter.fromJson(result.body)
                val distanceMeters = response
                    ?.features
                    ?.firstOrNull()
                    ?.properties
                    ?.segments
                    ?.firstOrNull()
                    ?.distance
                if (distanceMeters == null || distanceMeters <= 0.0) {
                    DistanceResult.ApiError
                } else {
                    DistanceResult.Success(distanceMeters)
                }
            }
            NetworkCallResult.NetworkError -> DistanceResult.NetworkError
            NetworkCallResult.ApiError -> DistanceResult.ApiError
        }
    }

    private fun executeRequest(request: Request): NetworkCallResult {
        var lastException: IOException? = null
        repeat(2) { attempt ->
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        return if (body != null) {
                            NetworkCallResult.Success(body)
                        } else {
                            NetworkCallResult.ApiError
                        }
                    }
                    val shouldRetry = response.code >= 500 && attempt == 0
                    if (!shouldRetry) {
                        return NetworkCallResult.ApiError
                    }
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt == 1) {
                    return NetworkCallResult.NetworkError
                }
            }
        }
        return if (lastException != null) {
            NetworkCallResult.NetworkError
        } else {
            NetworkCallResult.ApiError
        }
    }
}

private data class Coordinates(
    val lat: Double,
    val lon: Double
)

private sealed class GeocodeResult {
    data class Success(val coordinates: Coordinates) : GeocodeResult()
    object NetworkError : GeocodeResult()
    object ApiError : GeocodeResult()
    object NoResults : GeocodeResult()
}

private sealed class NetworkCallResult {
    data class Success(val body: String) : NetworkCallResult()
    object NetworkError : NetworkCallResult()
    object ApiError : NetworkCallResult()
}

private data class GeocodeResponse(
    val features: List<GeocodeFeature> = emptyList()
)

private data class GeocodeFeature(
    val geometry: GeocodeGeometry
)

private data class GeocodeGeometry(
    val coordinates: List<Double>
)

private data class RouteResponse(
    val features: List<RouteFeature> = emptyList()
)

private data class RouteFeature(
    val properties: RouteProperties
)

private data class RouteProperties(
    val segments: List<RouteSegment>
)

private data class RouteSegment(
    val distance: Double
)
