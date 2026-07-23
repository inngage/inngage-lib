package br.com.inngage.sdk.internal.service.subscription.data

import android.content.Context
import android.os.Build
import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import br.com.inngage.sdk.internal.platform.appinfo.AppInfoProvider
import br.com.inngage.sdk.internal.platform.deviceinfo.DeviceUuidProvider
import br.com.inngage.sdk.internal.platform.location.GeoLocation
import br.com.inngage.sdk.internal.platform.location.LocationProvider
import br.com.inngage.sdk.internal.platform.permission.PermissionChecker
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriberEntity
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriptionRepository
import br.com.inngage.sdk.internal.service.subscription.domain.UserDataEntity
import org.json.JSONObject
import java.util.Locale



/**
 * HTTP-backed implementation of [SubscriptionRepository].
 *
 * Always targets [InngageConfig.API_PROD_ENDPOINT].
 * After a successful subscription, persists the `appId` returned by the server
 * so that the In-App Message fetch can include it in the request body.
 *
 * Identification rule: `uuid` is always the per-installation UUID from
 * [DeviceUuidProvider]; `identifier` is the consumer-provided value when
 * present, otherwise the same UUID (anonymous subscriber). The effective
 * identifier is persisted for later use by `addUserData`.
 */
internal class SubscriptionRepositoryImpl(
    context: Context,
    private val httpClient: HttpClient = HttpClient(),
    private val appInfoProvider: AppInfoProvider = AppInfoProvider(context),
    private val deviceUuidProvider: DeviceUuidProvider = DeviceUuidProvider(context),
    private val locationProvider: LocationProvider = LocationProvider(context),
    private val permissionChecker: PermissionChecker = PermissionChecker(context)
) : SubscriptionRepository {

    private val tag = InngageConfig.TAG
    private val storage = PreferencesStorage(context)

    override suspend fun subscribe(entity: SubscriberEntity): Result<Unit> {
        // Persist the FCM token up-front so other APIs (e.g. In-App fetch) can
        // rely on it even if this subscription request fails on the network.
        storage.putString(InngageConfig.PREF_FCM_TOKEN, entity.registration)

        val uuid = deviceUuidProvider.getUuid()
        val identifier = entity.identifier.ifBlank { uuid }
        storage.putString(InngageConfig.PREF_IDENTIFIER, identifier)

        val location: GeoLocation? = if (entity.requestGeoLocation) locationProvider.getLocation() else null
        val body = buildBody(entity, identifier, uuid, location)
        val endpoint = InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_SUBSCRIPTION_V4
        return httpClient.post(body, endpoint).map { responseBody ->
            Log.i(tag, "Subscription response received: $responseBody")
            runCatching {
                val json = JSONObject(responseBody)
                val inner = json.optJSONObject("registerSubscriberResponse")
                    ?: json.optJSONObject("data")
                    ?: json
                // Try direct "appId" field first, then parse from statusDescription
                val appId = inner.optString("appId").takeIf { it.isNotBlank() }
                    ?: extractAppIdFromDescription(inner.optString("statusDescription"))
                if (!appId.isNullOrBlank()) {
                    storage.putString(InngageConfig.PREF_APP_ID, appId)
                    Log.i(tag, "Subscription: persisted appId=$appId")
                }
            }.onFailure { Log.w(tag, "Subscription: could not parse appId: ${it.message}") }
        }
    }

    /** Extracts the numeric app ID from strings like: `...in app ["522"]` */
    private fun extractAppIdFromDescription(desc: String): String? =
        Regex("""in app \["(\d+)"\]""").find(desc)?.groupValues?.getOrNull(1)

    override suspend fun addUserData(entity: UserDataEntity): Result<Unit> {
        val body = JSONObject().apply {
            put("app_token", entity.appToken)
            entity.identifier?.let { put("identifier", it) }
            entity.customFields?.let { put("custom_field", it) }
            entity.email?.let { put("email", it) }
            entity.phoneNumber?.let { put("phone_number", it) }
        }
        val payload = JSONObject().put("fieldsRequest", body)
        val endpoint = InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_ADD_USER_DATA
        return httpClient.post(payload, endpoint).map { /* discard body */ }
    }

    private fun buildBody(
        entity: SubscriberEntity,
        identifier: String,
        uuid: String,
        location: GeoLocation?
    ): JSONObject {
        val appInfo  = appInfoProvider.getAppInfo()
        val locale   = Locale.getDefault()

        val body = JSONObject().apply {
            put("identifier", identifier)
            put("registration", entity.registration)
            put("platform", InngageConfig.PLATFORM)
            put("sdk_version", InngageConfig.SDK_VERSION)
            put("app_token", entity.appToken)
            put("device_model", Build.MODEL)
            put("device_manufacturer", Build.MANUFACTURER)
            put("os_locale", locale.displayCountry)
            put("os_language", locale.displayLanguage)
            put("os_version", Build.VERSION.RELEASE)
            put("app_version", appInfo.versionName)
            put("app_installed_in", appInfo.installationDate)
            put("app_updated_in", appInfo.updateDate)
            put("uuid", uuid)
            put("opt_in", if (permissionChecker.areNotificationsEnabled()) "1" else "0")
        }

        location?.let { body.put("lat", it.lat.toString()); body.put("long", it.lon.toString()) }
        entity.email?.let { body.put("email", it) }
        entity.phoneNumber?.let { body.put("phone", it) }
        entity.customFields?.let { body.put("custom_field", it) }

        Log.d(tag, "Subscription request built for identifier=${body.optString("identifier")}")
        return JSONObject().put("registerSubscriberRequest", body)
    }
}
