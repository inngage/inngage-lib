package br.com.inngage.sdk.internal.service.inapp

import android.content.Context
import android.content.SharedPreferences
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.service.inapp.data.InAppMessageV2RepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("InAppMessageV2RepositoryImpl")
class InAppMessageV2RepositoryImplTest {

    private val httpClient = mockk<HttpClient>()

    // Context + SharedPreferences mocks (needed by PreferencesStorage constructor)
    private val sharedPrefsEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val sharedPrefs       = mockk<SharedPreferences>(relaxed = true)
    private val context           = mockk<Context>()

    private lateinit var repository: InAppMessageV2RepositoryImpl

    private val expectedEndpoint =
        InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_INAPP_V2

    private val enabledResponse = """
        {
          "inAppMessage": {
            "enabled": true,
            "type": "Banner",
            "style": { "backgroundColor": "#7043CC", "titleColor": "#FFFFFF", "bodyColor": "#FFFFFF" },
            "media": {
              "carousel": {
                "enabled": true,
                "position": "TOP",
                "items": [
                  { "image": "https://img1.url", "content": { "title": "Hello", "body": "World" } }
                ]
              }
            }
          }
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.edit() } returns sharedPrefsEditor

        // Defaults: subscription already completed
        every { sharedPrefs.getString(InngageConfig.PREF_APP_ID, any()) } returns "522"
        every { sharedPrefs.getString(InngageConfig.PREF_FCM_TOKEN, any()) } returns "fcm-token-xyz"
        every { sharedPrefs.getBoolean(InngageConfig.PREF_INAPP_FIRST_ACCESS, any()) } returns true

        repository = InAppMessageV2RepositoryImpl(context, httpClient)
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("guards")
    inner class Guards {

        @Test
        @DisplayName("fails without calling the API when appId is missing")
        fun `fails when appId missing`() = runTest {
            every { sharedPrefs.getString(InngageConfig.PREF_APP_ID, any()) } returns ""

            val result = repository.fetchMessage()

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { httpClient.post(any(), any()) }
        }

        @Test
        @DisplayName("fails without calling the API when registration (FCM token) is missing")
        fun `fails when registration missing`() = runTest {
            every { sharedPrefs.getString(InngageConfig.PREF_FCM_TOKEN, any()) } returns ""

            val result = repository.fetchMessage()

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { httpClient.post(any(), any()) }
        }
    }

    // ── Request contract ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("request contract")
    inner class RequestContract {

        @Test
        @DisplayName("posts appId, registration, firstAccess and channelId=6 to /v4/message/objectMessage")
        fun `posts correct body to correct endpoint`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success(enabledResponse)

            repository.fetchMessage()

            coVerify(exactly = 1) {
                httpClient.post(
                    withArg { body ->
                        assertEquals(522, body.getInt("appId"))
                        assertEquals("fcm-token-xyz", body.getString("registration"))
                        assertTrue(body.getBoolean("firstAccess"))
                        assertEquals(InngageConfig.INAPP_CHANNEL_ID, body.getInt("channelId"))
                        assertFalse(body.has("deviceUuid"))
                        assertFalse(body.has("mobileType"))
                        assertFalse(body.has("fcmToken"))
                    },
                    expectedEndpoint
                )
            }
        }

        @Test
        @DisplayName("sends firstAccess=false on recurring calls")
        fun `sends firstAccess false when already accessed`() = runTest {
            every { sharedPrefs.getBoolean(InngageConfig.PREF_INAPP_FIRST_ACCESS, any()) } returns false
            coEvery { httpClient.post(any(), any()) } returns Result.success(enabledResponse)

            repository.fetchMessage()

            coVerify {
                httpClient.post(
                    withArg { body -> assertFalse(body.getBoolean("firstAccess")) },
                    any()
                )
            }
        }
    }

    // ── firstAccess persistence ───────────────────────────────────────────────

    @Nested
    @DisplayName("firstAccess persistence")
    inner class FirstAccessPersistence {

        @Test
        @DisplayName("flips firstAccess to false only after a successful response")
        fun `flips firstAccess after success`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success(enabledResponse)

            repository.fetchMessage()

            verify(exactly = 1) {
                sharedPrefsEditor.putBoolean(InngageConfig.PREF_INAPP_FIRST_ACCESS, false)
            }
        }

        @Test
        @DisplayName("keeps firstAccess=true when the HTTP call fails")
        fun `does not flip firstAccess on failure`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns
                Result.failure(java.io.IOException("Network error"))

            repository.fetchMessage()

            verify(exactly = 0) {
                sharedPrefsEditor.putBoolean(InngageConfig.PREF_INAPP_FIRST_ACCESS, any())
            }
        }
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("response parsing")
    inner class ResponseParsing {

        @Test
        @DisplayName("returns parsed message on success")
        fun `returns parsed message on success`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success(enabledResponse)

            val result = repository.fetchMessage()

            assertTrue(result.isSuccess)
            val msg = result.getOrNull()
            assertNotNull(msg)
            assertEquals("Banner", msg?.type)
            assertEquals("#7043CC", msg?.style?.backgroundColor)
            assertTrue(msg?.media?.carousel?.isEnabled == true)
            assertEquals(1, msg?.media?.carousel?.items?.size)
            assertEquals("https://img1.url", msg?.media?.carousel?.items?.first()?.image)
            assertEquals("Hello", msg?.media?.carousel?.items?.first()?.content?.title)
        }

        @Test
        @DisplayName("parses the flat production payload (media.items, no enabled/carousel wrapper)")
        fun `parses flat production payload`() = runTest {
            val flatResponse = """
                {
                  "type": "Message",
                  "style": {
                    "position": "center",
                    "backgroundColor": "#000000",
                    "borderColor": "#00FF11",
                    "shadow": true,
                    "titleColor": "#FF0000",
                    "bodyColor": "#FF0000"
                  },
                  "media": {
                    "position": "TOP",
                    "items": [
                      {
                        "image": "https://cdn.example/img.png",
                        "imageType": "fill",
                        "content": { "title": "Oi testando novo", "body": "teste do in app" },
                        "actions": {
                          "backgroundClick": null,
                          "buttons": [
                            { "text": "teste 1", "action": { "type": "weblink", "url": "https://a" } },
                            { "text": "teste 2", "action": { "type": "deeplink", "url": "https://b" } }
                          ]
                        }
                      }
                    ]
                  },
                  "notId": "6a5f75a2db10e749c5211bb0"
                }
            """.trimIndent()
            coEvery { httpClient.post(any(), any()) } returns Result.success(flatResponse)

            val result = repository.fetchMessage()

            assertTrue(result.isSuccess)
            val msg = result.getOrNull()
            assertNotNull(msg)
            assertTrue(msg?.enabled == true)                     // absent flag defaults to enabled
            assertEquals("Message", msg?.type)
            assertEquals(1, msg?.media?.carousel?.items?.size)    // single item ⇒ banner
            val item = msg?.media?.carousel?.items?.first()
            assertEquals("Oi testando novo", item?.content?.title)
            assertEquals(2, item?.actions?.buttons?.size)
        }

        @Test
        @DisplayName("returns null when message is disabled")
        fun `returns null when disabled`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns
                Result.success("""{"inAppMessage":{"enabled":false}}""")

            val result = repository.fetchMessage()

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @Test
        @DisplayName("returns null when no in-app payload key is present")
        fun `returns null when payload key absent`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("""{"other":"value"}""")

            val result = repository.fetchMessage()

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @Test
        @DisplayName("returns failure when the HTTP call fails")
        fun `returns failure when http call fails`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns
                Result.failure(java.io.IOException("Network error"))

            val result = repository.fetchMessage()

            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("returns failure on malformed JSON")
        fun `returns failure on malformed JSON`() = runTest {
            coEvery { httpClient.post(any(), any()) } returns Result.success("not-json-at-all")

            val result = repository.fetchMessage()

            assertTrue(result.isFailure)
        }
    }
}
