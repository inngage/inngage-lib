package br.com.inngage.sdk.internal.service.location.application

import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.util.Validators
import br.com.inngage.sdk.internal.service.location.domain.Coordinates
import br.com.inngage.sdk.internal.service.location.domain.GeoDistance
import br.com.inngage.sdk.internal.service.location.domain.LocationCache
import br.com.inngage.sdk.internal.service.location.domain.LocationFailure
import br.com.inngage.sdk.internal.service.location.domain.LocationRepository
import br.com.inngage.sdk.internal.service.location.domain.LocationUpdateEntity
import br.com.inngage.sdk.internal.service.location.domain.LocationUpdateResult
import br.com.inngage.sdk.internal.service.location.domain.SentLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sends the subscriber's position to `/v4/subscription/location`.
 *
 * Flow:
 * 1. Validates [appToken].
 * 2. Resolves the coordinates: the explicit pair when given, otherwise
 *    [deviceLocationProvider] — `null` means "nothing to send" and resolves
 *    to [LocationUpdateResult.Unavailable] (not a failure).
 * 3. Resolves the subscriber identity (`identifier` → persisted identifier,
 *    plus persisted FCM `registration` and installation `uuid`).
 * 4. Skips the request when the position is within
 *    [InngageConfig.LOCATION_THROTTLE_METERS] of the last accepted one and
 *    was sent less than [InngageConfig.LOCATION_THROTTLE_MS] ago — the
 *    backend would answer `recorded: false` anyway.
 * 5. Posts and, on success, remembers the position in [cache].
 *
 * @param repository             Data layer — injected for testability.
 * @param cache                  Last accepted position, for the local throttle.
 * @param deviceLocationProvider Reads the device position; must never throw.
 * @param identifierProvider     Identifier persisted by the last `subscribe()`.
 * @param registrationProvider   FCM token persisted by the last `subscribe()`.
 * @param uuidProvider           Per-installation UUID.
 * @param clock                  Current time in millis — injected for testability.
 * @param dispatcher             I/O dispatcher — injected for testability.
 */
internal class UpdateLocationUseCase(
    private val repository: LocationRepository,
    private val cache: LocationCache,
    private val deviceLocationProvider: suspend () -> Coordinates?,
    private val identifierProvider: () -> String = { "" },
    private val registrationProvider: () -> String = { "" },
    private val uuidProvider: () -> String = { "" },
    private val clock: () -> Long = System::currentTimeMillis,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val tag = InngageConfig.TAG

    /**
     * @param appToken   SDK application token (required).
     * @param identifier Optional user identifier; falls back to the persisted one.
     * @param latitude   Optional explicit latitude (-90..90); requires [longitude].
     * @param longitude  Optional explicit longitude (-180..180); requires [latitude].
     */
    suspend fun execute(
        appToken: String,
        identifier: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<LocationUpdateResult> = withContext(dispatcher) {
        if (!Validators.isValidAppToken(appToken)) {
            return@withContext fail(LocationFailure.InvalidAppToken(InngageConfig.INVALID_APP_TOKEN))
        }

        // 2 — Coordinates
        val coordinates: Coordinates = when {
            latitude != null && longitude != null -> Coordinates(latitude, longitude)
            latitude != null || longitude != null -> return@withContext fail(
                LocationFailure.InvalidCoordinates("latitude and longitude must be provided together")
            )
            else -> runCatching { deviceLocationProvider() }.getOrNull() ?: run {
                Log.d(tag, "updateLocation: no device position available — skipping")
                return@withContext Result.success(LocationUpdateResult.Unavailable)
            }
        }
        if (!coordinates.isValid) {
            return@withContext fail(
                LocationFailure.InvalidCoordinates(
                    "lat must be between -90 and 90 and long between -180 and 180"
                )
            )
        }

        // 3 — Identity
        val resolvedIdentifier = identifier?.takeIf { it.isNotBlank() }
            ?: identifierProvider().takeIf { it.isNotBlank() }
        val registration = registrationProvider().takeIf { it.isNotBlank() }
        val uuid = uuidProvider().takeIf { it.isNotBlank() }
        if (resolvedIdentifier == null && registration == null && uuid == null) {
            return@withContext fail(
                LocationFailure.MissingIdentity("no identifier, registration or uuid — call subscribe() first")
            )
        }

        // 4 — Local throttle
        val now = clock()
        cache.lastSent()?.let { last ->
            val elapsed = now - last.sentAtMillis
            val moved = GeoDistance.metersBetween(last.coordinates, coordinates)
            if (elapsed in 0 until InngageConfig.LOCATION_THROTTLE_MS &&
                moved < InngageConfig.LOCATION_THROTTLE_METERS
            ) {
                Log.d(tag, "updateLocation: throttled (moved ${moved.toInt()} m, ${elapsed / 1000} s ago)")
                return@withContext Result.success(LocationUpdateResult.Throttled)
            }
        }

        // 5 — Send
        val entity = LocationUpdateEntity(
            appToken     = appToken,
            identifier   = resolvedIdentifier,
            registration = registration,
            uuid         = uuid,
            coordinates  = coordinates
        )
        repository.sendLocation(entity).also { result ->
            result.onSuccess {
                cache.saveLastSent(SentLocation(coordinates, now))
                Log.d(tag, "updateLocation: $it")
            }
            result.onFailure { Log.e(tag, "updateLocation failed: ${it.message}") }
        }
    }

    private fun fail(failure: LocationFailure): Result<LocationUpdateResult> {
        Log.w(tag, "updateLocation: ${failure.message}")
        return Result.failure(failure)
    }
}
