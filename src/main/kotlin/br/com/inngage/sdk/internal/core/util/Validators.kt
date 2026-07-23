package br.com.inngage.sdk.internal.core.util

import br.com.inngage.sdk.internal.core.config.InngageConfig
import org.json.JSONObject

/**
 * Input validators for the subscription workflow.
 * Replaces ValidateProperties.java using idiomatic Kotlin.
 */
internal object Validators {

    /** Returns `true` if [appToken] is non-blank and at least 8 characters. */
    fun isValidAppToken(appToken: String?): Boolean =
        !appToken.isNullOrBlank() && appToken.length >= 8

    /**
     * Returns `true` if [identifier] is null (device UUID will be used as fallback)
     * or non-blank when explicitly provided.
     */
    fun isValidIdentifier(identifier: String?): Boolean =
        identifier == null || identifier.isNotBlank()

    /**
     * Returns `true` if [customFields] is null (no custom fields provided)
     * or is a non-empty JSON object.
     */
    fun isValidCustomField(customFields: JSONObject?): Boolean =
        customFields == null || customFields.length() > 0

    /**
     * Validates subscription input parameters, throwing [IllegalArgumentException]
     * on the first violation.
     */
    fun requireValidSubscriptionParams(
        appToken: String?,
        identifier: String?,
        customFields: JSONObject?
    ) {
        require(isValidAppToken(appToken)) { InngageConfig.INVALID_APP_TOKEN }
        require(isValidIdentifier(identifier)) { InngageConfig.INVALID_IDENTIFIER }
        require(isValidCustomField(customFields)) { InngageConfig.INVALID_CUSTOM_FIELD }
    }
}
