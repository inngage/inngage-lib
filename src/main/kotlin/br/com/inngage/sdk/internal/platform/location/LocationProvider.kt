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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

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
     *
     * Never throws and never blocks indefinitely: any failure obtaining the
     * location — missing permission, Play Services error, or no fix within
     * [LOCATION_TIMEOUT_MS] — resolves to `null` so the caller can proceed
     * (e.g. subscribe without `lat`/`long`) instead of failing the whole flow.
     */
    suspend fun getLocation(): GeoLocation? {
        if (!hasLocationPermission()) {
            Log.w(tag, "Location permission not granted — skipping geo-location")
            return null
        }
        return runCatching {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                getLastKnownLocation() ?: requestFreshLocation()
            }
        }.getOrElse { e ->
            Log.w(tag, "Failed to obtain location — skipping geo-location: ${e.message}")
            null
        }
    }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private suspend fun getLastKnownLocation(): GeoLocation? = suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (cont.isActive) cont.resume(location?.let { GeoLocation(it.latitude, it.longitude) })
            }
            .addOnFailureListener { e ->
                Log.e(tag, "getLastLocation failed: ${e.message}")
                if (cont.isActive) cont.resume(null)
            }
    }

    @Suppress("MissingPermission")
    private suspend fun requestFreshLocation(): GeoLocation? = suspendCancellableCoroutine { cont ->
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedClient.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (loc == null) Log.w(tag, "Fresh location result was null")
                if (cont.isActive) cont.resume(loc?.let { GeoLocation(it.latitude, it.longitude) })
            }
        }

        // Ensure the update callback is torn down if we time out / get cancelled.
        cont.invokeOnCancellation { fusedClient.removeLocationUpdates(callback) }
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private companion object {
        /** Upper bound for a single location fetch; on expiry we subscribe without coordinates. */
        const val LOCATION_TIMEOUT_MS = 5_000L
    }
}

