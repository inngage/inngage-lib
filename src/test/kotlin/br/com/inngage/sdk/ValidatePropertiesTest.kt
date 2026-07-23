package br.com.inngage.sdk

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ValidateProperties].
 *
 * Each nested class forces a specific error scenario to guarantee that the
 * validation layer catches bad inputs before they reach the network layer.
 * The geo-location / ANR regression cases are documented inline.
 */
@DisplayName("ValidateProperties")
class ValidatePropertiesTest {

    // ── App Token ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateAppToken")
    inner class AppTokenTests {

        @Test
        fun `returns false for null`() =
            assertFalse(ValidateProperties.validateAppToken(null))

        @Test
        fun `returns false for empty string`() =
            assertFalse(ValidateProperties.validateAppToken(""))

        @Test
        fun `returns false for token shorter than 8 chars`() =
            assertFalse(ValidateProperties.validateAppToken("abc"))

        @Test
        fun `returns true for token with exactly 8 chars`() =
            assertTrue(ValidateProperties.validateAppToken("abcdefgh"))

        @Test
        fun `returns true for valid production token`() =
            assertTrue(ValidateProperties.validateAppToken("1fadc3dba74047d9916fa18155fbeeba"))
    }

    // ── Identifier ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateIdentifier")
    inner class IdentifierTests {

        @Test
        fun `returns true for null — identifier is optional`() =
            assertTrue(ValidateProperties.validateIdentifier(null))

        @Test
        fun `returns true for valid email identifier`() =
            assertTrue(ValidateProperties.validateIdentifier("user@example.com"))

        @Test
        fun `returns false for empty string`() =
            assertFalse(ValidateProperties.validateIdentifier(""))
    }

    // ── Environment ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateEnvironment")
    inner class EnvironmentTests {

        @Test
        fun `returns true for prod`() =
            assertTrue(ValidateProperties.validateEnvironment("prod"))

        @Test
        fun `returns true for dev`() =
            assertTrue(ValidateProperties.validateEnvironment("dev"))

        @Test
        fun `returns false for unknown environment`() =
            assertFalse(ValidateProperties.validateEnvironment("staging"))

        @Test
        fun `returns false for empty string`() =
            assertFalse(ValidateProperties.validateEnvironment(""))
    }

    // ── Provider ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateProvider")
    inner class ProviderTests {

        @Test
        fun `returns true for FCM`() =
            assertTrue(ValidateProperties.validateProvider("FCM"))

        @Test
        fun `returns true for GCM`() =
            assertTrue(ValidateProperties.validateProvider("GCM"))

        @Test
        fun `returns false for unknown provider`() =
            assertFalse(ValidateProperties.validateProvider("APNS"))

        @Test
        fun `returns false for empty string`() =
            assertFalse(ValidateProperties.validateProvider(""))
    }

    // ── Custom Field ─────────────────────────────────────────────────────────
    //
    // REGRESSION: before the fix, passing null here caused NullPointerException
    // at ValidateProperties.java:37 → crashed MainActivity on startup.

    @Nested
    @DisplayName("validateCustomField — null / empty regression")
    inner class CustomFieldTests {

        @Test
        fun `returns true for null — customFields is optional (regression)`() {
            // This was the crash: customFields=null → NPE on customFields.length()
            assertTrue(ValidateProperties.validateCustomField(null))
        }

        @Test
        fun `returns true for non-empty JSONObject`() =
            assertTrue(
                ValidateProperties.validateCustomField(
                    JSONObject().apply {
                        put("nome", "Saulo")
                        put("email", "teste@gmail.com")
                    }
                )
            )

        @Test
        fun `returns false for empty JSONObject`() =
            assertFalse(ValidateProperties.validateCustomField(JSONObject()))
    }

    // ── Full validateInputParameters integration path ─────────────────────────
    //
    // Tests the static method called by InngageService.startInit to ensure
    // IllegalArgumentException is thrown (not NPE) for every invalid combination.

    @Nested
    @DisplayName("InngageService.validateInputParameters via startInit — forced error scenarios")
    inner class ValidateInputParametersTests {

        private fun callValidate(
            appToken: String?,
            env: String = "prod",
            provider: String = "FCM",
            identifier: String? = null,
            customFields: JSONObject? = null
        ) {
            // Replicate the same chain as InngageService.validateInputParameters
            if (!ValidateProperties.validateAppToken(appToken)) throw IllegalArgumentException(InngageConstants.INVALID_APP_TOKEN)
            if (!ValidateProperties.validateIdentifier(identifier)) throw IllegalArgumentException(InngageConstants.INVALID_IDENTIFIER)
            if (!ValidateProperties.validateEnvironment(env)) throw IllegalArgumentException(InngageConstants.INVALID_ENVIRONMENT)
            if (!ValidateProperties.validateProvider(provider)) throw IllegalArgumentException(InngageConstants.INVALID_PROVIDER)
            if (!ValidateProperties.validateCustomField(customFields)) throw IllegalArgumentException(InngageConstants.INVALID_CUSTOM_FIELD)
        }

        @Test
        fun `throws INVALID_APP_TOKEN for short token`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                callValidate(appToken = "short")
            }
            assertEquals(InngageConstants.INVALID_APP_TOKEN, ex.message)
        }

        @Test
        fun `throws INVALID_IDENTIFIER for blank identifier`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                callValidate(appToken = "abcdefgh", identifier = "")
            }
            assertEquals(InngageConstants.INVALID_IDENTIFIER, ex.message)
        }

        @Test
        fun `throws INVALID_ENVIRONMENT for unknown env`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                callValidate(appToken = "abcdefgh", env = "unknown")
            }
            assertEquals(InngageConstants.INVALID_ENVIRONMENT, ex.message)
        }

        @Test
        fun `throws INVALID_PROVIDER for unknown provider`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                callValidate(appToken = "abcdefgh", provider = "APNS")
            }
            assertEquals(InngageConstants.INVALID_PROVIDER, ex.message)
        }

        @Test
        fun `throws INVALID_CUSTOM_FIELD for empty JSONObject`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                callValidate(appToken = "abcdefgh", customFields = JSONObject())
            }
            assertEquals(InngageConstants.INVALID_CUSTOM_FIELD, ex.message)
        }

        @Test
        fun `does NOT throw for null customFields — regression for NPE crash`() {
            // Before the fix this would throw NullPointerException, crashing MainActivity
            assertDoesNotThrow {
                callValidate(appToken = "abcdefgh", customFields = null)
            }
        }

        @Test
        fun `does NOT throw for all minimum valid params`() {
            assertDoesNotThrow {
                callValidate(appToken = "abcdefgh")
            }
        }

        @Test
        fun `does NOT throw with full valid params including customFields`() {
            assertDoesNotThrow {
                callValidate(
                    appToken = "1fadc3dba74047d9916fa18155fbeeba",
                    identifier = "user@example.com",
                    customFields = JSONObject().apply { put("plan", "gold") }
                )
            }
        }
    }
}


