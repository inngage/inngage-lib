package br.com.inngage.sdk.internal.service.notificationstatus

import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.service.notificationstatus.data.NotificationStatusRepositoryImpl
import br.com.inngage.sdk.internal.core.http.HttpClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("NotificationStatusRepositoryImpl")
class NotificationStatusRepositoryImplTest {

    private val dispatcher  = UnconfinedTestDispatcher()
    private val httpClient  = mockk<HttpClient>()
    private val repo = NotificationStatusRepositoryImpl(httpClient)

    @Test
    @DisplayName("returns success when HTTP 200")
    fun `returns success on 200`() = runTest(dispatcher) {
        coEvery { httpClient.post(any(), any()) } returns Result.success("""{"ok":true}""")

        val result = repo.sendCallback(
            notificationId = "notif-123",
            appToken       = "apptoken-abc",
            endpoint       = "${InngageConfig.API_PROD_ENDPOINT}${InngageConfig.PATH_NOTIFICATION_CALLBACK}"
        )
        assertTrue(result.isSuccess)
    }

    @Test
    @DisplayName("returns failure when HTTP fails")
    fun `returns failure on HTTP error`() = runTest(dispatcher) {
        coEvery { httpClient.post(any(), any()) } returns Result.failure(RuntimeException("timeout"))

        val result = repo.sendCallback("notif-123", "apptoken-abc", "https://api.test.com/notification/")
        assertTrue(result.isFailure)
    }

    @Test
    @DisplayName("posts to the provided endpoint")
    fun `posts to correct endpoint`() = runTest(dispatcher) {
        val endpoint = "https://api.inngage.com.br/v1/notification/"
        coEvery { httpClient.post(any(), endpoint) } returns Result.success("")

        repo.sendCallback("id-1", "tok-1", endpoint)

        coVerify(exactly = 1) { httpClient.post(any(), endpoint) }
    }

    @Test
    @DisplayName("request body contains id, notid and app_token")
    fun `body includes notid field`() = runTest(dispatcher) {
        val endpoint = "https://api.inngage.com.br/v1/notification/"
        coEvery { httpClient.post(any(), any()) } returns Result.success("")

        repo.sendCallback("nid-42", "my-token", endpoint)

        coVerify {
            httpClient.post(
                withArg { payload ->
                    val req = payload.getJSONObject("notificationRequest")
                    assertEquals("nid-42", req.getString("id"))
                    assertEquals("nid-42", req.getString("notid"))
                    assertEquals("my-token", req.getString("app_token"))
                },
                endpoint
            )
        }
    }
}

