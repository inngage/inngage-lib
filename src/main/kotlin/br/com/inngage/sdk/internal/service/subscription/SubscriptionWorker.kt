package br.com.inngage.sdk.internal.service.subscription

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.service.subscription.application.SubscribeUseCase
import br.com.inngage.sdk.internal.service.subscription.data.SubscriptionRepositoryImpl
import org.json.JSONObject

/**
 * WorkManager worker that performs the subscriber registration in the background.
 *
 * In-App Messages are not triggered here — display is app-driven via
 * [br.com.inngage.sdk.InngageClient.showInAppMessage].
 */
internal class SubscriptionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tag = InngageConfig.TAG

    override suspend fun doWork(): Result {
        val appToken     = inputData.getString(InngageConfig.EXTRA_TOKEN) ?: return Result.failure()
        val identifier   = inputData.getString(InngageConfig.EXTRA_IDENTIFIER)
        val customFields = inputData.getString(InngageConfig.EXTRA_CUSTOM_FIELD)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        val email        = inputData.getString(InngageConfig.EXTRA_EMAIL)
        val phone        = inputData.getString(InngageConfig.EXTRA_PHONE)
        val requestGeo   = inputData.getBoolean(InngageConfig.EXTRA_GEO, false)

        Log.d(tag, "SubscriptionWorker starting for appToken=$appToken")

        val useCase = SubscribeUseCase(
            repository = SubscriptionRepositoryImpl(applicationContext)
        )

        return useCase.execute(
            appToken           = appToken,
            identifier         = identifier,
            customFields       = customFields,
            email              = email,
            phoneNumber        = phone,
            requestGeoLocation = requestGeo
        ).fold(
            onSuccess = {
                Log.d(tag, "SubscriptionWorker succeeded")
                Result.success()
            },
            onFailure = {
                Log.e(tag, "SubscriptionWorker failed: ${it.message}")
                Result.failure()
            }
        )
    }
}
