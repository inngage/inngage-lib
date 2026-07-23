package br.com.inngage.sdk.internal.api

import android.content.Context
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import br.com.inngage.sdk.internal.service.events.application.SendEventUseCase
import br.com.inngage.sdk.internal.service.events.data.EventRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Public facade for event tracking.
 * Wraps [SendEventUseCase] — consumers call this from any Activity or Service.
 */
internal object InngageEvents {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @JvmStatic
    @JvmOverloads
    fun sendEvent(
        context: Context,
        appToken: String,
        eventName: String,
        identifier: String? = null,
        eventValues: JSONObject? = null,
        conversionValue: Any? = null,
        conversionNotid: String? = null,
        conversionEvent: Boolean = false
    ) {
        val appContext = context.applicationContext
        val useCase = SendEventUseCase(
            repository           = EventRepositoryImpl(),
            registrationProvider = {
                PreferencesStorage(appContext).getString(InngageConfig.PREF_FCM_TOKEN)
            }
        )
        scope.launch {
            useCase.execute(
                appToken        = appToken,
                eventName       = eventName,
                identifier      = identifier,
                eventValues     = eventValues,
                conversionValue = conversionValue,
                conversionNotid = conversionNotid,
                conversionEvent = conversionEvent
            )
        }
    }
}
