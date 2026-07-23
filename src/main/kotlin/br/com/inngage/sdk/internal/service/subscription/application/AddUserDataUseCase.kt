package br.com.inngage.sdk.internal.service.subscription.application

import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.util.Validators
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriptionRepository
import br.com.inngage.sdk.internal.service.subscription.domain.UserDataEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Sends a subscriber profile update (`addUserData`) to the Inngage API.
 *
 * Identification rule: when `identifier` is provided it is used as-is;
 * otherwise the identifier persisted by the last subscription (the anonymous
 * installation UUID, in the login-screen flow) is used, so the data is linked
 * to the subscriber created earlier. When neither exists the field is omitted —
 * only `app_token` is mandatory in the request.
 *
 * @param repository         Data layer — injected for testability.
 * @param identifierProvider Supplies the identifier persisted by the last
 *                           subscription, used as fallback.
 * @param dispatcher         I/O dispatcher — injected for testability.
 */
internal class AddUserDataUseCase(
    private val repository: SubscriptionRepository,
    private val identifierProvider: () -> String = { "" },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val tag = InngageConfig.TAG

    /**
     * @param appToken     SDK application token (required).
     * @param identifier   Optional user identifier (e.g. login/e-mail).
     * @param customFields Optional custom attributes JSON.
     * @param email        Optional user e-mail.
     * @param phoneNumber  Optional user phone number.
     */
    suspend fun execute(
        appToken: String,
        identifier: String? = null,
        customFields: JSONObject? = null,
        email: String? = null,
        phoneNumber: String? = null
    ): Result<Unit> = withContext(dispatcher) {
        if (!Validators.isValidAppToken(appToken))
            return@withContext Result.failure(
                IllegalArgumentException(InngageConfig.INVALID_APP_TOKEN)
            )

        if (identifier.isNullOrBlank() && customFields == null && email == null && phoneNumber == null)
            return@withContext Result.failure(
                IllegalArgumentException("addUserData requires at least one field to send")
            )

        val resolvedIdentifier = identifier?.takeIf { it.isNotBlank() }
            ?: identifierProvider().takeIf { it.isNotBlank() }
        if (resolvedIdentifier == null) {
            Log.w(tag, "addUserData: no identifier available — sending without one")
        }

        val entity = UserDataEntity(
            appToken     = appToken,
            identifier   = resolvedIdentifier,
            customFields = customFields,
            email        = email,
            phoneNumber  = phoneNumber
        )

        repository.addUserData(entity).also { result ->
            result.onSuccess { Log.d(tag, "addUserData sent successfully") }
            result.onFailure { Log.e(tag, "addUserData failed: ${it.message}") }
        }
    }
}
