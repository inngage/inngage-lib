package br.com.inngage.sdk.internal.service.subscription

import br.com.inngage.sdk.internal.service.subscription.application.AddUserDataUseCase
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriptionRepository
import br.com.inngage.sdk.internal.service.subscription.domain.UserDataEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AddUserDataUseCase")
class AddUserDataUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<SubscriptionRepository>()

    private fun useCase(persistedIdentifier: String = "persisted-uuid") =
        AddUserDataUseCase(repository, { persistedIdentifier }, dispatcher)

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        fun `returns failure for invalid appToken`() = runTest(dispatcher) {
            val result = useCase().execute("short", email = "a@b.com")
            assertTrue(result.isFailure)
            assertInstanceOf(IllegalArgumentException::class.java, result.exceptionOrNull())
        }

        @Test
        fun `returns failure when no field is provided`() = runTest(dispatcher) {
            val result = useCase().execute("valid-app-token")
            assertTrue(result.isFailure)
            assertInstanceOf(IllegalArgumentException::class.java, result.exceptionOrNull())
        }
    }

    // ── Identifier resolution ─────────────────────────────────────────────────

    @Nested
    @DisplayName("identifier resolution")
    inner class IdentifierResolution {

        @Test
        fun `uses provided identifier`() = runTest(dispatcher) {
            coEvery { repository.addUserData(any()) } returns Result.success(Unit)

            useCase().execute("valid-app-token", identifier = "user-42")

            coVerify(exactly = 1) {
                repository.addUserData(match { e: UserDataEntity -> e.identifier == "user-42" })
            }
        }

        @Test
        fun `falls back to the identifier persisted by subscribe`() = runTest(dispatcher) {
            coEvery { repository.addUserData(any()) } returns Result.success(Unit)

            useCase().execute("valid-app-token", email = "cliente@example.com")

            coVerify(exactly = 1) {
                repository.addUserData(match { e: UserDataEntity -> e.identifier == "persisted-uuid" })
            }
        }

        @Test
        fun `omits identifier when none is available`() = runTest(dispatcher) {
            coEvery { repository.addUserData(any()) } returns Result.success(Unit)

            useCase(persistedIdentifier = "").execute("valid-app-token", email = "cliente@example.com")

            coVerify(exactly = 1) {
                repository.addUserData(match { e: UserDataEntity -> e.identifier == null })
            }
        }
    }

    // ── Delegation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delegation")
    inner class Delegation {

        @Test
        fun `delegates all fields to the repository`() = runTest(dispatcher) {
            coEvery { repository.addUserData(any()) } returns Result.success(Unit)
            val fields = JSONObject().put("plano", "premium").put("pontos", 1500)

            val result = useCase().execute(
                "valid-app-token",
                identifier  = "user-42",
                customFields = fields,
                email       = "cliente@example.com",
                phoneNumber = "81988887777"
            )

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                repository.addUserData(match { e: UserDataEntity ->
                    e.appToken == "valid-app-token" &&
                    e.identifier == "user-42" &&
                    e.customFields === fields &&
                    e.email == "cliente@example.com" &&
                    e.phoneNumber == "81988887777"
                })
            }
        }

        @Test
        fun `returns failure when repository fails`() = runTest(dispatcher) {
            coEvery { repository.addUserData(any()) } returns
                Result.failure(RuntimeException("network error"))

            val result = useCase().execute("valid-app-token", email = "a@b.com")
            assertTrue(result.isFailure)
        }
    }
}
