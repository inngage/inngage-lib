package br.com.inngage.sdk.internal.service.inapp.domain

/**
 * Contract for fetching an [InAppMessageV2] from the Inngage backend.
 */
internal interface InAppMessageV2Repository {
    /**
     * Fetches the in-app message payload.
     *
     * @return [Result.success] with the parsed message (or `null` if absent in response),
     *         or [Result.failure] on network/parse error.
     */
    suspend fun fetchMessage(): Result<InAppMessageV2?>
}

