package br.com.inngage.sdk.internal.api

import android.content.Context
import android.content.Intent
import br.com.inngage.sdk.internal.service.notificationstatus.application.HandleNotificationUseCase
import br.com.inngage.sdk.internal.service.notificationstatus.data.NotificationStatusRepositoryImpl
import br.com.inngage.sdk.internal.service.notificationstatus.domain.NotificationPayload

/**
 * Internal facade for push notification tap handling.
 *
 * Responsible **only** for:
 * - Parsing the FCM payload from an Activity Intent.
 * - Firing the open callback to `/v1/notification/`.
 * - Delivering the [NotificationPayload] to the consumer.
 * - Navigating to `deep` (external browser) or `inapp` (Chrome Custom Tab).
 *
 * In-App Message display is handled separately by [InngageInapp].
 */
internal object InngagePush {

    private val useCase = HandleNotificationUseCase(
        repository = NotificationStatusRepositoryImpl()
    )

    /**
     * Handles a push notification tap intent.
     *
     * Sends the open callback to Inngage and navigates according to the payload:
     * - `deep` key → external browser (suppressed when [blockDeepLink] is `true`).
     * - `inapp` key → Chrome Custom Tab inside the app (never suppressed).
     *
     * @param context             Activity or application context.
     * @param intent              The [Intent] received by the host Activity.
     * @param appToken            SDK application token.
     * @param blockDeepLink       When `true`, external deep-link navigation is skipped.
     * @param onNotificationClick Optional callback invoked with the full [NotificationPayload].
     */
    @JvmStatic
    @JvmOverloads
    fun handleNotification(
        context: Context,
        intent: Intent,
        appToken: String,
        blockDeepLink: Boolean = false,
        onNotificationClick: ((NotificationPayload) -> Unit)? = null
    ) {
        useCase.execute(context, intent, appToken, blockDeepLink, onNotificationClick)
    }
}

