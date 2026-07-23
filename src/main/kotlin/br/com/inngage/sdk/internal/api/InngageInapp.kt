package br.com.inngage.sdk.internal.api

import android.content.Context
import br.com.inngage.sdk.internal.service.inapp.application.FetchAndShowInAppV2UseCase
import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Internal facade for In-App Message display.
 *
 * Responsible **only** for fetching and rendering In-App Messages from the Inngage backend.
 * Push notification tap handling is the responsibility of [InngagePush].
 */
internal object InngageInapp {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Fetches and displays an In-App Message v2 from the Inngage backend.
     *
     * The SDK calls the Inngage In-App v2 endpoint, validates the message, and presents it
     * as a full-screen overlay. Supports banners, carousels, buttons, and deep-links.
     *
     * @param context       Application or Activity context.
     * @param handledBySdk  When `true` (default) the SDK resolves button/background
     *                      actions itself. When `false` the SDK does not navigate and
     *                      delivers the actions to the app via the callbacks below.
     * @param onActions     Callback with the full list of message actions, invoked once
     *                      on display; only when [handledBySdk] is `false`.
     * @param onActionClick Callback invoked on each button/background tap with that
     *                      specific action (including its `url`); only when
     *                      [handledBySdk] is `false`.
     */
    @JvmStatic
    fun showInAppMessage(
        context: Context,
        handledBySdk: Boolean = true,
        onActions: ((List<InAppActionData>) -> Unit)? = null,
        onActionClick: ((InAppActionData) -> Unit)? = null
    ) {
        scope.launch {
            FetchAndShowInAppV2UseCase(context.applicationContext)
                .execute(handledBySdk, onActions, onActionClick)
        }
    }
}
