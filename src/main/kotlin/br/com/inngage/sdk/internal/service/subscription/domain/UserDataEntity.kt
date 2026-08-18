package br.com.inngage.sdk.internal.service.subscription.domain

import org.json.JSONObject

/**
 * Domain entity for a subscriber profile update (`addUserData`).
 *
 * Only [appToken] is mandatory; every other field is optional and is omitted
 * from the request body when `null`.
 */
internal data class UserDataEntity(
    val appToken: String,
    val identifier: String? = null,
    val customFields: JSONObject? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    /** Persisted FCM registration token, sent so the backend can match the device. */
    val registration: String? = null
)
