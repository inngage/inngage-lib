package br.com.inngage.sdk.internal.service.location

import br.com.inngage.sdk.internal.service.location.application.UpdateLocationUseCase
import br.com.inngage.sdk.internal.service.location.domain.Coordinates
import br.com.inngage.sdk.internal.service.location.domain.LocationCache
import br.com.inngage.sdk.internal.service.location.domain.LocationFailure
import br.com.inngage.sdk.internal.service.location.domain.LocationRepository
import br.com.inngage.sdk.internal.service.location.domain.LocationUpdateEntity
import br.com.inngage.sdk.internal.service.location.domain.LocationUpdateResult
import br.com.inngage.sdk.internal.service.location.domain.SentLocation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("UpdateLocationUseCase")
class UpdateLocationUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<LocationRepository>()
    private val cache      = mockk<LocationCache>(relaxed = true)

    private val saoPaulo = Coordinates(-23.5505, -46.6333)
    private val now      = 1_000_000L

    @BeforeEach
    fun setUp() {
        every { cache.lastSent() } returns null
        coEvery { repository.sendLocation(any()) } returns Result.success(LocationUpdateResult.Recorded)
    }

    private fun useCase(
        device: Coordinates? = saoPaulo,
        identifier: String = "persisted-uuid",
        registration: String = "fcm-token",
        uuid: String = "install-uuid",
        clock: Long = now
    ) = UpdateLocationUseCase(
        repository             = repository,
        cache                  = cache,
        deviceLocationProvider = { device },
        identifierProvider     = { identifier },
        registrationProvider   = { registration },
        uuidProvider           = { uuid },
        clock                  = { clock },
        dispatcher             = dispatcher
    )

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        fun `rejects short appToken`() = runTest(dispatcher) {
            val result = useCase().execute("short")
            assertTrue(result.isFailure)
            assertInstanceOf(LocationFailure.InvalidAppToken::class.java, result.exceptionOrNull())
            coVerify(exactly = 0) { repository.sendLocation(any()) }
        }

        @Test
        fun `rejects latitude without longitude`() = runTest(dispatcher) {
            val result = useCase().execute("valid-app-token", latitude = -23.0)
            assertInstanceOf(LocationFailure.InvalidCoordinates::class.java, result.exceptionOrNull())
        }

        @Test
        fun `rejects out-of-range coordinates`() = runTest(dispatcher) {
            val result = useCase().execute("valid-app-token", latitude = 91.0, longitude = 10.0)
            assertInstanceOf(LocationFailure.InvalidCoordinates::class.java, result.exceptionOrNull())
            coVerify(exactly = 0) { repository.sendLocation(any()) }
        }

        @Test
        fun `accepts zero coordinates`() = runTest(dispatcher) {
            val result = useCase().execute("valid-app-token", latitude = 0.0, longitude = 0.0)
            assertTrue(result.isSuccess)
            coVerify { repository.sendLocation(match { it.coordinates == Coordinates(0.0, 0.0) }) }
        }

        @Test
        fun `fails when no identity is available`() = runTest(dispatcher) {
            val result = useCase(identifier = "", registration = "", uuid = "").execute("valid-app-token")
            assertInstanceOf(LocationFailure.MissingIdentity::class.java, result.exceptionOrNull())
            coVerify(exactly = 0) { repository.sendLocation(any()) }
        }
    }

    // ── Coordinates source ────────────────────────────────────────────────────

    @Nested
    @DisplayName("coordinates")
    inner class CoordinatesSource {

        @Test
        fun `explicit coordinates win over device position`() = runTest(dispatcher) {
            useCase(device = saoPaulo).execute("valid-app-token", latitude = 10.0, longitude = 20.0)
            coVerify { repository.sendLocation(match { it.coordinates == Coordinates(10.0, 20.0) }) }
        }

        @Test
        fun `reads device position when none given`() = runTest(dispatcher) {
            useCase(device = saoPaulo).execute("valid-app-token")
            coVerify { repository.sendLocation(match { it.coordinates == saoPaulo }) }
        }

        @Test
        fun `returns Unavailable without calling the API when device has no fix`() = runTest(dispatcher) {
            val result = useCase(device = null).execute("valid-app-token")
            assertEquals(LocationUpdateResult.Unavailable, result.getOrNull())
            coVerify(exactly = 0) { repository.sendLocation(any()) }
        }

        @Test
        fun `treats a throwing provider as Unavailable`() = runTest(dispatcher) {
            val uc = UpdateLocationUseCase(
                repository = repository, cache = cache,
                deviceLocationProvider = { throw IllegalStateException("boom") },
                uuidProvider = { "u" }, dispatcher = dispatcher
            )
            assertEquals(LocationUpdateResult.Unavailable, uc.execute("valid-app-token").getOrNull())
        }
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("identity")
    inner class Identity {

        @Test
        fun `explicit identifier is sent with persisted registration and uuid`() = runTest(dispatcher) {
            val sent = slot<LocationUpdateEntity>()
            coEvery { repository.sendLocation(capture(sent)) } returns Result.success(LocationUpdateResult.Recorded)

            useCase().execute("valid-app-token", identifier = "user@test.com")

            assertEquals("user@test.com", sent.captured.identifier)
            assertEquals("fcm-token", sent.captured.registration)
            assertEquals("install-uuid", sent.captured.uuid)
            assertEquals("valid-app-token", sent.captured.appToken)
        }

        @Test
        fun `falls back to persisted identifier`() = runTest(dispatcher) {
            val sent = slot<LocationUpdateEntity>()
            coEvery { repository.sendLocation(capture(sent)) } returns Result.success(LocationUpdateResult.Recorded)

            useCase().execute("valid-app-token")

            assertEquals("persisted-uuid", sent.captured.identifier)
        }

        @Test
        fun `omits blank identifiers`() = runTest(dispatcher) {
            val sent = slot<LocationUpdateEntity>()
            coEvery { repository.sendLocation(capture(sent)) } returns Result.success(LocationUpdateResult.Recorded)

            useCase(identifier = "", registration = "").execute("valid-app-token")

            assertEquals(null, sent.captured.identifier)
            assertEquals(null, sent.captured.registration)
            assertEquals("install-uuid", sent.captured.uuid)
        }
    }

    // ── Throttle ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("throttle")
    inner class Throttle {

        @Test
        fun `skips when same place and less than 5 minutes ago`() = runTest(dispatcher) {
            every { cache.lastSent() } returns SentLocation(saoPaulo, now - 60_000L)

            val result = useCase().execute("valid-app-token")

            assertEquals(LocationUpdateResult.Throttled, result.getOrNull())
            coVerify(exactly = 0) { repository.sendLocation(any()) }
        }

        @Test
        fun `sends when moved more than 100 m even if recent`() = runTest(dispatcher) {
            every { cache.lastSent() } returns SentLocation(Coordinates(-23.5505, -46.6333), now - 60_000L)
            // ~1.1 km north
            val result = useCase(device = Coordinates(-23.5405, -46.6333)).execute("valid-app-token")

            assertEquals(LocationUpdateResult.Recorded, result.getOrNull())
            coVerify(exactly = 1) { repository.sendLocation(any()) }
        }

        @Test
        fun `sends when same place but more than 5 minutes ago`() = runTest(dispatcher) {
            every { cache.lastSent() } returns SentLocation(saoPaulo, now - 6 * 60_000L)

            useCase().execute("valid-app-token")

            coVerify(exactly = 1) { repository.sendLocation(any()) }
        }

        @Test
        fun `does not throttle when clock went backwards`() = runTest(dispatcher) {
            every { cache.lastSent() } returns SentLocation(saoPaulo, now + 60_000L)

            useCase().execute("valid-app-token")

            coVerify(exactly = 1) { repository.sendLocation(any()) }
        }
    }

    // ── Cache update ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cache")
    inner class Cache {

        @Test
        fun `remembers the position after a successful send`() = runTest(dispatcher) {
            useCase().execute("valid-app-token")
            verify { cache.saveLastSent(SentLocation(saoPaulo, now)) }
        }

        @Test
        fun `remembers the position when backend says unchanged`() = runTest(dispatcher) {
            coEvery { repository.sendLocation(any()) } returns Result.success(LocationUpdateResult.Unchanged)
            useCase().execute("valid-app-token")
            verify { cache.saveLastSent(any()) }
        }

        @Test
        fun `does not remember the position on failure`() = runTest(dispatcher) {
            coEvery { repository.sendLocation(any()) } returns
                Result.failure(LocationFailure.SubscriberNotFound("not found"))

            val result = useCase().execute("valid-app-token")

            assertTrue(result.isFailure)
            assertInstanceOf(LocationFailure.SubscriberNotFound::class.java, result.exceptionOrNull())
            verify(exactly = 0) { cache.saveLastSent(any()) }
        }
    }
}
