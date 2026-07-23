package br.com.inngage.sdk.internal.orchestration

import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.service.inapp.application.InAppIntentMapper
import br.com.inngage.sdk.internal.service.push.domain.PushConfig
import org.json.JSONObject

/**
 * Routes an incoming push message to either the InApp or notification display path.
 *
 * Replaces `InngageMessagingService.sendData()`.
 * Push configuration is held here as a mutable state; set it before FCM is started.
 */
internal object NotificationRouter {

    private val tag = InngageConfig.TAG

    /** Current push display configuration — updated via [br.com.inngage.sdk.internal.api.InngagePushFacade]. */
    @Volatile
    var pushConfig: PushConfig = PushConfig()

    private const val FIELD_NOT_ID = "notId"

    /**
     * Routes [json] from an FCM message into the notification tap [intent].
     *
     * In-App Messages are no longer triggered from push payloads — they are fetched
     * on demand, only when the host app calls
     * [br.com.inngage.sdk.InngageClient.showInAppMessage].
     *
     * @param context  Application context.
     * @param intent   The Intent being built for the push tap action.
     * @param json     The full FCM data payload.
     */
    fun routePush(context: Context, intent: Intent, json: JSONObject) {
        if (json.has(FIELD_NOT_ID)) {
            runCatching {
                InAppIntentMapper.writeToIntent(
                    intent,
                    json.toString(),
                    InngageConfig.NOTIFICATION_KEYS
                )
            }.onFailure {
                Log.e(tag, "Failed to route notification data: ${it.message}")
            }
        }
    }
}

