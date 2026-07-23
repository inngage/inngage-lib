package br.com.inngage.sdk.internal.service.inapp

import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2Validator
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Carousel
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2CarouselItem
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Content
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Media
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Style
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("InAppMessageV2Validator")
class InAppMessageV2ValidatorTest {

    private fun baseMessage(
        enabled: Boolean = true,
        type: String = "Banner",
        backgroundImage: String? = null,
        items: List<InAppV2CarouselItem> = listOf(
            InAppV2CarouselItem(content = InAppV2Content(title = "Hello"))
        )
    ) = InAppMessageV2(
        enabled = enabled,
        type    = type,
        style   = InAppV2Style(backgroundImage = backgroundImage),
        media   = InAppV2Media(InAppV2Carousel(isEnabled = items.isNotEmpty(), items = items))
    )

    @Nested
    @DisplayName("valid messages")
    inner class ValidMessages {

        @Test
        fun `passes when a slide has a title`() {
            val result = InAppMessageV2Validator.validate(baseMessage())
            assertTrue(result.isSuccess)
        }

        @Test
        fun `passes when a slide has only a body`() {
            val items = listOf(InAppV2CarouselItem(content = InAppV2Content(body = "Body text")))
            val result = InAppMessageV2Validator.validate(baseMessage(items = items))
            assertTrue(result.isSuccess)
        }

        @Test
        fun `passes when a slide has only an image`() {
            val items = listOf(InAppV2CarouselItem(image = "https://img1.url"))
            val result = InAppMessageV2Validator.validate(baseMessage(items = items))
            assertTrue(result.isSuccess)
        }

        @Test
        fun `passes when only backgroundImage is set`() {
            val result = InAppMessageV2Validator.validate(
                baseMessage(backgroundImage = "https://bg.url", items = emptyList())
            )
            assertTrue(result.isSuccess)
        }
    }

    @Nested
    @DisplayName("invalid messages")
    inner class InvalidMessages {

        @Test
        fun `fails when enabled is false`() {
            val result = InAppMessageV2Validator.validate(baseMessage(enabled = false))
            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
        }

        @Test
        fun `fails when type is blank`() {
            val result = InAppMessageV2Validator.validate(baseMessage(type = ""))
            assertTrue(result.isFailure)
        }

        @Test
        fun `fails when no slide has renderable content`() {
            val items = listOf(InAppV2CarouselItem())
            val result = InAppMessageV2Validator.validate(baseMessage(items = items))
            assertTrue(result.isFailure)
        }

        @Test
        fun `fails when there are no slides and no backgroundImage`() {
            val result = InAppMessageV2Validator.validate(baseMessage(items = emptyList()))
            assertTrue(result.isFailure)
        }
    }
}
