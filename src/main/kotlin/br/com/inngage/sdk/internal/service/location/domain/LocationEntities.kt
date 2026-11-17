package br.com.inngage.sdk.internal.service.location.domain

/**
 * A geographic coordinate pair, independent of any platform location type.
 */
internal data class Coordinates(val latitude: Double, val longitude: Double) {
    /** `true` when both values are inside the ranges accepted by the API. */
    val isValid: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0
}

/**
 * Request sent to `POST /v4/subscription/location`.
 *
 * At least one of [identifier], [registration] or [uuid] must be present —
 * the backend resolves the subscriber in that order.
 */
internal data class LocationUpdateEntity(
    val appToken: String,
    val identifier: String? = null,
    val registration: String? = null,
    val uuid: String? = null,
    val coordinates: Coordinates
)

/**
 * The last position accepted by the backend, kept locally so repeated
 * positions can be skipped without a network call.
 */
internal data class SentLocation(
    val coordinates: Coordinates,
    val sentAtMillis: Long
)

/**
 * Outcome of a location update. Every value is a success from the caller's
 * point of view — nothing needs to be retried.
 */
internal sealed class LocationUpdateResult {
    /** The backend stored the new position. */
    data object Recorded : LocationUpdateResult()

    /** The backend answered `recorded: false` — same place, too soon. */
    data object Unchanged : LocationUpdateResult()

    /** Skipped locally: too close to the last sent position and too soon. */
    data object Throttled : LocationUpdateResult()

    /** No position could be read (permission missing or no fix in time). */
    data object Unavailable : LocationUpdateResult()
}
