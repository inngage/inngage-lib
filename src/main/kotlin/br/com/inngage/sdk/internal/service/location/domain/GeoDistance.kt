package br.com.inngage.sdk.internal.service.location.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Great-circle distance between two coordinates (haversine formula).
 * Pure Kotlin so the domain stays free of `android.location.Location`.
 */
internal object GeoDistance {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun metersBetween(a: Coordinates, b: Coordinates): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val h = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h))
    }
}
