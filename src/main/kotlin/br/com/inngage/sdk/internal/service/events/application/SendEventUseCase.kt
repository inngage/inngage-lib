package br.com.inngage.sdk.internal.service.events.application

import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.service.events.domain.EventEntity
import br.com.inngage.sdk.internal.service.events.domain.EventRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Sends a named event to the Inngage API.
 *
 * Replaces the static `InngageService.sendEvent()` overloads.
 *
 * Identification rule: when [execute]'s `identifier` is provided it is used as-is;
 * otherwise the FCM registration token from [registrationProvider] is used.
 * If neither is available the event fails — a subscription must run first.
 *
 * @param repository           Data layer — injected for testability.
 * @param registrationProvider Supplies the persisted FCM token used as fallback
 *                             identification when no `identifier` is given.
 * @param dispatcher           I/O dispatcher — injected for testability.
 */
internal class SendEventUseCase(
    private val repository: EventRepository,
    private val registrationProvider: () -> String = { "" },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val tag = InngageConfig.TAG

    /**
     * @param appToken        SDK application token (required).
     * @param eventName       Name of the event to track (required).
     * @param identifier      Optional user identifier (e.g. email or user ID).
     * @param eventValues     Optional JSON payload with event metadata.
     * @param conversionValue Optional conversion value — sent only when [conversionEvent] is `true`.
     * @param conversionNotid Optional notification id tied to the conversion — sent only when
     *                        [conversionEvent] is `true`.
     * @param conversionEvent Marks this event as a conversion. Defaults to `false`.
     */
    suspend fun execute(
        appToken: String,
        eventName: String,
        identifier: String? = null,
        eventValues: JSONObject? = null,
        conversionValue: Any? = null,
        conversionNotid: String? = null,
        conversionEvent: Boolean = false
    ): Result<Unit> = withContext(dispatcher) {
        if (appToken.isBlank())
            return@withContext Result.failure(IllegalArgumentException("appToken must not be blank"))
        if (eventName.isBlank())
            return@withContext Result.failure(IllegalArgumentException("eventName must not be blank"))

        val resolvedIdentifier = identifier?.takeIf { it.isNotBlank() }
        val registration = if (resolvedIdentifier == null) {
            registrationProvider().takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(
                    IllegalStateException(
                        "No identifier provided and no FCM token persisted — call subscribe() first"
                    )
                )
        } else null

        val entity = EventEntity(
            appToken        = appToken,
            eventName       = eventName,
            identifier      = resolvedIdentifier,
            registration    = registration,
            eventValues     = eventValues,
            conversionValue = conversionValue,
            conversionNotid = conversionNotid,
            conversionEvent = conversionEvent
        )

        repository.sendEvent(entity).also { result ->
            result.onSuccess { Log.d(tag, "Event '$eventName' sent successfully") }
            result.onFailure { Log.e(tag, "Event '$eventName' failed: ${it.message}") }
        }
    }
}
