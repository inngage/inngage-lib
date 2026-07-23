package br.com.inngage.sdk.internal.service.inapp

import br.com.inngage.sdk.internal.service.inapp.application.InAppCallbackHolder
import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("InAppCallbackHolder")
class InAppCallbackHolderTest {

    @AfterEach
    fun tearDown() = InAppCallbackHolder.clear()

    @Test
    @DisplayName("set stores the flag and both callbacks")
    fun `set stores values`() {
        var receivedList: List<InAppActionData>? = null
        var receivedClick: InAppActionData? = null
        InAppCallbackHolder.set(
            handledBySdk = false,
            onActions = { receivedList = it },
            onActionClick = { receivedClick = it }
        )

        assertEquals(false, InAppCallbackHolder.handledBySdk)
        InAppCallbackHolder.onActions?.invoke(emptyList())
        InAppCallbackHolder.onActionClick?.invoke(
            InAppActionData(0, 0, "button", "x", "deeplink", "https://x", emptyMap())
        )
        assertTrue(receivedList != null)
        assertTrue(receivedClick != null)
    }

    @Test
    @DisplayName("clear resets to defaults (handledBySdk=true, no callbacks)")
    fun `clear resets`() {
        InAppCallbackHolder.set(handledBySdk = false, onActions = { }, onActionClick = { })
        InAppCallbackHolder.clear()

        assertTrue(InAppCallbackHolder.handledBySdk)
        assertNull(InAppCallbackHolder.onActions)
        assertNull(InAppCallbackHolder.onActionClick)
    }
}
