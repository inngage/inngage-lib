package br.com.inngage.sdk.internal.service.inapp.data

import android.content.Context
import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2Repository
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Action
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2ActionType
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Actions
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Button
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2ButtonStyle
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Carousel
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2CarouselItem
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Content
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Media
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Style
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fetches and parses [InAppMessageV2] from the Inngage in-app endpoint via HTTP POST.
 *
 * Request body: `{ appId, registration, firstAccess, channelId }`.
 * `appId` is persisted from the subscription response and `registration` is the
 * FCM token persisted during subscription — both are required; the fetch is
 * skipped until a subscription has completed.
 * `firstAccess` is `true` until the first successful (HTTP 200) fetch, then `false`.
 *
 * @param context    Application context — used to read [PreferencesStorage].
 * @param httpClient HTTP client used for the POST request.
 */
internal class InAppMessageV2RepositoryImpl(
    private val context: Context,
    private val httpClient: HttpClient = HttpClient()
) : InAppMessageV2Repository {

    private val storage = PreferencesStorage(context)

    companion object {
        private const val TAG = InngageConfig.TAG_INAPP
    }

    override suspend fun fetchMessage(): Result<InAppMessageV2?> {
        val endpoint = InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_INAPP_V2

        val appId = storage.getString(InngageConfig.PREF_APP_ID)
        val registration = storage.getString(InngageConfig.PREF_FCM_TOKEN)

        Log.d(
            TAG,
            "InApp fetchMessage → loaded from prefs: " +
                "appId=${if (appId.isBlank()) "(missing)" else "'$appId'"}, " +
                "registration=${if (registration.isBlank()) "(missing)" else "'${registration.take(12)}…' (${registration.length} chars)"}"
        )

        if (appId.isBlank() || registration.isBlank()) {
            val missing = listOfNotNull(
                "appId".takeIf { appId.isBlank() },
                "registration".takeIf { registration.isBlank() }
            ).joinToString(" and ")
            Log.w(TAG, "InApp fetchMessage skipped — $missing not available yet (subscription pending)")
            return Result.failure(
                IllegalStateException("$missing missing — subscribe must complete first")
            )
        }
        val firstAccess = storage.getBoolean(InngageConfig.PREF_INAPP_FIRST_ACCESS, true)

        val body = JSONObject().apply {
            put("appId", appId.toIntOrNull() ?: appId)
            put("registration", registration)
            put("firstAccess", firstAccess)
            put("channelId", InngageConfig.INAPP_CHANNEL_ID)
        }

        Log.d(TAG, "InApp fetchMessage → endpoint: $endpoint, appId: '$appId', firstAccess: $firstAccess")

        return httpClient.post(body, endpoint).mapCatching { json ->
            // Only flip firstAccess after a successful response, so a failed
            // first attempt still counts as first access on the next try.
            if (firstAccess) {
                storage.putBoolean(InngageConfig.PREF_INAPP_FIRST_ACCESS, false)
            }
            val root = JSONObject(json)
            val result = parseResponse(root)
            Log.d(TAG, "InApp fetchMessage ← parsed: ${if (result == null) "null (no enabled inApp)" else "InAppMessageV2(type=${result.type})"}")
            result
        }
    }

    private fun parseResponse(root: JSONObject): InAppMessageV2? {
        // Support every response envelope the backend may send:
        //   { "inAppMessage": { ... } }   ← wrapped
        //   { "payload":      { ... } }   ← wrapped (legacy)
        //   { "type": ..., "media": ... } ← flat (current production format)
        val payloadObj = root.optJSONObject("inAppMessage")
            ?: root.optJSONObject("payload")
            ?: root

        // An explicit `enabled:false` suppresses the message.
        if (payloadObj.has("enabled") && !payloadObj.optBoolean("enabled")) return null

        // Distinguish "here is an in-app" from an unrelated / empty response:
        // the production payload has no `enabled` flag, so we key off the presence
        // of the message structure (media/type) instead.
        if (!payloadObj.has("media") && !payloadObj.has("type")) return null

        return parseInAppMessage(payloadObj)
    }

    private fun parseInAppMessage(obj: JSONObject) = InAppMessageV2(
        enabled = obj.optBoolean("enabled", true),
        type    = obj.optString("type", "Banner"),
        style   = obj.optJSONObject("style")?.let { parseStyle(it) } ?: InAppV2Style(),
        media   = obj.optJSONObject("media")?.let { parseMedia(it) } ?: InAppV2Media()
    )

    private fun parseStyle(obj: JSONObject) = InAppV2Style(
        position        = obj.optString("position", "center"),
        backgroundColor = obj.optString("backgroundColor"),
        backgroundImage = obj.optString("backgroundImage").takeIf { it.isNotBlank() },
        borderColor     = obj.optString("borderColor"),
        hasShadow       = obj.optBoolean("shadow", false),
        titleColor      = obj.optString("titleColor", "#000000"),
        bodyColor       = obj.optString("bodyColor", "#000000")
    )

    private fun parseMedia(obj: JSONObject): InAppV2Media {
        // Production payload puts `items`/`position` directly under `media`.
        // A nested `media.carousel` wrapper is also accepted for compatibility.
        val source = obj.optJSONObject("carousel") ?: obj
        return InAppV2Media(carousel = parseCarousel(source))
    }

    private fun parseCarousel(obj: JSONObject) = InAppV2Carousel(
        isEnabled = obj.optBoolean("enabled", false),
        position  = obj.optString("position", "TOP"),
        items     = obj.optJSONArray("items")?.let { parseCarouselItems(it) } ?: emptyList()
    )

    private fun parseCarouselItems(arr: JSONArray): List<InAppV2CarouselItem> =
        (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            InAppV2CarouselItem(
                image     = item.optString("image"),
                imageType = item.optString("imageType", "fill"),
                content   = item.optJSONObject("content")?.let { parseContent(it) } ?: InAppV2Content(),
                actions   = item.optJSONObject("actions")?.let { parseActions(it) } ?: InAppV2Actions()
            )
        }

    private fun parseContent(obj: JSONObject) = InAppV2Content(
        title = obj.optString("title"),
        body  = obj.optString("body")
    )

    private fun parseActions(obj: JSONObject) = InAppV2Actions(
        backgroundClick = obj.optJSONObject("backgroundClick")?.let { parseAction(it) },
        buttons         = obj.optJSONArray("buttons")?.let { parseButtons(it) } ?: emptyList()
    )

    private fun parseButtons(arr: JSONArray): List<InAppV2Button> =
        (0 until arr.length()).map { i ->
            val btn      = arr.getJSONObject(i)
            val styleObj = btn.optJSONObject("style")
            InAppV2Button(
                text   = btn.optString("text"),
                style  = InAppV2ButtonStyle(
                    backgroundColor = styleObj?.optString("backgroundColor") ?: "#000000",
                    textColor       = styleObj?.optString("textColor") ?: "#FFFFFF",
                    hoverColor      = styleObj?.optString("hoverColor") ?: ""
                ),
                action = btn.optJSONObject("action")?.let { parseAction(it) }
            )
        }

    private fun parseAction(obj: JSONObject): InAppV2Action {
        val typeStr = obj.optString("type", "dismiss").lowercase()
        val actionType = when (typeStr) {
            "deeplink", "deep_link" -> InAppV2ActionType.DEEP_LINK
            "weblink"               -> InAppV2ActionType.WEBLINK
            "metadata"              -> InAppV2ActionType.METADATA
            "in_app_url", "inapp"   -> InAppV2ActionType.IN_APP_URL
            else                    -> InAppV2ActionType.DISMISS
        }
        val metadataMap = obj.optJSONObject("metadata")?.let { meta ->
            meta.keys().asSequence().associate { key -> key to meta.optString(key) }
        } ?: emptyMap()
        return InAppV2Action(type = actionType, url = obj.optString("url"), metadata = metadataMap)
    }
}
