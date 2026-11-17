package br.com.inngage.sdk.internal.api

import android.content.Context
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.orchestration.SdkInitializer

/**
 * Public facade for subscriber location updates (`/v4/subscription/location`).
 * Wraps [SdkInitializer] — consumers call this through
 * [br.com.inngage.sdk.InngageClient].
 */
internal object InngageLocation {

    @JvmStatic
    @JvmOverloads
    fun updateLocation(
        context: Context,
        appToken: String,
        identifier: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        SdkInitializer.updateLocation(context, appToken, identifier, latitude, longitude)
    }

    @JvmStatic
    @JvmOverloads
    fun startLocationUpdates(
        context: Context,
        appToken: String,
        identifier: String? = null,
        intervalMinutes: Long = InngageConfig.LOCATION_DEFAULT_INTERVAL_MINUTES
    ) {
        SdkInitializer.startLocationUpdates(context, appToken, identifier, intervalMinutes)
    }

    @JvmStatic
    fun stopLocationUpdates(context: Context) {
        SdkInitializer.stopLocationUpdates(context)
    }
}
