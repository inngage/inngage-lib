package br.com.inngage.sdk.internal.service.inapp.domain

/**
 * Maps the internal [InAppMessageV2] model into the public [InAppActionData] list
 * handed to the host app when it opts to handle In-App actions itself.
 *
 * Pure Kotlin — no Android dependencies — so it is fully unit-testable on the JVM.
 */
internal object InAppActionMapper {

    /**
     * Flattens every actionable element of [message] into a list of [InAppActionData]:
     * one entry per button and one per slide background click, across all slides,
     * tagged with the originating [InAppActionData.slideIndex] and
     * [InAppActionData.source].
     *
     * A button with no explicit action falls back to the slide's background click
     * (mirroring the SDK's own click resolution); when neither is present it resolves
     * to a `"dismiss"` action.
     */
    fun map(message: InAppMessageV2): List<InAppActionData> =
        message.media.carousel.items.flatMapIndexed { index, item ->
            val entries = mutableListOf<InAppActionData>()

            item.actions.buttons.forEachIndexed { buttonIndex, button ->
                val action = button.action ?: item.actions.backgroundClick
                entries += toData(index, buttonIndex, "button", button.text, action)
            }

            item.actions.backgroundClick?.let { bg ->
                entries += toData(index, -1, "background", null, bg)
            }

            entries
        }

    /**
     * Builds a single [InAppActionData] for one action, tagged with its origin. Used both
     * for the on-display list ([map]) and for the per-click callback so the two paths stay
     * consistent.
     */
    fun toData(
        slideIndex: Int,
        buttonIndex: Int,
        source: String,
        buttonText: String?,
        action: InAppV2Action?
    ): InAppActionData = InAppActionData(
        slideIndex  = slideIndex,
        buttonIndex = buttonIndex,
        source      = source,
        buttonText  = buttonText,
        type        = typeString(action?.type),
        url         = action?.url?.takeIf { it.isNotBlank() },
        metadata    = action?.metadata ?: emptyMap()
    )

    private fun typeString(type: InAppV2ActionType?): String = when (type) {
        InAppV2ActionType.IN_APP_URL -> "in_app_url"
        InAppV2ActionType.DEEP_LINK  -> "deeplink"
        InAppV2ActionType.WEBLINK    -> "weblink"
        InAppV2ActionType.METADATA   -> "metadata"
        InAppV2ActionType.DISMISS, null -> "dismiss"
    }
}
