package br.com.inngage.sdk.internal.service.inapp.domain

/**
 * Public, dependency-free representation of a single In-App Message action
 * (a button or a slide background tap).
 *
 * Delivered to the consumer via
 * [br.com.inngage.sdk.InngageClient.showInAppMessage]'s `onActions` callback when
 * `handledBySdk = false` — i.e. when the host app opts to handle the actions itself
 * instead of letting the SDK navigate.
 *
 * A single In-App Message produces a list of these: one entry per button and one per
 * slide background click, across all slides.
 *
 * @property slideIndex  Zero-based index of the slide this action belongs to.
 * @property buttonIndex Zero-based index of the button within the slide; `-1` for a
 *                       `"background"` action. Together with [slideIndex] this uniquely
 *                       identifies which button on which slide was tapped.
 * @property source      Where the action comes from: `"button"` or `"background"`.
 * @property buttonText  Button label; `null` for a `"background"` action.
 * @property type        Action type: `"deeplink"`, `"weblink"`, `"in_app_url"`,
 *                        `"metadata"` or `"dismiss"`.
 * @property url         Destination URL/URI associated with [type]; `null` when absent.
 * @property metadata    Arbitrary key-value pairs for `"metadata"` actions (empty otherwise).
 */
data class InAppActionData(
    val slideIndex: Int,
    val buttonIndex: Int,
    val source: String,
    val buttonText: String?,
    val type: String,
    val url: String?,
    val metadata: Map<String, String>
)
