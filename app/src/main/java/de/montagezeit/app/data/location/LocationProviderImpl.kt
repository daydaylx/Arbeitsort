package de.montagezeit.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import de.montagezeit.app.domain.model.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Implementierung von LocationProvider mittels FusedLocationProviderClient
 *
 * Strategie:
 * 1) Frische Last-Known-Location (wenn brauchbar)
 * 2) Balanced Anfrage (schnell, stromsparend)
 * 3) Optional High-Accuracy Retry (wenn nötig + möglich)
 */
class LocationProviderImpl(
    private val context: Context
) : LocationProvider {

    companion object {
        private const val LAST_KNOWN_MAX_AGE_MS = 2 * 60 * 1000L
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Ermittelt den aktuellen Standort mit abgestufter Best-Effort Strategie.
     *
     * @param timeoutMs Gesamt-Timeout in Millisekunden
     * @return LocationResult
     */
    @SuppressLint("MissingPermission") // Berechtigungen werden vorab geprüft
    override suspend fun getCurrentLocation(timeoutMs: Long): LocationResult {
        if (!hasAnyLocationPermission()) {
            return LocationResult.Unavailable
        }

        if (!isLocationEnabled()) {
            return LocationResult.Unavailable
        }

        val totalTimeout = LocationAcquisitionStrategy.normalizeTotalTimeout(timeoutMs)
        val startedAt = SystemClock.elapsedRealtime()

        val lastKnown = getFreshLastKnownLocation()
        if (lastKnown != null && LocationAcquisitionStrategy.shouldUseLastKnown(lastKnown)) {
            return lastKnown
        }

        val stageOneTimeout = LocationAcquisitionStrategy.stageOneTimeout(totalTimeout)

        val stageOneResult = requestLocation(
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            timeoutMs = stageOneTimeout
        )

        if (!LocationAcquisitionStrategy.shouldTryHighAccuracy(stageOneResult) || !hasFineLocationPermission()) {
            return stageOneResult
        }

        val elapsedMs = SystemClock.elapsedRealtime() - startedAt
        val remainingTimeout = totalTimeout - elapsedMs
        if (!LocationAcquisitionStrategy.hasEnoughTimeForStageTwo(remainingTimeout)) {
            return stageOneResult
        }

        val stageTwoResult = requestLocation(
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            timeoutMs = remainingTimeout
        )
        return LocationAcquisitionStrategy.chooseBetterResult(stageOneResult, stageTwoResult)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLastKnownLocation(): LocationResult? {
        return try {
            val last = suspendCancellableCoroutine<Location?> { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } ?: return null

            if (!isRecent(last)) return null
            processLocation(last)
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLocation(priority: Int, timeoutMs: Long): LocationResult {
        if (timeoutMs <= 0L) return LocationResult.Timeout

        val cancellationTokenSource = CancellationTokenSource()
        return try {
            val location = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    continuation.invokeOnCancellation { cancellationTokenSource.cancel() }

                    fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
                        .addOnSuccessListener { result ->
                            if (continuation.isActive) continuation.resume(result)
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                        .addOnCanceledListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                }
            }

            when {
                location != null -> processLocation(location)
                else -> LocationResult.Timeout
            }
        } catch (e: SecurityException) {
            LocationResult.Unavailable
        } catch (e: kotlinx.coroutines.CancellationException) {
            LocationResult.Timeout
        } catch (e: Exception) {
            LocationResult.Unavailable
        } finally {
            cancellationTokenSource.cancel()
        }
    }

    private fun processLocation(location: Location): LocationResult {
        return LocationAcquisitionStrategy.toLocationResult(
            lat = location.latitude,
            lon = location.longitude,
            accuracyMeters = location.accuracy
        )
    }

    private fun hasAnyLocationPermission(): Boolean {
        return hasFineLocationPermission() || hasCoarseLocationPermission()
    }

    private fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isRecent(location: Location): Boolean {
        val locationRealtimeNanos = location.elapsedRealtimeNanos
        if (locationRealtimeNanos <= 0L) return true
        val ageMs = (SystemClock.elapsedRealtimeNanos() - locationRealtimeNanos) / 1_000_000L
        return ageMs in 0..LAST_KNOWN_MAX_AGE_MS
    }

    private fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        } catch (e: Exception) {
            false
        }
    }
}
