package br.com.inngage.sdk.internal.core
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.util.Validators
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
@DisplayName("Validators")
class ValidatorsTest {
    @Nested @DisplayName("isValidAppToken") inner class AppTokenValidation {
        @Test fun `returns false for null`()    = assertFalse(Validators.isValidAppToken(null))
        @Test fun `returns false for blank`()   = assertFalse(Validators.isValidAppToken("   "))
        @Test fun `returns false for short`()   = assertFalse(Validators.isValidAppToken("abc"))
        @Test fun `returns true for 8 chars`()  = assertTrue(Validators.isValidAppToken("abcdefgh"))
        @Test fun `returns true for long`()     = assertTrue(Validators.isValidAppToken("my-valid-token-123"))
    }
    @Nested @DisplayName("isValidIdentifier") inner class IdentifierValidation {
        @Test fun `returns true for null`()       = assertTrue(Validators.isValidIdentifier(null))
        @Test fun `returns true for non-blank`()  = assertTrue(Validators.isValidIdentifier("user@ex.com"))
        @Test fun `returns false for blank`()     = assertFalse(Validators.isValidIdentifier(""))
    }
    @Nested @DisplayName("isValidCustomField") inner class CustomFieldValidation {
        @Test fun `returns true for null`()     = assertTrue(Validators.isValidCustomField(null))
        @Test fun `returns true for non-empty`()= assertTrue(Validators.isValidCustomField(JSONObject().apply { put("k","v") }))
        @Test fun `returns false for empty`()   = assertFalse(Validators.isValidCustomField(JSONObject()))
    }
    @Nested @DisplayName("requireValidSubscriptionParams") inner class RequireValidParams {
        @Test fun `throws for invalid appToken`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                Validators.requireValidSubscriptionParams("bad", null, null)
            }
            assertEquals(InngageConfig.INVALID_APP_TOKEN, ex.message)
        }
        @Test fun `throws for blank identifier`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                Validators.requireValidSubscriptionParams("abcdefgh", "", null)
            }
            assertEquals(InngageConfig.INVALID_IDENTIFIER, ex.message)
        }
        @Test fun `throws for empty customFields`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                Validators.requireValidSubscriptionParams("abcdefgh", null, JSONObject())
            }
            assertEquals(InngageConfig.INVALID_CUSTOM_FIELD, ex.message)
        }
        @Test fun `does not throw for minimal valid params`() {
            assertDoesNotThrow { Validators.requireValidSubscriptionParams("abcdefgh", null, null) }
        }
        @Test fun `does not throw with all valid params`() {
            assertDoesNotThrow {
                Validators.requireValidSubscriptionParams("abcdefgh", "user@test.com", JSONObject().put("plan","gold"))
            }
        }
    }
}
