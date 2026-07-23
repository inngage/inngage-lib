package br.com.inngage.sdk.internal.service.events.domain

/** Repository contract for event tracking operations. */
internal interface EventRepository {
    /**
     * Sends a named event to the Inngage API.
     * @return [Result.success] on HTTP 200, [Result.failure] otherwise.
     */
    suspend fun sendEvent(entity: EventEntity): Result<Unit>
}

