package br.com.inngage.sdk.internal.service.inapp.domain

import java.io.Serializable

/**
 * Root domain model for an In-App Message v2 payload.
 *
 * Implements [Serializable] so it can be handed directly to
 * [br.com.inngage.sdk.internal.service.inapp.InAppMessageV2Activity] as an Intent
 * extra — no intermediate JSON (de)serialisation.
 *
 * Whether the message renders as a **banner** or a **carousel** is decided at render
 * time by the number of slides: a single [InAppV2Media.carousel] item is a banner,
 * more than one is a carousel.
 *
 * @property enabled Whether this message should be shown.
 * @property type    Display type reported by the backend (e.g. "Message", "Banner").
 * @property style   Global visual style (background color, border, title/body colors, position).
 * @property media   Media configuration containing the slides.
 */
internal data class InAppMessageV2(
    val enabled: Boolean,
    val type: String,
    val style: InAppV2Style,
    val media: InAppV2Media
) : Serializable

/** Global visual style for the in-app message card. */
internal data class InAppV2Style(
    val position: String = "center",
    val backgroundColor: String = "",
    val backgroundImage: String? = null,
    val borderColor: String = "",
    val hasShadow: Boolean = false,
    val titleColor: String = "#000000",
    val bodyColor: String = "#000000"
) : Serializable

/** Media wrapper containing the carousel / slide config. */
internal data class InAppV2Media(
    val carousel: InAppV2Carousel = InAppV2Carousel()
) : Serializable

/** Carousel configuration — also used for single-slide (Banner) display. */
internal data class InAppV2Carousel(
    val isEnabled: Boolean = false,
    val position: String = "TOP",
    val items: List<InAppV2CarouselItem> = emptyList()
) : Serializable

/**
 * A single slide with optional image, per-slide textual content, and per-slide actions.
 *
 * @property image     Remote image URL (may be blank when there is no image for this slide).
 * @property imageType Hint for image scaling — "fill" maps to CENTER_CROP.
 * @property content   Optional title and body for this slide.
 * @property actions   Optional background-click action and buttons for this slide.
 */
internal data class InAppV2CarouselItem(
    val image: String = "",
    val imageType: String = "fill",
    val content: InAppV2Content = InAppV2Content(),
    val actions: InAppV2Actions = InAppV2Actions()
) : Serializable

/** Textual content fields for a slide. */
internal data class InAppV2Content(
    val title: String = "",
    val body: String = ""
) : Serializable

/** Actions container (background tap + button list). */
internal data class InAppV2Actions(
    val backgroundClick: InAppV2Action? = null,
    val buttons: List<InAppV2Button> = emptyList()
) : Serializable

/** An individual button rendered inside a slide. */
internal data class InAppV2Button(
    val text: String = "",
    val style: InAppV2ButtonStyle = InAppV2ButtonStyle(),
    val action: InAppV2Action? = null
) : Serializable

/** Visual style for an individual button. */
internal data class InAppV2ButtonStyle(
    val backgroundColor: String = "#000000",
    val textColor: String = "#FFFFFF",
    val hoverColor: String = ""
) : Serializable

/**
 * A resolved action attached to a button, carousel item background, or card background.
 *
 * @property type     What to do when the action fires.
 * @property url      Target URL or deep-link URI (empty for [InAppV2ActionType.DISMISS]).
 * @property metadata Arbitrary key-value pairs for [InAppV2ActionType.METADATA] actions.
 */
internal data class InAppV2Action(
    val type: InAppV2ActionType = InAppV2ActionType.DISMISS,
    val url: String = "",
    val metadata: Map<String, String> = emptyMap()
) : Serializable

/** Supported action types for in-app message interactions. */
internal enum class InAppV2ActionType {
    /** Open URL in the in-app browser (Chrome Custom Tab). */
    IN_APP_URL,
    /** Fire a deep-link Intent. */
    DEEP_LINK,
    /** Open URL in the external browser. */
    WEBLINK,
    /** Deliver structured metadata to the host app — no navigation. */
    METADATA,
    /** Simply dismiss the in-app message. */
    DISMISS
}
