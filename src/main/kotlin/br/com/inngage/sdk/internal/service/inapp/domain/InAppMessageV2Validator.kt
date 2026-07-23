package br.com.inngage.sdk.internal.service.inapp.domain

/**
 * Validates business rules for [InAppMessageV2] before rendering.
 */
internal object InAppMessageV2Validator {

    /**
     * Validates [message] against the following rules:
     * - `enabled` must be `true`
     * - `type` must not be blank
     * - At least one slide must have an image, title, or body
     *
     * @return [Result.success] with the message if valid,
     *         [Result.failure] with an [IllegalStateException] describing the violation.
     */
    fun validate(message: InAppMessageV2): Result<InAppMessageV2> {
        if (!message.enabled) {
            return Result.failure(IllegalStateException("InAppMessageV2 is disabled (enabled=false)"))
        }
        if (message.type.isBlank()) {
            return Result.failure(IllegalStateException("InAppMessageV2 type must not be blank"))
        }
        val hasContent = message.media.carousel.items.any { item ->
            item.image.isNotBlank()
                || item.content.title.isNotBlank()
                || item.content.body.isNotBlank()
        } || !message.style.backgroundImage.isNullOrBlank()

        if (!hasContent) {
            return Result.failure(IllegalStateException("InAppMessageV2 has no renderable content"))
        }
        return Result.success(message)
    }
}
