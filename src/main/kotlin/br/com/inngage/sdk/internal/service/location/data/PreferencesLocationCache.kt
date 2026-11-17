package br.com.inngage.sdk.internal.service.location.data

import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import br.com.inngage.sdk.internal.service.location.domain.Coordinates
import br.com.inngage.sdk.internal.service.location.domain.LocationCache
import br.com.inngage.sdk.internal.service.location.domain.SentLocation

/**
 * [LocationCache] persisted in [PreferencesStorage] so the throttle survives
 * process restarts (the periodic worker runs in a fresh process).
 */
internal class PreferencesLocationCache(
    private val storage: PreferencesStorage
) : LocationCache {

    override fun lastSent(): SentLocation? {
        val lat = storage.getString(InngageConfig.PREF_LAST_LOCATION_LAT).toDoubleOrNull() ?: return null
        val lon = storage.getString(InngageConfig.PREF_LAST_LOCATION_LONG).toDoubleOrNull() ?: return null
        val at  = storage.getLong(InngageConfig.PREF_LAST_LOCATION_AT, -1L)
        if (at < 0) return null
        return SentLocation(Coordinates(lat, lon), at)
    }

    override fun saveLastSent(location: SentLocation) {
        storage.putString(InngageConfig.PREF_LAST_LOCATION_LAT, location.coordinates.latitude.toString())
        storage.putString(InngageConfig.PREF_LAST_LOCATION_LONG, location.coordinates.longitude.toString())
        storage.putLong(InngageConfig.PREF_LAST_LOCATION_AT, location.sentAtMillis)
    }

    override fun clear() {
        storage.remove(InngageConfig.PREF_LAST_LOCATION_LAT)
        storage.remove(InngageConfig.PREF_LAST_LOCATION_LONG)
        storage.remove(InngageConfig.PREF_LAST_LOCATION_AT)
    }
}
