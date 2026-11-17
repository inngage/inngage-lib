package br.com.inngage.sdk.internal.service.location.data

import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.core.http.HttpException
import br.com.inngage.sdk.internal.service.location.domain.LocationFailure
import br.com.inngage.sdk.internal.service.location.domain.LocationRepository
import br.com.inngage.sdk.internal.service.location.domain.LocationUpdateEntity
import br.com.inngage.sdk.internal.service.location.domain.LocationUpdateResult
import org.json.JSONObject

/**
 * HTTP-backed implementation of [LocationRepository].
 *
 * Posts a flat JSON body to [InngageConfig.PATH_SUBSCRIPTION_LOCATION]:
 * `{ app_token, identifier?, registration?, uuid?, lat, long }`.
 * Coordinates are sent as numbers. Null identifiers are omitted.
 */
internal class LocationRepositoryImpl(
    private val httpClient: HttpClient = HttpClient()
) : LocationRepository {

    private val tag = InngageConfig.TAG

    override suspend fun sendLocation(entity: LocationUpdateEntity): Result<LocationUpdateResult> {
        val body = JSONObject().apply {
            put("app_token", entity.appToken)
            entity.identifier?.let { put("identifier", it) }
            entity.registration?.let { put("registration", it) }
            entity.uuid?.let { put("uuid", it) }
            put("lat", entity.coordinates.latitude)
            put("long", entity.coordinates.longitude)
        }
        val endpoint = InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_SUBSCRIPTION_LOCATION

        return httpClient.post(body, endpoint).fold(
            onSuccess = { responseBody -> Result.success(parseOutcome(responseBody)) },
            onFailure = { error -> Result.failure(mapFailure(error)) }
        )
    }

    private fun parseOutcome(responseBody: String): LocationUpdateResult {
        val recorded = runCatching { JSONObject(responseBody).optBoolean("recorded", true) }
            .getOrElse { true }
        return if (recorded) LocationUpdateResult.Recorded else LocationUpdateResult.Unchanged
    }

    private fun mapFailure(error: Throwable): LocationFailure {
        if (error !is HttpException) {
            return LocationFailure.Network(error.message ?: "network error", error)
        }
        val message = runCatching { JSONObject(error.body).optString("message") }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: error.message ?: "HTTP ${error.code}"
        Log.w(tag, "Location update rejected [${error.code}]: $message")
        return when (error.code) {
            400 -> LocationFailure.BadRequest(message)
            401 -> LocationFailure.Unauthorized(message)
            404 -> LocationFailure.SubscriberNotFound(message)
            else -> LocationFailure.Network(message, error)
        }
    }
}
