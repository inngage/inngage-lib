package br.com.inngage.sdk.internal.platform.firebase

import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import com.google.firebase.messaging.FirebaseMessagingService

/**
 * Listens for FCM token refresh events.
 *
 * Replaces the deprecated [br.com.inngage.sdk.InngageInstanceIDListenerService].
 * The refreshed token is persisted so APIs that read it from storage stay
 * current; re-subscription is handled by the `SubscriptionWorker` triggered
 * from [br.com.inngage.sdk.internal.orchestration.SdkInitializer].
 *
 * Declared in AndroidManifest.xml inside the `<application>` block.
 */
internal class FirebaseTokenRefreshService : FirebaseMessagingService() {

    private val tag = InngageConfig.TAG_FIREBASE

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PreferencesStorage(applicationContext).putString(InngageConfig.PREF_FCM_TOKEN, token)
        Log.d(tag, "FCM token refreshed and persisted — re-subscription will be triggered on next app launch")
        // Re-subscription is handled at app startup via SdkInitializer.
        // We intentionally do NOT call subscribe() here to avoid duplicate
        // WorkManager jobs (WorkManager uniqueness policy handles dedup).
    }
}

