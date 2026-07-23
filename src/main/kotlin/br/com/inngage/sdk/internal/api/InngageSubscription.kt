package br.com.inngage.sdk.internal.api

import android.content.Context
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import br.com.inngage.sdk.internal.orchestration.SdkInitializer
import br.com.inngage.sdk.internal.service.subscription.application.AddUserDataUseCase
import br.com.inngage.sdk.internal.service.subscription.data.SubscriptionRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Public facade for subscription registration and subscriber profile updates.
 * Wraps [SdkInitializer] — consumers call this from `Application.onCreate()`.
 */
internal object InngageSubscription {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @JvmStatic
    @JvmOverloads
    fun subscribe(
        context: Context,
        appToken: String,
        identifier: String? = null,
        customFields: JSONObject? = null,
        email: String? = null,
        phoneNumber: String? = null,
        requestGeoLocation: Boolean = false
    ) {
        SdkInitializer.scheduleSubscription(
            context            = context,
            appToken           = appToken,
            identifier         = identifier,
            customFields       = customFields,
            email              = email,
            phoneNumber        = phoneNumber,
            requestGeoLocation = requestGeoLocation
        )
    }

    @JvmStatic
    @JvmOverloads
    fun addUserData(
        context: Context,
        appToken: String,
        identifier: String? = null,
        customFields: JSONObject? = null,
        email: String? = null,
        phoneNumber: String? = null
    ) {
        val appContext = context.applicationContext
        val useCase = AddUserDataUseCase(
            repository         = SubscriptionRepositoryImpl(appContext),
            identifierProvider = {
                PreferencesStorage(appContext).getString(InngageConfig.PREF_IDENTIFIER)
            }
        )
        scope.launch {
            useCase.execute(appToken, identifier, customFields, email, phoneNumber)
        }
    }
}
