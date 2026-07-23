package br.com.inngage.sdk.internal.service.notificationstatus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.util.DeepLinkHandler
import br.com.inngage.sdk.internal.service.notificationstatus.application.HandleNotificationUseCase
import br.com.inngage.sdk.internal.service.notificationstatus.domain.NotificationPayload
import br.com.inngage.sdk.internal.service.notificationstatus.domain.NotificationStatusRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("HandleNotificationUseCase")
class HandleNotificationUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<NotificationStatusRepository>(relaxed = true)
    private val context    = mockk<Context>(relaxed = true)

    private lateinit var useCase: HandleNotificationUseCase

    @BeforeEach
    fun setUp() {
        useCase = HandleNotificationUseCase(repository, dispatcher)
        mockkObject(DeepLinkHandler)
        every { DeepLinkHandler.openDeepLink(any(), any()) } returns Unit
        every { DeepLinkHandler.openInBrowser(any(), any()) } returns Unit
        coEvery { repository.sendCallback(any(), any(), any()) } returns Result.success(Unit)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun intentWith(vararg pairs: Pair<String, String>): Intent {
        val extras = Bundle()
        pairs.forEach { (k, v) -> extras.putString(k, v) }
        return mockk {
            every { this@mockk.extras } returns extras
            pairs.forEach { (k, v) -> every { getStringExtra(k) } returns v }
            // keys not provided return null
            InngageConfig.NOTIFICATION_KEYS.forEach { key ->
                if (pairs.none { it.first == key }) every { getStringExtra(key) } returns null
            }
        }
    }

    // ── no-op cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("does nothing when intent has no extras")
    fun `no-op when extras are null`() = runTest(dispatcher) {
        val intent = mockk<Intent> { every { extras } returns null }
        useCase.execute(context, intent, "token")
        coVerify(exactly = 0) { repository.sendCallback(any(), any(), any()) }
    }

    @Test
    @DisplayName("does nothing when notId is blank")
    fun `no-op when notId is blank`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "")
        useCase.execute(context, intent, "token")
        coVerify(exactly = 0) { repository.sendCallback(any(), any(), any()) }
    }

    // ── callback ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("fires open callback with id, notid and app_token")
    fun `fires callback on notification tap`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "nid-1", "id" to "nid-1")
        useCase.execute(context, intent, "APP_TOKEN")

        val expectedEndpoint = "${InngageConfig.API_PROD_ENDPOINT}${InngageConfig.PATH_NOTIFICATION_CALLBACK}"
        coVerify(exactly = 1) { repository.sendCallback("nid-1", "APP_TOKEN", expectedEndpoint) }
    }

    // ── onNotificationClick ───────────────────────────────────────────────────

    @Test
    @DisplayName("invokes onNotificationClick with correct payload")
    fun `delivers payload to consumer callback`() = runTest(dispatcher) {
        val intent = intentWith(
            "notId" to "nid-3",
            "id"    to "nid-3",
            "title" to "Hello",
            "body"  to "World",
            "type"  to "deep",
            "url"   to "https://example.com"
        )

        var received: NotificationPayload? = null
        useCase.execute(context, intent, "TOKEN") { received = it }

        assertNotNull(received)
        assertEquals("nid-3",               received!!.notId)
        assertEquals("Hello",               received!!.title)
        assertEquals("deep",                received!!.type)
        assertEquals("https://example.com", received!!.url)
    }

    @Test
    @DisplayName("onNotificationClick is optional — no crash when null")
    fun `null callback does not crash`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "nid-4")
        useCase.execute(context, intent, "TOKEN", onNotificationClick = null)
    }

    // ── navigation: deep ──────────────────────────────────────────────────────

    @Test
    @DisplayName("opens external browser for type=deep when blockDeepLink is false")
    fun `opens deep link when not blocked`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "nid-5", "type" to "deep", "url" to "https://deep.example.com")
        useCase.execute(context, intent, "TOKEN", blockDeepLink = false)
        verify(exactly = 1) { DeepLinkHandler.openDeepLink(context, "https://deep.example.com") }
    }

    @Test
    @DisplayName("suppresses external browser when blockDeepLink is true and type=deep")
    fun `blocks deep link when blockDeepLink is true`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "nid-6", "type" to "deep", "url" to "https://deep.example.com")
        useCase.execute(context, intent, "TOKEN", blockDeepLink = true)
        verify(exactly = 0) { DeepLinkHandler.openDeepLink(any(), any()) }
    }

    @Test
    @DisplayName("does not navigate when url is absent")
    fun `no navigation when url absent`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "nid-7", "type" to "deep")
        useCase.execute(context, intent, "TOKEN", blockDeepLink = false)
        verify(exactly = 0) { DeepLinkHandler.openDeepLink(any(), any()) }
    }

    // ── navigation: inapp ─────────────────────────────────────────────────────

    @Test
    @DisplayName("opens in-app browser for type=inapp regardless of blockDeepLink")
    fun `opens inapp browser even when blockDeepLink is true`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "nid-8", "type" to "inapp", "url" to "https://inapp.example.com")
        useCase.execute(context, intent, "TOKEN", blockDeepLink = true)
        verify(exactly = 1) { DeepLinkHandler.openInBrowser(context, "https://inapp.example.com") }
    }

    @Test
    @DisplayName("opens in-app browser when blockDeepLink is false")
    fun `opens inapp browser when blockDeepLink is false`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "nid-9", "type" to "inapp", "url" to "https://inapp.example.com")
        useCase.execute(context, intent, "TOKEN", blockDeepLink = false)
        verify(exactly = 1) { DeepLinkHandler.openInBrowser(context, "https://inapp.example.com") }
    }

    // ── payload fields ────────────────────────────────────────────────────────

    @Test
    @DisplayName("type and url are null when not present in payload")
    fun `type and url are null when absent`() = runTest(dispatcher) {
        val intent = intentWith("notId" to "nid-10", "title" to "T", "body" to "B")
        var received: NotificationPayload? = null
        useCase.execute(context, intent, "TOKEN") { received = it }
        assertNull(received!!.type)
        assertNull(received!!.url)
        assertNull(received!!.additionalData)
    }
}
