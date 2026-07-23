package br.com.inngage.sdk.internal.core.config

/**
 * Central configuration constants for the Inngage SDK.
 * Replaces InngageConstants.java and IPreferenceConstants.java.
 */
internal object InngageConfig {

    // ── SDK metadata ──────────────────────────────────────────────────────────
    const val PLATFORM = "android"
    const val SDK_VERSION = "5.0.0"
    const val TAG = "inngage-lib"
    const val TAG_FIREBASE = "Inngage-FirebaseService"
    const val TAG_NOTIFY = "Inngage-Notify"
    const val TAG_INAPP = "Inngage-InApp"
    const val TAG_ERROR = "Inngage-Notify-Error"

    // ── Endpoints ─────────────────────────────────────────────────────────────
    const val API_PROD_ENDPOINT = "https://api.inngage.com.br"

    // ── API paths ─────────────────────────────────────────────────────────────
    const val PATH_SUBSCRIPTION = "/v1/subscription/"
    const val PATH_SUBSCRIPTION_V4 = "/v4/subscription/"
    const val PATH_ADD_USER_DATA = "/v4/subscription/addCustomField"
    const val PATH_GEOLOCATION = "/v1/geolocation/"
    const val PATH_NOTIFICATION_CALLBACK = "/v1/notification/"
    const val PATH_EVENTS = "/v1/events/newEvent/"
    const val PATH_SESSION = "/v1/session/"

    // ── In App Message v2 ─────────────────────────────────────────────────────
    const val PATH_INAPP_V2 = "/v4/message/objectMessage"
    /** Fixed channel identifier for the mobile in-app message channel. */
    const val INAPP_CHANNEL_ID = 6

    // ── Provider identifiers ──────────────────────────────────────────────────
    const val PROVIDER_FCM = "FCM"
    const val PROVIDER_GCM = "GCM"

    // ── WorkManager input keys ────────────────────────────────────────────────
    const val EXTRA_TOKEN        = "APP_TOKEN"
    const val EXTRA_IDENTIFIER   = "IDENTIFIER"
    const val EXTRA_CUSTOM_FIELD = "CUSTOM_FIELDS"
    const val EXTRA_EMAIL        = "EMAIL"
    const val EXTRA_PHONE        = "PHONE_NUMBER"
    const val EXTRA_GEO          = "GEOLOCATION"

    // ── SharedPreferences keys ────────────────────────────────────────────────
    const val PREF_DEVICE_UUID = "deviceUUID"
    const val PREF_FCM_TOKEN   = "fcmToken"
    const val PREF_IDENTIFIER  = "identifier"
    const val PREF_APP_ID = "appId"
    const val PREF_INAPP_FIRST_ACCESS = "inAppFirstAccess"
    const val PREF_INNGAGE_ENV = "inngageEnvironment"
    const val PREF_UPDATE_INTERVAL = "updateInterval"
    const val PREF_PRIORITY_ACCURACY = "priorityAccuracy"
    const val PREF_DISPLACEMENT = "displacement"
    const val PREF_DISTANCE = "distance"

    // ── Notification intent keys ──────────────────────────────────────────────
    // Order must stay stable — HandleNotificationUseCase maps by index.
    // Index: 0=notId, 1=id, 2=title, 3=body, 4=type, 5=url, 6=additional_data
    val NOTIFICATION_KEYS: Array<String> = arrayOf(
        "notId", "id", "title", "body", "type", "url", "additional_data"
    )

    // ── Validation messages ───────────────────────────────────────────────────
    const val INVALID_APP_TOKEN = "Verify if the value of APP_TOKEN was informed"
    const val INVALID_ENVIRONMENT = "Verify if the value of ENVIRONMENT was informed"
    const val INVALID_PROVIDER = "Verify if the value of PROVIDER was informed"
    const val INVALID_IDENTIFIER = "Verify if the value of IDENTIFIER was informed"
    const val INVALID_CUSTOM_FIELD = "Verify if the value of CUSTOM_FIELD was informed"
}

