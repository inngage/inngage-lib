package br.com.inngage.sdk.internal.service.subscription.application

import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.util.Validators
import br.com.inngage.sdk.internal.platform.firebase.FirebaseTokenProvider
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriberEntity
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriptionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Orchestrates a device subscription:
 * 1. Validates [appToken], [identifier] and [customFields].
 * 2. Obtains the FCM push token.
 * 3. Delegates to [SubscriptionRepository] to send the request.
 */
internal class SubscribeUseCase(
    private val repository: SubscriptionRepository,
    private val tokenProvider: FirebaseTokenProvider = FirebaseTokenProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val tag = InngageConfig.TAG

    /**
     * Runs the full subscription flow.
     *
     * @return [Result.success] on success, [Result.failure] with the cause otherwise.
     */
    suspend fun execute(
        appToken: String,
        identifier: String? = null,
        customFields: JSONObject? = null,
        email: String? = null,
        phoneNumber: String? = null,
        requestGeoLocation: Boolean = false
    ): Result<Unit> = withContext(dispatcher) {
        // 1 — Validate
        try {
            Validators.requireValidSubscriptionParams(appToken, identifier, customFields)
        } catch (e: IllegalArgumentException) {
            Log.w(tag, "Subscription validation failed: ${e.message}")
            return@withContext Result.failure(e)
        }

        // 2 — Fetch FCM token
        val fcmToken = runCatching { tokenProvider.getToken() }.getOrElse { e ->
            Log.e(tag, "Failed to obtain FCM token: ${e.message}")
            return@withContext Result.failure(e)
        }

        // 3 — Build entity and subscribe
        val entity = SubscriberEntity(
            appToken           = appToken,
            identifier         = identifier ?: "",
            registration       = fcmToken,
            email              = email,
            phoneNumber        = phoneNumber,
            customFields       = customFields,
            requestGeoLocation = requestGeoLocation
        )

        repository.subscribe(entity).also { result ->
            result.onSuccess { Log.d(tag, "Subscription successful") }
            result.onFailure { Log.e(tag, "Subscription failed: ${it.message}") }
        }
    }
}
