package br.com.inngage.sdk.internal.service.subscription.domain

/**
 * Repository contract for subscription operations.
 * Implementations live in `data/` — business logic depends only on this interface.
 */
internal interface SubscriptionRepository {
    /**
     * Sends a subscription registration request to the Inngage API.
     *
     * @return [Result.success] on HTTP 2xx, [Result.failure] wrapping [SubscriptionFailure] otherwise.
     */
    suspend fun subscribe(entity: SubscriberEntity): Result<Unit>

    /**
     * Sends a subscriber profile update (`fieldsRequest`) to the Inngage API.
     *
     * @return [Result.success] on HTTP 2xx, [Result.failure] otherwise.
     */
    suspend fun addUserData(entity: UserDataEntity): Result<Unit>
}

