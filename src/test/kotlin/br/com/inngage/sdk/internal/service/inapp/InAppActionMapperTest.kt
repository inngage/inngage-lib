package br.com.inngage.sdk.internal.service.inapp

import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionMapper
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Action
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2ActionType
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Actions
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Button
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Carousel
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2CarouselItem
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Content
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Media
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Style
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("InAppActionMapper")
class InAppActionMapperTest {

    private fun message(items: List<InAppV2CarouselItem>) = InAppMessageV2(
        enabled = true,
        type = "Message",
        style = InAppV2Style(),
        media = InAppV2Media(carousel = InAppV2Carousel(items = items))
    )

    @Test
    @DisplayName("maps every action type to its wire string")
    fun `maps action types`() {
        val items = listOf(
            InAppV2CarouselItem(
                actions = InAppV2Actions(
                    buttons = listOf(
                        InAppV2Button(text = "deep", action = InAppV2Action(InAppV2ActionType.DEEP_LINK, "https://d")),
                        InAppV2Button(text = "web", action = InAppV2Action(InAppV2ActionType.WEBLINK, "https://w")),
                        InAppV2Button(text = "inapp", action = InAppV2Action(InAppV2ActionType.IN_APP_URL, "https://i")),
                        InAppV2Button(text = "meta", action = InAppV2Action(InAppV2ActionType.METADATA, metadata = mapOf("k" to "v"))),
                        InAppV2Button(text = "dismiss", action = InAppV2Action(InAppV2ActionType.DISMISS))
                    )
                )
            )
        )

        val result = InAppActionMapper.map(message(items))

        assertEquals(5, result.size)
        assertEquals(listOf("deeplink", "weblink", "in_app_url", "metadata", "dismiss"), result.map { it.type })
        assertTrue(result.all { it.source == "button" })
        assertEquals(listOf(0, 1, 2, 3, 4), result.map { it.buttonIndex })
        assertTrue(result.all { it.slideIndex == 0 })
        assertEquals("https://d", result[0].url)
        assertEquals(mapOf("k" to "v"), result[3].metadata)
        assertNull(result[3].url) // blank url → null
        assertNull(result[4].url) // dismiss has no url
    }

    @Test
    @DisplayName("tags slideIndex and preserves button text across multiple slides")
    fun `maps multiple slides`() {
        val items = listOf(
            InAppV2CarouselItem(
                content = InAppV2Content(title = "A"),
                actions = InAppV2Actions(buttons = listOf(InAppV2Button(text = "b0", action = InAppV2Action(InAppV2ActionType.WEBLINK, "https://0"))))
            ),
            InAppV2CarouselItem(
                content = InAppV2Content(title = "B"),
                actions = InAppV2Actions(buttons = listOf(InAppV2Button(text = "b1", action = InAppV2Action(InAppV2ActionType.DEEP_LINK, "https://1"))))
            )
        )

        val result = InAppActionMapper.map(message(items))

        assertEquals(2, result.size)
        assertEquals(0, result[0].slideIndex)
        assertEquals("b0", result[0].buttonText)
        assertEquals(1, result[1].slideIndex)
        assertEquals("b1", result[1].buttonText)
    }

    @Test
    @DisplayName("emits a background entry with null buttonText")
    fun `maps background click`() {
        val items = listOf(
            InAppV2CarouselItem(
                actions = InAppV2Actions(
                    backgroundClick = InAppV2Action(InAppV2ActionType.WEBLINK, "https://bg"),
                    buttons = listOf(InAppV2Button(text = "btn", action = InAppV2Action(InAppV2ActionType.DEEP_LINK, "https://d")))
                )
            )
        )

        val result = InAppActionMapper.map(message(items))

        assertEquals(2, result.size)
        assertEquals("button", result[0].source)
        assertEquals("background", result[1].source)
        assertEquals(-1, result[1].buttonIndex)
        assertNull(result[1].buttonText)
        assertEquals("https://bg", result[1].url)
    }

    @Test
    @DisplayName("toData builds a single action tagged with its slide/button context")
    fun `toData single action`() {
        val data = InAppActionMapper.toData(
            slideIndex = 2,
            buttonIndex = 1,
            source = "button",
            buttonText = "Buy",
            action = InAppV2Action(InAppV2ActionType.WEBLINK, "https://buy")
        )

        assertEquals(2, data.slideIndex)
        assertEquals(1, data.buttonIndex)
        assertEquals("button", data.source)
        assertEquals("Buy", data.buttonText)
        assertEquals("weblink", data.type)
        assertEquals("https://buy", data.url)
    }

    @Test
    @DisplayName("a button without action falls back to the slide background click")
    fun `button falls back to background`() {
        val items = listOf(
            InAppV2CarouselItem(
                actions = InAppV2Actions(
                    backgroundClick = InAppV2Action(InAppV2ActionType.DEEP_LINK, "https://bg"),
                    buttons = listOf(InAppV2Button(text = "no-action", action = null))
                )
            )
        )

        val result = InAppActionMapper.map(message(items))

        // button (fallback to bg) + background entry
        assertEquals(2, result.size)
        assertEquals("button", result[0].source)
        assertEquals("deeplink", result[0].type)
        assertEquals("https://bg", result[0].url)
    }

    @Test
    @DisplayName("a button with neither action nor background resolves to dismiss")
    fun `button with no action nor background is dismiss`() {
        val items = listOf(
            InAppV2CarouselItem(
                actions = InAppV2Actions(buttons = listOf(InAppV2Button(text = "x", action = null)))
            )
        )

        val result = InAppActionMapper.map(message(items))

        assertEquals(1, result.size)
        assertEquals("dismiss", result[0].type)
        assertNull(result[0].url)
    }

    @Test
    @DisplayName("returns empty list when there are no items")
    fun `empty when no items`() {
        assertTrue(InAppActionMapper.map(message(emptyList())).isEmpty())
    }
}
