package br.com.inngage.sdk.internal.service.subscription.domain

import org.json.JSONObject

/**
 * Domain entity representing a device subscription request.
 */
internal data class SubscriberEntity(
    val appToken: String,
    val identifier: String,
    val registration: String,       // FCM token
    val email: String? = null,
    val phoneNumber: String? = null,
    val customFields: JSONObject? = null,
    val requestGeoLocation: Boolean = false
)
