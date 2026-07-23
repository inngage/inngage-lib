package br.com.inngage.sdk.internal.service.subscription

import android.content.Context
import android.content.SharedPreferences
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.platform.appinfo.AppInfo
import br.com.inngage.sdk.internal.platform.appinfo.AppInfoProvider
import br.com.inngage.sdk.internal.platform.deviceinfo.DeviceUuidProvider
import br.com.inngage.sdk.internal.platform.location.LocationProvider
import br.com.inngage.sdk.internal.platform.permission.PermissionChecker
import br.com.inngage.sdk.internal.service.subscription.data.SubscriptionRepositoryImpl
import br.com.inngage.sdk.internal.service.subscription.domain.SubscriberEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SubscriptionRepositoryImpl")
class SubscriptionRepositoryImplTest {

    private val dispatcher       = UnconfinedTestDispatcher()
    private val httpClient       = mockk<HttpClient>()
    private val appInfoProvider  = mockk<AppInfoProvider>()
    private val deviceUuidProvider = mockk<DeviceUuidProvider>()
    private val locationProvider = mockk<LocationProvider>()
    private val permissionChecker = mockk<PermissionChecker>()

    // Context + SharedPreferences mocks (needed by PreferencesStorage constructor)
    private val sharedPrefsEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val sharedPrefs       = mockk<SharedPreferences>(relaxed = true)
    private val context           = mockk<Context>()

    private lateinit var repository: SubscriptionRepositoryImpl

    @BeforeEach
    fun setUp() {
        // Stub Context so that PreferencesStorage can be instantiated
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.edit() } returns sharedPrefsEditor
        every { sharedPrefsEditor.putString(any(), any()) } returns sharedPrefsEditor

        // Default platform stubs
        every { appInfoProvider.getAppInfo() } returns AppInfo("2024-01-01", "2024-06-01", "1.0.0")
        every { deviceUuidProvider.getUuid() } returns "uuid-1234"
        every { permissionChecker.areNotificationsEnabled() } returns true

        repository = SubscriptionRepositoryImpl(
            context            = context,
            httpClient         = httpClient,
            appInfoProvider    = appInfoProvider,
            deviceUuidProvider = deviceUuidProvider,
            locationProvider   = locationProvider,
            permissionChecker  = permissionChecker
        )
    }

    // ── Request body ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("request body")
    inner class RequestBody {

        @Test
        @DisplayName("contains registration field with the FCM token value")
        fun `body includes registration with fcm token`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns
                Result.success("""{"registerSubscriberResponse":{"appId":"123"}}""")

            repository.subscribe(
                SubscriberEntity(
                    appToken     = "app-token-abc",
                    identifier   = "user@test.com",
                    registration = "fcm-token-xyz"
                )
            )

            coVerify {
                httpClient.post(
                    withArg { payload ->
                        val req = payload.getJSONObject("registerSubscriberRequest")
                        assertEquals("fcm-token-xyz", req.getString("registration"))
                    },
                    any()
                )
            }
        }

        @Test
        @DisplayName("contains app_token, identifier and registration together")
        fun `body includes app_token, identifier and registration`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.subscribe(
                SubscriberEntity(
                    appToken     = "my-app-token",
                    identifier   = "user-123",
                    registration = "fcm-device-token"
                )
            )

            coVerify {
                httpClient.post(
                    withArg { payload ->
                        val req = payload.getJSONObject("registerSubscriberRequest")
                        assertEquals("my-app-token", req.getString("app_token"))
                        assertEquals("user-123", req.getString("identifier"))
                        assertEquals("fcm-device-token", req.getString("registration"))
                    },
                    any()
                )
            }
        }

        @Test
        @DisplayName("registration is never blank — reflects exactly what was provided")
        fun `registration preserves token value exactly`() = runTest(dispatcher) {
            val exactToken = "APA91bHPRgkFLJu7zC12a4LuJ5g"
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.subscribe(
                SubscriberEntity(appToken = "tok", identifier = "id", registration = exactToken)
            )

            coVerify {
                httpClient.post(
                    withArg { payload ->
                        val req = payload.getJSONObject("registerSubscriberRequest")
                        assertEquals(exactToken, req.getString("registration"))
                    },
                    any()
                )
            }
        }
    }

    // ── Endpoint ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("endpoint")
    inner class Endpoint {

        @Test
        @DisplayName("posts to the v4/subscription path")
        fun `posts to correct v4 endpoint`() = runTest(dispatcher) {
            val expectedEndpoint =
                InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_SUBSCRIPTION_V4
            coEvery { httpClient.post(any(), expectedEndpoint) } returns Result.success("")

            repository.subscribe(SubscriberEntity("tok", "id", "fcm"))

            coVerify(exactly = 1) { httpClient.post(any(), expectedEndpoint) }
        }
    }

    // ── Identifier / uuid rules ───────────────────────────────────────────────

    @Nested
    @DisplayName("identifier and uuid rules")
    inner class IdentifierUuidRules {

        @Test
        @DisplayName("sends the installation UUID as both identifier and uuid when identifier is blank")
        fun `anonymous subscription uses uuid as identifier`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.subscribe(SubscriberEntity("tok", "", "fcm"))

            coVerify {
                httpClient.post(
                    withArg { payload ->
                        val req = payload.getJSONObject("registerSubscriberRequest")
                        assertEquals("uuid-1234", req.getString("identifier"))
                        assertEquals("uuid-1234", req.getString("uuid"))
                    },
                    any()
                )
            }
        }

        @Test
        @DisplayName("sends the user identifier and keeps uuid as the installation UUID")
        fun `identified subscription keeps installation uuid`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.subscribe(SubscriberEntity("tok", "user@test.com", "fcm"))

            coVerify {
                httpClient.post(
                    withArg { payload ->
                        val req = payload.getJSONObject("registerSubscriberRequest")
                        assertEquals("user@test.com", req.getString("identifier"))
                        assertEquals("uuid-1234", req.getString("uuid"))
                    },
                    any()
                )
            }
        }

        @Test
        @DisplayName("persists the effective identifier for later addUserData calls")
        fun `persists effective identifier`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.subscribe(SubscriberEntity("tok", "", "fcm"))

            verify(exactly = 1) {
                sharedPrefsEditor.putString(InngageConfig.PREF_IDENTIFIER, "uuid-1234")
            }
        }
    }

    // ── addUserData ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addUserData")
    inner class AddUserData {

        @Test
        @DisplayName("posts the fieldsRequest envelope to /v4/subscription/addCustomField")
        fun `posts full body to correct endpoint`() = runTest(dispatcher) {
            val expectedEndpoint =
                InngageConfig.API_PROD_ENDPOINT + InngageConfig.PATH_ADD_USER_DATA
            coEvery { httpClient.post(any(), expectedEndpoint) } returns Result.success("")

            repository.addUserData(
                br.com.inngage.sdk.internal.service.subscription.domain.UserDataEntity(
                    appToken     = "app-token-abc",
                    identifier   = "user-42",
                    customFields = org.json.JSONObject().put("plano", "premium"),
                    email        = "cliente@example.com",
                    phoneNumber  = "81988887777"
                )
            )

            coVerify(exactly = 1) {
                httpClient.post(
                    withArg { payload ->
                        val req = payload.getJSONObject("fieldsRequest")
                        assertEquals("app-token-abc", req.getString("app_token"))
                        assertEquals("user-42", req.getString("identifier"))
                        assertEquals("premium", req.getJSONObject("custom_field").getString("plano"))
                        assertEquals("cliente@example.com", req.getString("email"))
                        assertEquals("81988887777", req.getString("phone_number"))
                    },
                    expectedEndpoint
                )
            }
        }

        @Test
        @DisplayName("omits null fields from the body")
        fun `omits null fields`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.addUserData(
                br.com.inngage.sdk.internal.service.subscription.domain.UserDataEntity(
                    appToken = "app-token-abc",
                    email    = "cliente@example.com"
                )
            )

            coVerify {
                httpClient.post(
                    withArg { payload ->
                        val req = payload.getJSONObject("fieldsRequest")
                        assertEquals("app-token-abc", req.getString("app_token"))
                        assertEquals("cliente@example.com", req.getString("email"))
                        org.junit.jupiter.api.Assertions.assertFalse(req.has("identifier"))
                        org.junit.jupiter.api.Assertions.assertFalse(req.has("custom_field"))
                        org.junit.jupiter.api.Assertions.assertFalse(req.has("phone_number"))
                        org.junit.jupiter.api.Assertions.assertFalse(req.has("channels"))
                    },
                    any()
                )
            }
        }

        @Test
        @DisplayName("returns failure when HTTP call fails")
        fun `returns failure on HTTP error`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns
                Result.failure(RuntimeException("network error"))

            val result = repository.addUserData(
                br.com.inngage.sdk.internal.service.subscription.domain.UserDataEntity(
                    appToken = "tok", email = "a@b.com"
                )
            )

            assertTrue(result.isFailure)
        }
    }

    // ── FCM token persistence ─────────────────────────────────────────────────

    @Nested
    @DisplayName("fcm token persistence")
    inner class FcmTokenPersistence {

        @Test
        @DisplayName("persists the FCM token before sending the request")
        fun `persists fcm token on subscribe`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns Result.success("")

            repository.subscribe(SubscriberEntity("tok", "id", "fcm-token-persisted"))

            verify(exactly = 1) {
                sharedPrefsEditor.putString(InngageConfig.PREF_FCM_TOKEN, "fcm-token-persisted")
            }
        }

        @Test
        @DisplayName("persists the FCM token even when the HTTP call fails")
        fun `persists fcm token even on network failure`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns
                Result.failure(RuntimeException("network error"))

            repository.subscribe(SubscriberEntity("tok", "id", "fcm-token-persisted"))

            verify(exactly = 1) {
                sharedPrefsEditor.putString(InngageConfig.PREF_FCM_TOKEN, "fcm-token-persisted")
            }
        }
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("error handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("returns failure when HTTP call fails")
        fun `returns failure on HTTP error`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns
                Result.failure(RuntimeException("network error"))

            val result = repository.subscribe(SubscriberEntity("tok", "id", "fcm"))

            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("returns success even when appId is absent from response")
        fun `returns success when appId is missing`() = runTest(dispatcher) {
            coEvery { httpClient.post(any(), any()) } returns Result.success("{}")

            val result = repository.subscribe(SubscriberEntity("tok", "id", "fcm"))

            assertTrue(result.isSuccess)
        }
    }
}

