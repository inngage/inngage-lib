package br.com.inngage.sdk.internal.orchestration

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import br.com.inngage.sdk.internal.platform.deviceinfo.DeviceUuidProvider
import br.com.inngage.sdk.internal.platform.lifecycle.AppForegroundObserver
import br.com.inngage.sdk.internal.platform.location.LocationProvider
import br.com.inngage.sdk.internal.platform.permission.PermissionChecker
import br.com.inngage.sdk.internal.service.location.LocationUpdateWorker
import br.com.inngage.sdk.internal.service.location.application.UpdateLocationUseCase
import br.com.inngage.sdk.internal.service.location.data.LocationRepositoryImpl
import br.com.inngage.sdk.internal.service.location.data.PreferencesLocationCache
import br.com.inngage.sdk.internal.service.location.domain.Coordinates
import br.com.inngage.sdk.internal.service.subscription.SubscriptionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            InngageConfig.WORK_SUBSCRIPTION,
            ExistingWorkPolicy.KEEP,
            request
        )

        Log.d(tag, "Subscription work enqueued (policy=KEEP)")
    }

    // ── Location ──────────────────────────────────────────────────────────────

    /**
     * Wires an [UpdateLocationUseCase] against the real platform providers.
     * The only place that constructs the location service dependencies.
     *
     * The device position is read only when the app is in the foreground or
     * holds background-location permission; otherwise Android would silently
     * return nothing after the 5 s timeout, so the read is skipped up-front.
     */
    fun createUpdateLocationUseCase(context: Context): UpdateLocationUseCase {
        val appContext  = context.applicationContext
        val storage     = PreferencesStorage(appContext)
        val permissions = PermissionChecker(appContext)
        val provider    = LocationProvider(appContext)

        return UpdateLocationUseCase(
            repository = LocationRepositoryImpl(),
            cache      = PreferencesLocationCache(storage),
            deviceLocationProvider = {
                val canRead = AppForegroundObserver.isInForeground() ||
                        permissions.hasBackgroundLocationPermission()
                if (!canRead) {
                    Log.d(tag, "Location skipped: app in background without background permission")
                    null
                } else {
                    provider.getLocation()?.let { Coordinates(it.lat, it.lon) }
                }
            },
            identifierProvider   = { storage.getString(InngageConfig.PREF_IDENTIFIER) },
            registrationProvider = { storage.getString(InngageConfig.PREF_FCM_TOKEN) },
            uuidProvider         = { DeviceUuidProvider(appContext).getUuid() }
        )
    }

    /**
     * One-shot location update, run immediately on the I/O dispatcher.
     * Never throws; the outcome is logged.
     */
    fun updateLocation(
        context: Context,
        appToken: String,
        identifier: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val useCase = createUpdateLocationUseCase(context)
        scope.launch {
            useCase.execute(appToken, identifier, latitude, longitude)
        }
    }

    /**
     * Keeps the subscriber position fresh with two cheap triggers:
     *
     * 1. **App foreground** — every time the app comes to the foreground the
     *    position is read and sent (subject to the local 5 min / 100 m throttle).
     *    This is what catches the case where the location permission was granted
     *    *after* `subscribe()` ran: the next foreground transition sends it.
     * 2. **Periodic work** — a [PeriodicWorkRequest] every [intervalMinutes]
     *    (never below [InngageConfig.LOCATION_MIN_INTERVAL_MINUTES], a WorkManager
     *    limit) with a network constraint. It survives process death and reboots.
     *    In the background it only reads the position when the host app holds
     *    `ACCESS_BACKGROUND_LOCATION`; otherwise it completes immediately.
     *
     * No foreground service, no continuous location listener, no wake locks held
     * by the SDK — nothing that could drain the battery or interrupt the app.
     * Safe to call on every `Application.onCreate()`: the periodic request is
     * updated in place ([ExistingPeriodicWorkPolicy.UPDATE]) and the foreground
     * observer is registered once.
     */
    fun startLocationUpdates(
        context: Context,
        appToken: String,
        identifier: String? = null,
        intervalMinutes: Long = InngageConfig.LOCATION_DEFAULT_INTERVAL_MINUTES
    ) {
        val appContext = context.applicationContext
        val interval   = intervalMinutes.coerceAtLeast(InngageConfig.LOCATION_MIN_INTERVAL_MINUTES)

        val data = Data.Builder().apply {
            putString(InngageConfig.EXTRA_TOKEN, appToken)
            identifier?.let { putString(InngageConfig.EXTRA_IDENTIFIER, it) }
        }.build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequest.Builder(LocationUpdateWorker::class.java, interval, TimeUnit.MINUTES)
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            InngageConfig.WORK_LOCATION_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        AppForegroundObserver.register {
            updateLocation(appContext, appToken, identifier)
        }

        Log.d(tag, "Location updates started (every $interval min + on app foreground)")
    }

    /** Cancels the periodic work and the foreground trigger. Idempotent. */
    fun stopLocationUpdates(context: Context) {
        AppForegroundObserver.unregister()
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(InngageConfig.WORK_LOCATION_PERIODIC)
        Log.d(tag, "Location updates stopped")
    }
}
