package br.com.inngage.sdk.internal.platform.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import br.com.inngage.sdk.internal.core.config.InngageConfig
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Domain model for a geographic coordinate pair.
 */
internal data class GeoLocation(val lat: Double, val lon: Double)

/**
 * Retrieves the device's current location using [FusedLocationProviderClient].
 *
 * Returns `null` if:
 * - Location permission is not granted.
 * - No cached or active location is available within the timeout.
 *
 * Platform-specific — lives exclusively in `platform/location/`.
 */
internal class LocationProvider(private val context: Context) {

    private val tag = InngageConfig.TAG
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    /**
     * Attempts to return the device's last known or freshly requested location.
     * Returns `null` when permission is missing or location is unavailable.
     */
    suspend fun getLocation(): GeoLocation? {
        if (!hasLocationPermission()) {
            Log.w(tag, "Location permission not granted — skipping geo-location")
            return null
        }
        return getLastKnownLocation() ?: requestFreshLocation()
    }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private suspend fun getLastKnownLocation(): GeoLocation? = suspendCoroutine { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                cont.resume(location?.let { GeoLocation(it.latitude, it.longitude) })
            }
            .addOnFailureListener { e ->
                Log.e(tag, "getLastLocation failed: ${e.message}")
                cont.resume(null)
            }
    }

    @Suppress("MissingPermission")
    private suspend fun requestFreshLocation(): GeoLocation? = suspendCoroutine { cont ->
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    cont.resume(GeoLocation(loc.latitude, loc.longitude))
                } else {
                    Log.w(tag, "Fresh location result was null")
                    cont.resume(null)
                }
                fusedClient.removeLocationUpdates(this)
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }
}

