package br.com.inngage.sdk.internal.service.location.domain

/**
 * Repository contract for the location endpoint.
 * Implementations live in `data/` — business logic depends only on this interface.
 */
internal interface LocationRepository {
    /**
     * Posts the position to `/v4/subscription/location`.
     *
     * @return [Result.success] with [LocationUpdateResult.Recorded] or
     *         [LocationUpdateResult.Unchanged] on HTTP 2xx, [Result.failure]
     *         wrapping a [LocationFailure] otherwise.
     */
    suspend fun sendLocation(entity: LocationUpdateEntity): Result<LocationUpdateResult>
}

/**
 * Local memory of the last position accepted by the backend.
 */
internal interface LocationCache {
    fun lastSent(): SentLocation?
    fun saveLastSent(location: SentLocation)
    fun clear()
}
