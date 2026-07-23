package br.com.inngage.sdk.internal.api

import br.com.inngage.sdk.internal.orchestration.NotificationRouter
import br.com.inngage.sdk.internal.service.push.domain.PushConfig

/**
 * Public facade for push notification display configuration.
 * Replaces the mutable singleton [br.com.inngage.sdk.InngagePushConfig].
 */
internal object InngagePushFacade {

    /**
     * Applies [config] to the SDK's push display settings.
     * Must be called before the first FCM message is received.
     */
    @JvmStatic
    fun configure(config: PushConfig) {
        NotificationRouter.pushConfig = config
    }

    /** Returns the currently active [PushConfig]. */
    @JvmStatic
    fun currentConfig(): PushConfig = NotificationRouter.pushConfig
}

