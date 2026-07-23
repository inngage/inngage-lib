package br.com.inngage.sdk

import android.content.Context
import android.content.Intent
import br.com.inngage.sdk.internal.api.InngageEvents
import br.com.inngage.sdk.internal.api.InngageInapp
import br.com.inngage.sdk.internal.api.InngagePush
import br.com.inngage.sdk.internal.api.InngagePushFacade
import br.com.inngage.sdk.internal.api.InngageSubscription
import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionData
import br.com.inngage.sdk.internal.service.notificationstatus.domain.NotificationPayload
import br.com.inngage.sdk.internal.service.push.domain.PushConfig
import org.json.JSONObject

/**
 * Single public entry point for the Inngage Android SDK.
 *
 * All methods are `@JvmStatic` for transparent Java interoperability.
 *
 * ## Quick-start
 * ```kotlin
 * // Application.onCreate():
 * InngageClient.subscribe(context = this, appToken = "your-app-token")
 *
 * // Activity.onResume():
 * InngageClient.handleNotification(
 *     context  = this,
 *     intent   = intent,
 *     appToken = "your-app-token"
 * ) { payload ->
 *     // Optional: react to the notification tap with the full FCM payload
 * }
 * ```
 */
object InngageClient {

    // ── Subscription ──────────────────────────────────────────────────────────

    /**
     * Registers the device with the Inngage platform.
     * Must be called from [android.app.Application.onCreate].
     *
     * Identification: when [identifier] is omitted, the SDK generates a random
     * per-installation UUID (persisted and reused on every launch) and sends it
     * as both `identifier` and `uuid` — an anonymous subscriber. When
     * [identifier] is provided it is sent as-is and `uuid` still carries the
     * installation UUID. Use [addUserData] after login to enrich the profile.
     *
     * @param context           Application context.
     * @param appToken          Your Inngage application token (min 8 chars).
     * @param identifier        Optional user identifier (e.g. email). Defaults to the installation UUID.
     * @param customFields      Optional extra JSON attributes for segmentation.
     * @param email             Optional user email address.
     * @param phoneNumber       Optional user phone number.
     * @param requestGeoLocation When `true`, the device's last-known location is included.
     */
    @JvmStatic
    @JvmOverloads
    fun subscribe(
        context: Context,
        appToken: String,
        identifier: String? = null,
        customFields: JSONObject? = null,
        email: String? = null,
        phoneNumber: String? = null,
        requestGeoLocation: Boolean = false
    ) {
        InngageSubscription.subscribe(
            context, appToken,
            identifier, customFields, email, phoneNumber, requestGeoLocation
        )
    }

    /**
     * Sends additional user data to enrich an existing subscriber profile.
     *
     * Typical flow: call [subscribe] on the login screen (anonymous subscriber),
     * then call this after login — when the user's data is known — from the
     * main screen.
     *
     * Identification: when [identifier] is omitted, the identifier persisted by
     * the last [subscribe] call (the anonymous installation UUID) is used, so
     * the data is linked to the subscriber created earlier.
     *
     * At least one of [identifier], [customFields], [email] or [phoneNumber]
     * must be provided; the request is dropped (logged) otherwise.
     *
     * @param context      Application or Activity context.
     * @param appToken     Your Inngage application token (required).
     * @param identifier   Optional user identifier (e.g. login/e-mail).
     * @param customFields Optional custom attributes JSON
     *                     (e.g. `{"plano": "premium", "pontos": 1500}`).
     * @param email        Optional user e-mail.
     * @param phoneNumber  Optional user phone number.
     */
    @JvmStatic
    @JvmOverloads
    fun addUserData(
        context: Context,
        appToken: String,
        identifier: String? = null,
        customFields: JSONObject? = null,
        email: String? = null,
        phoneNumber: String? = null
    ) {
        InngageSubscription.addUserData(context, appToken, identifier, customFields, email, phoneNumber)
    }

    // ── Events ────────────────────────────────────────────────────────────────

    /**
     * Sends a named event to Inngage for analytics and automation.
     *
     * Identification: when [identifier] is provided it is sent as `identifier`;
     * otherwise the SDK sends the FCM token persisted during [subscribe] as
     * `registration`. If neither is available the event is dropped (logged) —
     * call [subscribe] at least once first.
     *
     * Conversion: set [conversionEvent] to `true` to mark this event as a
     * conversion; only then are [conversionValue] and [conversionNotid] sent.
     *
     * @param context         Application or Activity context.
     * @param appToken        Your Inngage application token (required).
     * @param eventName       Name of the event (required, e.g. `"purchase_completed"`).
     * @param identifier      Optional user identifier matching the one used in [subscribe].
     * @param eventValues     Optional JSON payload with event metadata.
     * @param conversionValue Optional conversion value (number or string).
     * @param conversionNotid Optional notification id tied to the conversion.
     * @param conversionEvent Marks the event as a conversion. Defaults to `false`.
     */
    @JvmStatic
    @JvmOverloads
    fun sendEvent(
        context: Context,
        appToken: String,
        eventName: String,
        identifier: String? = null,
        eventValues: JSONObject? = null,
        conversionValue: Any? = null,
        conversionNotid: String? = null,
        conversionEvent: Boolean = false
    ) {
        InngageEvents.sendEvent(
            context, appToken, eventName,
            identifier, eventValues, conversionValue, conversionNotid, conversionEvent
        )
    }

    // ── Push config ───────────────────────────────────────────────────────────

    /**
     * Configures push notification display settings.
     * Call before the first FCM message arrives (ideally in `Application.onCreate()`).
     *
     * @param config A [PushConfig] built with [PushConfig.Builder].
     */
    @JvmStatic
    fun configurePush(config: PushConfig) {
        InngagePushFacade.configure(config)
    }

    // ── Push notification tap handling ────────────────────────────────────────

    /**
     * Processes a notification tap intent.
     *
     * Actions performed (in order):
     * 1. Fires the open callback to Inngage (`/v1/notification/` with `id`, `notid`, `app_token`).
     * 2. Invokes [onNotificationClick] with the full [NotificationPayload] (if provided).
     * 3. If `payload.type == "deep"` and [blockDeepLink] is `false` →
     *    opens `payload.url` in the device's default external browser.
     * 4. If `payload.type == "inapp"` →
     *    opens `payload.url` in a Chrome Custom Tab inside the app (never blocked).
     *
     * Call this from [android.app.Activity.onResume] to handle taps in all app states
     * (foreground, background, and killed).
     *
     * **Java usage:**
     * ```java
     * InngageClient.handleNotification(this, getIntent(), "APP_TOKEN");
     * // With callback:
     * InngageClient.handleNotification(this, getIntent(), "APP_TOKEN", false,
     *     payload -> { /* handle payload */ return Unit.INSTANCE; });
     * ```
     *
     * @param context             Activity or application context.
     * @param intent              The [Intent] received by the host Activity.
     * @param appToken            Your Inngage application token.
     * @param blockDeepLink       When `true`, external deep-link navigation is skipped.
     *                            In-app browser navigation is never blocked.
     * @param onNotificationClick Optional callback invoked with the FCM [NotificationPayload].
     */
    @JvmStatic
    @JvmOverloads
    fun handleNotification(
        context: Context,
        intent: Intent,
        appToken: String,
        blockDeepLink: Boolean = false,
        onNotificationClick: ((NotificationPayload) -> Unit)? = null
    ) {
        InngagePush.handleNotification(context, intent, appToken, blockDeepLink, onNotificationClick)
    }

    // ── In-App Messages ───────────────────────────────────────────────────────

    /**
     * Fetches and displays an In-App Message from the Inngage backend.
     *
     * The SDK calls the Inngage In-App v2 endpoint, validates the message, and presents it
     * as a full-screen overlay. Supports banners, carousels, buttons, and deep-links.
     *
     * This is independent from push notification handling. Call it at any point where
     * you want to trigger in-app message display (e.g. after login, on key screens).
     *
     * ### Who handles the actions
     * - `handledBySdk = true` (default): the SDK resolves button/background actions
     *   itself (deep-link, browser, etc.) — the callbacks are **not** invoked.
     * - `handledBySdk = false`: the SDK still renders the message but does **not**
     *   navigate. Instead it hands the actions to the host app:
     *     - [onActions] fires once, on display, with the full list of [InAppActionData].
     *     - [onActionClick] fires on each button/background tap with that specific
     *       action — including its `url` — so the app can, e.g., open a screen matching
     *       the tapped button's URL. Tapping still dismisses the message.
     *
     * @param context       Application or Activity context.
     * @param handledBySdk  Whether the SDK (true) or the host app (false) handles actions.
     * @param onActions     Callback with every action of the message (on display); only
     *                      when [handledBySdk] is `false`.
     * @param onActionClick Callback with the specific action tapped (with its `url`); only
     *                      when [handledBySdk] is `false`.
     */
    @JvmStatic
    @JvmOverloads
    fun showInAppMessage(
        context: Context,
        handledBySdk: Boolean = true,
        onActions: ((List<InAppActionData>) -> Unit)? = null,
        onActionClick: ((InAppActionData) -> Unit)? = null
    ) {
        InngageInapp.showInAppMessage(context, handledBySdk, onActions, onActionClick)
    }
}
