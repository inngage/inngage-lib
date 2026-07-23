package br.com.inngage.sdk.internal.service.events

import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.service.events.data.EventRepositoryImpl
import br.com.inngage.sdk.internal.service.events.domain.EventEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("EventRepositoryImpl")
class EventRepositoryImplTest {

    private val httpClient = mockk<HttpClient>()
    private val repository = EventRepositoryImpl(httpClient)

    private val expectedEndpoint = InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_EVENTS

    private fun baseEntity(
        identifier: String? = "user@test.com",
        registration: String? = null,
        eventValues: JSONObject? = null,
        conversionValue: Any? = null,
        conversionNotid: String? = null,
        conversionEvent: Boolean = false
    ) = EventEntity(
        appToken        = "app-token-abc",
        eventName       = "purchase",
        identifier      = identifier,
        registration    = registration,
        eventValues     = eventValues,
        conversionValue = conversionValue,
        conversionNotid = conversionNotid,
        conversionEvent = conversionEvent
    )

    private fun capturedBody(): JSONObject {
        var body: JSONObject? = null
        coVerify {
            httpClient.post(
                withArg { payload -> body = payload.getJSONObject("newEventRequest") },
                expectedEndpoint
            )
        }
        return body!!
    }

    // ── Identification ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("identification")
    inner class Identification {

        @Test
        @DisplayName("sends identifier and omits registration when identifier is present")
        fun `sends identifier when present`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.sendEvent(baseEntity(identifier = "user@test.com"))

            val body = capturedBody()
            assertEquals("user@test.com", body.getString("identifier"))
            assertFalse(body.has("registration"))
        }

        @Test
        @DisplayName("sends registration and omits identifier when identifier is absent")
        fun `sends registration when identifier absent`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.sendEvent(baseEntity(identifier = null, registration = "fcm-token-xyz"))

            val body = capturedBody()
            assertEquals("fcm-token-xyz", body.getString("registration"))
            assertFalse(body.has("identifier"))
        }
    }

    // ── Body contract ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("body contract")
    inner class BodyContract {

        @Test
        @DisplayName("always sends app_token, event_name and conversion_event=false by default")
        fun `sends required fields with default conversion_event`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.sendEvent(baseEntity())

            val body = capturedBody()
            assertEquals("app-token-abc", body.getString("app_token"))
            assertEquals("purchase", body.getString("event_name"))
            assertFalse(body.getBoolean("conversion_event"))
        }

        @Test
        @DisplayName("omits event_values when not provided")
        fun `omits event_values when null`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.sendEvent(baseEntity(eventValues = null))

            assertFalse(capturedBody().has("event_values"))
        }

        @Test
        @DisplayName("sends event_values when provided")
        fun `sends event_values when present`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.sendEvent(baseEntity(eventValues = JSONObject().put("amount", 100)))

            val body = capturedBody()
            assertEquals(100, body.getJSONObject("event_values").getInt("amount"))
        }
    }

    // ── Conversion gating ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("conversion gating")
    inner class ConversionGating {

        @Test
        @DisplayName("sends conversion_value and conversion_notid when conversion_event is true")
        fun `sends conversion fields when conversion_event true`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.sendEvent(
                baseEntity(conversionEvent = true, conversionValue = 99.9, conversionNotid = "not-123")
            )

            val body = capturedBody()
            assertTrue(body.getBoolean("conversion_event"))
            assertEquals(99.9, body.getDouble("conversion_value"))
            assertEquals("not-123", body.getString("conversion_notid"))
        }

        @Test
        @DisplayName("omits conversion_value and conversion_notid when conversion_event is false")
        fun `omits conversion fields when conversion_event false`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.sendEvent(
                baseEntity(conversionEvent = false, conversionValue = 99.9, conversionNotid = "not-123")
            )

            val body = capturedBody()
            assertFalse(body.getBoolean("conversion_event"))
            assertFalse(body.has("conversion_value"))
            assertFalse(body.has("conversion_notid"))
        }
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("error handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("returns failure when HTTP call fails")
        fun `returns failure on HTTP error`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns
                Result.failure(RuntimeException("network error"))

            val result = repository.sendEvent(baseEntity())

            assertTrue(result.isFailure)
        }
    }
}
