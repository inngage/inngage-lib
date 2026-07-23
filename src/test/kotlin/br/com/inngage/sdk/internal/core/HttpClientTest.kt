package br.com.inngage.sdk.internal.core

import br.com.inngage.sdk.internal.core.http.HttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.HttpURLConnection

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("HttpClient")
class HttpClientTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @AfterEach
    fun tearDown() = unmockkAll()

    private fun fakeConn(
        code: Int,
        body: String = ""
    ): HttpURLConnection = mockk(relaxed = true) {
        every { responseCode } returns code
        every { inputStream } returns body.byteInputStream()
        every { outputStream } returns java.io.ByteArrayOutputStream()
    }

    private fun clientWith(conn: HttpURLConnection) = HttpClient(
        dispatcher = testDispatcher,
        connectionFactory = { conn }
    )

    // ── 200 OK ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns Success with response body on HTTP 200")
    fun `returns success on 200`() = runTest(testDispatcher) {
        val body = """{"status":"ok"}"""
        val result = clientWith(fakeConn(200, body))
            .post(JSONObject().put("key", "val"), "https://api.test.com/endpoint")

        assertTrue(result.isSuccess)
        assertEquals(body, result.getOrNull())
    }

    @Test
    @DisplayName("returns Success with response body on HTTP 201 (e.g. subscriber created)")
    fun `returns success on 201`() = runTest(testDispatcher) {
        val body = """{"registerSubscriberResponse":{"statusDescription":"Successfully: subscriberCreated"}}"""
        val result = clientWith(fakeConn(201, body))
            .post(JSONObject().put("key", "val"), "https://api.test.com/endpoint")

        assertTrue(result.isSuccess)
        assertEquals(body, result.getOrNull())
    }

    @Test
    @DisplayName("returns Success on HTTP 204 with empty body")
    fun `returns success on 204`() = runTest(testDispatcher) {
        val result = clientWith(fakeConn(204))
            .post(JSONObject(), "https://api.test.com/endpoint")

        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
    }

    // ── Non-2xx ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns Failure with IOException on HTTP 500")
    fun `returns failure on non-200`() = runTest(testDispatcher) {
        val result = clientWith(fakeConn(500))
            .post(JSONObject(), "https://api.test.com/endpoint")

        assertTrue(result.isFailure)
        assertInstanceOf(IOException::class.java, result.exceptionOrNull())
    }

    @Test
    @DisplayName("returns Failure with IOException on HTTP 401")
    fun `returns failure on 401`() = runTest(testDispatcher) {
        val result = clientWith(fakeConn(401))
            .post(JSONObject().put("app_token", "xyz"), "https://api.test.com/sub")

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex?.message?.contains("401") == true)
    }

    // ── I/O error ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns Failure when connection throws IOException")
    fun `returns failure on connection error`() = runTest(testDispatcher) {
        val client = HttpClient(
            dispatcher = testDispatcher,
            connectionFactory = { throw IOException("Connection refused") }
        )
        val result = client.post(JSONObject(), "https://api.test.com/endpoint")

        assertTrue(result.isFailure)
        assertInstanceOf(IOException::class.java, result.exceptionOrNull())
    }
}


