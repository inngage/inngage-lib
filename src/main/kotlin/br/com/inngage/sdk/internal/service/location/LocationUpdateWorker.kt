package br.com.inngage.sdk.internal.service.location

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.orchestration.SdkInitializer
import br.com.inngage.sdk.internal.service.location.domain.LocationFailure

/**
 * WorkManager worker that reads the device position and posts it to
 * `/v4/subscription/location`.
 *
 * Scheduled as periodic work by [SdkInitializer.startLocationUpdates]. Runs in
 * the background with a network constraint, so it never touches the UI thread
 * and is batched by the system with other deferred jobs (Doze-friendly).
 *
 * Retry policy: transient failures ([LocationFailure.Network]) are retried
 * with backoff up to [MAX_ATTEMPTS]; everything else (permanent failures,
 * missing permission, no fix) completes the run and waits for the next period.
 */
internal class LocationUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tag = InngageConfig.TAG

    override suspend fun doWork(): Result {
        val appToken   = inputData.getString(InngageConfig.EXTRA_TOKEN) ?: return Result.failure()
        val identifier = inputData.getString(InngageConfig.EXTRA_IDENTIFIER)

        Log.d(tag, "LocationUpdateWorker starting (attempt ${runAttemptCount + 1})")

        val useCase = SdkInitializer.createUpdateLocationUseCase(applicationContext)
        return useCase.execute(appToken = appToken, identifier = identifier).fold(
            onSuccess = {
                Log.d(tag, "LocationUpdateWorker finished: $it")
                Result.success()
            },
            onFailure = { error ->
                val transient = error is LocationFailure.Network
                if (transient && runAttemptCount < MAX_ATTEMPTS - 1) {
                    Log.w(tag, "LocationUpdateWorker will retry: ${error.message}")
                    Result.retry()
                } else {
                    Log.e(tag, "LocationUpdateWorker failed: ${error.message}")
                    Result.failure()
                }
            }
        )
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
