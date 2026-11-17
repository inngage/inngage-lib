package br.com.inngage.sdk.internal.service.location

import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.core.http.HttpException
import br.com.inngage.sdk.internal.service.location.data.LocationRepositoryImpl
import br.com.inngage.sdk.internal.service.location.domain.Coordinates
import br.com.inngage.sdk.internal.service.location.domain.LocationFailure
import br.com.inngage.sdk.internal.service.location.domain.LocationUpdateEntity
import br.com.inngage.sdk.internal.service.location.domain.LocationUpdateResult
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("LocationRepositoryImpl")
class LocationRepositoryImplTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val httpClient = mockk<HttpClient>()
    private val repository = LocationRepositoryImpl(httpClient)

    private val endpoint = InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_SUBSCRIPTION_LOCATION

    private fun entity(
        identifier: String? = "cliente-123",
        registration: String? = "fcm-token",
        uuid: String? = null
    ) = LocationUpdateEntity(
        appToken     = "app-token-abc",
        identifier   = identifier,
        registration = registration,
        uuid         = uuid,
        coordinates  = Coordinates(-23.5505, -46.6333)
    )

    private val bodySlot = slot<JSONObject>()

    private fun stub(result: Result<String>) {
        coEvery { httpClient.post(capture(bodySlot), endpoint) } returns result
    }

    @Nested
    @DisplayName("request body")
    inner class RequestBody {

        @Test
        fun `is flat JSON with numeric coordinates and app_token`() = runTest(dispatcher) {
            stub(Result.success("""{"status":"1","recorded":true}"""))

            repository.sendLocation(entity())

            val body = bodySlot.captured
            assertEquals("app-token-abc", body.getString("app_token"))
            assertEquals("cliente-123", body.getString("identifier"))
            assertEquals("fcm-token", body.getString("registration"))
            assertEquals(-23.5505, body.getDouble("lat"), 1e-9)
            assertEquals(-46.6333, body.getDouble("long"), 1e-9)
            assertFalse(body.has("uuid"))
            assertFalse(body.has("locationRequest"))
        }

        @Test
        fun `omits null identifiers and includes uuid when present`() = runTest(dispatcher) {
            stub(Result.success("""{"status":"1","recorded":true}"""))

            repository.sendLocation(entity(identifier = null, registration = null, uuid = "browser-uuid"))

            val body = bodySlot.captured
            assertFalse(body.has("identifier"))
            assertFalse(body.has("registration"))
            assertEquals("browser-uuid", body.getString("uuid"))
        }
    }

    @Nested
    @DisplayName("response mapping")
    inner class ResponseMapping {

        @Test
        fun `recorded true maps to Recorded`() = runTest(dispatcher) {
            stub(Result.success("""{"status":"1","message":"Location recorded","appId":123,"recorded":true}"""))
            assertEquals(LocationUpdateResult.Recorded, repository.sendLocation(entity()).getOrNull())
        }

        @Test
        fun `recorded false maps to Unchanged and is still a success`() = runTest(dispatcher) {
            stub(Result.success("""{"status":"1","message":"Location unchanged, skipped","recorded":false}"""))
            val result = repository.sendLocation(entity())
            assertTrue(result.isSuccess)
            assertEquals(LocationUpdateResult.Unchanged, result.getOrNull())
        }

        @Test
        fun `unparseable 2xx body is treated as Recorded`() = runTest(dispatcher) {
            stub(Result.success("not json"))
            assertEquals(LocationUpdateResult.Recorded, repository.sendLocation(entity()).getOrNull())
        }
    }

    @Nested
    @DisplayName("failure mapping")
    inner class FailureMapping {

        private fun http(code: Int, body: String) =
            Result.failure<String>(HttpException(code, body, endpoint))

        @Test
        fun `404 maps to SubscriberNotFound with backend message`() = runTest(dispatcher) {
            stub(http(404, """{"status":"0","message":"Subscriber not found in app [123]"}"""))
            val error = repository.sendLocation(entity()).exceptionOrNull()
            assertInstanceOf(LocationFailure.SubscriberNotFound::class.java, error)
            assertEquals("Subscriber not found in app [123]", error?.message)
        }

        @Test
        fun `401 maps to Unauthorized`() = runTest(dispatcher) {
            stub(http(401, """{"status":"0","message":"app_token is invalid or expired."}"""))
            assertInstanceOf(LocationFailure.Unauthorized::class.java, repository.sendLocation(entity()).exceptionOrNull())
        }

        @Test
        fun `400 maps to BadRequest`() = runTest(dispatcher) {
            stub(http(400, """{"status":"0","message":"lat is required and must be a number between -90 and 90."}"""))
            assertInstanceOf(LocationFailure.BadRequest::class.java, repository.sendLocation(entity()).exceptionOrNull())
        }

        @Test
        fun `500 maps to transient Network failure`() = runTest(dispatcher) {
            stub(http(500, """{"status":"0","message":"Internal Server Error"}"""))
            val error = repository.sendLocation(entity()).exceptionOrNull()
            assertInstanceOf(LocationFailure.Network::class.java, error)
            assertFalse(error is LocationFailure.Permanent)
        }

        @Test
        fun `connection error maps to transient Network failure`() = runTest(dispatcher) {
            stub(Result.failure(IOException("Connection refused")))
            val error = repository.sendLocation(entity()).exceptionOrNull()
            assertInstanceOf(LocationFailure.Network::class.java, error)
            assertEquals("Connection refused", error?.message)
        }

        @Test
        fun `non-JSON error body falls back to HTTP message`() = runTest(dispatcher) {
            stub(http(404, "<html>not found</html>"))
            val error = repository.sendLocation(entity()).exceptionOrNull()
            assertInstanceOf(LocationFailure.SubscriberNotFound::class.java, error)
            assertEquals("HTTP 404 from $endpoint", error?.message)
        }
    }
}
