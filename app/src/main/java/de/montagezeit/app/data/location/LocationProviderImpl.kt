package de.montagezeit.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import de.montagezeit.app.domain.model.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementierung von LocationProvider mittels FusedLocationProviderClient
 * 
 * Best-effort COARSE location mit Timeout-Handling
 */
class LocationProviderImpl(
    private val context: Context
) : LocationProvider {
    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    /**
     * Ermittelt den aktuellen Standort mit COARSE location
     * 
     * @param timeoutMs Timeout in Millisekunden (empfohlen: 10000-15000)
     * @return LocationResult
     */
    @SuppressLint("MissingPermission") // Berechtigungen werden geprüft
    override suspend fun getCurrentLocation(timeoutMs: Long): LocationResult {
        // Prüfen ob Location-Berechtigung vorhanden ist
        if (!hasLocationPermission()) {
            return LocationResult.Unavailable
        }
        
        // Prüfen ob GPS eingeschaltet ist
        if (!isLocationEnabled()) {
            return LocationResult.Unavailable
        }
        
        val cancellationTokenSource = CancellationTokenSource()
        
        return try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_LOW_POWER,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val result = processLocation(location)
                        continuation.resume(result)
                    } else {
                        continuation.resume(LocationResult.Unavailable)
                    }
                }.addOnFailureListener { exception ->
                    // Timeout oder anderer Fehler
                    continuation.resume(LocationResult.Timeout)
                }.addOnCanceledListener {
                    continuation.resume(LocationResult.Timeout)
                }
            }
        } catch (e: Exception) {
            LocationResult.Timeout
        }
    }
    
    /**
     * Verarbeitet eine empfangene Location und prüft Genauigkeit
     */
    private fun processLocation(location: Location): LocationResult {
        val accuracy = location.accuracy
        
        // Accuracy Rules aus Spec
        return when {
            accuracy > 3000.0f -> {
                // Accuracy zu niedrig
                LocationResult.LowAccuracy(accuracy)
            }
            else -> {
                // OK
                LocationResult.Success(
                    lat = location.latitude,
                    lon = location.longitude,
                    accuracyMeters = accuracy
                )
            }
        }
    }
    
    /**
     * Prüft ob Location-Berechtigung vorhanden ist
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Prüft ob Location Provider aktiviert ist (GPS/Network)
     */
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
