package br.com.inngage.sdk.internal.service.inapp.application

import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.service.inapp.InAppMessageV2Activity
import br.com.inngage.sdk.internal.service.inapp.data.InAppMessageV2RepositoryImpl
import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionData
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2Repository
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2Validator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches, validates, and launches the In-App Message v2 Activity.
 *
 * The [repository] defaults to [InAppMessageV2RepositoryImpl] which requires [Context]
 * to read stored `appId` and `deviceUuid` from [PreferencesStorage].
 *
 * @param context     Application context — forwarded to the default repository and Activity launch.
 * @param repository  Data source for the message (override in tests).
 * @param dispatcher  Dispatcher for UI operations (override in tests).
 */
internal class FetchAndShowInAppV2UseCase(
    private val context: Context,
    private val repository: InAppMessageV2Repository = InAppMessageV2RepositoryImpl(context),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    private val tag = InngageConfig.TAG_INAPP

    /**
     * Fetches the in-app message, validates it, and starts [InAppMessageV2Activity].
     *
     * @param handledBySdk  When `true` (default) the SDK resolves actions itself
     *                      (deep-links, browser, etc.). When `false` the SDK does not
     *                      navigate; the callbacks below deliver the actions to the app.
     * @param onActions     Invoked once, on display, with every action of the message.
     *                      Only used when [handledBySdk] is `false`.
     * @param onActionClick Invoked on each button/background tap with that specific
     *                      action (including its `url`). Only used when [handledBySdk]
     *                      is `false`.
     * @return [Result.success] on launch, [Result.failure] if fetch or validation fails.
     */
    suspend fun execute(
        handledBySdk: Boolean = true,
        onActions: ((List<InAppActionData>) -> Unit)? = null,
        onActionClick: ((InAppActionData) -> Unit)? = null
    ): Result<Unit> {
        Log.d(tag, "FetchAndShowInAppV2: execute() started")

        val fetchResult = repository.fetchMessage()
        if (fetchResult.isFailure) {
            Log.e(tag, "FetchAndShowInAppV2: fetch failed → ${fetchResult.exceptionOrNull()?.message}")
            return Result.failure(fetchResult.exceptionOrNull()!!)
        }

        val message = fetchResult.getOrNull()
        if (message == null) {
            Log.w(tag, "FetchAndShowInAppV2: no in-app to show — enabled=false or empty response")
            return Result.success(Unit)
        }

        Log.d(tag, "FetchAndShowInAppV2: message parsed OK → type=${message.type}, items=${message.media.carousel.items.size}")

        val validationResult = InAppMessageV2Validator.validate(message)
        if (validationResult.isFailure) {
            Log.w(tag, "FetchAndShowInAppV2: validation failed → ${validationResult.exceptionOrNull()?.message}")
            return Result.failure(validationResult.exceptionOrNull()!!)
        }

        Log.d(tag, "FetchAndShowInAppV2: validation OK — launching InAppMessageV2Activity")

        return withContext(dispatcher) {
            runCatching {
                if (!InAppMessageV2Activity.tryAcquire()) {
                    Log.w(tag, "FetchAndShowInAppV2: InApp already showing — skipping launch")
                    return@runCatching Unit
                }
                // Hand the callbacks + flag off to the Activity (a lambda can't ride in the Intent).
                InAppCallbackHolder.set(handledBySdk, onActions, onActionClick)
                val intent = Intent(context, InAppMessageV2Activity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(InAppMessageV2Activity.EXTRA_MESSAGE, message)
                    putExtra(InAppMessageV2Activity.EXTRA_HANDLED_BY_SDK, handledBySdk)
                }
                context.startActivity(intent)
                Log.d(tag, "FetchAndShowInAppV2: startActivity called (handledBySdk=$handledBySdk)")
                Unit
            }.also { result ->
                result.onFailure {
                    // Release the guard and drop the callback so a later trigger can retry cleanly.
                    InAppMessageV2Activity.release()
                    InAppCallbackHolder.clear()
                    Log.e(tag, "FetchAndShowInAppV2: startActivity failed → ${it.message}", it)
                }
            }
        }
    }
}
