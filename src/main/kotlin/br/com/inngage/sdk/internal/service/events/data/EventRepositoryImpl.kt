package br.com.inngage.sdk.internal.service.events.data

import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.service.events.domain.EventEntity
import br.com.inngage.sdk.internal.service.events.domain.EventRepository
import org.json.JSONObject

/**
 * HTTP-backed implementation of [EventRepository].
 *
 * Builds the `newEventRequest` JSON payload:
 * - `app_token` and `event_name` are always sent.
 * - `identifier` is sent when present; otherwise `registration` (FCM token) is sent.
 * - `event_values` is sent only when provided.
 * - `conversion_event` is always sent (`false` by default);
 *   `conversion_value` and `conversion_notid` are sent only when it is `true`.
 */
internal class EventRepositoryImpl(
    private val httpClient: HttpClient = HttpClient()
) : EventRepository {

    override suspend fun sendEvent(entity: EventEntity): Result<Unit> {
        val body = JSONObject().apply {
            put("app_token", entity.appToken)
            put("event_name", entity.eventName)
            entity.identifier?.let { put("identifier", it) }
                ?: put("registration", entity.registration)
            entity.eventValues?.let { put("event_values", it) }
            put("conversion_event", entity.conversionEvent)
            if (entity.conversionEvent) {
                entity.conversionValue?.let { put("conversion_value", it) }
                entity.conversionNotid?.let { put("conversion_notid", it) }
            }
        }
        val payload = JSONObject().put("newEventRequest", body)
        return httpClient.post(payload, InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_EVENTS)
            .map { /* discard body */ }
    }
}
