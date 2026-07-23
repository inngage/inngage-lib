package br.com.inngage.sdk.internal.orchestration

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.service.subscription.SubscriptionWorker
import org.json.JSONObject

/**
 * Bootstraps the Inngage SDK from the host app's [android.app.Application.onCreate].
 *
 * Schedules a [SubscriptionWorker] with [ExistingWorkPolicy.KEEP] to avoid
 * duplicate subscriptions on repeated calls within the same app session.
 *
 * In-App Messages are never triggered automatically by the SDK — the host app
 * controls display via [br.com.inngage.sdk.InngageClient.showInAppMessage].
 */
internal object SdkInitializer {

    private val tag = InngageConfig.TAG
    private const val WORK_NAME = "inngage_subscription"

    /**
     * Enqueues a subscription registration WorkManager job.
     * Safe to call on every `Application.onCreate()` — WorkManager deduplication
     * with [ExistingWorkPolicy.KEEP] prevents duplicate network calls.
     */
    fun scheduleSubscription(
        context: Context,
        appToken: String,
        identifier: String? = null,
        customFields: JSONObject? = null,
        email: String? = null,
        phoneNumber: String? = null,
        requestGeoLocation: Boolean = false
    ) {
        val data = Data.Builder().apply {
            putString(InngageConfig.EXTRA_TOKEN, appToken)
            identifier?.let    { putString(InngageConfig.EXTRA_IDENTIFIER, it) }
            customFields?.let  { putString(InngageConfig.EXTRA_CUSTOM_FIELD, it.toString()) }
            email?.let         { putString(InngageConfig.EXTRA_EMAIL, it) }
            phoneNumber?.let   { putString(InngageConfig.EXTRA_PHONE, it) }
            putBoolean(InngageConfig.EXTRA_GEO, requestGeoLocation)
        }.build()

        val request = OneTimeWorkRequest.Builder(SubscriptionWorker::class.java)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )

        Log.d(tag, "Subscription work enqueued (policy=KEEP)")
    }
}
