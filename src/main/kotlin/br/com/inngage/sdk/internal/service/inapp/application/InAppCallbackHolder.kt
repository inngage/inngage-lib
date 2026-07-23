package br.com.inngage.sdk.internal.service.inapp.application

import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionData

/**
 * Process-static hand-off for the `showInAppMessage` callback and its handling flag.
 *
 * A callback lambda cannot be serialised into an [android.content.Intent], so it cannot
 * travel to [br.com.inngage.sdk.internal.service.inapp.InAppMessageV2Activity] the way
 * the message payload does. Since only one In-App is ever visible at a time (guarded by
 * [br.com.inngage.sdk.internal.service.inapp.InAppMessageV2Activity.tryAcquire]), a single
 * slot is sufficient.
 *
 * The Activity MUST call [clear] in `onDestroy` so the (possibly Activity-capturing)
 * app lambda is not retained.
 */
internal object InAppCallbackHolder {

    /** When `false`, the SDK does not navigate and the app handles actions via the callbacks. */
    @Volatile
    var handledBySdk: Boolean = true

    /** App callback invoked once, on display, with every action of the message. */
    @Volatile
    var onActions: ((List<InAppActionData>) -> Unit)? = null

    /** App callback invoked on each button/background tap with that specific action. */
    @Volatile
    var onActionClick: ((InAppActionData) -> Unit)? = null

    /** Stores the current hand-off values before launching the Activity. */
    fun set(
        handledBySdk: Boolean,
        onActions: ((List<InAppActionData>) -> Unit)?,
        onActionClick: ((InAppActionData) -> Unit)?
    ) {
        this.handledBySdk = handledBySdk
        this.onActions = onActions
        this.onActionClick = onActionClick
    }

    /** Clears the hand-off — call from the Activity's `onDestroy`. */
    fun clear() {
        handledBySdk = true
        onActions = null
        onActionClick = null
    }
}
