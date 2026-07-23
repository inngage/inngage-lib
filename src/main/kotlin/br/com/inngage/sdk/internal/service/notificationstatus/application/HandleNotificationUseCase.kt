package br.com.inngage.sdk.internal.service.notificationstatus.application

import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.util.DeepLinkHandler
import br.com.inngage.sdk.internal.service.notificationstatus.domain.NotificationPayload
import br.com.inngage.sdk.internal.service.notificationstatus.domain.NotificationStatusRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles an incoming push notification Intent:
 * 1. Parses the FCM payload from Intent extras into a [NotificationPayload].
 * 2. Sends the open callback to Inngage (`/v1/notification/` with `id`, `notid`, `app_token`).
 * 3. Delivers the payload to the optional [onNotificationClick] consumer callback.
 * 4. If [blockDeepLink] is `false` and [NotificationPayload.type] is `"deep"` → opens external browser.
 * 5. If [NotificationPayload.type] is `"inapp"` → opens Chrome Custom Tab (never blocked).
 *
 * @param repository Data layer — injected for testability.
 * @param dispatcher I/O dispatcher — injected for testability.
 */
internal class HandleNotificationUseCase(
    private val repository: NotificationStatusRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val tag = InngageConfig.TAG

    /**
     * @param context             Application context — used for navigation.
     * @param intent              The Activity Intent containing notification extras.
     * @param appToken            SDK application token.
     * @param blockDeepLink       When `true`, external deep-link navigation is suppressed.
     *                            In-app browser navigation is always performed.
     * @param onNotificationClick Optional callback invoked with the full [NotificationPayload].
     *                            Runs on the calling thread (main).
     */
    fun execute(
        context: Context,
        intent: Intent,
        appToken: String,
        blockDeepLink: Boolean = false,
        onNotificationClick: ((NotificationPayload) -> Unit)? = null
    ) {
        val extras = intent.extras ?: return
        if (extras.isEmpty) return

        // keys: 0=notId, 1=id, 2=title, 3=body, 4=type, 5=url, 6=additional_data
        val keys   = InngageConfig.NOTIFICATION_KEYS
        val values = Array(keys.size) { i -> intent.getStringExtra(keys[i]).orEmpty() }

        val notId          = values.getOrElse(0) { "" }.ifBlank { return }
        val id             = values.getOrElse(1) { "" }
        val title          = values.getOrElse(2) { "" }
        val body           = values.getOrElse(3) { "" }
        val type           = values.getOrElse(4) { "" }.takeIf { it.isNotBlank() }
        val url            = values.getOrElse(5) { "" }.takeIf { it.isNotBlank() }
        val additionalData = values.getOrElse(6) { "" }.takeIf { it.isNotBlank() }

        val payload = NotificationPayload(
            notId          = notId,
            id             = id,
            title          = title,
            body           = body,
            type           = type,
            url            = url,
            additionalData = additionalData
        )

        val endpoint = InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_NOTIFICATION_CALLBACK

        // Fire-and-forget open callback
        CoroutineScope(dispatcher).launch {
            repository.sendCallback(notId, appToken, endpoint)
                .onSuccess { Log.d(tag, "Notification callback sent: $notId") }
                .onFailure { Log.e(tag, "Notification callback failed: ${it.message}") }
        }

        // Consumer callback — runs on main thread
        onNotificationClick?.invoke(payload)

        // Navigation
        if (url != null) {
            when (type) {
                "deep"  -> if (!blockDeepLink) DeepLinkHandler.openDeepLink(context, url)
                "inapp" -> DeepLinkHandler.openInBrowser(context, url)   // never blocked
            }
        }
    }
}
