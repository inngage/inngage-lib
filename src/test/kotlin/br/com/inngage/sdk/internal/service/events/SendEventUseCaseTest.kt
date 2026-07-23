package br.com.inngage.sdk.internal.service.events

import br.com.inngage.sdk.internal.service.events.application.SendEventUseCase
import br.com.inngage.sdk.internal.service.events.domain.EventEntity
import br.com.inngage.sdk.internal.service.events.domain.EventRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SendEventUseCase")
class SendEventUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<EventRepository>()

    private fun useCase(registration: String = "fcm-token-xyz") =
        SendEventUseCase(repository, { registration }, dispatcher)

    // ── Input validation ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("input validation")
    inner class InputValidation {

        @Test
        fun `returns failure for blank appToken`() = runTest(dispatcher) {
            val result = useCase().execute("", "event")
            assertTrue(result.isFailure)
            assertInstanceOf(IllegalArgumentException::class.java, result.exceptionOrNull())
            assertTrue(result.exceptionOrNull()?.message?.contains("appToken") == true)
        }

        @Test
        fun `returns failure for blank eventName`() = runTest(dispatcher) {
            val result = useCase().execute("token123", "")
            assertTrue(result.isFailure)
            assertInstanceOf(IllegalArgumentException::class.java, result.exceptionOrNull())
        }

        @Test
        fun `returns failure when identifier is absent and no FCM token is persisted`() = runTest(dispatcher) {
            val result = useCase(registration = "").execute("token123", "event")
            assertTrue(result.isFailure)
            assertInstanceOf(IllegalStateException::class.java, result.exceptionOrNull())
        }
    }

    // ── Identification rule ───────────────────────────────────────────────────

    @Nested
    @DisplayName("identification rule")
    inner class IdentificationRule {

        @Test
        fun `uses identifier when provided and omits registration`() = runTest(dispatcher) {
            coEvery { repository.sendEvent(any()) } returns Result.success(Unit)

            useCase().execute("tok", "purchase", identifier = "user@test.com")

            coVerify(exactly = 1) {
                repository.sendEvent(match { e: EventEntity ->
                    e.identifier == "user@test.com" && e.registration == null
                })
            }
        }

        @Test
        fun `falls back to registration when identifier is null`() = runTest(dispatcher) {
            coEvery { repository.sendEvent(any()) } returns Result.success(Unit)

            useCase().execute("tok", "purchase")

            coVerify(exactly = 1) {
                repository.sendEvent(match { e: EventEntity ->
                    e.identifier == null && e.registration == "fcm-token-xyz"
                })
            }
        }

        @Test
        fun `treats blank identifier as absent`() = runTest(dispatcher) {
            coEvery { repository.sendEvent(any()) } returns Result.success(Unit)

            useCase().execute("tok", "purchase", identifier = "  ")

            coVerify(exactly = 1) {
                repository.sendEvent(match { e: EventEntity ->
                    e.identifier == null && e.registration == "fcm-token-xyz"
                })
            }
        }
    }

    // ── Success ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("successful event")
    inner class SuccessPath {

        @Test
        fun `delegates to repository with correct entity`() = runTest(dispatcher) {
            coEvery { repository.sendEvent(any()) } returns Result.success(Unit)
            val values = JSONObject().put("amount", 100)

            val result = useCase().execute(
                "appToken1", "purchase",
                identifier      = "user@test.com",
                eventValues     = values,
                conversionValue = 99.9,
                conversionNotid = "not-123",
                conversionEvent = true
            )

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                repository.sendEvent(match { e: EventEntity ->
                    e.appToken == "appToken1" &&
                    e.eventName == "purchase" &&
                    e.identifier == "user@test.com" &&
                    e.eventValues != null &&
                    e.conversionValue == 99.9 &&
                    e.conversionNotid == "not-123" &&
                    e.conversionEvent
                })
            }
        }

        @Test
        fun `conversionEvent defaults to false`() = runTest(dispatcher) {
            coEvery { repository.sendEvent(any()) } returns Result.success(Unit)

            val result = useCase().execute("appToken1", "login", identifier = "user@test.com")

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                repository.sendEvent(match { e: EventEntity -> !e.conversionEvent })
            }
        }
    }

    // ── Failure ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("failure path")
    inner class FailurePath {

        @Test
        fun `returns failure when repository fails`() = runTest(dispatcher) {
            coEvery { repository.sendEvent(any()) } returns Result.failure(RuntimeException("network error"))

            val result = useCase().execute("appToken1", "event", identifier = "id")
            assertTrue(result.isFailure)
        }
    }
}
