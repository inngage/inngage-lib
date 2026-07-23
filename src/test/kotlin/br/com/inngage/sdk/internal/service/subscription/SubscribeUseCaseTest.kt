package br.com.inngage.sdk.internal.service.subscription

import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.platform.firebase.FirebaseTokenProvider
import br.com.inngage.sdk.internal.service.subscription.application.SubscribeUseCase
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriberEntity
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SubscribeUseCase")
class SubscribeUseCaseTest {

    private val dispatcher    = UnconfinedTestDispatcher()
    private val repository    = mockk<SubscriptionRepository>()
    private val tokenProvider = mockk<FirebaseTokenProvider>()

    private val useCase = SubscribeUseCase(
        repository    = repository,
        tokenProvider = tokenProvider,
        dispatcher    = dispatcher
    )

    private val validToken    = "valid-app-token"
    private val fakeFcmToken  = "fcm-device-token-xyz"

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("input validation")
    inner class InputValidation {

        @Test
        fun `returns failure for invalid appToken`() = runTest(dispatcher) {
            val result = useCase.execute("bad")
            assertTrue(result.isFailure)
            assertInstanceOf(IllegalArgumentException::class.java, result.exceptionOrNull())
        }

        @Test
        fun `returns failure for blank identifier`() = runTest(dispatcher) {
            val result = useCase.execute(validToken, identifier = "")
            assertTrue(result.isFailure)
        }
    }

    // ── FCM token failure ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("FCM token retrieval")
    inner class FcmTokenRetrieval {

        @Test
        fun `returns failure when tokenProvider throws`() = runTest(dispatcher) {
            coEvery { tokenProvider.getToken() } throws RuntimeException("Firebase unavailable")

            val result = useCase.execute(validToken)
            assertTrue(result.isFailure)
        }
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("successful subscription")
    inner class SuccessPath {

        @Test
        fun `delegates to repository with correct entity`() = runTest(dispatcher) {
            coEvery { tokenProvider.getToken() } returns fakeFcmToken
            coEvery { repository.subscribe(any()) } returns Result.success(Unit)

            val result = useCase.execute(
                appToken    = validToken,
                identifier  = "user@test.com",
                email       = "user@test.com"
            )

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                repository.subscribe(match { entity: SubscriberEntity ->
                    entity.appToken == validToken &&
                    entity.registration == fakeFcmToken &&
                    entity.identifier == "user@test.com"
                })
            }
        }

        @Test
        fun `returns failure when repository fails`() = runTest(dispatcher) {
            coEvery { tokenProvider.getToken() } returns fakeFcmToken
            coEvery { repository.subscribe(any()) } returns Result.failure(RuntimeException("network error"))

            val result = useCase.execute(validToken)
            assertTrue(result.isFailure)
        }
    }
}
